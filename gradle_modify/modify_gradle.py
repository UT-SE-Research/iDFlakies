import sys
import fileinput

def modify(path):
    f = open(path, "r")
    f2 = open(path, "r")
    buildscript = False
    repositories = False
    mavenLocal = False
    dependency = False
    result = ""
    cnt = 0
    no_buildscript = False

    dependencies = """
    classpath group: 'edu.illinois.cs',
    name: 'idflakies', 
    version: '1.1.0'
    
    classpath group: 'edu.illinois.cs',
    name: 'testrunner-gradle-plugin', 
    version: '1.2'

    """

    full_buildscript = """
    buildscript {
        repositories {
            mavenLocal()
            mavenCentral()
            jcenter()
        }

        dependencies {
            classpath group: 'edu.illinois.cs',
            name: 'idflakies', 
            version: '1.1.0'
            
            classpath group: 'edu.illinois.cs',
            name: 'testrunner-gradle-plugin', 
            version: '1.2'
        }
    }

    """

    #determine if buildscript{} is in the build file 
    lines = f2.read()
    lines = lines.replace(" ", "")
    if ("buildscript{" not in lines):
        result += full_buildscript
        no_buildscript = True
        cnt = 3

    subprojects = False
    allprojects = False 
    #determine if subprojects{} and allprojects{} are in the build file 
    if ("subprojects{" in lines):
        subprojects = True
    if ("allprojects{" in lines):
        allprojects = True
    
    stack = []
    isInSubproject = False 
    # add line to output by finding the location of repositories{}, buildscript{}
    for line in f.readlines():
        if (cnt < 3):
            if ("buildscript {" in line or "buildscript{" in line):
                buildscript = True
                cnt += 1
            elif (buildscript and "repositories {" in line):
                repositories = True
            if (mavenLocal):
                result += "mavenLocal()\n"
                mavenLocal = False
                repositories = False
                cnt += 1
            if (buildscript and repositories):
                mavenLocal = True
            if (dependency):
                result += dependencies
                dependency = False
                cnt += 1
            if (buildscript and "dependencies {" in line):
                dependency = True
        
        line_no_space = line.replace(" ", "")
        if (subprojects and "subprojects{" in line_no_space.strip()):
            # result += "apply plugin: 'testrunner' \n"
            isInSubproject = True
        if (isInSubproject):
            for char in line_no_space.strip():
                if (char == "{"):
                    stack.append(char)
                elif (char == "}" and stack[-1] == "{"):
                    stack.pop()
                elif (char == "}"):
                    stack.append(char)
            if (len(stack) == 0):
                isInSubproject = False
                result += "apply plugin: 'testrunner' \n"   
        result += line 
        if (not subprojects and allprojects and "allprojects{" in line.strip()):
            result += "apply plugin: 'testrunner' \n"
    
    if (subprojects or not allprojects):
        result += "\napply plugin: 'testrunner'"

    output = open(path, "w")
    output.write(result)
    output.close()

if __name__ == "__main__":
    for path in sys.stdin: 
        path = path.strip()
        print("modify: " + path)
        modify(path)