package edu.illinois.cs.testrunner.testobjects

import java.lang.reflect.Method

class JUnitTestCaseClass(loader : ClassLoader, clz: Class[_]) extends GeneralTestClass {

    def fullyQualifiedName(m: Method): String =
        clz.getCanonicalName ++ "." ++ m.getName

    override def tests(): Stream[String] = {
        clz.getDeclaredMethods().toStream
            .filter(_.getName().startsWith("test")).map(fullyQualifiedName)
    }
}
