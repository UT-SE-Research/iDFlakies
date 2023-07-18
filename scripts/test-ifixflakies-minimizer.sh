#!/bin/bash

if [[ ${1} == "" ]]; then
    echo "Please provide the path to your csv file with the format "URL,SHA,MODULE,expectedPolluters,expectedCleaners" on each line. Unless only 1 module exists, a module must be provided."
    exit
fi

flag=0    #Global flag designed to represent the status of the overall status of the whole build when running on a CI
scriptDir=$(cd $(dirname $0); pwd)
csvFile=$(cd $(dirname $1); pwd)/$(basename $1)
ARTIFACT_VERSION="2.0.1-SNAPSHOT"
mvnCommand="idflakies:minimize"

cd ${scriptDir}

if [[ ! -f ${csvFile} ]]; then
    echo "No such csv exists in the given directory."
    exit
fi

if [[ ! -d testing-script-results ]]; then
    mkdir testing-script-results
fi
cd testing-script-results


function setOriginalOrder() {        #Copies the original order of tests we want to use for each individual project, as recomputing it on every run leads to inconsistencies
    currDir=$(pwd)
    projName=$1
    currModule=$(echo ${2} | cut -d'|' -f1)
    if [[ ${currModule} != "" ]]; then
        cd ${currModule}
    fi
    mkdir .dtfixingtools
    fileName=${currModule//[/]/@}
    if [[ ${currModule} == "" ]]; then
        cp ${scriptDir}/original-order-files/${projName} .dtfixingtools/original-order
    else
        cp ${scriptDir}/original-order-files/${fileName} .dtfixingtools/original-order
    fi
    cd ${currDir}
}


function setFlakyLists() {
    currDir=$(pwd)
    projName=$1
    currModule=$(echo ${2} | cut -d'|' -f1)
    if [[ ${currModule} != "" ]]; then
        cd ${currModule}
    fi
    mkdir -p .dtfixingtools/detection-results
    fileName=${currModule//[/]/@}
    if [[ ${currModule} == "" ]]; then
        cp ${scriptDir}/flaky-lists-files/${projName} .dtfixingtools/detection-results/flaky-lists.json
    else
        cp ${scriptDir}/flaky-lists-files/${fileName} .dtfixingtools/detection-results/flaky-lists.json
    fi
    cd ${currDir}
}


function checkMinimizeResults() {
    odtest=$1
    expectedPolluters=$2
    expectedCleaners=$3
    projectURL=$4
    currModule=$5

    if [[ ${currModule} != "" ]]; then
        cd ${currModule}
    fi
    cd .dtfixingtools/minimized/
    minimizedfile=$(find -name "${odtest}-*.json")  #assume there is the minimized file corresponding to the OD test
    if [[ ${minimizedfile} == "" ]]; then
        echo "Minimized file for ${odtest} in ${projectURL} does not exist. %%%%%"
        flag=1
        return 1
    fi
    python ${scriptDir}/parse-minimized.py ${minimizedfile} "${expectedPolluters}" "${expectedCleaners}"
    exitcode=$?
    if [[ ${exitcode} == 0 ]]; then
        echo "All expected polluters/cleaners found in ${projectURL}."
        return 0
    elif [[ ${exitcode} == 1 ]]; then
        echo "Did not find the correct polluters in ${projectURL}. %%%%%"
        flag=1
        return 1
    elif [[ ${exitcode} == 2 ]]; then
        echo "Did not find the correct cleaners in ${projectURL}. %%%%%"
        flag=1
        return 1
    else
        echo "Something went wrong in ${projectURL}. %%%%%"
        return 1
    fi
}


#CSV Splicing:
{
    read
    while IFS="," read -r URL SHA MODULE odtest expectedPolluters expectedCleaners; do

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
        bash ${scriptDir}/../pom-modify/modify-project.sh ${projectDirectory} idflakies-maven-plugin ${ARTIFACT_VERSION} #only support iFixFlakies logic in Maven form




        #4. Maven install the proj

        if [[ ${MODULE} != "" ]]; then
            PL="-pl $(echo ${MODULE} | sed 's;|;,;g')"  #some projects rely on multiple dependendencies to be installed, so the | delimiter we used in csv must be converted into a comma for mvn install
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

        mvn install -DskipTests ${MVNOPTIONS} ${PL} -am -B -q
        if [[ $? != 0 ]]; then
            echo "Installation of projects under ${URL} was not successful. %%%%%"
            flag=1
        else

            #5. Using a preset original order and detected flaky tests
            setOriginalOrder ${starr[4]} ${MODULE}
            setFlakyLists ${starr[4]} ${MODULE}
            mvn ${mvnCommand} ${MVNOPTIONS} ${PL} -B
            if [[ $? != 0 ]]; then
                echo "${URL} iFixFlakies minimizer was not successful. %%%%%"
                flag=1
            else
                checkMinimizeResults ${odtest} "${expectedPolluters}" "${expectedCleaners}" ${URL} $(echo ${MODULE} | cut -d'|' -f1)  #some projects, such as incubator-dubbo, have modules that must be installed but don't necessarily produce flaky tests. Therefore, only the first listed module (before the |) is to be checked in this function
            fi
        fi
        cd ${scriptDir}/testing-script-results

    done
} < ${csvFile}



#6. Save all relevant data

if [[ ! -d ARTIFACTS ]]; then
    mkdir ARTIFACTS
fi
for d in $(find -name .dtfixingtools); do
    cp -r --parents ${d} ${scriptDir}/testing-script-results/ARTIFACTS/
done


exit ${flag}
