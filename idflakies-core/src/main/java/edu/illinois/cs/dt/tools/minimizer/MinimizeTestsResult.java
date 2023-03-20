package edu.illinois.cs.dt.tools.minimizer;

import com.google.gson.Gson;
import com.reedoei.eunomia.collections.ListUtil;
import com.reedoei.eunomia.io.IOUtil;
import com.reedoei.eunomia.io.files.FileUtil;
import edu.illinois.cs.dt.tools.utility.MD5;
import edu.illinois.cs.dt.tools.utility.OperationTime;
import edu.illinois.cs.dt.tools.utility.PathManager;
import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.runner.Runner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MinimizeTestsResult {
    private static final int VERIFY_REPEAT_COUNT = 1;
    private static final int MAX_SUBSEQUENCES = 1000;

    private final OperationTime time;
    private final TestRunResult expectedRun;
    private final Result expected;
    private final String dependentTest;
    private final List<PolluterData> polluters;
    private final String hash;
    private final FlakyClass flakyClass;    // The classification of this one's dependent test can be "OD" or "NOD" (if reruns found it to be not order-dependent)

    public static MinimizeTestsResult fromPath(final Path path) throws IOException {
        return fromString(FileUtil.readFile(path));
    }

    public static MinimizeTestsResult fromString(final String jsonString) {
        return new Gson().fromJson(jsonString, MinimizeTestsResult.class);
    }

    public MinimizeTestsResult(final OperationTime time, final TestRunResult expectedRun, final Result expected,
                               final String dependentTest, final List<PolluterData> polluters, final FlakyClass flakyClass) {
        this.time = time;
        this.expectedRun = expectedRun;
        this.expected = expected;
        this.dependentTest = dependentTest;
        this.polluters = polluters;
        this.hash = MD5.hashOrder(expectedRun.testOrder());
        this.flakyClass = flakyClass;
    }

    public OperationTime time() {
        return this.time;
    }

    public String hash() {
        return this.hash;
    }

    public FlakyClass flakyClass() {
        return this.flakyClass;
    }

    private boolean isExpected(final Runner runner, final List<String> deps) {
        final List<String> order = new ArrayList<>(deps);
        order.add(dependentTest());

        return runner
                .runList(order)
                .get()
                .results()
                .get(dependentTest()).result().equals(expected());
    }

    public boolean verify(final Runner runner) throws Exception {
        return verify(runner, VERIFY_REPEAT_COUNT);
    }

    public boolean verify(final Runner runner, final int verifyCount) throws Exception {
        List<PolluterData> pollutersToRemove = new ArrayList<>();
        for (PolluterData polluter : polluters) {
            try {
                for (int i = 0; i < verifyCount; i++) {
                    List<String> deps = polluter.deps();
                    final List<List<String>> depLists = ListUtil.sample(ListUtil.subsequences(deps), MAX_SUBSEQUENCES);
                    int check = 1;
                    int totalChecks = 2 + depLists.size() - 1;

                    IOUtil.printClearLine(String.format("Verifying %d of %d. Running check %d of %d.", i + 1, verifyCount, check++, totalChecks));
                    // Check that it's correct with the dependencies
                    if (!isExpected(runner, deps)) {
                        throw new MinimizeTestListException("Got unexpected result when running with all dependencies!");
                    }

                    // Only run the first check if there are no dependencies.
                    if (deps.isEmpty()) {
                        continue;
                    }

                    verifyDependencies(runner, verifyCount, i, deps, depLists, check, totalChecks);
                }

                System.out.println();
            } catch (MinimizeTestListException ex) {
                System.out.println("Got exception when trying to verify dependencies: " + polluter.deps());
                ex.printStackTrace();
                pollutersToRemove.add(polluter);
            }
        }
        polluters.removeAll(pollutersToRemove);
        if (pollutersToRemove.size() > 0) { // Extreme measures, if there are polluters that were found to not work, then say whole test is NOD
            return false;
        }

        return true;
    }

    private void verifyDependencies(final Runner runner,
                                    final int verifyCount,
                                    final int i,
                                    final List<String> deps,
                                    final List<List<String>> depLists,
                                    int check,
                                    final int totalChecks) throws Exception {
        IOUtil.printClearLine(String.format("Verifying %d of %d. Running check %d of %d.", i + 1, verifyCount, check++, totalChecks));
        // Check that it's wrong without dependencies.
        if (isExpected(runner, new ArrayList<>())) {
            throw new MinimizeTestListException("Got expected result even without any dependencies!");
        }

        // Check that for any subsequence that isn't the whole list, it's wrong.
        for (final List<String> depList : depLists) {
            if (depList.equals(deps)) {
                continue;
            }

            IOUtil.printClearLine(String.format("Verifying %d of %d. Running check %d of %d.",  i + 1, verifyCount, check++, totalChecks));
            if (isExpected(runner, depList)) {
                throw new MinimizeTestListException("Got expected result without some dependencies! " + depList);
            }
        }
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public void save() {
        final Path outputPath = PathManager.minimizedPath(dependentTest(), hash, expected());

        try {
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<PolluterData> polluters() {
        return polluters;
    }

    public List<String> getFirstDeps() {
        if (polluters.isEmpty()) {
            return new ArrayList<>();
        }
        return polluters.get(0).deps();
    }

    public String dependentTest() {
        return dependentTest;
    }

    public Result expected() {
        return expected;
    }

    public List<String> withDeps() {
        final List<String> order = new ArrayList<>(getFirstDeps());
        if (!order.contains(dependentTest())) {
            order.add(dependentTest());
        }
        return order;
    }

    public TestRunResult expectedRun() {
        return expectedRun;
    }
}
