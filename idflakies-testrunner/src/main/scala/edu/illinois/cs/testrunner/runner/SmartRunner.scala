package edu.illinois.cs.testrunner.runner

import java.nio.file.Path
import java.util
import java.util.concurrent.TimeUnit

import edu.illinois.cs.testrunner.configuration.Configuration
import edu.illinois.cs.testrunner.data.framework.TestFramework
import edu.illinois.cs.testrunner.data.results.TestRunResult
import edu.illinois.cs.testrunner.util.{ExecutionInfo, ExecutionInfoBuilder}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
  * Use this when you want to run similar sets of tests multiple times
  * It will automatically time out using information from previous runs, detect flaky tests, and
  * generally perform some basic sanity checks on the results
  *
  * Concurrency safe (if the underlying test suite can run concurrently)
  */
class SmartRunner(testFramework: TestFramework, infoStore: TestInfoStore,
                  cp: String, env: java.util.Map[String, String], outputTo: Path) extends Runner {
    // TODO: Add ability to save/load test info

    override def framework(): TestFramework = testFramework

    override def run(testOrder: Stream[String]): Try[TestRunResult] = {
        val result = super.run(testOrder)

        this.synchronized(infoStore.update(testOrder.toList.asJava, result.toOption))
        val idempotentNumRuns = Configuration.config().getProperty("testplugin.runner.idempotent.num.runs", -1)
        val multiplier = if (idempotentNumRuns < 1) 1 else idempotentNumRuns

        // Make sure that we run exactly the set of tests that we intended to
        result.flatMap(result => {
            val resultSet = result.results().keySet().asScala.toSet
            val testSet = testOrder.toSet
            if ((multiplier * testOrder.length) == result.results().size ){
                Success(result)
            } else {
                Failure(new RuntimeException("Set of executed tests is not equal to test list that should have been executed (" +
                    result.results().size() + " tests executed, " + (multiplier * testOrder.length) +
		    " tests expected). Did you use testplugin.runner.idempotent.num.runs? Missing tests are: " + testSet.diff(resultSet)))
            }
        })
    }

    override def execution(testOrder: Stream[String], executionInfoBuilder: ExecutionInfoBuilder): ExecutionInfo =
        executionInfoBuilder.timeout(timeoutFor(testOrder), TimeUnit.SECONDS).build()

    def timeoutFor(testOrder: Stream[String]): Long = infoStore.getTimeout(testOrder.toList.asJava)

    def info(): TestInfoStore = infoStore

    override def environment(): util.Map[String, String] = env
    override def classpath(): String = cp
    override def outputPath(): Path = outputTo
}

object SmartRunner extends RunnerProvider[SmartRunner] {
    override def withFramework(framework: TestFramework, classpath: String,
                               environment: util.Map[String, String], outputPath: Path): SmartRunner =
        new SmartRunner(framework, new TestInfoStore, classpath, environment, outputPath)
}
