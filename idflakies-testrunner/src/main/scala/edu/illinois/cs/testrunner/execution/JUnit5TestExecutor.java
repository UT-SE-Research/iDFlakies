package edu.illinois.cs.testrunner.execution;

import edu.illinois.cs.testrunner.data.results.TestRunResult;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

// Use JUnit 5 API to run tests.
public class JUnit5TestExecutor {

    public static TestRunResult runTestsSeparately(
            final String testRunId, final List<String> tests) {
        // this is a workaround to get full control of test order
        //
        // When the testrunner executes JUnit 4 tests with JUnitTestExecutor,
        // the @BeforeClass/@AfterClass method only run once for **each class**, no matter
        // how many tests in the class.
        // However, since this method (runTestsSeparately) run each test separately,
        // @BeforeAll/@AfterAll method run for **each test**, which means @BeforeAll/@Afterall
        // method could run many time for a test class.
        // TODO: make @BeforeAll/@AfterAll behavior same as JUnit 4 test executor (JUnitTestExecutor)
        //
        // The above todo may be hard. But there is a simple improvement.
        // TODO: group consecutive tests from the same class into a batch to run
        List<MethodSelector> methods =
                tests.stream()
                     .map(name -> DiscoverySelectors.selectMethod(name))
                     .collect(Collectors.toList());
        JUnit5TestListener listener = new JUnit5TestListener();
        Launcher launcher = LauncherFactory.create();
        for (MethodSelector method : methods) {
            LauncherDiscoveryRequest request =
                    LauncherDiscoveryRequestBuilder
                            .request().selectors(method).build();
            launcher.execute(request, listener);
        }
        return new TestRunResult(testRunId,
                                 listener.getTestOrder(),
                                 listener.getResults());
    }

}