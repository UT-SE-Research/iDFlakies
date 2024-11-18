package edu.illinois.cs.testrunner.util

import java.lang.reflect.Method;
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

object Utility {
    def timestamp(): String =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss").format(Instant.now().atZone(ZoneId.systemDefault()))

    def getAllMethods(clz: Class[_]): Stream[Method] = {
        var curClz = clz;
        val methods = new ListBuffer[Method]()
        val nameSet = HashSet[String]()
        while (curClz != null) {
            for (m <- curClz.getDeclaredMethods) {
                val paramsStr = m.getParameterTypes.map(c => c.getName).mkString(",")
                val methodName = m.getName ++ "(" ++ paramsStr ++ ")"
                if (!nameSet.contains(methodName)) {
                    // exclude overridden method
                    methods.append(m)
                    nameSet.add(methodName)
                }
            }
            curClz = curClz.getSuperclass
        }
        methods.toStream
    }
}
