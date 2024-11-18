package edu.illinois.cs.testrunner.execution

import java.nio.file.{Files, Path, Paths}
import java.util.stream.Collectors

import edu.illinois.cs.testrunner.configuration.Configuration

import scala.util.Try


// This needs to be here so that Scala can find the class so we can execute from the command line
class Executor

object Executor {
    def main(args: Array[String]): Unit = {
        System.exit(args match {
            case Array(testRunId, testFramework, testsFile, configPath, outputPath) =>
                run(testRunId, testFramework, Paths.get(testsFile), Paths.get(configPath), Paths.get(outputPath))
            case _ => 1
        })
    }

    def run(testRunId: String, testFramework: String, testsFile: Path, configPath: Path, outputPath: Path): Int = {
        Configuration.reloadConfig(configPath)

        val tests = Files.lines(testsFile).collect(Collectors.toList())

        val result = Try(testFramework match {
            case "JUnit" =>
                JUnitTestExecutor.runOrder(testRunId, tests, true, false)
                                 .writeTo(outputPath.toAbsolutePath.toString)
            case "JUnit5" =>
                JUnit5TestExecutor.runTestsSeparately(testRunId, tests)
                                  .writeTo(outputPath.toAbsolutePath.toString)
            case _ => throw new Exception("Unknown test framework: " ++ testFramework)
        })

        if (result.isFailure) {
            result.failed.get.printStackTrace(System.err)
            2 // use 2 so that it's different than in the case statement in main() so we can tell
              // what kind of error we got
        } else {
            0
        }
    }
}
