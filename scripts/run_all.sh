subjNum="1"
maxSubjNum="3"
timesToRun="100"
dataDir="data/optimized_new_split"
dirPath="docker"

while [ $subjNum -le $maxSubjNum ]
do
  # dirPath="docker_$subjNum/"
  # if [ ! -d "$dirPath" ]; then
  #   echo "$dirPath does not exist. Creating now..."
  #   cp -r docker/$dirPath
  # fi

  dataFile="$dataDir/sorted_subject_data_$subjNum.csv"
  dockerDataFile="$dirPath/$dataFile"
  if [ ! -f "$dockerDataFile" ]; then
    echo "$dockerDataFile does not exist. Skipping..."
    subjNum=$[$subjNum+1]
    continue
  fi

  cd $dirPath
  nohup bash create_and_run_dockers.sh $dataFile $timesToRun > $subjNum.out 2>&1 &
  cd ../
  
  subjNum=$[$subjNum+1]
done
