#!/bin/bash

if [[ ${1} == "" ]]; then
    echo "Please provide the path to your csv file with the format "URL,SHA,\(optional\)MODULE" on each line. If no MODULE is being provided, make sure to end the line with a comma instead."
    exit
fi



flag=0
scriptDir=$(cd $(dirname $0); pwd)
csvFile=$(cd $(dirname $1); pwd)/$(basename $1)
cd ${scriptDir}

if [[ ! -f ${csvFile} ]]; then
    echo "No such csv exists in the given directory."
    exit
fi

if [[ ! -d testing-script-results ]]; then
    mkdir testing-script-results
fi
cd testing-script-results




#CSV Splicing:
while IFS="," read -r URL SHA MODULE; do

    renamedRepo=${URL}"/"
    readarray -d / -t starr <<< "${renamedRepo}"


    #1. Clone the project

    if [[ ! -d ${starr[4]} ]]; then
        git clone ${URL}.git ${starr[4]}
    fi




    #2. Navigate to proj and checkout SHA

    cd ${starr[4]}
    projectDirectory=$(pwd)
    git checkout -f ${SHA}




    #3. Modify pom file

    git checkout -f .
    cd ${scriptDir}
    cd ..
    cd pom-modify
    bash ./modify-project.sh ${projectDirectory} 1.2.0-SNAPSHOT




    #4. Maven install the proj

    cd ${projectDirectory}
    if [[ ${MODULE} != "" ]]; then
        PL="-pl ${MODULE}"
    else
        PL=""
    fi

    mvn install -DskipTests ${PL} -am

    if [[ $? != 0 ]]; then
        echo "Installation of projects under ${URL} was not successful."
        flag=1
    else

        #5. Run the default test for each

        mvn testrunner:testplugin -Ddetector.detector_type=random-class-method -Ddt.randomize.rounds=5 -Ddt.detector.original_order.all_must_pass=false ${PL}
        if [[ $? != 0 ]]; then
            echo "${URL} idflakies detect not successful."
            flag=1
        fi
    fi
    cd ${scriptDir}/testing-script-results

done < ${csvFile}


exit ${flag}      #test flag functionality on a CI
