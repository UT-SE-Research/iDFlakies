#!/bin/bash

ARTIFACT_ID="idflakies"
ARTIFACT_VERSION="1.1.0"
CONFIGURATION_CLASS="edu.illinois.cs.dt.tools.detection.DetectorPlugin"

if [[ $1 == "" ]]; then
    echo "arg1 - the path to the project, where high-level build.gradle is"
    echo "arg2 - (Optional) Custom version for the artifact (e.g., 1.0.2, 1.0.3-SNAPSHOT). Default is $ARTIFACT_VERSION"
    exit
fi

if [[ ! $2 == "" ]]; then
    ARTIFACT_VERSION=$2
fi

crnt=`pwd`
working_dir=`dirname $0`
project_path=$1

cd ${project_path}
project_path=`pwd`
cd - > /dev/null

cd ${working_dir}

find ${project_path}  -maxdepth 1 -name build.gradle | python modify_gradle.py ${ARTIFACT_ID} ${ARTIFACT_VERSION} ${CONFIGURATION_CLASS}
find ${project_path} -mindepth 2 -name build.gradle | xargs -I {} sh -c "echo 'apply plugin: '\''testrunner'\''' >> {}"
cd ${crnt}