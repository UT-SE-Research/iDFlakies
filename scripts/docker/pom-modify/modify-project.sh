#!/bin/bash

if [ "$1" == "" ]; then
    echo "arg1 - the path to the project, where high-level pom.xml is"
    exit
fi

crnt=`pwd`
working_dir=`dirname $0`
project_path=$1

cd ${project_path}
project_path=`pwd`
cd - > /dev/null

cd ${working_dir}

function process_poms
{
    while read pom; do
        ./modify-pom.sh ${pom}
    done
}

#find ${project_path} -name pom.xml | process_poms
javac PomFile.java
find ${project_path} -name pom.xml | grep -v "src/" | java PomFile
rm -f PomFile.class

cd ${crnt}
