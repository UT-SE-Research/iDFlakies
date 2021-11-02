#!/bin/bash
 

if [[ ${1} == "" ]]; then
    echo "Please provide the name of your csv file with the format "URL,SHA,MODULE,tests1-9" on each line."
    exit
fi


#0. File management/defining all helper functions

flag=0  #Global flag designed to represent the status of the overall status of the whole build when running on a CI
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


function setOriginalOrder() {
    projName=$1
    currModule=$2
    if [[ ${currModule} != "" ]]; then
        cd ${currModule}
    fi
    mkdir .dtfixingtools
    if [[ ${currModule} != "" ]]; then
        cp ${scriptDir}/original-order-files/${projName} .dtfixingtools/
        mv .dtfixingtools/${projName} .dtfixingtools/original-order
    else
        cp ${scriptDir}/original-order-files/${currModule} .dtfixingtools/
        mv .dtfixingtools/${currModule} .dtfixingtools/original-order
    fi
    cd ${projectDirectory}
}



function cleanUp() {
    testNum=$1
    currModule=$2
    if [[ ${currModule} != "" ]]; then
        cd ${currModule}
    fi
    if [[ ! -d ".dtfixingtools" ]]; then
        cd ${projectDirectory}
        return 0
    fi
    cp -r .dtfixingtools ${testNum}
    rm -r .dtfixingtools
    cd ${projectDirectory}
}



function checkDetType() {
    testNum=$1
    detectType=$2
    currModule=$3
    if [[ ${currModule} != "" ]]; then
        cd ${currModule}
    fi
    if [[ ! -d ${testNum} ]]; then
        cd ${projectDirectory}
        return 0
    fi
    cd ${testNum}
    cd detection-results
    if [[  -d ${detectType} ]]; then
        echo "Success: Flaky tests were found using the ${detectType} detector type."
        cd ${projectDirectory}
        return 0
    fi
    echo "No flaky tests were found using the ${detectType} detector type."
    cd ${projectDirectory}
    return 1
}



function checkNumberRounds() {
    testNum=$1
    expectedRounds=$2
    detectType=$3
    expectedBehavior=$4
    currModule=$5
    if [[ ${currModule} != "" ]]; then
        cd ${currModule}
    fi
    if [[ ! -d ${testNum} ]]; then
        cd ${projectDirectory}
        return 0
    fi
    cd ${testNum}
    cd detection-results
    if [[  -d ${detectType} ]]; then
        cd ${detectType}
        numRounds=$(ls | wc -l)
        if [ ${numRounds} ${expectedBehavior} ${expectedRounds} ]; then
            echo "Success: ${testNum} ran ${numRounds} round(s) as expected."
            cd ${projectDirectory}
            return 0
        else
            echo "Error: ${testNum} ran ${numRounds} round(s) when it should have ran ${expectedRounds}. %%%%%"
            flag=1
            cd ${projectDirectory}
            return 1
        fi
    fi
    cd ${projectDirectory}
    echo "No flaky tests were found using ${expectedRounds} rounds."
}



function checkAbsPath() {
    path=$1
    resultsFolder=$2
    projName=$3
    currModule=$4
    cd ${path}
    if [[ -d ${resultsFolder} ]]; then
        cd ${resultsFolder}
        if [[ ${currModule} != "" ]]; then
            cd ${currModule}
        else
            cd ${projName}
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
    testNum=$1
    expectedTests=$2
    currModule=$3
    if [[ $expectedTests == -* ]]; then
        expectedTests=$(echo ${expectedTests} | cut -d'-' -f2)
        if grep "$expectedTests" ${testNum}.log; then
            echo "Confirmed Expected Project Failure in ${testNum}"
            return 0
        else
            echo "ERROR: Unexpected Project Failure in ${testNum} %%%%%"
            flag=1
            return 1
        fi
    else
        if [[ ${currModule} != "" ]]; then
            cd ${currModule}
        fi
        cd ${testNum}
        cd detection-results
        numFlakyTests=$(awk '1' list.txt | wc -l)
        cd ${projectDirectory}

        if [[ ${expectedTests} == ${numFlakyTests} ]]; then
            echo "All expected tests were found in ${testNum}."
            return 0
        else
            if [ $expectedTests -gt $numFlakyTests ]; then
                let "x = $expectedTests - $numFlakyTests"
                echo "There were $x fewer tests found than expected in ${testNum}. %%%%%"
                flag=1
                return 1
            else
                let "x = $numFlakyTests - $expectedTests"
                echo "There were $x more tests found than expected in ${testNum}. %%%%%"
                flag=1
                return 1
           fi
        fi
    fi
}



