timesToRun="2"
dataDir="data/test_inputs"
dirPath="docker"

for d in $dirPath/$dataDir/*.csv ; do
  filename=$(basename -- "$d")
  dataFile=$dataDir/$filename
  dockerDataFile="$dirPath/$dataFile"
  if [ ! -f "$dockerDataFile" ]; then
    echo "$dockerDataFile does not exist. Skipping..."
    subjNum=$[$subjNum+1]
    continue
  fi

  filenamenoext="${filename%.*}"
  
  cd $dirPath
  nohup bash create_and_run_dockers.sh $dataFile $timesToRun > $filenamenoext.out 2>&1 &
  cd ../
done
