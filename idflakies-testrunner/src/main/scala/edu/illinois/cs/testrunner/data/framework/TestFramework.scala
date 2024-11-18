package edu.illinois.cs.testrunner.data.framework

import edu.illinois.cs.testrunner.testobjects.GeneralTestClass
import edu.illinois.cs.testrunner.testobjects.JUnitTestCaseClass
import edu.illinois.cs.testrunner.testobjects.JUnitTestClass
import edu.illinois.cs.testrunner.testobjects.JUnit5TestClass
import edu.illinois.cs.testrunner.util.Utility
import edu.illinois.cs.testrunner.util.ProjectWrapper
import org.apache.maven.project.MavenProject
import java.lang.annotation.Annotation
import java.lang.reflect.Modifier
import java.util.List;
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.util.Try

object TestFramework {
    def testFramework(project: ProjectWrapper): Option[TestFramework] = {
        // This method returns either JUnit 4 or JUnit 5 framework.
        // If the project contains both JUnit 4 and JUnit 5 tests, return Option.empty
        // Please use getListOfFrameworks for project mixes JUnit 4 and 5 tests

        // Not sure why we have to cast here, but with this, Scala can't seem to figure out that
        // we should get a list of dependencies

        val containJUnit4Dependency = project.containJunit4
        val containJUnit5Dependency = project.containJunit5

        if (containJUnit4Dependency && containJUnit5Dependency) {
            Option.empty
        } else if (containJUnit4Dependency) {
            Option(JUnit)
        } else if (containJUnit5Dependency) {
            Option(JUnit5)
        } else {
            Option.empty
        }
    }

    def getListOfFrameworks(project: ProjectWrapper): List[TestFramework] = {
        val listBuffer = ListBuffer[TestFramework]()

        if (project.containJunit4) {
            listBuffer.append(JUnit)
        }

        if (project.containJunit5) {
            listBuffer.append(JUnit5)
        }

        listBuffer.toList.asJava
    }

    // Needed for backward compatibility
    def testFramework(project: MavenProject): Option[TestFramework] = {
        // This method returns either JUnit 4 or JUnit 5 framework.
        // If the project contains both JUnit 4 and JUnit 5 tests, return Option.empty
        // Please use getListOfFrameworks for project mixes JUnit 4 and 5 tests

        // Not sure why we have to cast here, but with this, Scala can't seem to figure out that
        // we should get a list of dependencies
        val artifacts = project.getArtifacts.asScala

        val containJUnit4Dependency = artifacts.exists(artifact => artifact.getArtifactId == "junit")
        val containJUnit5Dependency = artifacts.exists(artifact => artifact.getArtifactId == "junit-jupiter") ||
                                      artifacts.exists(artifact => artifact.getArtifactId == "junit-jupiter-api")

        if (containJUnit4Dependency && containJUnit5Dependency) {
            Option.empty
        } else if (containJUnit4Dependency) {
            Option(JUnit)
        } else if (containJUnit5Dependency) {
            Option(JUnit5)
        } else {
            Option.empty
        }
    }

    def getListOfFrameworks(project: MavenProject): List[TestFramework] = {
        val artifacts = project.getArtifacts.asScala
        val listBuffer = ListBuffer[TestFramework]()

        if (artifacts.exists(artifact => artifact.getArtifactId == "junit")) {
            listBuffer.append(JUnit)
        }

        if (artifacts.exists(artifact => artifact.getArtifactId == "junit-jupiter")
                || artifacts.exists(artifact => artifact.getArtifactId == "junit-jupiter-api")) {
            listBuffer.append(JUnit5)
        }

        listBuffer.toList.asJava
    }

    def junitTestFramework(): TestFramework = {
        JUnit
    }

}

trait TestFramework {
    // return corresponding subclass of GeneralTestClass if the class matches with the framework
    def tryGenerateTestClass(loader: ClassLoader, clzName: String): Option[GeneralTestClass]

    def getDelimiter(): String
}

object JUnit extends TestFramework {
    val methodAnnotationStr: String = "org.junit.Test"

    override def tryGenerateTestClass(loader: ClassLoader, clzName: String)
            : Option[GeneralTestClass] = {
        val annotation: Class[_ <: Annotation] =
            loader.loadClass(methodAnnotationStr).asInstanceOf[Class[_ <: Annotation]]

        try {
            val clz = loader.loadClass(clzName)

            if (!Modifier.isAbstract(clz.getModifiers)) {
                val methods = clz.getMethods.toStream

                Try(if (methods.exists(_.getAnnotation(annotation) != null)) {
                    Option(new JUnitTestClass(loader, clz))
                } else if (loader.loadClass("junit.framework.TestCase").isAssignableFrom(clz)) {
                    Option(new JUnitTestCaseClass(loader, clz))
                } else {
                    Option.empty
                }).toOption.flatten
            } else {
                Option.empty
            }
        } catch {
            case e: NoClassDefFoundError => Option.empty
        }
    }

    override def toString(): String = "JUnit"

    // the splitor to split the class name and method name
    // for the full qualified name of JUnit 4 test
    // like com.package.JUnit4TestClass.TestA
    override def getDelimiter(): String = "."
}

object JUnit5 extends TestFramework {
    val methodAnnotationStr: String = "org.junit.jupiter.api.Test"
    val nestedAnnotationStr: String = "org.junit.jupiter.api.Nested"
    val disabledAnnotationStr: String = "org.junit.jupiter.api.Disabled"

    override def tryGenerateTestClass(loader: ClassLoader, clzName: String)
            : Option[GeneralTestClass] = {
        val testAnnotation: Class[_ <: Annotation] =
            loader.loadClass(methodAnnotationStr).asInstanceOf[Class[_ <: Annotation]]

        val disabledAnnotation: Class[_ <: Annotation] =
            loader.loadClass(disabledAnnotationStr).asInstanceOf[Class[_ <: Annotation]]

        try {
            val clz = loader.loadClass(clzName)

            if (clz.getAnnotation(disabledAnnotation) != null) {
                // skip disabled class
                return Option.empty
            }

            if (clz.isMemberClass) {
                val nestedAnnotation: Class[_ <: Annotation] =
                        loader.loadClass(nestedAnnotationStr)
                              .asInstanceOf[Class[_ <: Annotation]]
                if (Modifier.isStatic(clz.getModifiers) ||
                        clz.getAnnotation(nestedAnnotation) == null) {
                    // a nested test class should
                    // (1) be non-static
                    // (2) have @Nested annotation
                    //
                    // Skip unqualified test class
                    return Option.empty
                }
            }

            if (!Modifier.isAbstract(clz.getModifiers)) {
                val methods = Utility.getAllMethods(clz)

                Try(if (methods.exists(_.getAnnotation(testAnnotation) != null)) {
                    Option(new JUnit5TestClass(loader, clz))
                } else {
                    Option.empty
                }).toOption.flatten
            } else {
                Option.empty
            }
        } catch {
            case e: NoClassDefFoundError => Option.empty
        }
    }

    override def toString(): String = "JUnit5"

    // the splitor to split the class name and method name
    // for the full qualified name of JUnit 5 test
    // like com.package.JUnit5TestClass#TestA
    override def getDelimiter(): String = "#"
}