function checkTimeout() {
    testNum=$1
    expectedTime=$2
    if [[ ! -f ${testNum} ]]; then
        return 0
    fi

    if grep "Using a timeout of ${expectedTime}" ${testNum}.log; then
        echo "Success: ${testNum} used a timeout of ${expectedTime} as expected."
        return 0
    else
        echo "Error: ${testNum} did not use a timeout. %%%%%"
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
    bash ${scriptDir}/../pom-modify/modify-project.sh ${projectDirectory} 1.2.0-SNAPSHOT




    #4 Maven install the proj

    if [[ ${MODULE} != "" ]]; then
        PL="-pl ${MODULE}"
    else
        PL=""
    fi

    if [[ $URL == "https://github.com/wildfly/wildfly" ]]; then
        sed -i 's;<url>http://repository.jboss.org/nexus/content/groups/public/</url>;<url>https://repository.jboss.org/nexus/content/groups/public/</url>;' pom.xml
    fi

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



        setOriginalOrder ${starr[4]} ${MODULE}
        #Test 1: Simply test if the timeout configuration is working. Not testing accuracy here since it has proven to be nondeterministic
        set -o pipefail ; mvn testrunner:testplugin ${time}120 ${ogOrderPass}true ${detType}random-class-method ${PL} |& tee -a ${projectDirectory}/test1.log
        if [[ $? != 0 ]]; then
            echo "${URL} testrunner was not successful. %%%%%"
            flag=1
        fi
        cleanUp test1 ${MODULE}
        checkTimeout test1 120.0
        checkDetType test1 random-class-method ${MODULE}


        setOriginalOrder ${starr[4]} ${MODULE}
        #Test 2: Most standard test: random-class-method with 8 rounds
        set -o pipefail ; mvn testrunner:testplugin ${rounds}8 ${ogOrderPass}true ${detType}random-class-method ${PL} &> ${projectDirectory}/test2.log
        if [[ $? != 0 ]]; then
            echo "${URL} testrunner was not successful. %%%%%"
            flag=1
        fi
        cleanUp test2 ${MODULE}
        checkDetType test2 random-class-method ${MODULE}
        checkNumberRounds test2 8 random-class-method -ge ${MODULE}
        flakyTestsFound test2 "$testCount2" ${MODULE}


        setOriginalOrder ${starr[4]} ${MODULE}
        #Test 3: Test originalOrderPass being set to false
        set -o pipefail ; mvn testrunner:testplugin ${rounds}12 ${ogOrderPass}false ${detType}random-class-method ${PL} &> ${projectDirectory}/test3.log
        if [[ $? != 0 ]]; then
            echo "${URL} testrunner was not successful. %%%%%"
            flag=1
        fi
        cleanUp test3 ${MODULE}
        checkNumberRounds test3 12 random-class-method -ge ${MODULE}
        flakyTestsFound test3 "$testCount3" ${MODULE}


        setOriginalOrder ${starr[4]} ${MODULE}
        #Test 4: Try random-class determinant type as well as the verifyRounds function
        set -o pipefail ; mvn testrunner:testplugin ${rounds}12 ${ogOrderPass}true ${detType}random-class ${verRounds}2 ${PL} &> ${projectDirectory}/test4.log
        if [[ $? != 0 ]]; then
            echo "${URL} testrunner was not successful. %%%%%"
            flag=1
        fi
        cleanUp test4 ${MODULE}
        checkDetType test4 random-class ${MODULE}
        flakyTestsFound test4 "$testCount4" ${MODULE}


        setOriginalOrder ${starr[4]} ${MODULE}
        #Test 5: Try the countFirstFail config
        set -o pipefail ; mvn testrunner:testplugin ${rounds}8 ${ogOrderPass}true ${detType}random-class-method ${countFirstFail}true ${PL} &> ${projectDirectory}/test5.log
        if [[ $? != 0 ]]; then
            echo "${URL} testrunner was not successful. %%%%%"
            flag=1
        fi
        cleanUp test5 ${MODULE}
        checkNumberRounds test5 8 random-class-method -ge ${MODULE}
        flakyTestsFound test5 "$testCount5" ${MODULE}


        setOriginalOrder ${starr[4]} ${MODULE}
        #Test 6: Try the reverse determinant type. Make sure only 1 round of testdeterminty over numRounds
        set -o pipefail ; mvn testrunner:testplugin ${rounds}3 ${ogOrderPass}true ${detType}reverse ${PL} &> ${projectDirectory}/test6.log
        if [[ $? != 0 ]]; then
            echo "${URL} testrunner was not successful. %%%%%"
            flag=1
        fi
        cleanUp test6 ${MODULE}
        checkNumberRounds test6 1 reverse -ge ${MODULE}
        flakyTestsFound test6 "$testCount6" ${MODULE}


        setOriginalOrder ${starr[4]} ${MODULE}
        #Test 7: Try the reverse-class determinant type. Test the priority of configurations by throwing in a timeout as well, which should be ignored
        set -o pipefail ; mvn testrunner:testplugin ${time}120 ${ogOrderPass}true ${detType}reverse-class ${PL} &> ${projectDirectory}/test7.log
        if [[ $? != 0 ]]; then
            echo "${URL} testrunner was not successful. %%%%%"
            flag=1
        fi
        cleanUp test7 ${MODULE}
        checkNumberRounds test7 1 reverse-class -ge ${MODULE}
        flakyTestsFound test7 "$testCount7" ${MODULE}


        setOriginalOrder ${starr[4]} ${MODULE}
        #Test 8: Try setting semantics to be true, where there should be exactly as many rounds as specified. No more, no less
        set -o pipefail ; mvn testrunner:testplugin ${rounds}12 ${semantics}true ${PL} &> ${projectDirectory}/test8.log
        if [[ $? != 0 ]]; then
            echo "${URL} testrunner was not successful. %%%%%"
            flag=1
        fi
        cleanUp test8 ${MODULE}
        checkNumberRounds test8 12 random -eq ${MODULE}
        flakyTestsFound test8 "$testCount8" ${MODULE}


        #Test 9: Make sure we can always output test results to a set directory if necessary
        set -o pipefail ; mvn testrunner:testplugin ${rounds}12 ${absPath}/tmp/pathTest ${PL} &> ${projectDirectory}/test9.log
        if [[ $? != 0 ]]; then
            echo "${URL} testrunner was not successful. %%%%%"
            flag=1
        fi
        checkAbsPath /tmp pathTest ${starr[4]} ${MODULE}

    fi
    cd ${scriptDir}/MC-script-results

done < ${csvFile}




#6. Save all relevant data

if [[ ! -d ARTIFACTS ]]; then
    mkdir ARTIFACTS
fi


for d in test{1,2,3,4,5,6,7,8,9}; do
    for j in $(find -name ${d}.log); do
        cp -r --parents ${j} ${scriptDir}/MC-script-results/ARTIFACTS/
    done
    for k in $(find -name ${d}); do
        cp -r --parents ${k} ${scriptDir}/MC-script-results/ARTIFACTS/
    done
done


exit ${flag}

