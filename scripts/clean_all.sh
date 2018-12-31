subjNum="1"
maxSubjNum="3"
dirPath="docker/"
dataDir="data/optimized_new_split"

while [ $subjNum -le $maxSubjNum ]
do
  # dirPath="docker_$subjNum/"
  # if [ ! -d "$dirPath" ]; then
  #   echo "$dirPath does not exist. Skipping..."
  #   continue
  # fi

  dataFile="$dataDir/sorted_subject_data_$subjNum.csv"
  dockerDataFile="$dirPath/$dataFile"
  if [ ! -f "$dockerDataFile" ]; then
      echo "$dockerDataFile does not exist. Skipping..."
      subjNum=$[$subjNum+1]
      continue
  fi
  
  cd $dirPath
  rm -rf *_output/
  rm -rf *.out
  ./cleanimages.sh $dataFile
  cd ../
  
  subjNum=$[$subjNum+1]
done
