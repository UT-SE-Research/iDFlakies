#!/bin/bash

docker build -t $1 - < $2_Dockerfile
