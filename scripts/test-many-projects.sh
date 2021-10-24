#!/bin/bash

if [[ ${1} == "" ]]; then
    echo "Please provide the path to your csv file with the format "URL,SHA,MODULE,numTestsFound" on each line. Unless only 1 module exists, a module must be provided."
    exit
fi



flag=0    #Global flag designed to represent the status of the overall status of the whole build when running on a CI
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




function checkFlakyTests() {
    expectedTests=$1
    projectURL=$2
    currModule=$3
    if [[ ${expectedTests} == -1 ]]; then
        echo "EXPECTED PROJECT FAILURE %%%%%"
        return 0
    else
        if [[ ${currModule} != "" ]]; then
            cd ${currModule}
        fi
        cd .dtfixingtools
        cd detection-results
        numFlakyTests=$(awk '1' list.txt | wc -l)
        cd ${projectDirectory}

        if [[ ${expectedTests} == ${numFlakyTests} ]]; then
            echo "All expected tests were found in ${projectURL}."
            return 0
        elif [[ $projectURL == "https://github.com/undertow-io/undertow" ]]; then
            if [ $numFlakyTests == 1 ] || [ $numFlakyTests == 2 ]; then
                echo "All expected tests were found in ${projectURL}. NOTE: Non-deterministic project."
                return 0
            fi
        elif [ $projectURL == "https://github.com/apache/incubator-dubbo" ] && [ $currModule == "dubbo-cluster" ]; then
            if [ $numFlakyTests == 3 ] || [ $numFlakyTests == 4 ]; then
                echo "All expected tests were found in ${projectURL}. NOTE: Non-deterministic project."
                return 0
            fi
        else
            if [ $expectedTests -gt $numFlakyTests ]; then
                let "x = $expectedTests - $numFlakyTests"
                echo "There were $x fewer tests found than expected in ${projectURL}. %%%%%"
                flag=1
                return 1
            else
                let "x = $numFlakyTests - $expectedTests"
                echo "There were $x more tests found than expected in ${projectURL}. %%%%%"
                flag=1
                return 1
            fi
        fi
    fi
}




#CSV Splicing:
while IFS="," read -r URL SHA MODULE numTests; do

    renamedRepo=${URL}"/"
    readarray -d / -t starr <<< "${renamedRepo}"

    echo "DATE: "
    date
    echo "BEGINNING OF $URL"

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
    bash ${scriptDir}/../pom-modify/modify-project.sh ${projectDirectory} 1.2.0-SNAPSHOT





    #4. Maven install the proj

    if [[ ${MODULE} != "" ]]; then
        PL="-pl $(echo ${MODULE} | sed 's;|;,;g')"  #some projects rely on multiple dependendencies to be installed, so the | delimited we used in csv must be converted into a comma for mvn install
    else
        PL=""
    fi
    MVNOPTIONS="-Dfindbugs.skip=true -Dmaven.javadoc.skip=true -Denforcer.skip=true -Drat.skip=true -Dmdep.analyze.skip=true -Dgpg.skip -Dmaven.javadoc.skip=true -Ddependency-check.skip=true"

    if [[ $URL == "https://github.com/pholser/junit-quickcheck" ]]; then    #junit-quickcheck references outdated plugins in current pom files, so they must be updated accordingly to mvn install
        sed -i 's;<artifactId>findbugs-maven-plugin</artifactId>;<artifactId>findbugs-maven-plugin</artifactId><version>3.0.5</version>;' core/pom.xml
        sed -i 's;<artifactId>findbugs-maven-plugin</artifactId>;<artifactId>findbugs-maven-plugin</artifactId><version>3.0.5</version>;' generators/pom.xml
        sed -i 's;<artifactId>findbugs-maven-plugin</artifactId>;<artifactId>findbugs-maven-plugin</artifactId><version>3.0.5</version>;' guava/pom.xml
    fi
    if [[ $URL == "https://github.com/wildfly/wildfly" ]]; then             #running wildfly on CI causes errors when jboss repo is referenced via http rather than https in pom file
        sed -i 's;<url>http://repository.jboss.org/nexus/content/groups/public/</url>;<url>https://repository.jboss.org/nexus/content/groups/public/</url>;' pom.xml
    fi

    mvn install -DskipTests ${MVNOPTIONS} ${PL} -am -B
    if [[ $? != 0 ]]; then
        echo "Installation of projects under ${URL} was not successful. %%%%%"
        flag=1
    else

        #5. Run the default test for each
        mvn testrunner:testplugin -Ddetector.detector_type=random-class-method -Ddt.randomize.rounds=5 -Ddt.detector.original_order.all_must_pass=false -Ddt.detector.roundsemantics.total=true ${MVNOPTIONS} ${PL} -B
        if [[ $? != 0 ]]; then
            echo "${URL} idflakies detect not successful. %%%%%"
            flag=1
        else
            checkFlakyTests ${numTests} ${URL} $(echo ${MODULE} | cut -d'|' -f1)   #some projects, such as incubator-dubbo, have modules that must be installed but don't necessarily produce flaky tests. Therefore, only the first listed module (before the |) is to be checked in this function
        fi
    fi
    cd ${scriptDir}/testing-script-results

done < ${csvFile}



#6. Save all relevant data

if [[ ! -d ARTIFACTS ]]; then
    mkdir ARTIFACTS
fi
for d in $(find -name .dtfixingtools); do
    cp -r --parents ${d} ${scriptDir}/testing-script-results/ARTIFACTS/
done


exit ${flag}
