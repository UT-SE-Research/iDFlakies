package edu.illinois.cs.testrunner.util

import java.nio.file.Path
import java.util.concurrent.TimeUnit

class ExecutionInfoBuilder(clz: Class[_]) {
    private var classpath: String = System.getProperty("java.class.path")
    private var javaAgent: Option[Path] = Option.empty
    private var javaOpts: List[String] = List()
    private var timeout: Long = -1
    private var timeoutUnit: TimeUnit = TimeUnit.SECONDS
    private var properties: List[(String, String)] = List()
    private var outputPath: Path = null
    private var environment: java.util.Map[String, String] = new java.util.HashMap[String, String]()

    def timeout(timeout: Long, timeoutUnit: TimeUnit): ExecutionInfoBuilder = {
        this.timeout = timeout
        this.timeoutUnit = timeoutUnit
        this
    }

    def classpath(classpath: String): ExecutionInfoBuilder = {
        this.classpath = classpath
        this
    }

    def javaAgent(javaAgent: Path): ExecutionInfoBuilder = {
        this.javaAgent = Option(javaAgent)
        this
    }

    def javaOpts(javaOpts: List[String]): ExecutionInfoBuilder = {
        this.javaOpts = javaOpts
        this
    }

    def addJavaOpts(javaOpts: List[String]): ExecutionInfoBuilder = {
        this.javaOpts = this.javaOpts ++ javaOpts
        this
    }

    def addProperty(property: (String, String)): ExecutionInfoBuilder = {
        properties = property :: properties
        this
    }

    def addProperty(key: String, value: String): ExecutionInfoBuilder = addProperty((key, value))

    def javaOpt(opt: String): ExecutionInfoBuilder = {
        javaOpts = opt :: javaOpts
        this
    }

    def outputPath(outputPath: Path): ExecutionInfoBuilder = {
        this.outputPath = outputPath
        this
    }

    def environment(env: java.util.Map[String, String]): ExecutionInfoBuilder = {
        environment = env
        this
    }

    def build(): ExecutionInfo = {
        ExecutionInfo(classpath, javaAgent, javaOpts, properties, environment, clz, outputPath, timeout, timeoutUnit)
    }
}