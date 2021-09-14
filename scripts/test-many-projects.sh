#!/bin/bash

if [[ ${1} == "" ]]; then
    echo "Please provide the path to your csv file with the format "URL,SHA,MODULE,numTestsFound" on each line. Unless only 1 module exists, a module must be provided."
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

	
	#CHANGE ALL ECHO STATEMENTS TO TEE? to save all in 1 "GeneralLogs.txt?"?
	#check wildfly sed
        #function syntax



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
    cd ${scriptDir}
    cd ..
    cd pom-modify
    bash ./modify-project.sh ${projectDirectory} 1.2.0-SNAPSHOT
    cd ${projectDirectory}




    function checkFlakyTests() {

    numFlakyTests=0     
    if [[ ${1} == -1 ]]; then
        echo "EXPECTED PROJECT FAILURE %%%%%"
    else
        if [[ ${2} != "" ]]; then
            cd ${2}
        fi
        cd .dtfixingtools
        cd detection-results
	pwd
        numFlakyTests=$(wc -l list.txt)
        echo $numFlakyTests
	#exit
        cd ${projectDirectory}
	
	
        if [[ ${1} == ${numFlakyTests} ]]; then
	    echo "All expected tests were found in ${3}."
	    return 0
        else
	    if (( $1 > $numFlakyTests )); then
		let "x = $1 - $numFlakyTests"
                echo "There were $x less tests found than expected in ${3}. %%%%%"
                flag=1
                return 1
            else
		let "x = $numFlakyTests - $1"
                echo "There were ${x} more tests found than expected in ${3}. %%%%%"
                flag=1
                return 1
            fi
        fi
    fi
    }




    #4. Maven install the proj
    
    if [[ ${MODULE} != "" ]]; then
        PL="-pl $(echo ${MODULE} | sed 's;|;,;g')"
    else
        PL=""
    fi
    MVNOPTIONS="-Dfindbugs.skip=true -Dmaven.javadoc.skip=true -Denforcer.skip=true -Drat.skip=true -Dmdep.analyze.skip=true -Dgpg.skip -Dmaven.javadoc.skip=true -Ddependency-check.skip=true"
    if [[ $URL == "https://github.com/pholser/junit-quickcheck" ]]; then
        sed -i 's;<artifactId>findbugs-maven-plugin</artifactId>;<artifactId>findbugs-maven-plugin</artifactId><version>3.0.5</version>;' core/pom.xml
        sed -i 's;<artifactId>findbugs-maven-plugin</artifactId>;<artifactId>findbugs-maven-plugin</artifactId><version>3.0.5</version>;' generators/pom.xml
        sed -i 's;<artifactId>findbugs-maven-plugin</artifactId>;<artifactId>findbugs-maven-plugin</artifactId><version>3.0.5</version>;' guava/pom.xml
    fi
    if [[ $URL == "https://github.com/wildfly/wildfly" ]]; then
        sed -i 's;<url>http://repository.jboss.org/nexus/content/groups/public/</url><layout>;https://repository.jboss.org/nexus/content/groups/public/</url><layout>;' pom.xml
    fi
    mvn install -DskipTests ${MVNOPTIONS} ${PL} -am -B
    if [[ $? != 0 ]]; then
        echo "Installation of projects under ${URL} was not successful. %%%%%"      #ADD TEE HERE INTO SOME GLOBAL LOG
        flag=1
    else

        #5. Run the default test for each
        mvn testrunner:testplugin -Ddetector.detector_type=random-class-method -Ddt.randomize.rounds=5 -Ddt.detector.original_order.all_must_pass=false -Ddt.detector.roundsemantics.total=true ${MVNOPTIONS} ${PL} -B
        if [[ $? != 0 ]]; then
            echo "${URL} idflakies detect not successful. %%%%%"
            flag=1
        else
           checkFlakyTests ${numTests} $(echo ${MODULE} | cut -d'|' -f1) ${URL}
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
