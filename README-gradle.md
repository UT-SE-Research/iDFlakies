## Using iDFlakies on a Gradle project
### Dependency Requirements
1. Junit 4/5 required, due to the original iDFlakies' dependency requirement.
2. Scala (if the project uses Scala) version should be < 2.13, due to the original iDFlakies' dependency requirement and backward incompatibility for some Scala functions.
3. Gradle (if kotlin is used): [5.0, 6.5.1], due to gradle internal bugs: https://github.com/gradle/gradle/issues/14727. Otherwise, there's no requirement on Gradle version. 
### Automatically setting up the build.gradle for iDFlakies
Run the following command to automatically setup the build.gradle for iDFlakies.
```
bash gradle-modify/modify_gradle.sh path_to_gradle_project
```
### Manually setting up the build.gradle for iDFlakies
We can configure the build.gradle file manually instead. We need to follow the rule listed below to successfully set up the project. 
1. First we need to add the following dependencies and repository to the `buildscript{}` in build.gradle 
   ```groovy
   buildscript {
      repositories {
         ...
         mavenCentral()
      }
   
      dependencies {
         ...
         classpath group: 'edu.illinois.cs',
         name: 'idflakies', 
         /* use iDFlakies from Maven Central */
         version: '1.1.0'
         /* use the following version instead if you have built iDFlakies locally and want to use the locally built version*/
         /* version: '1.2.0-SNAPSHOT'*/
         /* For instructions on building iDFlakies locally, please see 'Use locally built version of iDFlakies' section below in this document. */
         
         classpath group: 'edu.illinois.cs',
         name: 'testrunner-gradle-plugin', 
         version: '1.2'
         ...
      }
   }
   ```
2. Then, add `apply plugin: 'testrunner'` to the build.gradle file. The location of this code snippet depends on whether build.gradle file has `allprojects{}` and `subprojects{}`. 
   * If there are both `allprojects{}` and `subprojects{}` methods in gradle.build, then the `apply plugin: "testrunner"` command should be added to the end of `subprojects{}` and the root (outside of all methods in the build file).
     * Example: 
       ```groovy
       buildscript {
          repositories {
             mavenCentral()
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
       
       allprojects {
          repositories {
             mavenCentral()
         }
       }
       
       subprojects {
          apply plugin: 'maven'
          apply plugin: 'signing'
          apply plugin: 'checkstyle'
          /* add apply plugin: "testrunner" to the end of the subproject{} */
          apply plugin: 'testrunner'
       }
       
       /* add apply plugin: "testrunner" to the root */ 
       apply plugin: 'testrunner'
       ```
   * If only `allprojects{}`method is in gradle.build, the command is added to  `allprojects{}` only.
     * Example: 
       ```groovy
       buildscript {
          repositories {
             mavenCentral()
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
       
       allprojects {
          repositories {
             mavenCentral()
          }
          /* add apply plugin: "testrunner" to the end of the allprojects{} only */
          apply plugin: 'testrunner'
       }
       ```
   * If only `subprojects{}` method is in gradle.build, the command is added to both the end of `subprojects{}` and the root. 
     * Example: 
       ```groovy
       buildscript {
          repositories {
             mavenCentral()
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
       
       subprojects {
          apply plugin: 'maven'
          apply plugin: 'signing'
          apply plugin: 'checkstyle'
          /* add apply plugin: "testrunner" to the end of the subproject{} */
          apply plugin: 'testrunner'
       }
       
       /* add apply plugin: "testrunner" to the root */ 
       apply plugin: 'testrunner'
       ```
   * If there's no `allprojects{}` and `subprojects{}` in gradle.build, only add the command to the root.
     * Example:
       ```groovy
       buildscript {
          repositories {
             mavenCentral()
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
       
       apply plugin: 'testrunner'
       ```
3. For each subproject with build.gradle file in its directory, append `apply plugin: 'testrunner'` to the end of the build.gradle in each subproject. 
### Use locally built version of iDFlakies 
Go to iDFlakies root directory, and run the following command to install iDFlakies locally: 
```
mvn clean install
```
Modify build.gradle according to the code snippet below. `MavenLocal()` should be added to make sure the plugin uses locally built iDFlakies. The version of the iDFlakies should be set to '1.2.0-SNAPSHOT'.
```groovy
buildscript {
   repositories {
      ...
      mavenCentral()
      /* use mavenLocal() for locally built iDFlakies*/
      mavenLocal()
   }

   dependencies {
      ...
      classpath group: 'edu.illinois.cs',
      name: 'idflakies', 
      /* use locally built iDFlakies */
      version: '1.2.0-SNAPSHOT'
      
      classpath group: 'edu.illinois.cs',
      name: 'testrunner-gradle-plugin', 
      version: '1.2'
      ...
   }
}
```
### Running iDFlakies on Gradle project 
After the build.gradle has been configured, one can run iDFlakies on the Gradle project with the following command: 
```
./gradlew testplugin -Dtestplugin.className=edu.illinois.cs.dt.tools.detection.DetectorPlugin -Ddetector.detector_type=random-class-method -Ddt.randomize.rounds=10 -Ddt.detector.original_order.all_must_pass=false
```
### Projects in which the tool has been tested
This [spreadsheet](https://docs.google.com/spreadsheets/d/1Jr5zbOZjKFI6a4ntHa0kzbF8-5AgnZEo3G7CUSFWjwA) shows all the projects we have tested, and their compatibility with the Gradle modify tool and the iDFlakies plugin.
