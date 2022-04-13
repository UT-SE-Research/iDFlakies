#!/bin/bash

if [[ ${1} == "" ]]; then
    echo "Please provide the path to your csv file with the format "URL,SHA,MODULE,numTestsFound" on each line. Unless only 1 module exists, a module must be provided."
    exit
fi

if [[ ${2} != "idflakies-legacy" && ${2} != "idflakies-maven-plugin" ]]; then
    echo "Please provide the plugin we're testing. Options are idflakies-legacy (to run testrunner) and idflakies-maven-plugin."
    exit
fi



flag=0    #Global flag designed to represent the status of the overall status of the whole build when running on a CI
scriptDir=$(cd $(dirname $0); pwd)
csvFile=$(cd $(dirname $1); pwd)/$(basename $1)
ARTIFACT_ID=$2
ARTIFACT_VERSION="2.0.0-SNAPSHOT"
mvnCommand=""
if [[ ${ARTIFACT_ID} == "idflakies-legacy" ]]; then
    mvnCommand="testrunner:testplugin"
elif [[ ${ARTIFACT_ID} == "idflakies-maven-plugin" ]]; then
    mvnCommand="idflakies:detect"
fi

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
        cd .dtfixingtools/detection-results/
        numFlakyTests=$(awk '1' list.txt | wc -l)    #Counts number of lines in list.txt file
        cd ${projectDirectory}

        if [[ ${expectedTests} == ${numFlakyTests} ]]; then
            echo "All expected tests were found in ${projectURL}."
            return 0
        elif [[ $projectURL == "https://github.com/undertow-io/undertow" ]]; then
            if [ $numFlakyTests == 1 ] || [ $numFlakyTests == 2 ]; then
                echo "All expected tests were found in ${projectURL}. NOTE: Non-deterministic project."
                return 0
            fi
        elif [[ $projectURL == "https://github.com/sonatype-nexus-community/nexus-repository-helm" ]]; then
            if [ $numFlakyTests == 0 ] || [ $numFlakyTests == 1 ]; then
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
{
    read
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
        bash ${scriptDir}/../pom-modify/modify-project.sh ${projectDirectory} ${ARTIFACT_ID} ${ARTIFACT_VERSION}




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

            #5. Using a preset original order, run the default test for each
            setOriginalOrder ${starr[4]} ${MODULE}
            mvn ${mvnCommand} -Ddetector.detector_type=random-class-method -Ddt.randomize.rounds=5 -Ddt.detector.original_order.all_must_pass=false -Ddt.detector.roundsemantics.total=true ${MVNOPTIONS} ${PL} -B
            if [[ $? != 0 ]]; then
                echo "${URL} iDFlakies was not successful. %%%%%"
                flag=1
            else
                checkFlakyTests ${numTests} ${URL} $(echo ${MODULE} | cut -d'|' -f1)   #some projects, such as incubator-dubbo, have modules that must be installed but don't necessarily produce flaky tests. Therefore, only the first listed module (before the |) is to be checked in this function
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
