package edu.illinois.cs.testrunner.execution;

import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.data.results.TestResult;
import edu.illinois.cs.testrunner.data.results.TestResultFactory;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.Failure;
import org.junit.runners.model.InitializationError;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class JUnitTestExecutor {
    // If IDEMPOTENT_NUM_RUNS > 1 and CONSEC_IDEMPOTENT is false, then the run will be C1.t1, C1.t2, C1.t1, C1.t2
    // If IDEMPOTENT_NUM_RUNS > 1 and CONSEC_IDEMPOTENT is true, then the run will be C1.t1, C1.t1, C1.t2, C1.t2
    public static final int IDEMPOTENT_NUM_RUNS =
        Configuration.config().getProperty("testplugin.runner.idempotent.num.runs", -1);
    public static final boolean CONSEC_IDEMPOTENT  =
        Configuration.config().getProperty("testplugin.runner.consec.idempotent", false);

    public static TestRunResult runOrder(final String testRunId,
                                         final List<String> testList,
                                         final boolean skipMissing,
                                         final boolean runSeparately)
            throws ClassNotFoundException{
        final JUnitTestExecutor executor;
        if (skipMissing) {
            executor = JUnitTestExecutor.skipMissing(testList);
        } else {
            executor = JUnitTestExecutor.testOrder(testList);
        }

        if (runSeparately) {
            return executor.executeSeparately(testRunId);
        } else {
            return executor.executeWithJUnit4Runner(testRunId);
        }
    }

    private final List<JUnitTest> testOrder = new ArrayList<>();
    private final Map<String, JUnitTest> testMap = new HashMap<>();
    private final Set<TestResult> knownResults = new HashSet<>();

    public JUnitTestExecutor(final JUnitTest test) {
        this(Collections.singletonList(test));
    }

    public JUnitTestExecutor(final List<JUnitTest> tests) {
        this(tests, new HashSet<>());
    }

    public JUnitTestExecutor(final List<JUnitTest> tests, final Set<TestResult> knownResults) {
        for (final JUnitTest test : tests) {
            if (test.isClassCompatible()) {
                testOrder.add(test);
                testMap.put(test.name(), test);
            } else {
                System.out.println("  Detected incompatible test case (" + test.name() + ")");
                knownResults.add(TestResultFactory.missing(test.name()));
            }
        }

        this.knownResults.addAll(knownResults);
    }

    //package.class.method
    public static JUnitTestExecutor singleton(final String fullMethodName) throws ClassNotFoundException {
        return JUnitTestExecutor.testOrder(Collections.singletonList(fullMethodName));
    }

    public static JUnitTestExecutor skipMissing(final List<String> testOrder) {
        final List<JUnitTest> tests = new ArrayList<>();
        final Set<TestResult> knownResults = new HashSet<>();

        for (int i = 0; i < testOrder.size(); i++) {
            final String fullMethodName = testOrder.get(i);
            try {
                final JUnitTest test = new JUnitTest(fullMethodName, i);

                tests.add(test);
            } catch (ClassNotFoundException e) {
                knownResults.add(TestResultFactory.missing(fullMethodName));
                System.out.println("  Skipped missing test : " + fullMethodName);
            } catch (ExceptionInInitializerError e) {
                knownResults.add(TestResultFactory.failOrError(e, 0.0, fullMethodName));
                System.out.println("Test failed in initialization: " + fullMethodName);
            } catch (Throwable e) {
                System.out.println("[ERROR] Encountered exception while initializing JUnitTest for '" + fullMethodName + "'");
                throw e;
            }
        }

        return new JUnitTestExecutor(tests, knownResults);
    }

    public static JUnitTestExecutor testOrder(final List<String> testOrder) throws ClassNotFoundException {
        final List<JUnitTest> tests = new ArrayList<>();

        for (int i = 0; i < testOrder.size(); i++) {
            final String fullMethodName = testOrder.get(i);

            try {
                tests.add(new JUnitTest(fullMethodName, i));
            } catch (Throwable e) {
                System.out.println("[ERROR] Encountered exception while initializing JUnitTest for '" + fullMethodName + "'");
                throw e;
            }
        }

        return new JUnitTestExecutor(tests);
    }

    private boolean notContains(final Map<String, Double> testRuntimes,
                                final Map<String, JUnitTest> passingTests,
                                final String fullTestName) {
        if (!testRuntimes.containsKey(fullTestName)) {
            System.out.println("[ERROR]: No running time measured for test name '" + fullTestName + "'");
            return true;
        }

        if (!passingTests.containsKey(fullTestName)) {
            System.out.println("[ERROR]: No JUnitTest found for test name '" + fullTestName + "'");
            return true;
        }

        return false;
    }

    private Set<TestResult> results(final Result re, final List<JUnitTest> tests, final TestListener listener) {
        final Set<TestResult> results = new HashSet<>(knownResults);

        final Map<String, JUnitTest> passingTests = new HashMap<>();

        // To keep track of tests that have multiple errors, so we don't report the result multiple
        // times.
        final Set<String> handledTests = new HashSet<>();

        // So we can keep track of tests that didn't get run (i.e., skipped).
        final Set<String> allTests = new HashSet<>();
        for (final JUnitTest test : tests) {
            allTests.add(test.name());
        }

        // We can only mark a test as passing if it actually ran.
        for (final String testName : listener.runtimes().keySet()) {
            if (testMap.containsKey(testName)) {
                passingTests.put(testName, testMap.get(testName));
            } else {
                System.out.println("[ERROR] Unexpected test executed: " + testName);
            }
        }

        for (final Failure failure : re.getFailures()) {
            // If the description is a test (that is, a single test), then handle it normally.
            // Otherwise, the ENTIRE class failed during initialization or some such thing.
            if (failure.getDescription().isTest()) {
                final String fullTestName = JUnitTestRunner.fullName(failure.getDescription());

                if (handledTests.contains(fullTestName) || notContains(listener.runtimes(), passingTests, fullTestName)) {
                    continue;
                }

                results.add(TestResultFactory.failOrError(failure.getException(), listener.runtimes().get(fullTestName), fullTestName));

                passingTests.remove(fullTestName);
                allTests.remove(fullTestName);
                handledTests.add(fullTestName);
            } else {
                // The ENTIRE class failed, so we need to mark every test from this class as failing.
                final String className = failure.getDescription().getClassName();

                // Make a set first so that we can modify the original hash map
                for (final JUnitTest test : testOrder) {
                    if (test.javaClass().getCanonicalName().equals(className) && !handledTests.contains(test.name())) {
                        results.add(TestResultFactory.failOrError(failure.getException(), 0.0, test.name()));

                        if (passingTests.containsKey(test.name())) {
                            passingTests.remove(test.name());
                            allTests.remove(test.name());
                            handledTests.add(test.name());
                        }
                    }
                }
            }
        }

        for (final String fullMethodName : listener.ignored()) {
            if (!handledTests.contains(fullMethodName)) {
                results.add(TestResultFactory.ignored(fullMethodName));
                allTests.remove(fullMethodName);
                handledTests.add(fullMethodName);
            }
        }

        for (final String fullMethodName : passingTests.keySet()) {
            if (handledTests.contains(fullMethodName) || notContains(listener.runtimes(), passingTests, fullMethodName)) {
                continue;
            }

            results.add(TestResultFactory.passing(listener.runtimes().get(fullMethodName), fullMethodName));
            allTests.remove(fullMethodName);
        }

        for (final String fullMethodName : allTests) {
            if (!handledTests.contains(fullMethodName)) {
                results.add(TestResultFactory.missing(fullMethodName));
            }
        }

        return results;
    }

    private void executeHelper(final String testRunId, final List<JUnitTest> tests, final TestRunResult finalResult) {
        // This will happen only if no tests are selected by the filter.
        // In this case, we will throw an exception with a name that makes sense.
        if (tests.isEmpty()) {
            throw new EmptyTestListException(testOrder);
        }

        final JUnitCore core = new JUnitCore();

        final TestListener listener = new TestListener();
        core.addListener(listener);
        Result re;

        // Keep track of what test classes and test descriptions, for use in constructing Request
        final List<Class> classes = new ArrayList<>();
        final List<Description> descs = new ArrayList<>();
        for (JUnitTest test : tests) {
            if (!classes.contains(test.javaClass())) {
                classes.add(test.javaClass());
            }
            descs.add(test.description());
        }
        int idempotentRuns = Math.max(1, IDEMPOTENT_NUM_RUNS);
        int randomizeRuns = Configuration.config().getProperty("dt.randomize.rounds", -1);
        if (idempotentRuns > 1 && randomizeRuns > 1) {
            // iDFlakies encounters a NPE when verifying non-idempotent OD tests
            throw new RuntimeException("dt.randomize.rounds is set to " + randomizeRuns +
                                       " and testplugin.runner.idempotent.num.runs is set to " +
                                       idempotentRuns + ". Either configuration must be <= 1.");
        }
        for (int i = 0; i < idempotentRuns; i++) {
            // Construct the request that filters out only tests we need and sorts in specified order
            re = core.run(Request.classes(classes.toArray(new Class[0])).
                          // Filter to only include passed in tests
                          filterWith(new Filter() {
                                  @Override
                                      public boolean shouldRun(Description description) {
                                      if (description.isTest()) {
                                          return descs.contains(description);
                                      }

                                      for (Description each : description.getChildren()) {
                                          if (shouldRun(each)) {
                                              return true;
                                          }
                                      }
                                      return false;
                                  }

                                  @Override
                                      public String describe() {
                                      return "Filter by list";
                                  }
                              }).
                          // Sort to run based on passed in tests
                          sortWith(new Comparator<Description>() {
                                  @Override
                                  public int compare(Description d1, Description d2) {
                                      return descs.indexOf(d1) - descs.indexOf(d2);
                                  }
                              }));

            final Set<TestResult> results = results(re, tests, listener);

            for (final JUnitTest test : tests) {
                String testName = (IDEMPOTENT_NUM_RUNS  > 1) ? (test.name() + ":" + i) : test.name();
                finalResult.testOrder().add(testName);
            }

            for (final TestResult result : results) {
                String testName = (IDEMPOTENT_NUM_RUNS  > 1) ? (result.name() + ":" + i) : result.name();
                finalResult.results().put(testName, result);
            }
        }
    }

    private TestRunResult execute(final String testRunId, final List<JUnitTest> allTests) {
        final TestRunResult finalResult = TestRunResult.empty(testRunId);
        if (CONSEC_IDEMPOTENT) {
            for (JUnitTest oneTest : allTests) {
                List<JUnitTest> tests = new ArrayList<>();
                tests.add(oneTest);
                executeHelper(testRunId, tests, finalResult);
            }
        } else {
            executeHelper(testRunId, allTests, finalResult);
        }
        return finalResult;
    }

    public Optional<TestRunResult> execute(final String testRunId) {
        final TestRunResult results = execute(testRunId, testOrder);

        final List<String> testOrderNames =
                testOrder.stream().map(JUnitTest::name).collect(Collectors.toList());

        return Optional.of(new TestRunResult(testRunId, testOrderNames, results.results()));

    }

    public TestRunResult executeSeparately(final String testRunId) {
        final TestRunResult results = TestRunResult.empty(testRunId);

        for (final JUnitTest test : testOrder) {
            final TestRunResult testResult = execute(testRunId, Collections.singletonList(test));

            results.results().putAll(testResult.results());
        }

        return results;
    }

    public TestRunResult executeWithJUnit4Runner(final String testRunId) {
        return execute(testRunId, testOrder);
    }

}
