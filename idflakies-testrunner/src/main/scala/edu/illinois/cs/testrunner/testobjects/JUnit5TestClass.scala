package edu.illinois.cs.testrunner.testobjects

import edu.illinois.cs.testrunner.util.Utility
import java.lang.annotation.Annotation
import java.lang.reflect.Method

import scala.collection.JavaConverters._

class JUnit5TestClass(loader: ClassLoader, clz: Class[_]) extends GeneralTestClass {

    def fullyQualifiedName(method: Method): String = {
        val paramsStr = method.getParameterTypes.map(c => c.getName).mkString(",")
        clz.getName ++ "#" ++ method.getName ++ "(" ++ paramsStr ++ ")"
    }

    override def tests(): Stream[String] = {
        val junit5TestAnnotation: Class[_ <: Annotation] =
            loader.loadClass("org.junit.jupiter.api.Test").asInstanceOf[Class[_ <: Annotation]]
        Utility.getAllMethods(clz)
               .filter(_.getAnnotation(junit5TestAnnotation) != null)
               .map(fullyQualifiedName)
    }
}
