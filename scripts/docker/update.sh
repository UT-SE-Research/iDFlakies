#!/bin/bash

SCRIPT_USERNAME="idflakies"
TOOL_REPO="iDFlakies"

# Script that updates Git repositories of tooling code to latest and rebuilds

cd /home/$SCRIPT_USERNAME/testrunner/ && git pull && /home/$SCRIPT_USERNAME/apache-maven/bin/mvn clean install -B
cd /home/$SCRIPT_USERNAME/$TOOL_REPO/ && git pull && /home/$SCRIPT_USERNAME/apache-maven/bin/mvn clean install -B
