#!/bin/bash


if [[ ${1} == "" ]]; then
    echo "Please provide the name of your csv file with the format "URL,SHA,MODULE,tests1-9" on each line."
    exit
fi


#0. File management/defining all helper functions

flag=0
scriptDir=$(cd $(dirname $0); pwd)
csvFile=$(cd $(dirname $1); pwd)/$(basename $1)
cd ${scriptDir}

if [[ ! -f ${csvFile} ]]; then
    echo "No such csv exists in the given directory."
    exit
fi

if [[ ! -d MC-script-results ]]; then
    mkdir MC-script-results
fi
cd MC-script-results



function cleanUp() {
    if [[ ${2} != "" ]]; then
        cd ${2}
    fi
    if [[ ! -d ".dtfixingtools" ]]; then
        cd ${projectDirectory}
        return 0
    fi
    cp -r .dtfixingtools ${1}
    rm -r .dtfixingtools
    cd ${projectDirectory}
}



function checkDetType() {
    if [[ ${3} != "" ]]; then
        cd ${3}
    fi
    if [[ ! -d ${1} ]]; then
        cd ${projectDirectory}
        return 0
    fi
    cd ${1}
    cd detection-results
    if [[  -d ${2} ]]; then
        echo "Success: Flaky tests were found using the ${2} detector type."
        cd ${projectDirectory}
        return 0
    fi
    echo "No flaky tests were found using the ${2} detector type."
    cd ${projectDirectory}
    return 1
}



function checkNumberRounds() {
    if [[ ${5} != "" ]]; then
        cd ${5}
    fi
    if [[ ! -d ${1} ]]; then
        cd ${projectDirectory}
        return 0
    fi
    cd ${1}
    cd detection-results
    if [[  -d ${3} ]]; then
        cd ${3}
        numRounds=$(ls | wc -l)
        if [ ${numRounds} ${4} ${2} ]; then
            echo "Success: ${1} ran ${numRounds} round(s) as expected."
            cd ${projectDirectory}
            return 0
        else
            echo "Error: ${1} ran ${numRounds} rounds when it should have ran ${2}. %%%%%"
            flag=1
            cd ${projectDirectory}
            return 1
        fi
    fi
    cd ${projectDirectory}
    echo "No flaky tests were found using ${2} rounds."
}



function checkAbsPath() {
    cd ${1}
    if [[ -d ${2} ]]; then
        cd ${2}
        if [[ ${4} != "" ]]; then
            cd ${4}
        else
            cd ${3}
        fi
        if [ -d "detection-results" ]; then
            echo "Success: dt.cache.absolute.path configuration works as intended."
            return 0
        fi
    fi
    echo "Error: dt.cache.absolute.path not working as intended. %%%%%"
    flag=1
    return 1
}



function flakyTestsFound() {
    expectedTests=$2
    if [[ $expectedTests == -* ]]; then
        expectedTests=$(echo ${expectedTests} | cut -d'-' -f2)
        if grep "$expectedTests" ${1}.log; then
            echo "Confirmed Expected Project Failure in ${1}"
            return 0
        else
            echo "ERROR: Unexpected Project Failure in ${1} %%%%%"
            flag=1
            return 1
        fi
    else
        if [[ ${3} != "" ]]; then
            cd ${3}
        fi
        cd ${1}
        cd detection-results
        numFlakyTests=$(awk '1' list.txt | wc -l)
        cd ${projectDirectory}

        if [[ ${expectedTests} == ${numFlakyTests} ]]; then
            echo "All expected tests were found in ${1}."
            return 0
        else
            if [ $expectedTests -gt $numFlakyTests ]; then
                let "x = $expectedTests - $numFlakyTests"
                echo "There were $x fewer tests found than expected in ${1}. %%%%%"
                flag=1
                return 1
            else
                let "x = $numFlakyTests - $expectedTests"
                echo "There were $x more tests found than expected in ${1}. %%%%%"
                flag=1
                return 1
           fi
        fi
    fi
}



function checkTimeout() {
    if [[ ! -f ${1} ]]; then
        return 0
    fi

    if grep "Using a timeout of ${2}" ${1}.log; then
        echo "Success: ${1} used a timeout of ${2} as expected."
        return 0
    else
        echo "Error: ${1} did not use a timeout. %%%%%"
        flag=1
        return 1
    fi
}




