package edu.illinois.cs.dt.tools.detection;

import com.reedoei.testrunner.configuration.Configuration;
import com.reedoei.testrunner.data.results.Result;
import com.reedoei.testrunner.data.results.TestResult;
import com.reedoei.testrunner.data.results.TestRunResult;
import com.reedoei.testrunner.mavenplugin.TestPluginPlugin;
import com.reedoei.testrunner.runner.Runner;
import edu.illinois.cs.dt.tools.runner.data.DependentTest;
import edu.illinois.cs.dt.tools.runner.data.TestRun;
import scala.collection.Set;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class DetectorUtil {

    //
    private static List<String> NODs = new ArrayList();
    private static List<String> consistentlyFail = new ArrayList();
    //

    public static TestRunResult originalResults(final List<String> originalOrder, final Runner runner) {
        final int originalOrderTries = Configuration.config().getProperty("dt.detector.original_order.retry_count", 3);
        final boolean allMustPass = Configuration.config().getProperty("dt.detector.original_order.all_must_pass", true);

        System.out.println("[INFO] Getting original results (" + originalOrder.size() + " tests).");

//////
        int maxFailTimes = 0;
        int passrounds = 0;
//////

        TestRunResult origResult = null;
        Map<String, Integer> record = new HashMap<String, Integer>(); //build a map link the string and number of times it fails

        //boolean allPassing = false;
        // Try to run it three times, to see if we can get everything to pass (except for ignored tests)
        for (int i = 0; i < originalOrderTries; i++) {
            origResult = runner.runList(originalOrder).get();

            try {
                Files.write(DetectorPathManager.originalResultsLog(), (origResult.id() + "\n").getBytes(),
                        Files.exists(DetectorPathManager.originalOrderPath()) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
            } catch (IOException ignored) {}

            if (allPass(origResult)) {
                //allPassing = true;
                passrounds += 1;
                //break;
                //rather than break immediately when there is a passing round, I would like to run all rounds for checking NOD tests

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            } else {
                System.out.println("Analyzing...");
                for (Iterator iter = originalOrder.iterator(); iter.hasNext();) { //a iter to go through every string
                    String str = (String)iter.next();
                    System.out.println(str);
                    TestResult temp = origResult.results().get(str); //get the test result for specific string from map
                    if(!(temp.result().equals(Result.PASS) || temp.result().equals(Result.SKIPPED))) {
                        int timesOfFAIL = 1; // need to be modified
                        if(record.containsKey(str)) {
                            timesOfFAIL += record.get(str);
                        }
                        record.put(str, timesOfFAIL);
                        if(timesOfFAIL > maxFailTimes){
                            maxFailTimes = timesOfFAIL; //record the maxFailTime after xxx tests do not exist
                        }
                    }
                }
            }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        }
        List<String> newOrder = originalOrder;

        if (passrounds != originalOrderTries) {
            //if (allMustPass) {
            //    throw new NoPassingOrderException("No passing order for tests (" + originalOrderTries + " runs)");
            //} else {
            //TestPluginPlugin.info("No passing order for tests (" + originalOrderTries + " runs). Continuing anyway with last run.");
            TestPluginPlugin.info("Consistently fail tests or NOD tests exist (" + originalOrderTries + " runs). Removing fail tests");
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            Iterator<String> iter = record.keySet().iterator();
            //Integer tolerance = originalOrderTries;
            Integer tolerance = maxFailTimes;
            while(iter.hasNext()){
                String str=iter.next();
                Integer value = record.get(str);
                if(value >= tolerance) {
                    newOrder.remove(str);
                    if(value == originalOrderTries){
                        consistentlyFail.add(str);
                    } else {
                        NODs.add(str);
                    }
                    break; //depend on our configuration - delete xxx tests all together or one by one
                }
            }
            //with newOrder, rerun the function
            origResult = originalResults(newOrder, runner);
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            //}
        } else {
            System.out.println("-------------------------------[TEST RESULT]-------------------------------");
            System.out.println("Passing Order");
            for (Iterator iter = newOrder.iterator(); iter.hasNext();) {
                String str = (String) iter.next();
                System.out.println(str);
            }
            System.out.println("Consistently Fails");
            for (Iterator iter = consistentlyFail.iterator(); iter.hasNext();) {
                String str = (String) iter.next();
                System.out.println(str);
            }
            System.out.println("NODs");
            for (Iterator iter = NODs.iterator(); iter.hasNext();) {
                String str = (String) iter.next();
                System.out.println(str);
            }
            System.out.println("-------------------------------[TEST RESULT]-------------------------------\"");
        }

        return origResult;
    }

    public static boolean allPass(final TestRunResult testRunResult) {
        return testRunResult.results().values().stream()
                // Ignored tests will show up as SKIPPED, but that's fine because surefire would've skipped them too
                .allMatch(tr -> tr.result().equals(Result.PASS) || tr.result().equals(Result.SKIPPED));
    }

    private static <T> List<T> before(final List<T> ts, final T t) {
        final int i = ts.indexOf(t);

        if (i != -1) {
            return new ArrayList<>(ts.subList(0, Math.min(ts.size(), i)));
        } else {
            return new ArrayList<>();
        }
    }

    public static List<DependentTest> flakyTests(final TestRunResult intended,
                                                 final TestRunResult revealed,
                                                 final boolean onlyFirstFailure) {
        final List<DependentTest> result = new ArrayList<>();

        for (final Map.Entry<String, TestResult> entry : intended.results().entrySet()) {
            final String testName = entry.getKey();
            final TestResult intendedResult = entry.getValue();
            final Map<String, TestResult> revealedResults = revealed.results();

            if (revealedResults.containsKey(testName)) {
                final Result revealedResult = revealedResults.get(testName).result();
                if (!revealedResult.equals(intendedResult.result())) {
                    result.add(new DependentTest(testName,
                            new TestRun(before(intended.testOrder(), testName), intendedResult.result(), intended.id()),
                            new TestRun(before(revealed.testOrder(), testName), revealedResult, revealed.id())));

                    if (onlyFirstFailure) {
                        // Only keep the first failure, if any
                        break;
                    }
                }
            }
        }
        return result;
    }
}