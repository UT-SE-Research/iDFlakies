## Using iDFlakies on a Gradle project

### Install iDFlakies and testrunner 

Go to the testrunner root directory, and run: 

```
mvn clean install
```

Then go to iDFlakies root directory, and run: 

```
mvn clean install
```

### Automatically setting up the build.gradle for iDFlakies

Run the following command to  automatically setup the build.gradle for iDFlakies.

```
bash gradle_modify/modify_gradle.sh path_to_gradle_project
```

### Manually setting up the build.gradle for iDFlakies

We can configure the build.gradle file manually instead. We need to follow the rule listed below to successfully set up the project. 

1. First we need to add the following dependencies and repository to the `buildscript{}` in build.gradle

   ```groovy
   buildscript {
      repositories {
         ...
         mavenLocal()
         ...
      }
   
      dependencies {
         ...
         classpath group: 'edu.illinois.cs',
         name: 'idflakies', 
         version: '1.1.0-SNAPSHOT'
         
         classpath group: 'edu.illinois.cs',
         name: 'testrunner-gradle-plugin', 
         version: '1.2-SNAPSHOT'
         ...
      }
   }
   ```

2. Then, add `apply plugin: 'testrunner'` to the build.gradle file. The location of this code snippet depends on whether build.gradle file has `allprojects{}` and `subprojects{}`. 

   * If there are both `allprojects{}` and `subprojects{}` methods in gradle.build, then the `apply plugin: "testrunner"` command should be added to `allprojects{}` only.
   * If only `allprojects{}`method is in gradle.build, the command is added to  `allprojects{}`
   * If only `subprojects{}` method is in gradle.build, the command is added to both `subprojects{}` and outside of all the methods in the build file
   * If there's no `allprojects{}` and `subprojects{}` in gradle.build, only add the command outside of all the methods

   Here is an example where this code snippet is added outside of all methods.

   ```groovy
   buildscript {
      repositories {
         ...
         mavenLocal()
         ...
      }
   
      dependencies {
         ...
         classpath group: 'edu.illinois.cs',
         name: 'idflakies', 
         version: '1.1.0-SNAPSHOT'
         
         classpath group: 'edu.illinois.cs',
         name: 'testrunner-gradle-plugin', 
         version: '1.2-SNAPSHOT'
         ...
      }
   }
   
   apply plugin: 'testrunner'
   ```

In some projects, such as Caffeine (https://github.com/ben-manes/caffeine), we need to move the `apply plugin: 'testrunner'` code snippet in `allprojects{}` to the end of `subprojects{}`. 

### Running iDFlakies on Gradle project 

After the build.gralde has been configured, one can run iDFlakies on the Gradle project with the following command: 

```
./gradlew testplugin -Dtestplugin.className=edu.illinois.cs.dt.tools.detection.DetectorPlugin -Ddetector.detector_type=random-class-method -Ddt.randomize.rounds=10 -Ddt.detector.original_order.all_must_pass=false
```




