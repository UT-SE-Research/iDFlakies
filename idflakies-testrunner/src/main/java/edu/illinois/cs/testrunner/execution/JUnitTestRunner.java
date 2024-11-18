package edu.illinois.cs.testrunner.execution;

import edu.illinois.cs.testrunner.configuration.Configuration;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.builders.AnnotatedBuilder;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.Fail;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.rules.MethodRule;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class JUnitTestRunner extends BlockJUnit4ClassRunner {
    public static String fullName(final Description description) {
        return description.getClassName() + "." + description.getMethodName();
    }

    public static List<Method> getAllMethods(final Class<?> clz) {
        final List<Method> methods = new ArrayList<>(Arrays.asList(clz.getDeclaredMethods()));

        if (clz.getSuperclass() != null) {
            methods.addAll(getAllMethods(clz.getSuperclass()));
        }

        return methods;
    }

    private final Set<String> ranBeforeClassSet = new HashSet<>();
    private final List<JUnitTest> tests = new ArrayList<>();

    private RunNotifier notifier = null;

    public JUnitTestRunner(final List<JUnitTest> tests) throws InitializationError {
        // Necessary so we can use all of the JUnit runner code written for BlockJUnit4ClassRunner.
        super(DummyClass.class);
        this.tests.addAll(tests);
    }

    @Override
    protected List<FrameworkMethod> getChildren() {
        final List<FrameworkMethod> children = new ArrayList<>();

        for (final JUnitTest test : tests) {
            children.add(test.frameworkMethod());
        }

        return children;
    }

    @Override
    public void run(RunNotifier notifier) {
        this.notifier = notifier;

        final String testListenerClassName = Configuration.config().getProperty("testrunner.testlistener_class", "");

        if (!"".equals(testListenerClassName)) {
            try {
                final Class<?> clz = Class.forName(testListenerClassName);
                final Object o = clz.newInstance();
                notifier.addListener((RunListener) o);
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException ignored) {}
        }

        final EachTestNotifier testNotifier = new EachTestNotifier(notifier, this.getDescription());

        for (final JUnitTest test : tests) {
            if (test.javaClass().getAnnotation(Ignore.class) == null) {
                try {
                    Configuration.config().properties().setProperty("testrunner.current_test", fullName(test.description()));
                    runChild(test, notifier);
                } catch (AssumptionViolatedException e) {
                    testNotifier.fireTestIgnored();
                } catch (StoppedByUserException e) {
                    throw e;
                } catch (Throwable e) {
                    testNotifier.addFailure(e);
                }
            }
        }
    }

    private boolean ranBeforeClass(final FrameworkMethod method) {
        return ranBeforeClassSet.contains(method.getMethod().getDeclaringClass().getCanonicalName());
    }

    private boolean isLastMethod(final FrameworkMethod method) {
        final String fullTestName = JUnitTest.methodName(method);

        boolean found = false;
        for (final JUnitTest test : tests) {
            if (test.name().equals(fullTestName)) {
                found = true;
            } else if (test.javaClass().equals(method.getMethod().getDeclaringClass()) && found) {
                // If we've found the method passed in, and we find another method in the same class
                // we aren't done with the class yet.
                return false;
            }
        }

        return true;
    }

    private Statement beforeClasses(final JUnitTest test) {
        final List<FrameworkMethod> befores = test.testClass().getAnnotatedMethods(BeforeClass.class);
        return new RunBefores(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                // Intentionally empty.
            }
        }, befores, null);
    }

    private Statement afterClasses(final JUnitTest test) {
        final List<FrameworkMethod> afters = test.testClass().getAnnotatedMethods(AfterClass.class);
        return new RunAfters(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                // Intentionally empty.
            }
        }, afters, null);
    }

    private Statement withBefores(JUnitTest test, Object target,
                                  Statement statement) {
        List<FrameworkMethod> befores = new ArrayList<>(test.testClass().getAnnotatedMethods(Before.class));

        for (final Method method : getAllMethods(test.javaClass())) {
            if (method.getName().toLowerCase().equals("setup") &&
                    !Modifier.isStatic(method.getModifiers()) &&
                    method.getParameterTypes().length == 0) {
                method.setAccessible(true);

                final FrameworkMethod fMethod = new FrameworkMethod(method);

                if (!befores.contains(fMethod)) {
                    befores.add(fMethod);
                }
            }
        }

        return befores.isEmpty() ? statement : new RunBefores(statement, befores, target);
    }

    private Statement withAfters(JUnitTest test, Object target,
                                 Statement statement) {
        List<FrameworkMethod> afters = new ArrayList<>(test.testClass().getAnnotatedMethods(After.class));

        for (final Method method : getAllMethods(test.javaClass())) {
            if (method.getName().toLowerCase().equals("teardown") &&
                    !Modifier.isStatic(method.getModifiers()) &&
                    method.getParameterTypes().length == 0) {
                method.setAccessible(true);

                final FrameworkMethod fMethod = new FrameworkMethod(method);

                if (!afters.contains(fMethod)) {
                    afters.add(fMethod);
                }
            }
        }

        return afters.isEmpty() ? statement : new RunAfters(statement, afters, target);
    }

    private Statement withRules(JUnitTest test, Object target, Statement statement) {
        List<TestRule> testRules = getTestRules(test, target);
        Statement result = statement;
        result = withMethodRules(test, testRules, target, result);
        result = withTestRules(test, testRules, result);

        return result;
    }

    private Statement withMethodRules(JUnitTest test, List<TestRule> testRules,
                                      Object target, Statement result) {
        try {
            for (MethodRule each : getMethodRules(test, target)) {
                if (!testRules.contains(each)) {
                    result = each.apply(result, test.frameworkMethod(), target);
                }
            }
        } catch (Throwable ignored) {}
        return result;
    }

    private List<MethodRule> getMethodRules(final JUnitTest test, final Object target) {
        return rules(test, target);
    }

    /**
     * @param target the test case instance
     * @return a list of MethodRules that should be applied when executing this
     *         test
     */
    private List<MethodRule> rules(final JUnitTest test, final Object target) {
        final List<MethodRule> rules = test.testClass().getAnnotatedMethodValues(target, Rule.class, MethodRule.class);

        rules.addAll(test.testClass().getAnnotatedFieldValues(target, Rule.class, MethodRule.class));

        return rules;
    }

    private Statement withTestRules(JUnitTest test, List<TestRule> testRules,
                                    Statement statement) {
        try {
            return testRules.isEmpty() ? statement :
                    new RunRules(statement, testRules, test.description());
        } catch (Throwable ignored) {}

        return statement;
    }

    private List<TestRule> getTestRules(JUnitTest test, Object target) {
        try {
            List<TestRule> result = test.testClass().getAnnotatedMethodValues(target, Rule.class, TestRule.class);

            result.addAll(test.testClass().getAnnotatedFieldValues(target, Rule.class, TestRule.class));

            // Add a timeout rule to every test if enabled
            final int universalTimeout = Configuration.config().getProperty("testplugin.runner.timeout.universal", -1);
            if (universalTimeout > 0) {
                result.add(Timeout.builder()
                        .withLookingForStuckThread(true)
                        .withTimeout(universalTimeout, TimeUnit.SECONDS)
                        .build());
            }

            return result;
        } catch (Throwable ignored) {}

        return new ArrayList<>();
    }

    private Statement methodBlock(final JUnitTest test) {
        Object testObj;
        try {
            testObj = (new ReflectiveCallable() {
                protected Object runReflectiveCall() throws Throwable {
                    // for JUnit 3.8.2
                    try {
                        return test.javaClass().getConstructor(String.class).newInstance(test.getTestName());
                    } catch (NoSuchMethodException e) {
                        return test.testClass().getOnlyConstructor().newInstance();
                    }
                }
            }).run();
        } catch (Throwable e) {
            return new Fail(e);
        }

        final FrameworkMethod method = test.frameworkMethod();

        Statement statement = methodInvoker(method, testObj);
        statement = possiblyExpectingExceptions(method, testObj, statement);
        statement = withPotentialTimeout(method, testObj, statement);
        statement = withBefores(test, testObj, statement);
        statement = withAfters(test, testObj, statement);
        statement = withRules(test, testObj, statement);
        return statement;
    }

    private void runChild(JUnitTest test, RunNotifier notifier) {
        final FrameworkMethod method = test.frameworkMethod();

        if (method == null) {
            System.out.println("Test method not found: " + test.name());
            throw new TestNotFoundException(test);
        }

        final EachTestNotifier eachNotifier = new EachTestNotifier(notifier, test.description());

        if (method.getAnnotation(Ignore.class) != null) {
            eachNotifier.fireTestIgnored();
        } else {
            try {
                Statement statement = usingClassRunner(test);

                if (statement == null) {
                    try {
                        eachNotifier.fireTestStarted();

                        statement = methodBlock(test);

                        if (!ranBeforeClass(method)) {
                            ranBeforeClassSet.add(method.getMethod().getDeclaringClass().getCanonicalName());
                            // Run this way so it doesn't show up in the stack trace for the test and possibly cause the tools
                            // to incorrectly label it as dependent
                            beforeClasses(test).evaluate();
                        }

                        statement.evaluate();

                        if (isLastMethod(method)) {
                            // Run this way so it doesn't show up in the stack trace for the test and possibly cause the tools
                            // to incorrectly label it as dependent
                            afterClasses(test).evaluate();
                        }
                    } catch (AssumptionViolatedException e) {
                        eachNotifier.addFailedAssumption(e);
                    } catch (Throwable e) {
                        eachNotifier.addFailure(e);
                    } finally {
                        eachNotifier.fireTestFinished();
                    }
                } else {
                    // Here we assume that the statement returned will handle all beforeclass/afterclass and related methods.
                    // it show also take care of notification (via the notifier field in this class).
                    statement.evaluate();
                }
            }
            // We catch these exceptions because the use of Statement.evaluate forces it,
            // but these should be handled already (either inside the try or by the statement's execution).
            catch (AssumptionViolatedException e) {
                eachNotifier.addFailedAssumption(e);
            } catch (Throwable e) {
                eachNotifier.addFailure(e);
            }
        }
    }

    private Statement usingClassRunner(final JUnitTest test) {
        // TODO: Maybe use JUnit's request/runner mechanisms to accomplish this rather than what we currently have.
        // Make sure that it fixes the issue mentioned below regarding the NoTestsRemainException and PowerMockRunner.
        try {
            final Runner runner = new AnnotatedBuilder(null).runnerForClass(test.javaClass());

            if (runner != null) {
                return new Statement() {
                    @Override
                    public void evaluate() throws Throwable {
                        try {
                            Filter.matchMethodDescription(test.description()).apply(runner);
                        } catch (NoTestsRemainException ignored) {}

                        // Here we ignore NoTestsRemainException in favor of checking it ourselves.
                        // This is a workaround specifically for PowerMockRunner, which can chunk
                        // test methods and incorrectly report that no tests remain, but may also affect other
                        // custom runners.
                        if (runner.testCount() > 0) {
                            runner.run(notifier);
                        }
                    }
                };
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected Description describeChild(FrameworkMethod method) {
        if (method == null) {
            return Description.EMPTY;
        }
        return Description.createTestDescription(method.getMethod().getDeclaringClass(), method.getName());
    }

    public List<JUnitTest> tests() {
        return tests;
    }
}
