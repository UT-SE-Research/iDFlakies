#!/bin/bash


if [[ ${1} == "" ]] || [[ ${2} == "" ]]; then
    echo "Please provide the directory to your pom-modify file on your version of iDFlakies and the name of your csv file with the format "URL,SHA,MODULE,tests1-9" on each line."
    exit
fi

						
						
				
						#grep "ogorderdidnotpass"
						#if theres a negative sign, treat the rest of the input as a string, add a new function that greps for that string that affirms intended project failure

											 

											 

#0. File management/defining variables


flag=0
option1=0
scriptDir=$(cd $(dirname $0); pwd)
csvFile=$(cd $(dirname $2); pwd)/$(basename $2)
cd ${scriptDir}

if [[ ! -f ${csvFile} ]]; then
    echo "No such csv exists in the given directory."
    exit
fi

if [[ ! -d MP-script-results ]]; then
    mkdir MP-script-results
fi
cd MP-script-results



while IFS="," read -r URL SHA MODULE testCount1 testCount2 testCount3 testCount4 testCount5 testCount6 testCount7 testCount8 testCount9; do 

x=1
while [ $x -le 9 ]
do
     if[ testCount${i}[0] = "-" ]; then 
          #${testCount${i}}| cut -d'-' -f 2
     fi
     x=$(( $x + 1 ))
done
exit



#function EXPERIMENT() {

#echo ${1}

#if [ 5 ${2} ${1} ]; then
#	echo "greater or equal"
#else
#	echo "less"
#fi

#}

#EXPERIMENT ${MODULE}
#exit









renamedRepo=${URL}"/"
readarray -d / -t starr <<< "${renamedRepo}"






#cd into directory that contains the bash script itself 

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



#3.5 Define Helper functions for running/checking tests



function cleanUp() {

#list=$(find ${projectDirectory} -name ".dtfixingtools")


cd ${2}
cp -r .dtfixingtools ${1}
rm -r .dtfixingtools


cd ${projectDirectory}

}



function checkDetType() {
#list1=$(find ${projectDirectory} -name "${1}")


cd ${3}
cd ${1}
cd detection-results
if [[  -d ${2} ]]; then
	echo "Success: Flaky tests were found using the ${2} detector type."              #check this function later: the folder may still pop up with 0 tests found
	cd ${projectDirectory}
	return 0
fi


echo "No flaky tests were found using the ${2} detector type."
cd ${projectDirectory}
return 1

}



function checkNumberRounds() {

#list2=$(find ${projectDirectory} -name "${1}")


cd ${5}
cd ${1}
cd detection-results
if [[  -d ${3} ]]; then
	cd ${3}
	numRounds=$(ls -l | wc -l)
	if [ ${numRounds} ${4} ${2} ]; then
  		echo "Success: ${1} ran ${numRounds} as expected."
		cd ${projectDirectory}
		return 0
	else
		echo "Error: ${1} only ran ${numRounds} when it should have ran atleast ${2}. %%%%%"
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
	cd ${3}
	if [ -d "detection-results" ]; then
		echo "Success: dt.cache.absolute.path configuration works as intended."
		cd {projectDirectory}
		return 0
	fi	
fi
echo "Error: dt.cache.absolute.path not working as intended. %%%%%"
flag=1
cd {projectDirectory}
return 1


}




function flakyTestsFound() {
numFlakyTests=0
#list3=$(find ${projectDirectory} -name "${1}")


cd ${3}
cd ${1}
cd detection-results
numFlakyTests=$(wc -l list.txt)

cd ${projectDirectory}

if [[ ${2} == ${numFlakyTests} ]]; then
	echo "All expected tests were found."
	return 0
else
	if [ ${expectedTests} -gt ${numFlakyTests} ]; then
		echo "ERROR: There were $(( ${expectedTests} - ${numFlakyTests} )) less tests found than expected. %%%%%"
		flag=1
		return 1
	else
		echo "There were $(( ${numFlakyTests} - ${expectedTests} )) more tests found than expected. %%%%%"
		flag=1
		return 1
	fi
fi

}

function checkTimeout() {

if grep "Using a timeout of ${2}" ${1}.log; then
	echo "Success: ${1} used a timeout of ${2} as expected."
	return 0
else
	echo "Error: ${1} did not use a timeout. %%%%%"
	flag=1
	return 1
fi

}




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
	#SED HTTPS FOR WILDFLY

	mvn testrunner:testplugin ${time}120 ${ogOrderPass}true ${detType}random-class-method ${PL} &> ${projectDirectory}/test1.log       #CHANGE THESE TO TEE? OUTPUT TO CONSOLE ?    # >> means append	
	cleanUp test1 ${MODULE}
	checkTimeout test1 120.0 |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log
	checkDetType test1 random-class-method ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log
	flakyTestsFound test1 $testCount1 ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log


	mvn testrunner:testplugin ${rounds}8 ${ogOrderPass}true ${detType}random-class-method ${PL} &> ${projectDirectory}/test2.log
	cleanUp test2 ${MODULE}
	checkDetType test2 random ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log    					 #this might not work if ogorderpass is true (ex: http request)
	checkNumberRounds test2 8 random-class-method -ge ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log  							
	flakyTestsFound test2 $testCount2 ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log


	mvn testrunner:testplugin ${rounds}12 ${ogOrderPass}false ${detType}random-class-method ${PL} &> ${projectDirectory}/test3.log
	cleanUp test3 ${MODULE}
	checkNumberRounds test3 12 random-class-method -ge ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log					 #as an input, this function requires the name of folder with test results, expected # rounds, detector type used, and eq/ge (roundsemantics)
	flakyTestsFound test3 $testCount3 ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log


	mvn testrunner:testplugin ${rounds}12 ${ogOrderPass}true ${detType}random-class ${verRounds}2 ${PL} &> ${projectDirectory}/test4.log    #check if output logs shows number of verify rounds itself
	cleanUp test4 ${MODULE}
	checkDetType test4 random-class ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log
	flakyTestsFound test4 $testCount4 ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log


	mvn testrunner:testplugin ${rounds}8 ${ogOrderPass}true ${detType}random-class-method ${countFirstFail}true ${PL} &> ${projectDirectory}/test5.log
	cleanUp test5 ${MODULE}
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
	checkAbsPath /tmp pathTest ${MODULE} |& tee -a ${projectDirectory}/${starr[4]}GeneralLogs.log

	

fi
cd ${scriptDir}/MP-script-results
done < ${csvFile}




exit ${flag}
