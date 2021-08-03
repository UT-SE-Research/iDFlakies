#!/bin/bash

if [[ ${1} == "" ]] || [[ ${2} == "" ]]; then           
	echo "Please provide the directory to your pom-modify file on your version of iDFlakies followed by the name of your csv file with the format "URL,SHA,MODULE" on each line."
	exit
fi
					#Is it better to ask for full path to csv file? would splicing work same way?

if [[ ! -f ${2}.csv ]]; then            				
	echo "No such file exists in the current directory."
	exit
fi

originalDirectory=$(pwd)
flag=0

#SPLICE THROUGH A CSV
while IFS="," read -r URL SHA MODULE
do




renamedRepo=${URL}"/"
readarray -d / -t starr <<< "${renamedRepo}"

#working_dir=$(cd $(dirname $0)/../..; pwd)             #effort to get rid of input detailing pom-modify directory: how do these parameters work




#1. Clone the project.
if [[ ! -d ${starr[4]} ]]; then
	git clone ${URL}.git ${starr[4]}
fi     






#2. Navigate to proj and checkout SHA

     
cd ${starr[4]}
projectDirectory=$(pwd)    #eval commmand inside parenthesis
git checkout ${SHA}






#3. Modify pom file

git checkout -f .
cd ${1}

#cd ${working_dir}/$TOOL_REPO/scripts/docker/pom-modify
#echo $(pwd)                                                           #follow up to pom modify input


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
	                                                                         #should there be an else here?



#5. Run the default test for each 

	mvn idflakies:detect -Ddetector.detector_type=random-class-method -Ddt.randomize.rounds=5 -Ddt.detector.original_order.all_must_pass=false ${PL}
	if [[ $? != 0 ]]; then
		echo "${URL} idflakies detect not successful."
		flag=1
	fi
fi

cd ${originalDirectory} 



done < ${2}.csv


exit ${flag}      #test flag functionality on a CI

	



