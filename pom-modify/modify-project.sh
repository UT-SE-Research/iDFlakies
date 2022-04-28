#!/bin/bash

ARTIFACT_ID="idflakies-maven-plugin"
ARTIFACT_VERSION="2.0.0-SNAPSHOT"

if [[ $1 == "" ]]; then
    echo "arg1 - the path to the project, where high-level pom.xml is"
    echo "arg2 - (Optional) Preferred ID for the artifact. Current options are idflakies-legacy and idflakies-maven-plugin. Default is $ARTIFACT_ID"
    echo "arg3 - (Optional) Custom version for the artifact (e.g., 1.1.0, 1.2.0-SNAPSHOT). Default is $ARTIFACT_VERSION"
    exit
fi


if [[ ! $2 == "" ]]; then
    ARTIFACT_ID=$2
fi

if [[ ! $3 == "" ]]; then
    ARTIFACT_VERSION=$3
fi

crnt=`pwd`
working_dir=`dirname $0`
project_path=$1

cd ${project_path}
project_path=`pwd`
cd - > /dev/null

cd ${working_dir}

javac PomFile.java
find ${project_path} -name pom.xml | grep -v "src/" | java PomFile ${ARTIFACT_ID} ${ARTIFACT_VERSION}
rm -f PomFile.class

cd ${crnt}
