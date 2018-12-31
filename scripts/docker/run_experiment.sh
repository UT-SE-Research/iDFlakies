#!/bin/bash

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
if [[ "$4" =~ "$/" ]]; then
    script_to_run="$4"
elif [[ -z "$4" ]]; then
    # The default is run_project.sh
    script_to_run="/home/awshi2/dt-fixing-tools/scripts/docker/run_project.sh"
else
    # otherwise, assume it's relative to the docker directory
    script_to_run="/home/awshi2/dt-fixing-tools/scripts/docker/$4"
fi

slug=$1
rounds=$2
timeout=$3

git rev-parse HEAD
date

# Update all tooling
su - awshi2 -c "cd /home/awshi2/dt-fixing-tools/; git pull"

echo "*******************REED************************"
echo "Running update.sh"
date
su - awshi2 -c "/home/awshi2/dt-fixing-tools/scripts/docker/update.sh"

# Copy the test time log, if it is in the old location. Probably can remove this line if all containers are new.

if [[ -e "/home/awshi2/mvn-test-time.log" ]] && [[ ! -e "/home/awshi2/$slug/mvn-test-time.log" ]]; then
    cp "/home/awshi2/mvn-test-time.log" "/home/awshi2/$slug"
fi

# Start the script using the awshi2 user
su - awshi2 -c "$script_to_run ${slug} ${rounds} ${timeout}"

# Change permissions of results and copy outside the Docker image (assume outside mounted under /Scratch)
modifiedslug=$(echo ${slug} | sed 's;/;.;' | tr '[:upper:]' '[:lower:]')
cp -r /home/awshi2/output/ /Scratch/${modifiedslug}_output/
chown -R $(id -u):$(id -g) /Scratch/${modifiedslug}_output/
chmod -R 777 /Scratch/${modifiedslug}_output/
