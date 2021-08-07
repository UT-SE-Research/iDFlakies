#!/bin/bash

if [[ ${1} == "" ]]; then           
    echo "Please provide the the name of your csv file with the format "URL,SHA,MODULE" on each line."
    exit
fi




flag=0
scriptDir=$(dirname $0)
cd ${scriptDir}

if [[ ! -f ${1}.csv ]]; then            				
    echo "No such file exists in the current directory."      #should we mandate that the csv should be stored in this "scripts" folder along with this script?
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
    git checkout ${SHA}




    #3. Modify pom file

    git checkout -f .
    cd ${scriptDir}
    cd ..
    cd pom-modify
    bash ./modify-project.sh ${projectDirectory}




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
    cd ${scriptDir}
    cd testing-script-results

done < ${1}.csv


exit ${flag}      #test flag functionality on a CI
