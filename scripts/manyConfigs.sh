#!/bin/bash

# indicate on the ReadMe that to use this script, the user must provide GitHub URL, SHA to run iDFlakies on with a space inbetween.

if [[ ${1} == "" ]] || [[ ${2} == "" ]] || [[ ${3} == "" ]] || [[ ${4} == "" ]]; then
    echo "Please provide the GitHub Project Repository, the SHA, the directory to your pom-modify file on your version of iDFlakies, and the file name of the module you would like to test here."
    exit
fi

											 #RUN TESTRUNNER FOR EACH MVN IDFLAKIES:DETECT

											 #MAKE LIST OF EXPECTED RESULTS FOR EACH CONFIG USING TESTRUNNER

#0. Ask for configs, splice name of project           					 #REMOVE THIS: SAME CONFIGS FOR EACH PROJ : ALL AUTOMATION

echo "If you would like to provide certain configurations, please press 1. Otherwise, if you would like to run numerous defaults, press enter."
read option1
if [[ ${option1} == "1" ]]; then 
	echo "Please provide the following: timeout, # of rounds, detector type, whether original order must pass (true or false), verify rounds, and whether only first failure should count (true or false). Seperate all with a comma."
	read confi
	configurations=${confi}","
	readarray -d , -t configArray <<< "${configurations}"	
fi


#function EXPERIMENT() {

#echo ${1}

#if [ 5 ${2} ${1} ]; then
#	echo "greater or equal"
#else
#	echo "less"
#fi

#}

#EXPERIMENT ${4}
#exit





flag=0
while IFS="," read -r projName testCount1 testCount2 testCount3 testCount4 testCount5 testCount6 testCount7 testCount8 testCount9         #let a -1 for any of these imply an expected test failure of some sort
do




renamedRepo=${1}"/"
readarray -d / -t starr <<< "${renamedRepo}"

#working_dir=$(cd $(dirname $0)/../..; pwd)             #effort to get rid of input detailing pom-modify directory: how do these parameters work


if [[ ${projName} != ${starr[4]} ]]; then
	echo "Error in file structure of testExpectations.csv."
	flag=1
	exit
fi





#cd into directory that contains the bash script itself 

#1. Clone the project.
if [[ ! -d ${starr[4]} ]]; then
	git clone ${1}.git ${starr[4]}
fi     






#2. Navigate to proj and checkout SHA

     
cd ${starr[4]}
projectDirectory=$(pwd)
git checkout ${2}




#3. Modify pom file

git checkout -f .
cd ${3}

#cd ${working_dir}/$TOOL_REPO/scripts/docker/pom-modify
#echo $(pwd)                                                           #effort to get rid of input detailing pom-modify directory


bash ./modify-project.sh ${projectDirectory}







#4. Maven install the proj

PL="-pl ${4}"
cd ${projectDirectory}


mvn install -DskipTests ${PL} -am
if [[ $? != 0 ]]; then
	echo "Project ${starr[4]} was not installed successfully."                      
	flag=1
	exit										#should we remove the exit here?
fi


{ time -p timeout 1h mvn test -fn -B |& tee -a ${projectDirectory}/mvn-test.log ;} 2> ${projectDirectory}/mvn-test-time.log






#4.5 Define Helper functions for running/checking tests



function cleanUp() {

#list=$(find ${projectDirectory} -name ".dtfixingtools")


cd ${4}
cp -r .dtfixingtools ${1}
rm -r -v .dtfixingtools


cd ${projectDirectory}

}



function checkDetType() {
#list1=$(find ${projectDirectory} -name "${1}")


cd ${3}
cd ${1}
cd detection-results
if [[  -d ${2} ]]; then
	echo: "Success: Flaky tests were found using the ${2} detector type."              #check this function later: the folder may still pop up with 0 tests found
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
		echo "Error: ${1} only ran ${numRounds} when it should have ran atleast ${2}."
		cd ${projectDirectory}   
		return 1
	else
		echo "Success: ${1} ran ${numRounds} as expected."
		cd ${projectDirectory}
		return 0		
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
echo "Error: dt.cache.absolute.path not working as intended."
flag=1
cd {projectDirectory}
return 1       #use all these returns as a check with the csv


}




function flakyTestsFound() {            		#COUNT EVERY LINE IN LIST FILE 
numFlakyTests=0
#list3=$(find ${projectDirectory} -name "${1}")


cd ${3}
cd ${1}
cd detection-results
numFlakyTests=$((numFlakyTests + $(wc -l list.txt) ))

cd ${projectDirectory}

if [[ ${2} == ${numFlakyTests} ]]; then
	echo "All expected tests were found."
	return 0
else
	if [ ${expectedTests} -gt ${numFlakyTests} ]; then
		echo "There were $(( ${expectedTests} - ${numFlakyTests} )) less tests found than expected."
		flag=1
		return 1
	else
		echo "There were $(( ${numFlakyTests} - ${expectedTests} )) more tests found than expected."
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
	echo: "Error: ${1} did not use a timeout."
	flag=1
	return 1
fi

}




