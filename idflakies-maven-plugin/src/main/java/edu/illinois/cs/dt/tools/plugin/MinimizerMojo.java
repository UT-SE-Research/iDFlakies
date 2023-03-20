package edu.illinois.cs.dt.tools.plugin;

import com.reedoei.eunomia.collections.StreamUtil;
import edu.illinois.cs.dt.tools.detection.detectors.Detector;
import edu.illinois.cs.dt.tools.detection.detectors.RandomDetector;
import edu.illinois.cs.dt.tools.minimizer.MinimizeTestsResult;
import edu.illinois.cs.dt.tools.minimizer.TestMinimizer;
import edu.illinois.cs.dt.tools.minimizer.TestMinimizerBuilder;
import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.dt.tools.runner.data.DependentTest;
import edu.illinois.cs.dt.tools.runner.data.DependentTestList;
import edu.illinois.cs.dt.tools.runner.data.TestRun;
import edu.illinois.cs.dt.tools.utility.Level;
import edu.illinois.cs.dt.tools.utility.Logger;
import edu.illinois.cs.dt.tools.utility.PathManager;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.testrunner.runner.Runner;
import edu.illinois.cs.testrunner.runner.RunnerFactory;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "minimize", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class MinimizerMojo extends AbstractIDFlakiesMojo {

    private InstrumentingSmartRunner runner;
    private TestMinimizerBuilder builder;

    private final String TEST_TO_MINIMIZE = Configuration.config().getProperty("dt.minimizer.dependent.test", null);
    private final boolean GENERATE_FLAKIES = Configuration.config().getProperty("dt.minimizer.generate.list.flakies", false);
    public static final boolean USE_ORIGINAL_ORDER = Configuration.config().getProperty("dt.minimizer.use.original.order", false);
    private static final boolean VERIFY_DTS = Configuration.config().getProperty("dt.verify", true);
    public static final String FLAKY_LIST = Configuration.config().getProperty("dt.minimizer.flaky.list", null);
    public static final String ORIGINAL_ORDER = Configuration.config().getProperty("dt.minimizer.original.order", null);

    private Stream<TestMinimizer> fromDtList(Path path, MavenProject project) {
        if (FLAKY_LIST != null) {
            path = Paths.get(FLAKY_LIST);
            Logger.getGlobal().log(Level.INFO, "dt.minimizer.flaky.list argument specified: " + FLAKY_LIST);
        }
        Logger.getGlobal().log(Level.INFO, "Creating minimizers for file: " + path);

        try {
            if (!Files.exists(PathManager.cachePath())) {
                Files.createDirectories(PathManager.cachePath());
            }

            Path originalOrderPath = PathManager.originalOrderPath();
            List<String> originalOrder;
            if (Files.exists(originalOrderPath)) {
                originalOrder = Files.readAllLines(originalOrderPath);
            } else {
                originalOrder = new ArrayList<>();
            }

            if (ORIGINAL_ORDER != null) {
                Logger.getGlobal().log(Level.INFO, "Using specified original order. dt.minimizer.original.order argument specified: " + ORIGINAL_ORDER);
                List<String> originalOrderSpecified = Files.readAllLines(Paths.get(ORIGINAL_ORDER));

                if (originalOrderSpecified.equals(originalOrder)) {
                    Logger.getGlobal().log(Level.INFO, "Custom original order specified and found to be matching the existing original order at: "
                                          + originalOrderPath);
                } else {
                    Logger.getGlobal().log(Level.INFO, "Copying custom original order specified since it is different than the (non)existing original order at: "
                                          + originalOrderPath);

                    if (Files.exists(originalOrderPath)) {
                        String originalOrderRename = "original-order-" + System.currentTimeMillis();
                        Path renamePath = originalOrderPath.resolveSibling(originalOrderRename);
                        Files.move(originalOrderPath, renamePath);
                        Logger.getGlobal().log(Level.INFO, "Original order before copying is now moved to: " + renamePath);
                    }

                    // Copy the original order file to where we expect it to be since other parts of the tool still expects it to be there
                    // Future versions of iDFlakies should allow us to set the PathManager.originalOrderPath directly
                    Files.write(originalOrderPath, originalOrderSpecified);

                    Logger.getGlobal().log(Level.INFO, "Specified original order copied to: " + originalOrderPath);
                    originalOrder = originalOrderSpecified;
                }
            }

            if (!Files.exists(originalOrderPath) || originalOrder.isEmpty()) {
                Logger.getGlobal().log(Level.INFO, "Original order file not found or is empty. Creating original-order file now at: "
                                      + originalOrderPath);
                originalOrder = DetectorMojo.getOriginalOrder(mavenProject, this.runner.framework(), true);
                Files.write(originalOrderPath, originalOrder);
            }

            DependentTestList dependentTestList = DependentTestList.fromFile(path);
            if (dependentTestList == null && !GENERATE_FLAKIES) {
                throw new IllegalArgumentException("Dependent test list file is empty. " +
                                                   "If you would like iFixFlakies to try generating passing/failing orders using random-class-method, try running iFixFlakies with -Ddt.minimizer.generate.list.flakies=true. " +
                                                   "You can control the rounds for iFixFlakies to generate passing/failing orders with -Ddt.randomize.rounds=X where X is an integer. The default is 20.");
            } else if (dependentTestList == null) {
                // Run random class method a few times to see if test would be OD
                String coordinates = mavenProject.getGroupId() + ":" + mavenProject.getArtifactId() + ":" + mavenProject.getVersion();
                Detector detector = new RandomDetector("random", mavenProject.getBasedir(), runner,
                                                       DetectorMojo.moduleRounds(coordinates),
                                                       originalOrder);
                dependentTestList = new DependentTestList(detector.detect());
            }

            if (TEST_TO_MINIMIZE != null) {
                Logger.getGlobal().log(Level.INFO, "Filtering dependent test list to run only for: " + TEST_TO_MINIMIZE);
                Optional<DependentTest> dependentTest = dependentTestList.dts().stream().
                        filter(dt -> dt.name().equalsIgnoreCase(TEST_TO_MINIMIZE)).findFirst();

                if (!dependentTest.isPresent()) {
                    throw new IllegalArgumentException("Dependent test name is specificed but could not find matching dependent test.");
                } else {
                    return minimizers(dependentTest.get(), builder, runner, USE_ORIGINAL_ORDER ? originalOrder : null);
                }
            } else {
                List<String> finalOriginalOrder = originalOrder;
                if (dependentTestList.dts().size() > 1) {
                    Logger.getGlobal().log(Level.FINE, "More than one dependent test list detected. Original order cannot be trusted.");
                }
                return dependentTestList.dts().stream()
		    .flatMap(dt -> minimizers(dt, builder, runner, USE_ORIGINAL_ORDER ? finalOriginalOrder : null));
            }
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    public Stream<TestMinimizer> minimizers(final DependentTest dependentTest,
					    final TestMinimizerBuilder builder,
					    final Runner runner,
					    List<String> originalOrder) {
        final TestRun intended = dependentTest.intended();
        final TestRun revealed = dependentTest.revealed();
        final String name = dependentTest.name();
        final TestMinimizerBuilder minimizerBuilder = builder.dependentTest(name);

        if (VERIFY_DTS) {
            if (!intended.verify(name, runner, null) || !revealed.verify(name, runner, null)) {
                return Stream.of(minimizerBuilder.buildNOD());
            }
        }

        // Try running dependent test in isolation to determine which order to minimize
        // Also run it 10 times to be more confident that test is deterministic in its result
        final Result isolationResult = runner.runList(Collections.singletonList(name)).get().results().get(name).result();
        for (int i = 0; i < 9; i++) {
            Result rerunIsolationResult = runner.runList(Collections.singletonList(name)).get().results().get(name).result();
            // If ever get different result, then not confident in result, return
            if (!rerunIsolationResult.equals(isolationResult)) {
                System.out.println("Test " + name + " does not have consistent result in isolation, not order-dependent!");
                return Stream.of(minimizerBuilder.buildNOD());
            }
        }

        TestMinimizer tm;
        if (originalOrder != null) {
            Logger.getGlobal().log(Level.INFO, "Using original order to run Minimizer instead of intended or revealed order.");
            if (!isolationResult.equals(Result.PASS)) {
                tm = minimizerBuilder.testOrder(reorderOriginalOrder(intended.order(), originalOrder, name)).build();
            } else {
                tm = minimizerBuilder.testOrder(reorderOriginalOrder(revealed.order(), originalOrder, name)).build();
            }
        } else if (!isolationResult.equals(Result.PASS)) { // Does not pass in isolation, needs setter, so need to minimize passing order
            tm = minimizerBuilder.testOrder(intended.order()).build();
            
        } else {    // Otherwise passes in isolation, needs polluter, so need to minimize failing order
            tm = minimizerBuilder.testOrder(revealed.order()).build();
        }
        String victimBrittleStr = !isolationResult.equals(Result.PASS) 
                ? "Test is brittle. Result of running test in isolation is: " + isolationResult
                : "Test is victim. Result of running test in isolation is: " + isolationResult;
        Logger.getGlobal().log(Level.INFO, victimBrittleStr);
        return Stream.of(tm);
    }

    private List<String> reorderOriginalOrder(List<String> intended, List<String> originalOrder, String dependentTest) {
        List<String> retList = new ArrayList<>(intended);
        if (!intended.contains(dependentTest)) {
            retList.add(dependentTest);
        }

        for (String test : originalOrder) {
            if (!intended.contains(test)) {
                retList.add(test);
            }
        }
        try {
            Files.write(PathManager.originalOrderPath(), retList);
        } catch (IOException e) {
            Logger.getGlobal().log(Level.SEVERE, "Created new original order but could not write to "
                                           + PathManager.originalOrderPath());
            return retList;
        }
        Logger.getGlobal().log(Level.INFO, "Reordered original order to have some tests come first. For tests: " + intended);

        return retList;
    }

    public Stream<MinimizeTestsResult> runDependentTestFile(final Path dtFile, MavenProject project) {
        return fromDtList(dtFile, project).flatMap(minimizer -> {
            try {
                final MinimizeTestsResult result = minimizer.get();
                result.save();
                return Stream.of(result);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return Stream.empty();
        });
    }

    @Override
    public void execute() {
        super.execute();
        this.runner = InstrumentingSmartRunner.fromRunner(RunnerFactory.from(mavenProject).get(), mavenProject.getBasedir());
        this.builder = new TestMinimizerBuilder(runner);

        StreamUtil.seq(runDependentTestFile(PathManager.detectionFile(), mavenProject));
    }
}
