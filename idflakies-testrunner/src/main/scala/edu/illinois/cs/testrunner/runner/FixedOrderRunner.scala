package edu.illinois.cs.testrunner.runner

import java.nio.file.Path
import java.util

import edu.illinois.cs.testrunner.data.framework.TestFramework
import edu.illinois.cs.testrunner.util.{ExecutionInfo, ExecutionInfoBuilder}

class FixedOrderRunner(testFramework: TestFramework, cp: String,
                       env: java.util.Map[String, String], outputTo: Path) extends Runner {
    override def execution(testOrder: Stream[String], executionInfoBuilder: ExecutionInfoBuilder): ExecutionInfo =
        executionInfoBuilder.build()

    override def framework(): TestFramework = testFramework
    override def outputPath(): Path = outputTo
    override def classpath(): String = cp
    override def environment(): util.Map[String, String] = env
}

object FixedOrderRunner extends RunnerProvider[FixedOrderRunner] {
    override def withFramework(framework: TestFramework, classpath: String,
                               environment: util.Map[String, String], outputPath: Path): FixedOrderRunner =
        new FixedOrderRunner(framework, classpath, environment, outputPath)
}
