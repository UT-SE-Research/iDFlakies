package edu.illinois.cs.testrunner.util

import java.io.{FileOutputStream, PrintStream}
import java.nio.file.Path
import java.util.Objects
import java.util.concurrent.TimeUnit

import edu.illinois.cs.testrunner.configuration.Configuration

case class ExecutionInfo(classpath: String, javaAgent: Option[Path],
                         javaOpts: List[String],
                         properties: List[(String, String)],
                         environmentVariables: java.util.Map[String, String],
                         clz: Class[_], outputPath: Path,
                         timeout: Long, timeoutUnit: TimeUnit) {
    val useTimeouts: Boolean = Configuration.config.getProperty("testplugin.runner.use_timeout", true)

    /**
      * This is an ugly workaround for Scala objects, whose class names end with $ despite the static main method
      * being in the companion class without the $ (
      * https://stackoverflow.com/questions/52208297/scala-proper-way-to-get-name-of-class-for-an-object?noredirect=1#comment91366065_52208297)
      */
    def className: String = {
        val name = clz.getCanonicalName

        if (name.endsWith("$")) {
            name.substring(0, name.length - 1)
        } else {
            name
        }
    }

    def args(args: String*): List[String] =
        List("java", "-cp", classpath) ++
        javaAgent.map(p => List("-javaagent:" ++ p.toAbsolutePath.toString)).getOrElse(List.empty) ++
        javaOpts ++
        properties.map(p => "-D" ++ p._1 ++ "=" ++ p._2) ++
        List(Objects.requireNonNull(className)) ++
        args.toList

    def processBuilder(argVals: String*): ProcessBuilder = {
        val builder = if (outputPath == null) {
            new ProcessBuilder(args(argVals:_*): _*).inheritIO()
        } else {
            new ProcessBuilder(args(argVals:_*): _*)
        }

        // Don't set environment variables to null because it causes issues
        environmentVariables.forEach((k, v) => if (v != null) builder.environment().put(k, v))

        builder
    }

    def run(argVals: String*): Process = {
        val process = processBuilder(argVals:_*).start()

        if (outputPath != null) {
            val ps = new PrintStream(new FileOutputStream(outputPath.toFile))
            new StreamGobbler(process.getInputStream, ps).start()
            new StreamGobbler(process.getErrorStream, ps).start()
        }

        if (timeout > 0 && useTimeouts) {
            process.waitFor(timeout, timeoutUnit)
            //val b = process.waitFor(timeout, timeoutUnit)
            //if (b) process.exitValue() else -1
        } else {
            process.waitFor()
        }
        process
    }
}
