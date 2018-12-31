#!/bin/bash

if [[ $1 == "" ]] || [[ $2 == "" ]]; then
    echo "arg1 - GitHub SLUG"
    echo "arg2 - SHA"
    exit
fi

slug=$1
sha=$2

# Assume that detectorbase:latest Docker image has already been built and installed (see baseDockerfile)

# Starting with template Dockerfile, create custom Dockerfile for this one project
modifiedslug=$(echo ${slug} | sed 's;/;.;' | tr '[:upper:]' '[:lower:]')
customdocker=${modifiedslug}_Dockerfile
cp augDockerfile ${customdocker}
sed -i "s;<IMAGE>;detector-${modifiedslug};" ${customdocker}
sed -i "s;<MODIFIEDSLUG>;${modifiedslug};" ${customdocker}
sed -i "s;<SLUG>;${slug};g" ${customdocker}
sed -i "s;<SHA>;${sha};" ${customdocker}
