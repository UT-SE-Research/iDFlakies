#!/bin/bash

SCRIPT_USERNAME="idflakies"
TOOL_REPO="iDFlakies"

# This script is the entry point script that is run inside of the Docker image
# for running the experiment for a single project

if [[ $1 == "" ]] || [[ $2 == "" ]] || [[ $3 == "" ]]; then
    echo "arg1 - GitHub SLUG"
    echo "arg2 - Number of rounds"
    echo "arg3 - Timeout in seconds"
    echo "arg4 - Script to run (Optional)"
    exit
fi

# If it's an absolute path, just use it
if [[ "$4" =~ ^/ ]]; then
    script_to_run="$4"
elif [[ -z "$4" ]]; then
    # The default is run_project.sh
    script_to_run="/home/$SCRIPT_USERNAME/$TOOL_REPO/scripts/docker/run_project.sh"
else
    # otherwise, assume it's relative to the docker directory
    script_to_run="/home/$SCRIPT_USERNAME/$TOOL_REPO/scripts/docker/$4"
fi

slug=$1
rounds=$2
timeout=$3

git rev-parse HEAD
date

modifiedslug=$(echo ${slug} | sed 's;/;.;' | tr '[:upper:]' '[:lower:]')

mkdir -p /Scratch/all-output/${modifiedslug}_output/
chown "$SCRIPT_USERNAME" /Scratch/all-output/${modifiedslug}_output/
chmod 755 /Scratch/all-output/${modifiedslug}_output/

# Update all tooling
su - "$SCRIPT_USERNAME" -c "cd /home/$SCRIPT_USERNAME/$TOOL_REPO/; git pull"

echo "*******************IDFLAKIES DEBUG************************"
echo "Running update.sh"
date
su - "$SCRIPT_USERNAME" -c "/home/$SCRIPT_USERNAME/$TOOL_REPO/scripts/docker/update.sh"

# Copy the test time log, if it is in the old location. Probably can remove this line if all containers are new.

if [[ -e "/home/$SCRIPT_USERNAME/mvn-test-time.log" ]] && [[ ! -e "/home/$SCRIPT_USERNAME/$slug/mvn-test-time.log" ]]; then
    cp "/home/$SCRIPT_USERNAME/mvn-test-time.log" "/home/$SCRIPT_USERNAME/$slug"
fi

# Start the script using the $SCRIPT_USERNAME user
su - "$SCRIPT_USERNAME" -c "$script_to_run ${slug} ${rounds} ${timeout}"

# Change permissions of results and copy outside the Docker image (assume outside mounted under /Scratch)
mkdir -p "/Scratch/all-output/${modifiedslug}_output/misc-output/"
cp -r "/home/$SCRIPT_USERNAME/output/" "/Scratch/all-output/${modifiedslug}_output/misc-output/"
chown -R $(id -u):$(id -g) /Scratch/all-output/${modifiedslug}_output/
chmod -R 777 /Scratch/all-output/${modifiedslug}_output/

chown $(id -u):$(id -g) /Scratch/all-output/
chmod 777 /Scratch/all-output/

set +x
cd /Scratch/all-output/${modifiedslug}_output
OLD_IFS=$IFS
IFS=$'\n';
rm -f all_flaky_tests_list.csv 
for round_file in $(find -type f -name "round*.json"); do
    for test_name in $(python -m json.tool $round_file | grep \"name\": | sort -u | cut -d':' -f2 | cut -d'"' -f2 ); do
	echo $test_name,$round_file >> all_flaky_tests_list.csv;
    done
done
sort -u -o all_flaky_tests_list.csv all_flaky_tests_list.csv
cd -
