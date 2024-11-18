package edu.illinois.cs.testrunner.testobjects

import java.lang.annotation.Annotation

import org.junit.runners.model.{FrameworkMethod, TestClass}

import scala.collection.JavaConverters._

class JUnitTestClass(loader: ClassLoader, clz: Class[_]) extends GeneralTestClass {
    private def junitTestClass: TestClass = new TestClass(clz)

    def fullyQualifiedName(fm: FrameworkMethod): String =
        clz.getName ++ "." ++ fm.getName

    override def tests(): Stream[String] = {
        val testAnnotation: Class[_ <: Annotation] =
            loader.loadClass("org.junit.Test").asInstanceOf[Class[_ <: Annotation]]

        junitTestClass.getAnnotatedMethods(testAnnotation).asScala.toStream
            .map(fullyQualifiedName)
    }
}
