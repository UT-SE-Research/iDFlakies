#!/bin/bash

if [[ $1 == "" ]]; then
    echo "arg1 - Path to CSV file with project,sha"
    exit
fi

projfile=$1

for line in $(cat ${projfile}); do
    # Create name of image for each project, then delete it
    slug=$(echo ${line} | cut -d',' -f1 | rev | cut -d'/' -f1-2 | rev)
    modifiedslug=$(echo ${slug} | sed 's;/;.;' | tr '[:upper:]' '[:lower:]')
    image=detector-${modifiedslug}:latest
    docker rmi ${image} 2> /dev/null

    rm -rf ${modifiedslug}_Dockerfile
    rm -rf ${modifiedslug}_output/
done

# Delete the base tooling
docker rmi toolingdetectorbase:latest 2> /dev/null


rm -rf run_output.log
rm -rf nohup.out
