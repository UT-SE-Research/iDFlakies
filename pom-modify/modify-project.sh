#!/bin/bash

ARTIFACT_GROUPID="edu.illinois.cs"
ARTIFACT_ID="idflakies-legacy"
ARTIFACT_VERSION="1.1.0"
#flag=0    isnt this unnecessary since the plugin u want to add is contained within the artifact_id ?

if [[ $1 == "" ]]; then   #add arg3 and arg4 accordingly
    echo "arg1 - the path to the project, where high-level pom.xml is"
    echo "arg2 - (Optional) Custom groupID for the artifact (e.g., edu.illinois.cs). Default is $ARTIFACT_GROUPID"
    echo "arg3 - (Optional) Preferred ID for the artifact. Current options are idflakies-legacy and idflakies-maven-plugin. Default is $ARTIFACT_ID"
    echo "arg4 - (Optional) Custom version for the artifact (e.g., 1.1.0, 1.2.0-SNAPSHOT). Default is $ARTIFACT_VERSION"
    exit
fi

if [[ ! $2 == "" ]]; then
    ARTIFACT_GROUPID=$2
fi

if [[ ! $3 == "" ]]; then
    ARTIFACT_ID=$3
fi

if [[ ! $4 == "" ]]; then
    ARTIFACT_VERSION=$4
fi

crnt=`pwd`
working_dir=`dirname $0`
project_path=$1

cd ${project_path}
project_path=`pwd`
cd - > /dev/null

cd ${working_dir}

javac PomFile.java
find ${project_path} -name pom.xml | grep -v "src/" | java PomFile ${ARTIFACT_GROUPID} ${ARTIFACT_ID} ${ARTIFACT_VERSION}
rm -f PomFile.class

cd ${crnt}
