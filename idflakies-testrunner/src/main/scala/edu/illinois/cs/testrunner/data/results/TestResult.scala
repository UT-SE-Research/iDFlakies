package edu.illinois.cs.testrunner.data.results

case class TestResult(name: String, result: Result, time: Double, stackTrace: Array[String])
