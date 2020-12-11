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
    version: '1.1.0-SNAPSHOT'
    
    classpath group: 'edu.illinois.cs',
    name: 'testrunner-gradle-plugin', 
    version: '1.2-SNAPSHOT'

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
            version: '1.1.0-SNAPSHOT'
            
            classpath group: 'edu.illinois.cs',
            name: 'testrunner-gradle-plugin', 
            version: '1.2-SNAPSHOT'
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
        result += line 
        line = line.replace(" ", "")
        if (subprojects and allprojects and "allprojects{" in line.strip()):
            result += "apply plugin: 'testrunner' \n"
        elif (subprojects and not allprojects and "subprojects{" in line.strip()):
            result += "apply plugin: 'testrunner' \n"

    if (not allprojects):
        result += "\napply plugin: 'testrunner'"

    output = open(path, "w")
    output.write(result)
    output.close()


def modifySubproject(path):
    # append apply plugin to the end of the build file 
    f = open(path, "a")
    f.write("\napply plugin: 'testrunner' \n")
    f.close()


if __name__ == "__main__":
    for path in sys.stdin: 
        path = path.strip()
        print("modify: " + path)
        modify(path)