while IFS="," read -r URL SHA MODULE testCount1 testCount2 testCount3 testCount4 testCount5 testCount6 testCount7 testCount8 testCount9; do


    renamedRepo=${URL}"/"
    readarray -d / -t starr <<< "${renamedRepo}"


    #1. Clone the project.

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
    bash ./modify-project.sh ${projectDirectory} 1.2.0-SNAPSHOT
    cd ${projectDirectory}




    #4 Maven install the proj

    if [[ ${MODULE} != "" ]]; then
        PL="-pl ${MODULE}"
    else
        PL=""
    fi
    cd ${projectDirectory}


    mvn install -DskipTests ${PL} -am
    if [[ $? != 0 ]]; then
        echo "Project ${starr[4]} was not installed successfully.  %%%%%"
        flag=1
    else

        #5. Run tests

        echo "Start of logs for ${projectDirectory}"

        { time -p timeout 1h mvn test -fn -B ${PL} |& tee -a ${projectDirectory}/${MODULE}/mvn-test.log ;} 2> ${projectDirectory}/${MODULE}/mvn-test-time.log
        cp ${projectDirectory}/${MODULE}/mvn-test.log ${projectDirectory}/mvn-test.log
        cp ${projectDirectory}/${MODULE}/mvn-test-time.log ${projectDirectory}/mvn-test-time.log


        rounds="-Ddt.randomize.rounds="
        ogOrderPass="-Ddt.detector.original_order.all_must_pass="
        detType="-Ddetector.detector_type="
        countFirstFail="-Ddt.detector.count.only.first.failure="
        verRounds="-Ddt.verify.rounds="
        time="-Ddetector.timeout="
        semantics="-Ddt.detector.roundsemantics.total="
        absPath="-Ddt.cache.absolute.path="
        if [[ $URL == "https://github.com/wildfly/wildfly" ]]; then
            sed -i 's;<url>http://repository.jboss.org/nexus/content/groups/public/</url>;<url>https://repository.jboss.org/nexus/content/groups/public/</url>;' pom.xml
        fi


        mvn testrunner:testplugin ${time}120 ${ogOrderPass}true ${detType}random-class-method ${PL} |& tee -a ${projectDirectory}/test1.log 
        cleanUp test1 ${MODULE}
        checkTimeout test1 120.0 |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log
        checkDetType test1 random-class-method ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log
        flakyTestsFound test1 $testCount1 ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log


        mvn testrunner:testplugin ${rounds}8 ${ogOrderPass}true ${detType}random-class-method ${PL} &> ${projectDirectory}/test2.log
        cleanUp test2 ${MODULE}
        checkDetType test2 random ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log
        checkNumberRounds test2 8 random-class-method -ge ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log
        flakyTestsFound test2 $testCount2 ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log


        mvn testrunner:testplugin ${rounds}12 ${ogOrderPass}false ${detType}random-class-method ${PL} &> ${projectDirectory}/test3.log
        cleanUp test3 ${MODULE}
        checkNumberRounds test3 12 random-class-method -ge ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log                    #as an input, this function requires the name of folder with test results, expected # rounds, detector type used, and eq/ge (roundsemantics)
        flakyTestsFound test3 $testCount3 ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log


        mvn testrunner:testplugin ${rounds}12 ${ogOrderPass}true ${detType}random-class ${verRounds}2 ${PL} &> ${projectDirectory}/test4.log
        cleanUp test4 ${MODULE}
        checkDetType test4 random-class ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log
        flakyTestsFound test4 $testCount4 ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log


        mvn testrunner:testplugin ${rounds}8 ${ogOrderPass}true ${detType}random-class-method ${countFirstFail}true ${PL} &> ${projectDirectory}/test5.log
        cleanUp test5 ${MODULE} #pass in  an extra parameter, the name of the log file, and move it into the new file we create in cleanup func
        checkNumberRounds test5 8 random-class-method -ge ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log
        flakyTestsFound test5 $testCount5 ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log


        mvn testrunner:testplugin ${rounds}3 ${ogOrderPass}true ${detType}reverse ${PL} &> ${projectDirectory}/test6.log
        cleanUp test6 ${MODULE}
        checkNumberRounds test6 1 reverse -ge ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log  
        flakyTestsFound test6 $testCount6 ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log


        mvn testrunner:testplugin ${time}120 ${ogOrderPass}true ${detType}reverse-class ${PL} &> ${projectDirectory}/test7.log
        cleanUp test7 ${MODULE}
        checkNumberRounds test7 1 reverse-class -ge ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log
        flakyTestsFound test7 $testCount7 ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log


        mvn testrunner:testplugin ${rounds}12 ${semantics}true ${PL} &> ${projectDirectory}/test8.log
        cleanUp test8 ${MODULE}
        checkNumberRounds test8 12 random -eq ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log
        flakyTestsFound test8 $testCount8 ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log


        mvn testrunner:testplugin ${rounds}12 ${absPath}/tmp/pathTest ${PL} &> ${projectDirectory}/test9.log
        checkAbsPath /tmp pathTest ${starr[4]} ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log

    fi
    cd ${scriptDir}/MC-script-results

done < ${csvFile}




#6. Save all relevant data

if [[ ! -d ARTIFACTS ]]; then
    mkdir ARTIFACTS
fi
for d in test{1,2,3,4,5,6,7,8,9}; do    
    if [[ ! -d ${d} ]]; then
        continue
    fi
    cp -r --parents $(find -name ${d}) ${scriptDir}/MC-script-results/ARTIFACTS/
done



exit ${flag}

