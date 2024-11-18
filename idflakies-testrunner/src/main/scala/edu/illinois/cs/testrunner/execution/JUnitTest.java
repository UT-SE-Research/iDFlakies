package edu.illinois.cs.testrunner.execution;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runner.RunWith;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

import java.lang.reflect.Method;

public class JUnitTest {
    public static String methodName(final FrameworkMethod method) {
        return method.getMethod().getDeclaringClass().getCanonicalName() + "." + method.getName();
    }

    private final String fullMethodName;
    private final TestClass clz;
    private final int i;
    private final String junitMethod;

    public JUnitTest(final String fullMethodName, final int i) throws ClassNotFoundException {
        this.fullMethodName = fullMethodName;

        final String className = fullMethodName.substring(0, this.fullMethodName.lastIndexOf("."));

        this.clz = new TestClass(Class.forName(className));
        this.junitMethod = fullMethodName.substring(this.fullMethodName.lastIndexOf(".") + 1);

        this.i = i;
    }

    public JUnitTest(final String className, final String junitMethod) {
        try {
            this.clz = new TestClass(Class.forName(className));
            this.fullMethodName = this.clz.getJavaClass().getCanonicalName() + "." + junitMethod;
            this.junitMethod = junitMethod;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        i = 0;
    }

    public boolean isClassCompatible() {
        RunWith annotations = clz.getJavaClass().getAnnotation(RunWith.class);
        return annotations == null || !annotations.value().getName().equals("org.junit.runners.Parameterized");
    }

    public JUnitTest(Class<?> clzName, String junitMethod) {
        this.clz = new TestClass(clzName);
        this.fullMethodName = clzName.getName() + "." + junitMethod;
        this.junitMethod = junitMethod;
        i = 0;
    }

    public Description description() {
        return Description.createTestDescription(clz.getJavaClass(), junitMethod);
    }

    public String name() {
        return fullMethodName;
    }

    public Class<?> javaClass() {
        return clz.getJavaClass();
    }

    public TestClass testClass() {
        return clz;
    }

    public int index() {
        return i;
    }

    public String methodName() {
        return junitMethod;
    }

    public Request request() {
        return Request.method(clz.getJavaClass(), junitMethod);
    }

    public FrameworkMethod frameworkMethod() {
        for (final FrameworkMethod method : clz.getAnnotatedMethods(Test.class)) {
            if (methodName(method).equals(name())) {
                return method;
            }
        }

        // For JUnit 3, we don't have annotations, so just look through every method in the class.
        for (final Method method : clz.getJavaClass().getMethods()) {
            if (method.getName().equals(junitMethod)) {
                return new FrameworkMethod(method);
            }
        }

        return null;
    }

    public String getTestName() {
        return junitMethod;
    }
}