#5. Run tests


if [[ ${option1} == "1" ]]; then
	userOptions="-Ddetector.detector_type=${configArray[2]} -Ddt.detector.original_order.all_must_pass=${configArray[3]} -Ddt.verify.rounds=${configArray[4]} -Ddt.detector.count.only.first.failure=${configArray[5]}"
	
	mvn idflakies:detect -Ddetector.timeout=${configArray[0]} ${userOptions} ${PL}
	cleanUp timeoutTests

	mvn idflakies:detect -Ddt.randomize.rounds=${configArray[1]} ${userOptions} ${PL}
	cleanUp roundsTests


else
	rounds="-Ddt.randomize.rounds="
	ogOrderPass="-Ddt.detector.original_order.all_must_pass="
	detType="-Ddetector.detector_type="
	countFirstFail="-Ddt.detector.count.only.first.failure="
	verRounds="-Ddt.verify.rounds="
	time="-Ddetector.timeout="
	semantics="-Ddt.detector.roundsemantics.total="
	absPath="-Ddt.cache.absolute.path="
	

		#ERROR RUNNING TESTRUNNER TIMEOUT CONFIG
	mvn idflakies:detect ${time}120 ${ogOrderPass}true ${detType}random-class-method ${PL} &> ${projectDirectory}/test1.log  
	cleanUp test1 ${4}
	checkTimeout test1 120.0
	checkDetType test1 random-class-method ${4} &> ${projectDirectory}/${projName}GeneralLogs.log        #can you repeat this for every function call so that it adds output to the same log file?
	flakyTestsFound test1 testCount1 ${4} &> ${projectDirectory}/${projName}GeneralLogs.log


	mvn idflakies:detect ${rounds}8 ${ogOrderPass}true ${detType}random-class-method ${PL} &> ${projectDirectory}/test2.log
	cleanUp test2 ${4}
	checkDetType test2 random ${4}    					 #this might not work if ogorderpass is true (ex: http request)
	checkNumberRounds test2 8 random-class-method -ge ${4}     							
	flakyTestsFound test2 testCount2 ${4}


	mvn idflakies:detect ${rounds}12 ${ogOrderPass}false ${detType}random-class-method ${PL} &> ${projectDirectory}/test3.log
	cleanUp test3 ${4}
	checkNumberRounds test3 12 random-class-method -ge ${4}					 #as an input, this function requires the name of folder with test results, expected # rounds, detector type used, and eq/ge (roundsemantics)
	flakyTestsFound test3 testCount3 ${4}


	mvn idflakies:detect ${rounds}12 ${ogOrderPass}true ${detType}random-class ${verRounds}2 ${PL} &> ${projectDirectory}/test4.log    #check if output logs shows number of verify rounds itself
	cleanUp test4 ${4}
	checkDetType test4 random-class ${4}      #run every check for every test, check number of tests (all should be in csv file)
	flakyTestsFound test4 testCount4 ${4}


	mvn idflakies:detect ${rounds}8 ${ogOrderPass}true ${detType}random-class-method ${countFirstFail}true ${PL} &> ${projectDirectory}/test5.log
	cleanUp test5 ${4}
	checkNumberRounds test5 8 random-class-method -ge ${4}
	flakyTestsFound test5 testCount5 ${4}


	mvn idflakies:detect ${rounds}3 ${ogOrderPass}true ${detType}reverse ${PL} &> ${projectDirectory}/test6.log
	cleanUp test6 ${4}
	checkNumberRounds test6 1 reverse -ge ${4}
	flakyTestsFound test6 testCount6 ${4}


	mvn idflakies:detect ${time}120 ${ogOrderPass}true ${detType}reverse-class ${PL} &> ${projectDirectory}/test7.log
	cleanUp test7 ${4}
	checkNumberRounds test7 1 reverse-class -ge ${4}
	flakyTestsFound test7 testCount7 ${4}


	mvn idflakies:detect ${rounds}12 ${semantics}true ${PL} &> ${projectDirectory}/test8.log
	cleanUp test8 ${4}
	checkNumberRounds test8 12 random -eq ${4}
	flakyTestsFound test8 testCount8 ${4}



	mvn idflakies:detect ${rounds}12 ${absPath}/tmp/pathTest ${PL}
	checkAbsPath /tmp pathTest ${4}

	

fi

done < testExpectations.csv




exit ${flag}




