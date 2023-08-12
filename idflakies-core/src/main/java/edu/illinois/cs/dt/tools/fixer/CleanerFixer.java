package edu.illinois.cs.dt.tools.fixer;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import edu.illinois.cs.dt.tools.minimizer.FlakyClass;
import edu.illinois.cs.dt.tools.minimizer.MinimizeTestsResult;
import edu.illinois.cs.dt.tools.minimizer.PolluterData;
import edu.illinois.cs.dt.tools.minimizer.cleaner.CleanerGroup;
import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.dt.tools.utility.BuildCommands;
import edu.illinois.cs.dt.tools.utility.ErrorLogger;
import edu.illinois.cs.dt.tools.utility.Level;
import edu.illinois.cs.dt.tools.utility.Logger;
import edu.illinois.cs.dt.tools.utility.OperationTime;
import edu.illinois.cs.dt.tools.utility.PathManager;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.testrunner.runner.Runner;

import org.apache.commons.lang3.tuple.ImmutablePair;
import scala.Option;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CleanerFixer {
    public static final String PATCH_LINE_SEP = "==========================";

    private List<Patch> patches;

    private InstrumentingSmartRunner runner;
    private List<Path> testSources;
    private String classpath;
    private BuildCommands buildCommands;

    // Some fields to help with computing time to first cleaner and outputing in log
    private long startTime;
    private boolean foundFirst;

    public CleanerFixer(final InstrumentingSmartRunner runner, final List<Path> testSources, final String classpath, final BuildCommands buildCommands) {
        this.runner = runner;
        this.testSources = testSources;
        this.classpath = classpath;
        this.buildCommands = buildCommands;
    }

    private boolean testOrderPasses(final List<String> tests) {
        return new FailingTestDetector(runner).notPassingTests(tests).orElse(new HashSet<>()).isEmpty();
    }

    private URLClassLoader projectClassLoader() {
        // Get the project classpath, it will be useful for many things
        List<URL> urlList = new ArrayList();
        for (String cp : this.classpath.split(":")) {
            try {
                urlList.add(new File(cp).toURL());
            } catch (MalformedURLException mue) {
                Logger.getGlobal().log(Level.SEVERE, "Classpath element " + cp + " is malformed!");
            }
        }
        URL[] urls = urlList.toArray(new URL[urlList.size()]);
        return URLClassLoader.newInstance(urls);
    }

    public void fix() {
        final ErrorLogger logger = new ErrorLogger();

        this.patches = new ArrayList<>();

        logger.runAndLogError(() -> {
            if (!Files.exists(PathManager.cachePath())) {
                Files.createDirectories(PathManager.cachePath());
            }

            startTime = System.currentTimeMillis();

            // Iterate through each minimized, collecing such that unique for dependent test, combine polluters
            Map<String, MinimizeTestsResult> minimizedResults = new HashMap<>();
            for (MinimizeTestsResult minimized : getMinimizeTestsResults()) {
                String dependentTest = minimized.dependentTest();
                if (!minimizedResults.containsKey(dependentTest)) {
                    minimizedResults.put(dependentTest, minimized);
                }
                // Iterate through all the polluters of the current minimized, add in new ones into the existing one
                for (PolluterData pd : minimized.polluters()) {
                    if (!minimizedResults.get(dependentTest).polluters().contains(pd)) {
                        minimizedResults.get(dependentTest).polluters().add(pd);
                    }
                }
            }
            for (String dependentTest : minimizedResults.keySet()) {
                MinimizeTestsResult minimized = minimizedResults.get(dependentTest);
                FixerResult fixerResult = OperationTime.runOperation(() -> {
                    return setupAndApplyFix(minimized);
                }, (patchResults, time) -> {
                    // Determine overall status by looking through result of each patch result
                    FixStatus overallStatus = FixStatus.NOD;    // Start with "lowest" enum, gets overriden by better fixes
                    for (PatchResult res : patchResults) {
                        if (res.status().ordinal() > overallStatus.ordinal()) {
                            overallStatus = res.status();
                        }
                    }
                    return new FixerResult(time, overallStatus, minimized.dependentTest(), patchResults);
                });
                fixerResult.save();
            }
            return null;
        });
    }

    private List<MinimizeTestsResult> getMinimizeTestsResults() throws Exception {
        List<MinimizeTestsResult> results = new ArrayList<>();

        // Get minimized results corresponding to all files under the minimized path
        if (Files.exists(PathManager.minimizedPath())) {
            for (File f : PathManager.minimizedPath().toFile().listFiles()) {
                if (f.isFile()) {
                    MinimizeTestsResult res = MinimizeTestsResult.fromPath(f.toPath());

                    // Consider whether user configured to fix only a single test
                    String dependentTest = Configuration.config().getProperty("dt.minimizer.dependent.test", null);
                    if (dependentTest != null) {
                        Logger.getGlobal().log(Level.INFO, "Filtering dependent test list to run only for: " + dependentTest);
                        if (res.dependentTest().equals(dependentTest)) {
                            results.add(res);
                            return results; // Only consider this one dependent test and return
                        }
                    } else {
                        results.add(res);
                    }
                }
            }
        }

        return results;
    }

    private boolean sameTestClass(String test1, String test2) {
        return test1.substring(0, test1.lastIndexOf('.')).equals(test2.substring(0, test2.lastIndexOf('.')));
    }

    private List<PatchResult> setupAndApplyFix(final MinimizeTestsResult minimized) throws Exception {
        startTime = System.currentTimeMillis();
        foundFirst = false;

        List<PatchResult> patchResults = new ArrayList<>();

        // Check that the minimized is not some NOD, in which case we do not proceed
        if (minimized.flakyClass() == FlakyClass.NOD) {
            Logger.getGlobal().log(Level.INFO, "Will not patch discovered NOD test " + minimized.dependentTest());
            patchResults.add(new PatchResult(OperationTime.instantaneous(), FixStatus.NOD, minimized.dependentTest(), "N/A", "N/A", 0, null));
            return patchResults;
        }

        // Get all test source files
        final List<Path> testFiles = this.testSources;

        // All minimized orders passed in should have some polluters before (or setters in the case of the order passing)
        if (minimized.polluters().isEmpty()) {
            Logger.getGlobal().log(Level.SEVERE, "No polluters for: " + minimized.dependentTest());
            patchResults.add(new PatchResult(OperationTime.instantaneous(), FixStatus.NO_DEPS, minimized.dependentTest(), "N/A", "N/A", 0, null));
            return patchResults;
        }

        Logger.getGlobal().log(Level.INFO, "Beginning to fix dependent test " + minimized.dependentTest());

        List<PolluterData> polluterDataOrder = new ArrayList<PolluterData>();
        boolean prepend;

        // If in a passing order and there are multiple potential setters, then prioritize the one in the same test class as dependent test
        if (minimized.expected().equals(Result.PASS)) {
            Set<PolluterData> pdWithSameTestClass = new HashSet<>();
            Set<PolluterData> pdWithDiffTestClass = new HashSet<>();
            for (PolluterData pd : minimized.polluters()) {
                // Only care about case of one polluter
                if (pd.deps().size() == 1) {
                    String setter = pd.deps().get(0);
                    // Want the one in the same test class
                    if (sameTestClass(setter, minimized.dependentTest())) {
                        pdWithSameTestClass.add(pd);
                    } else {
                        pdWithDiffTestClass.add(pd);
                    }
                }
            }
            // Add first in same test class ones, then the remaining ones
            polluterDataOrder.addAll(pdWithSameTestClass);
            polluterDataOrder.addAll(pdWithDiffTestClass);
            prepend = true;
        } else {
            // If case of failing order with polluters, best bet is one that has a cleaner, and in same test class as victim
            Set<PolluterData> pdNoCleaner = new HashSet<>();
            Set<PolluterData> pdWithCleaner = new HashSet<>();
            Set<PolluterData> pdWithSingleCleaner = new HashSet<>();
            Set<PolluterData> pdWithSingleCleanerSameTestClassVictim = new HashSet<>();
            Set<PolluterData> pdWithSingleCleanerSameTestClassPolluter = new HashSet<>();
            for (PolluterData pd : minimized.polluters()) {
                // Consider if has a cleaner
                if (!pd.cleanerData().cleaners().isEmpty()) {
                    pdWithCleaner.add(pd);
                    String polluter = pd.deps().get(pd.deps().size() - 1);  // If we're going to modify polluter, do it with the last one
                    // Would be best to have a cleaner group that is only one test
                    for (CleanerGroup cleanerGroup : pd.cleanerData().cleaners()) {
                        if (cleanerGroup.cleanerTests().size() == 1) {
                            pdWithSingleCleaner.add(pd);
                            // Even more ideal, if the cleaner is in the same test class as victim
                            String cleaner = cleanerGroup.cleanerTests().get(0);
                            if (sameTestClass(cleaner, minimized.dependentTest())) {
                                pdWithSingleCleanerSameTestClassVictim.add(pd);
                            }
                            // Also valid is if in the same test class as the polluter
                            if (sameTestClass(cleaner, polluter)) {
                                pdWithSingleCleanerSameTestClassPolluter.add(pd);
                            }
                        }
                    }
                } else {
                    pdNoCleaner.add(pd);
                }
            }
            // Remove from each level duplicates
            pdWithCleaner.removeAll(pdWithSingleCleaner);
            pdWithSingleCleaner.removeAll(pdWithSingleCleanerSameTestClassVictim);
            pdWithSingleCleaner.removeAll(pdWithSingleCleanerSameTestClassPolluter);
            pdWithSingleCleanerSameTestClassVictim.removeAll(pdWithSingleCleanerSameTestClassPolluter);
            // Prioritize based on those levels
            polluterDataOrder.addAll(pdWithSingleCleanerSameTestClassPolluter);
            polluterDataOrder.addAll(pdWithSingleCleanerSameTestClassVictim);
            polluterDataOrder.addAll(pdWithSingleCleaner);
            polluterDataOrder.addAll(pdWithCleaner);
            polluterDataOrder.addAll(pdNoCleaner);

            // If more than one polluter for dependent test, then favor fixing the dependent test
            if (polluterDataOrder.size() > 1) {
                prepend = true;
            } else {
                prepend = false;
            }
        }

        for (PolluterData polluterData : polluterDataOrder) {
            // Apply fix using specific passed in polluter data
            patchResults.addAll(setupAndApplyFix(minimized, polluterData, this.testSources, prepend));
        }
        return patchResults;
    }

    private List<PatchResult> setupAndApplyFix(final MinimizeTestsResult minimized,
                                               final PolluterData polluterData,
                                               final List<Path> testFiles,
                                               boolean prepend) throws Exception {
        List<PatchResult> patchResults = new ArrayList<>();

        String polluterTestName;
        Optional<JavaMethod> polluterMethodOpt;
        List<String> failingOrder;
        List<String> fullFailingOrder;

        List<String> cleanerTestNames = new ArrayList<>();  // Can potentially work with many cleaners, try them all

        String victimTestName = minimized.dependentTest();
        Optional<JavaMethod> victimMethodOpt = JavaMethod.find(victimTestName, testFiles, this.classpath);
        if (!victimMethodOpt.isPresent()) {
            Logger.getGlobal().log(Level.SEVERE, "Could not find victim method " + victimTestName);
            Logger.getGlobal().log(Level.SEVERE, "Tried looking in: " + testFiles);
            patchResults.add(new PatchResult(OperationTime.instantaneous(), FixStatus.MISSING_METHOD, victimTestName, "N/A", "N/A", 0, null));
            return patchResults;
        }

        // If dealing with a case of result with failure, then get standard cleaner logic from it
        if (!minimized.expected().equals(Result.PASS)) {
            // Failing order has both the dependent test and the dependencies
            failingOrder = polluterData.withDeps(minimized.dependentTest());

            polluterTestName = polluterData.deps().get(polluterData.deps().size() - 1); // If more than one polluter, want to potentially modify last one
            polluterMethodOpt = JavaMethod.find(polluterTestName, testFiles, this.classpath);

            if (polluterData.cleanerData().cleaners().isEmpty()) {
                Logger.getGlobal().log(Level.INFO, "Found polluters for " + victimTestName + " but no cleaners.");
                /*TestPluginPlugin.info("Trying prior patches to see if now is fixed.");
                if (applyPatchesAndRun(failingOrder, victimMethodOpt.get(), polluterMethodOpt.get())) {
                    TestPluginPlugin.info("Dependent test " + victimTestName + " can pass with patches from before.");
                } else {
                    TestPluginPlugin.info("Prior patches do not allow " + victimTestName + " to pass.");
                    writePatch(victimMethodOpt.get(), 0, null, 0, null, null, polluterMethodOpt.orElse(null), 0, "NO CLEANERS");
                }*/
                Path patch = writePatch(victimMethodOpt.get(), 0, null, 0, null, null, polluterMethodOpt.orElse(null), 0, "NO CLEANERS");
                patchResults.add(new PatchResult(OperationTime.instantaneous(), FixStatus.NO_CLEANER, victimTestName, polluterMethodOpt.isPresent() ? polluterMethodOpt.get().methodName() : "N/A", "N/A", 0, patch.toString()));
                return patchResults;
            }

            for (CleanerGroup cleanerGroup : polluterData.cleanerData().cleaners()) {
                // Only handle cleaner groups that have one test each
                if (cleanerGroup.cleanerTests().size() == 1) {
                    cleanerTestNames.add(cleanerGroup.cleanerTests().get(0));   // TODO: Handle cleaner group with more than one test
                }
            }
            fullFailingOrder = minimized.expectedRun().testOrder(); // Also grab the full failing order from the expected run's test order


        } else {
            // "Cleaner" when result is passing is the "polluting" test(s)
            // TODO: Handle group of setters with more than one test
            if (polluterData.deps().size() > 1) {
                Logger.getGlobal().log(Level.SEVERE, "There is more than one setter test (currently unsupported)");
                patchResults.add(new PatchResult(OperationTime.instantaneous(), FixStatus.UNSUPPORTED, victimTestName, "N/A", "N/A", 0, null));
                return patchResults;
            }
            polluterTestName = null;    // No polluter if minimized order is passing
            polluterMethodOpt = Optional.ofNullable(null);

            cleanerTestNames.add(polluterData.deps().get(0));   // Assume only one, get first...

            // Failing order should be just the dependent test by itself (as is the full failing order (for now))
            failingOrder = Collections.singletonList(minimized.dependentTest());
            fullFailingOrder = failingOrder;
        }

        if (polluterTestName != null && !polluterMethodOpt.isPresent()) {
            Logger.getGlobal().log(Level.SEVERE, "Could not find polluter method " + polluterTestName);
            Logger.getGlobal().log(Level.SEVERE, "Tried looking in: " + testFiles);
            patchResults.add(new PatchResult(OperationTime.instantaneous(), FixStatus.MISSING_METHOD, victimTestName, polluterTestName, "N/A", 0, null));
            return patchResults;
        }

        // Give up if cannot find valid cleaner (single test that makes the order pass)
        if (cleanerTestNames.isEmpty()) {
            Logger.getGlobal().log(Level.SEVERE, "Could not get a valid cleaner for " + victimTestName);
            patchResults.add(new PatchResult(OperationTime.instantaneous(), FixStatus.NO_CLEANER, victimTestName, polluterTestName, "N/A", 0, null));
            return patchResults;
        }

        // Check if we pass in isolation before fix
        Logger.getGlobal().log(Level.INFO, "Running victim test with polluter before adding code from cleaner.");
        if (testOrderPasses(failingOrder)) {
            Logger.getGlobal().log(Level.SEVERE, "Failing order doesn't fail.");
            Path patch = writePatch(victimMethodOpt.get(), 0, null, 0, null, null, polluterMethodOpt.orElse(null), 0, "NOT FAILING ORDER");
            patchResults.add(new PatchResult(OperationTime.instantaneous(), FixStatus.NOT_FAILING, victimTestName, polluterTestName, "N/A", 0, patch.toString()));
            return patchResults;
        }

        // Try to apply fix with all cleaners, but if one of them works, then we are good
        for (String cleanerTestName : cleanerTestNames) {
            // Reload methods
            victimMethodOpt = JavaMethod.find(victimTestName, testFiles, this.classpath);
            if (polluterMethodOpt.isPresent()) {
                polluterMethodOpt = JavaMethod.find(polluterTestName, testFiles, this.classpath);
            }
            Optional<JavaMethod> cleanerMethodOpt = JavaMethod.find(cleanerTestName, testFiles, this.classpath);
            if (!cleanerMethodOpt.isPresent()) {
                Logger.getGlobal().log(Level.SEVERE, "Could not find cleaner method " + cleanerTestName);
                Logger.getGlobal().log(Level.SEVERE, "Tried looking in: " + testFiles);
                continue;
            }
            Logger.getGlobal().log(Level.INFO, "Applying code from " + cleanerMethodOpt.get().methodName() + " to make " + victimMethodOpt.get().methodName() + " pass.");
            PatchResult patchResult = applyFix(failingOrder, fullFailingOrder, polluterMethodOpt.orElse(null), cleanerMethodOpt.get(), victimMethodOpt.get(), prepend);
            patchResults.add(patchResult);
            // A successful patch means we do not need to try all the remaining cleaners for this ordering
            if (!foundFirst && (patchResult.status().ordinal() > FixStatus.FIX_INVALID.ordinal())) {
                //return patchResults;
                double elapsedSeconds = System.currentTimeMillis() / 1000.0 - startTime / 1000.0;
                Logger.getGlobal().log(Level.INFO, "FIRST PATCH: Found first patch for dependent test " + victimMethodOpt.get().methodName() + " in " + elapsedSeconds + " seconds.");
                foundFirst = true;
            }
        }
        return patchResults;
    }

    private void backup(final JavaFile javaFile) throws IOException {
        final Path path = PathManager.backupPath(javaFile.path());
        Files.copy(javaFile.path(), path, StandardCopyOption.REPLACE_EXISTING);
    }

    private void restore(final JavaFile javaFile) throws IOException {
        final Path path = PathManager.backupPath(javaFile.path());
        Files.copy(path, javaFile.path(), StandardCopyOption.REPLACE_EXISTING);
    }

    private NodeList<Statement> getCodeFromAnnotatedMethod(final String testClassName, final JavaFile javaFile, final String annotation) throws Exception {
        NodeList<Statement> stmts = NodeList.nodeList();

        // Determine super classes, to be used for later looking up helper methods
        Class testClass = projectClassLoader().loadClass(testClassName);
        List<Class> superClasses = new ArrayList<>();
        Class currClass = testClass;
        while (currClass != null) {
            superClasses.add(currClass);
            currClass = currClass.getSuperclass();
        }

        // If the test class is a subclass of JUnit 3's TestCase, then there is no annotation, just handle setUp and tearDown
        boolean isJUnit3 = false;
        for (Class clazz : superClasses) {
            if (clazz.toString().equals("class junit.framework.TestCase")) {
                isJUnit3 = true;
                break;
            }
        }
        // In JUnit 3 mode, try to get statements in setUp/tearDown only if in local class; otherwise put in a call to method if in superclass
        if (isJUnit3) {
            // Check if the test class had defined a setUp/tearDown
            String methName = "";
            for (Method meth : testClass.getDeclaredMethods()) {
                if (annotation.equals("@org.junit.Before")) {
                    if (meth.getName().equals("setUp")) {
                        methName = "setUp";
                        break;
                    }
                } else if (annotation.equals("@org.junit.After")) {
                    if (meth.getName().equals("tearDown")) {
                        methName = "tearDown";
                        break;
                    }
                }
            }
            if (!methName.equals("")) {
                MethodDeclaration method = javaFile.findMethodDeclaration(testClassName + "." + methName);
                Optional<BlockStmt> body = method.getBody();
                if (body.isPresent()) {
                    if (method.getDeclarationAsString(false, true, false).contains("throws ")) {
                        // Wrap the body inside a big try statement to suppress any exceptions
                        ClassOrInterfaceType exceptionType = new ClassOrInterfaceType().setName(new SimpleName("Throwable"));
                        CatchClause catchClause = new CatchClause(new Parameter(exceptionType, "ex"), new BlockStmt());
                        stmts.add(new TryStmt(new BlockStmt(body.get().getStatements()), NodeList.nodeList(catchClause), new BlockStmt()));
                    } else {
                        stmts.addAll(body.get().getStatements());
                    }
                }
                return stmts;   // Finished getting all the statements
            }

            // If reached here, means should go over super classes to see if one of these methods is even defined
            for (Class clazz : superClasses) {
                for (Method meth : clazz.getDeclaredMethods()) {
                    if (annotation.equals("@org.junit.Before")) {
                        if (meth.getName().equals("setUp")) {
                            stmts.add(new ExpressionStmt(new MethodCallExpr(null, "setUp")));
                            return stmts;
                        }
                    } else if (annotation.equals("@org.junit.After")) {
                        if (meth.getName().equals("tearDown")) {
                            stmts.add(new ExpressionStmt(new MethodCallExpr(null, "tearDown")));
                            return stmts;
                        }
                    }
                }
            }
        }

        // Iterate through super classes going "upwards", starting with this test class, to get annotated methods
        // If already seen a method of the same name, then it is overriden, so do not include
        List<String> annotatedMethods = new ArrayList<>();
        List<String> annotatedMethodsLocal = new ArrayList<>();
        for (Class clazz : superClasses) {
            for (Method meth : clazz.getDeclaredMethods()) {
                for (Annotation anno : meth.getDeclaredAnnotations()) {
                    if (anno.toString().equals(annotation + "()")) {
                        if (!annotatedMethods.contains(meth.getName())) {
                            annotatedMethods.add(meth.getName());
                        }
                        if (clazz.equals(testClass)) {
                            annotatedMethodsLocal.add(meth.getName());
                        }
                    }
                }
            }
        }
        annotatedMethods.removeAll(annotatedMethodsLocal);

        // For Before, go last super class first, then inline the statements in test class
        if (annotation.equals("@org.junit.Before")) {
            for (int i = annotatedMethods.size() - 1; i >= 0; i--) {
                stmts.add(new ExpressionStmt(new MethodCallExpr(null, annotatedMethods.get(i))));
            }
            for (String methName : annotatedMethodsLocal) {
                MethodDeclaration method = javaFile.findMethodDeclaration(testClassName + "." + methName);
                Optional<BlockStmt> body = method.getBody();
                if (body.isPresent()) {
                    if (method.getDeclarationAsString(false, true, false).contains("throws ")) {
                        // Wrap the body inside a big try statement to suppress any exceptions
                        ClassOrInterfaceType exceptionType = new ClassOrInterfaceType().setName(new SimpleName("Throwable"));
                        CatchClause catchClause = new CatchClause(new Parameter(exceptionType, "ex"), new BlockStmt());
                        stmts.add(new TryStmt(new BlockStmt(body.get().getStatements()), NodeList.nodeList(catchClause), new BlockStmt()));
                    } else {
                        stmts.addAll(body.get().getStatements());
                    }
                }
            }
        } else {
            // For After, inline the statements in test class, then go first super class first
            for (String methName : annotatedMethodsLocal) {
                MethodDeclaration method = javaFile.findMethodDeclaration(testClassName + "." + methName);
                Optional<BlockStmt> body = method.getBody();
                if (body.isPresent()) {
                    if (method.getDeclarationAsString(false, true, false).contains("throws ")) {
                        // Wrap the body inside a big try statement to suppress any exceptions
                        ClassOrInterfaceType exceptionType = new ClassOrInterfaceType().setName(new SimpleName("Throwable"));
                        CatchClause catchClause = new CatchClause(new Parameter(exceptionType, "ex"), new BlockStmt());
                        stmts.add(new TryStmt(new BlockStmt(body.get().getStatements()), NodeList.nodeList(catchClause), new BlockStmt()));
                    } else {
                        stmts.addAll(body.get().getStatements());
                    }
                }
            }
            for (int i = 0; i < annotatedMethods.size() ; i++) {
                stmts.add(new ExpressionStmt(new MethodCallExpr(null, annotatedMethods.get(i))));
            }
        }

        return stmts;
    }

    private boolean applyPatchesAndRun(final List<String> failingOrder,
                                       final JavaMethod victimMethod,
                                       final JavaMethod polluterMethod) throws Exception {
        Logger.getGlobal().log(Level.INFO, "Applying patches from before to see if order still fails.");
        for (Patch patch : patches) {
            Logger.getGlobal().log(Level.INFO, "Apply patch for " + patch.methodToPatch().methodName());
            patch.applyPatch();

            // Try the patch out
            this.buildCommands.install();
            boolean passWithPatches = testOrderPasses(failingOrder);
            patch.restore();        // Regardless, restore patch file(s)
            this.buildCommands.install(); // Rebuild again, in preparation for next run
            if (passWithPatches) {
                Logger.getGlobal().log(Level.INFO, "Failing order no longer fails after patches.");
                // If this is a new dependent test and the patches fix it, then save a file for it
                // just to help indicate that the test has been fixed
                writePatch(victimMethod, 0, null, 0, null, null, polluterMethod, 0, "PRIOR PATCH FIXED (DEPENDENT=" + patch.victimMethod().methodName() + ",CLEANER=" + patch.cleanerMethod().methodName() + ", MODIFIED=" + patch.methodToPatch().methodName() + ")");
                return true;
            }
        }
        return false;
    }

    private JavaMethod getAuxiliaryMethod(JavaMethod methodToModify, boolean prepend) throws Exception {
        String className = methodToModify.getClassName();
        String methodName = "auxiliary";    // Default name is auxiliary
        String annotation = "";

        Class clazz = projectClassLoader().loadClass(className);

        // Prepending means getting the @Before, otherwise means getting the @After
        if (prepend) {
            annotation = "@org.junit.Before()";
        } else {
            annotation = "@org.junit.After()";
        }
        for (Method meth : clazz.getDeclaredMethods()) {
            for (Annotation anno : meth.getDeclaredAnnotations()) {
                if (anno.toString().equals(annotation)) {
                    methodName = meth.getName();
                    break;
                }
            }
        }

        // If does not exist, then need to make new, otherwise can just use
        String fullMethodName = className + "." + methodName;
        if (methodName.equals("auxiliary")) {
            methodToModify.javaFile().addMethod(fullMethodName, annotation.replace("()", "").replace("@", ""));
            methodToModify.javaFile().writeAndReloadCompilationUnit();
        }
        JavaMethod auxiliaryMethod = JavaMethod.find(fullMethodName, this.testSources, this.classpath).get();

        return auxiliaryMethod;
    }

    private ExpressionStmt getHelperCallStmt(JavaMethod cleanerMethod, boolean newTestClass) {
        Expression objectCreation = null;
        if (newTestClass) {
            objectCreation = new ObjectCreationExpr(null, new ClassOrInterfaceType(null, cleanerMethod.getClassName()), NodeList.nodeList());
        }
        Expression helperCall = new MethodCallExpr(objectCreation, "cleanerHelper");
        ExpressionStmt helperCallStmt = new ExpressionStmt(helperCall);
        return helperCallStmt;
    }

    private JavaMethod addHelperMethod(JavaMethod cleanerMethod, JavaMethod methodToModify, boolean newTestClass, boolean prepend) throws Exception {
        // The modification is to modify the cleaner class to add a helper, then have the other method call the helper
        ExpressionStmt helperCallStmt = getHelperCallStmt(cleanerMethod, newTestClass);
        if (prepend) {
            methodToModify.prepend(NodeList.nodeList(helperCallStmt));
        } else {
            methodToModify.append(NodeList.nodeList(helperCallStmt));
        }
        methodToModify.javaFile().writeAndReloadCompilationUnit();

        String helperName = cleanerMethod.getClassName() + ".cleanerHelper";
        cleanerMethod = JavaMethod.find(cleanerMethod.methodName(), this.testSources, this.classpath).get();    // Reload, just in case
        cleanerMethod.javaFile().addMethod(helperName, "org.junit.Test");
        cleanerMethod.javaFile().writeAndReloadCompilationUnit();
        JavaMethod helperMethod = JavaMethod.find(helperName, this.testSources, this.classpath).get();
        helperMethod.javaFile().writeAndReloadCompilationUnit();

        methodToModify = JavaMethod.find(methodToModify.methodName(), this.testSources, this.classpath).get();   // Reload, just in case

        return helperMethod;
    }

    private FixStatus checkCleanerRemoval(List<String> failingOrder, JavaMethod cleanerMethod, NodeList<Statement> cleanerStmts) throws Exception {
        try {
            // Try to modify the cleanerMethod to remove the cleaner statements
            NodeList<Statement> allStatements = cleanerMethod.body().getStatements();
            NodeList<Statement> strippedStatements = NodeList.nodeList();
            NodeList<Statement> otherCleanerStmts = NodeList.nodeList(cleanerStmts);
            int j = 0;
            for (int i = 0; i < allStatements.size(); i++) {
                // Do not include the statement if we see it from the cleaner statements
                if (otherCleanerStmts.contains(allStatements.get(i))) {
                    otherCleanerStmts.remove(allStatements.get(i));
                } else {
                    strippedStatements.add(allStatements.get(i));
                }
            }

            // If the stripped statements is still the same as all statements, then the cleaner statements must all be in @Before/After
            if (strippedStatements.equals(allStatements)) {
                Logger.getGlobal().log(Level.INFO, "All cleaner statements must be in setup/teardown.");
                return FixStatus.FIX_INLINE_SETUPTEARDOWN;  // Indicating statements were in setup/teardown
            }

            // Set the cleaner method body to be the stripped version
            restore(cleanerMethod.javaFile());
            cleanerMethod = JavaMethod.find(cleanerMethod.methodName(), this.testSources, this.classpath).get();    // Reload, just in case
            cleanerMethod.method().setBody(new BlockStmt(strippedStatements));
            cleanerMethod.javaFile().writeAndReloadCompilationUnit();
            try {
                this.buildCommands.install();
            } catch (Exception ex) {
                Logger.getGlobal().log(Level.FINE, "Error building the code after stripping statements, does not compile");
                //// Restore the state
                //restore(cleanerMethod.javaFile());
                //cleanerMethod = JavaMethod.find(cleanerMethod.methodName(), this.testSources, this.classpath).get();    // Reload, just in case
                return FixStatus.NOD;   // Indicating did not work (TODO: Make it more clear)
            }
            // First try running in isolation
            List<String> isolationOrder = Collections.singletonList(cleanerMethod.methodName());
            if (!testOrderPasses(isolationOrder)) {
                Logger.getGlobal().log(Level.INFO, "Running cleaner by itself after removing statements does not pass.");
                //// Restore the state
                //restore(cleanerMethod.javaFile());
                //cleanerMethod = JavaMethod.find(cleanerMethod.methodName(), this.testSources, this.classpath).get();    // Reload, just in case
                return FixStatus.NOD;   // Indicating did not work (TODO: Make it more clear)
            }
            // Then try running with the failing order, replacing the last test with this one
            List<String> newFailingOrder = new ArrayList<>(failingOrder);
            newFailingOrder.remove(newFailingOrder.size() - 1);
            newFailingOrder.add(cleanerMethod.methodName());
            if (testOrderPasses(newFailingOrder)) {
                Logger.getGlobal().log(Level.INFO, "Running cleaner in failing order after polluter still passes.");
                //// Restore the state
                //restore(cleanerMethod.javaFile());
                //cleanerMethod = JavaMethod.find(cleanerMethod.methodName(), this.testSources, this.classpath).get();    // Reload, just in case
                return FixStatus.NOD;   // Indicating did not work (TODO: Make it more clear)
            }

            //// Restore the state
            //restore(cleanerMethod.javaFile());
            //cleanerMethod = JavaMethod.find(cleanerMethod.methodName(), this.testSources, this.classpath).get();    // Reload, just in case
            return FixStatus.FIX_INLINE_CANREMOVE;  // Indicating statements can be removed
        } finally {
            // Restore the state
            restore(cleanerMethod.javaFile());
            cleanerMethod = JavaMethod.find(cleanerMethod.methodName(), this.testSources, this.classpath).get();    // Reload, just in case
            this.buildCommands.install();
        }
    }

    // Make the cleaner statements based on the cleaner method and what method needs to be modified in the process
    private NodeList<Statement> makeCleanerStatements(JavaMethod cleanerMethod, JavaMethod methodToModify) throws Exception {
        // If the cleaner method is annotated such that it is expected to fail, then wrap in try catch
        boolean expected = false;
        for (AnnotationExpr annotExpr : cleanerMethod.method().getAnnotations()) {
            if (annotExpr instanceof NormalAnnotationExpr) {
                NormalAnnotationExpr normalAnnotExpr = (NormalAnnotationExpr) annotExpr;
                for (MemberValuePair memberValuePair : normalAnnotExpr.getPairs()) {
                    if (memberValuePair.getName().toString().equals("expected")) {
                        expected = true;
                        break;
                    }
                }
            }
        }

        boolean isSameTestClass = sameTestClass(cleanerMethod.methodName(), methodToModify.methodName());

        final NodeList<Statement> cleanerStmts = NodeList.nodeList();
        // Note: consider both standard imported version (e.g., @Before) and weird non-imported version (e.g., @org.junit.Before)
        // Only include BeforeClass and Before if in separate classes (for both victim and polluter(s))
        if (!isSameTestClass) {
            cleanerStmts.add(new BlockStmt(getCodeFromAnnotatedMethod(cleanerMethod.getClassName(), cleanerMethod.javaFile(), "@org.junit.BeforeClass")));
        }
        cleanerStmts.add(new BlockStmt(getCodeFromAnnotatedMethod(cleanerMethod.getClassName(), cleanerMethod.javaFile(), "@org.junit.Before")));
        if (!expected) {
            cleanerStmts.addAll(cleanerMethod.body().getStatements());
        } else {
            // Wrap the body inside a big try statement to suppress any exceptions
            ClassOrInterfaceType exceptionType = new ClassOrInterfaceType().setName(new SimpleName("Throwable"));
            CatchClause catchClause = new CatchClause(new Parameter(exceptionType, "ex"), new BlockStmt());
            cleanerStmts.add(new TryStmt(new BlockStmt(cleanerMethod.body().getStatements()), NodeList.nodeList(catchClause), new BlockStmt()));
        }
        // Only include AfterClass and After if in separate classes (for both victim and polluter(s))
        cleanerStmts.add(new BlockStmt(getCodeFromAnnotatedMethod(cleanerMethod.getClassName(), cleanerMethod.javaFile(), "@org.junit.After")));
        if (!isSameTestClass) {
            cleanerStmts.add(new BlockStmt(getCodeFromAnnotatedMethod(cleanerMethod.getClassName(), cleanerMethod.javaFile(), "@org.junit.AfterClass")));
        }

        return cleanerStmts;
    }

    private int statementsSize(NodeList<Statement> stmts) {
        int size = 0;
        for (Statement stmt : stmts) {
            // Take care of try statement block
            if (stmt instanceof TryStmt) {
                size += ((TryStmt) stmt).getTryBlock().getStatements().size();
            } else {
                size++;
            }
        }
        return size;
    }

    // Helper method to try out combinations of including all code from cleaner method to make tests pass
    private Object[] findValidMethodToModify(JavaMethod cleanerMethod, List<String> failingOrder,
                                             JavaMethod victimMethod, JavaMethod polluterMethod) throws Exception {
        Object[] returnValues = new Object[5];

        for (Boolean newTestClass : new boolean[]{true, false}) {
            for (ImmutablePair<JavaMethod, Boolean> tuple :
                    new ImmutablePair[]{ImmutablePair.of(victimMethod, true), ImmutablePair.of(polluterMethod, false)}) {
                JavaMethod methodToModify = tuple.getLeft();
                if (methodToModify == null) {   // When polluter is null, so brittle
                    continue;
                }
                boolean prepend = tuple.getRight();

                // Start with all cleaner statements, based on what method to modify
                final NodeList<Statement> cleanerStmts = NodeList.nodeList();
                cleanerStmts.addAll(makeCleanerStatements(cleanerMethod, methodToModify));

                // Get a reference to the setup/teardown method, where we want to add the call to the helper
                //JavaMethod auxiliaryMethodToModify = getAuxiliaryMethod(methodToModify, prepend);

                // Get the helper method reference
                //JavaMethod helperMethod = addHelperMethod(cleanerMethod, auxiliaryMethodToModify, newTestClass, prepend);
                JavaMethod helperMethod = addHelperMethod(cleanerMethod, methodToModify, newTestClass, prepend);

                // Check if applying these cleaners on the method suffices
                Logger.getGlobal().log(Level.INFO, "Applying code from cleaner and recompiling.");
                CleanerFixerDeltaDebugger debugger = new CleanerFixerDeltaDebugger(this.buildCommands, this.runner, helperMethod, failingOrder, prepend);
                if (debugger.checkValid(cleanerStmts, false)) {
                    returnValues[0] = methodToModify;
                    //returnValues[1] = auxiliaryMethodToModify;
                    returnValues[1] = methodToModify;
                    returnValues[2] = helperMethod;
                    returnValues[3] = cleanerStmts;
                    returnValues[4] = prepend;
                    return returnValues;
                }
                Logger.getGlobal().log(Level.SEVERE, "Applying all of cleaner " + cleanerMethod.methodName() + " to " + methodToModify.methodName() + " does not fix!");
                restore(methodToModify.javaFile());
                restore(helperMethod.javaFile());
                this.buildCommands.install();
            }
        }

        return returnValues;

    }

    // Returns if applying the fix was successful or not
    private PatchResult applyFix(final List<String> failingOrder,
                                 final List<String> fullFailingOrder,
                                 final JavaMethod polluterMethod,
                                 final JavaMethod cleanerMethod,
                                 final JavaMethod victimMethod,
                                 boolean prepend) throws Exception {
        // Back up the files we are going to modify
        if (polluterMethod != null) {
            backup(polluterMethod.javaFile());
        }
        backup(victimMethod.javaFile());
        backup(cleanerMethod.javaFile());

        // Get the starting form
        Object[] startingValues = findValidMethodToModify(cleanerMethod, failingOrder, victimMethod, polluterMethod);
        JavaMethod methodToModify = (JavaMethod)startingValues[0];
        if (methodToModify == null) {   // If not method returned, means things are broken
            // Restore files back to what they were before and recompile, in preparation for later
            if (polluterMethod != null) {
                restore(polluterMethod.javaFile());
            }
            restore(victimMethod.javaFile());
            restore(cleanerMethod.javaFile());
            this.buildCommands.install();
            NodeList<Statement> initialCleanerStmts = makeCleanerStatements(cleanerMethod, victimMethod);
            Path patch = writePatch(victimMethod, 0, new BlockStmt(initialCleanerStmts), statementsSize(initialCleanerStmts), null, cleanerMethod, polluterMethod, 0, "CLEANER DOES NOT FIX");
            return new PatchResult(OperationTime.instantaneous(), FixStatus.CLEANER_FAIL, victimMethod.methodName(), "N/A", cleanerMethod.methodName(), 0, patch.toString());
        }
        final JavaMethod auxiliaryMethodToModify = (JavaMethod)startingValues[1];
        final JavaMethod finalHelperMethod = (JavaMethod)startingValues[2];
        final NodeList<Statement> cleanerStmts = (NodeList<Statement>)startingValues[3];
        final boolean finalPrepend = ((Boolean)startingValues[4]).booleanValue();

        // Minimizing cleaner code, which includes setup and teardown
        Logger.getGlobal().log(Level.INFO, "Going to modify " + methodToModify.methodName() + " to make failing order pass.");
        final List<OperationTime> elapsedTime = new ArrayList<>();
        int originalsize = statementsSize(cleanerStmts);
        final CleanerFixerDeltaDebugger finalDebugger = new CleanerFixerDeltaDebugger(this.buildCommands, this.runner, finalHelperMethod, failingOrder, finalPrepend);
        final NodeList<Statement> minimalCleanerStmts = OperationTime.runOperation(() -> {
            // Cleaner is good, so now we can start delta debugging
            NodeList<Statement> interCleanerStmts = NodeList.nodeList(cleanerStmts);
            NodeList<Statement> currentInterCleanerStmts;
            do {
                currentInterCleanerStmts = NodeList.nodeList(interCleanerStmts);
                interCleanerStmts = NodeList.nodeList();
                interCleanerStmts.addAll(finalDebugger.deltaDebug(currentInterCleanerStmts, 2));

                // Debug each statement further if they contain blocks, so debug within statements in that block(s)
                interCleanerStmts = debugFurther(interCleanerStmts, finalHelperMethod, failingOrder, finalPrepend, interCleanerStmts);

                // "Unravel" any blocks and potentially debug some more
                NodeList<Statement> unraveledCleanerStmts = NodeList.nodeList();
                for (int i = 0; i < interCleanerStmts.size(); i++) {
                    Statement stmt = interCleanerStmts.get(i);
                    if (stmt instanceof BlockStmt) {
                        BlockStmt blockStmt = (BlockStmt)stmt;

                        // If block is empty, just move on
                        if (blockStmt.isEmpty()) {
                            continue;
                        }

                        // Try to take all statements from this block out and see if still works combined with others
                        NodeList<Statement> tmpStmts = NodeList.nodeList();
                        tmpStmts.addAll(unraveledCleanerStmts);
                        tmpStmts.addAll(blockStmt.getStatements());
                        for (int j = i + 1; j < interCleanerStmts.size(); j++) {
                            tmpStmts.add(interCleanerStmts.get(j));
                        }
                        // Check if unraveling this block combined with rest still works
                        if (finalDebugger.checkValid(tmpStmts)) {
                            unraveledCleanerStmts.addAll(blockStmt.getStatements());
                        } else {
                            unraveledCleanerStmts.add(blockStmt);
                        }
                    } else {
                        unraveledCleanerStmts.add(stmt);
                    }
                }
                interCleanerStmts = unraveledCleanerStmts;
            // Continually loop and try to minimize more, until reach fixpoint
            // Can end up minimizing more after unraveling blocks and such, revealing more opportunities to minimize
            } while(!interCleanerStmts.equals(currentInterCleanerStmts));

            return interCleanerStmts;
        }, (finalCleanerStmts, time) -> {
            elapsedTime.add(time);
            return finalCleanerStmts;
        });

        int iterations = finalDebugger.getIterations();

        BlockStmt patchedBlock = new BlockStmt(minimalCleanerStmts);

        // Check that the results are valid
        if (!finalDebugger.checkValid(minimalCleanerStmts, false)) {
            Logger.getGlobal().log(Level.INFO, "Final minimal is not actually working!");
            restore(methodToModify.javaFile());
            restore(finalHelperMethod.javaFile());
            this.buildCommands.install();
            Path patch = writePatch(victimMethod, 0, patchedBlock, originalsize, methodToModify, cleanerMethod, polluterMethod, elapsedTime.get(0).elapsedSeconds(), "BROKEN MINIMAL");
            return new PatchResult(elapsedTime.get(0), FixStatus.FIX_INVALID, victimMethod.methodName(), polluterMethod != null ? polluterMethod.methodName() : "N/A", cleanerMethod.methodName(), iterations, patch.toString());
        }

        // Try to inline these statements into the method
        restore(methodToModify.javaFile());
        methodToModify = JavaMethod.find(methodToModify.methodName(), this.testSources, this.classpath).get();   // Reload, just in case
        CleanerFixerDeltaDebugger debugger = new CleanerFixerDeltaDebugger(this.buildCommands, this.runner, methodToModify, failingOrder, finalPrepend);
        boolean inlineSuccessful = debugger.checkValid(minimalCleanerStmts, false);
        if (!inlineSuccessful) {
            Logger.getGlobal().log(Level.INFO, "Inlining patch into " + methodToModify.methodName() + " not good enough to run.");
        }

        // Do the check of removing cleaner statements from cleaner itself and see if the cleaner now starts failing
        Logger.getGlobal().log(Level.INFO, "Trying to remove statements from cleaner to see if it becomes order-dependent.");
        FixStatus removalCheck = checkCleanerRemoval(failingOrder, cleanerMethod, minimalCleanerStmts);

        // Figure out what the final fix status should be
        FixStatus fixStatus;
        String status;
        if (inlineSuccessful) {
            if (removalCheck == FixStatus.FIX_INLINE_SETUPTEARDOWN) {
                fixStatus = FixStatus.FIX_INLINE_SETUPTEARDOWN;
                status = "INLINE SUCCESSFUL SETUPTEARDOWN";
            } else if (removalCheck == FixStatus.FIX_INLINE_CANREMOVE) {
                fixStatus = FixStatus.FIX_INLINE_CANREMOVE;
                status = "INLINE SUCCESSFUL CANREMOVE";
            } else {
                fixStatus = FixStatus.FIX_INLINE;
                status = "INLINE SUCCESSFUL";
            }
        } else {
            if (removalCheck == FixStatus.FIX_INLINE_SETUPTEARDOWN) {
                fixStatus = FixStatus.FIX_NO_INLINE_SETUPTEARDOWN;
                status = "INLINE FAIL SETUPTEARDOWN";
            } else if (removalCheck == FixStatus.FIX_INLINE_CANREMOVE) {
                fixStatus = FixStatus.FIX_NO_INLINE_CANREMOVE;
                status = "INLINE FAIL CANREMOVE";
            } else {
                fixStatus = FixStatus.FIX_NO_INLINE;
                status = "INLINE FAIL";
            }
        }

        // Write out the changes in the form of a patch
        int startingLine;
        if (finalPrepend) {
            startingLine = methodToModify.beginLine() + 1;  // Shift one, do not include declaration line
        } else {
            startingLine = methodToModify.endLine() - 1;    // Shift one, patch starts before end of method
        }
        Path patchFile = writePatch(victimMethod, startingLine, patchedBlock, originalsize, methodToModify, cleanerMethod, polluterMethod, elapsedTime.get(0).elapsedSeconds(), status);

        patches.add(new Patch(methodToModify, patchedBlock, finalPrepend, cleanerMethod, victimMethod, this.testSources, this.classpath, inlineSuccessful));

        // Report successful patching, report where the patch is
        Logger.getGlobal().log(Level.INFO, "Patching successful, patch file for " + victimMethod.methodName() + " found at: " + patchFile);

        // Restore the original file
        restore(methodToModify.javaFile());
        restore(finalHelperMethod.javaFile());
        // Final compile to get state to right place
        this.buildCommands.install();

        return new PatchResult(elapsedTime.get(0), fixStatus, victimMethod.methodName(), polluterMethod != null ? polluterMethod.methodName() : "N/A", cleanerMethod.methodName(), iterations, patchFile.toString());
    }

    // Debug list of statements even further, if any statement contains blocks
    private NodeList<Statement> debugFurther(NodeList<Statement> stmts, JavaMethod helperMethod,
                                             List<String> failingOrder, boolean prepend, NodeList<Statement> stmtsToRun) {
        CleanerFixerBlockDeltaDebugger debugger;

        // Iterate through all statements and try to debug further if contain block
        for (int i = 0; i < stmts.size(); i++) {
            Statement stmt = stmts.get(i);

            if (stmt instanceof BlockStmt) {
                BlockStmt blockStmt = (BlockStmt)stmt;

                debugger = new CleanerFixerBlockDeltaDebugger(this.buildCommands, this.runner, helperMethod, failingOrder, prepend, blockStmt, stmtsToRun);
                NodeList<Statement> minimalBlockStmts = NodeList.nodeList();
                minimalBlockStmts.addAll(debugger.deltaDebug(blockStmt.getStatements(), 2));
                blockStmt.setStatements(minimalBlockStmts);

                // Debug further nested blocks
                minimalBlockStmts = debugFurther(minimalBlockStmts, helperMethod, failingOrder, prepend, stmtsToRun);
                blockStmt.setStatements(minimalBlockStmts);
            } else if (stmt instanceof TryStmt) {
                TryStmt tryStmt = (TryStmt)stmt;

                // Do the try block part
                debugger = new CleanerFixerBlockDeltaDebugger(this.buildCommands, this.runner, helperMethod, failingOrder, prepend, tryStmt.getTryBlock(), stmtsToRun);
                NodeList<Statement> minimalBlockStmts = NodeList.nodeList();
                minimalBlockStmts.addAll(debugger.deltaDebug(tryStmt.getTryBlock().getStatements(), 2));
                tryStmt.setTryBlock(new BlockStmt(minimalBlockStmts));

                // Debug further nested blocks
                minimalBlockStmts = debugFurther(minimalBlockStmts, helperMethod, failingOrder, prepend, stmtsToRun);
                tryStmt.setTryBlock(new BlockStmt(minimalBlockStmts));

                // If has finally block, do that
                if (tryStmt.getFinallyBlock().isPresent()) {
                    debugger = new CleanerFixerBlockDeltaDebugger(this.buildCommands, this.runner, helperMethod, failingOrder, prepend, tryStmt.getFinallyBlock().get(), stmtsToRun);
                    minimalBlockStmts = NodeList.nodeList();
                    minimalBlockStmts.addAll(debugger.deltaDebug(tryStmt.getFinallyBlock().get().getStatements(), 2));
                    tryStmt.setFinallyBlock(new BlockStmt(minimalBlockStmts));

                    // Debug further nested blocks
                    minimalBlockStmts = debugFurther(minimalBlockStmts, helperMethod, failingOrder, prepend, stmtsToRun);
                    tryStmt.setFinallyBlock(new BlockStmt(minimalBlockStmts));

                    // If the finally block is empty, remove
                    if (minimalBlockStmts.isEmpty()) {
                        tryStmt.removeFinallyBlock();
                    }
                }

                // Special case for try: see if we can remove the try and change just to a normal block
                // This can happen if minimized enough statements as to remove the ones that actually throw exceptions
                if (!tryStmt.getFinallyBlock().isPresent()) {   // For now, only do if finally block is not there
                    BlockStmt blockStmt = new BlockStmt();
                    blockStmt.setStatements(tryStmt.getTryBlock().getStatements());

                    // Manipulate list to add this block at that location and remove the try that got shifted next
                    stmts.add(i, blockStmt);
                    stmts.remove(i + 1);

                    // Use debugger to just check if things work with this block instead of try
                    debugger = new CleanerFixerBlockDeltaDebugger(this.buildCommands, this.runner, helperMethod, failingOrder, prepend, blockStmt, stmtsToRun);
                    if (!debugger.checkValid(blockStmt.getStatements())) {
                        // If invalid, we should set the try statement back in
                        stmts.add(i, tryStmt);
                        stmts.remove(i + 1);
                    }
                }
            }
        }

        return stmts;
    }

    // Helper method to create a patch file adding in the passed in block
    // Includes a bunch of extra information that may be useful
    private Path writePatch(JavaMethod victimMethod, int begin, BlockStmt blockStmt, int originalsize,
                            JavaMethod modifiedMethod, JavaMethod cleanerMethod,
                            JavaMethod polluterMethod,
                            double elapsedTime, String status) throws IOException {
        List<String> patchLines = new ArrayList<>();
        patchLines.add("STATUS: " + status);
        patchLines.add("MODIFIED: " + (modifiedMethod == null ? "N/A" : modifiedMethod.methodName()));
        patchLines.add("MODIFIED FILE: " + (modifiedMethod == null ? "N/A" : modifiedMethod.javaFile().path()));
        patchLines.add("CLEANER: " + (cleanerMethod == null ? "N/A" : cleanerMethod.methodName()));
        patchLines.add("CLEANER FILE: " + (cleanerMethod == null ? "N/A" : cleanerMethod.javaFile().path()));
        patchLines.add("POLLUTER: " + (polluterMethod == null ? "N/A" : polluterMethod.methodName()));
        patchLines.add("POLLUTER FILE: " + (polluterMethod == null ? "N/A" : polluterMethod.javaFile().path()));
        patchLines.add("ORIGINAL CLEANER SIZE: " + (originalsize == 0 ? "N/A" : String.valueOf(originalsize)));
        patchLines.add("NEW CLEANER SIZE: " + (blockStmt != null ? String.valueOf(statementsSize(blockStmt.getStatements())) : "N/A"));
        patchLines.add("ELAPSED TIME: " + elapsedTime);

        // If there is a block to add (where it might not be if in error state and need to just output empty)
        if (blockStmt != null) {
            patchLines.add(PATCH_LINE_SEP);
            String[] lines = blockStmt.toString().split("\n");
            patchLines.add("@@ -" + begin +",0 +" + begin + "," + lines.length + " @@");
            for (String line : lines) {
                patchLines.add("+ " + line);
            }
        }
        Path patchFile = PathManager.fixerPath().resolve(victimMethod.methodName() + ".patch");  // The patch file is based on the dependent test
        Files.createDirectories(patchFile.getParent());

        // If the file exists, then need to give it a new name
        if (Files.exists(patchFile)) {
            // Keep adding to a counter to make unique name
            Path newPatchFile;
            int counter = 1;
            while (true) {
                newPatchFile = Paths.get(patchFile + "." + counter);
                if (!Files.exists(newPatchFile)) {  // Found a valid file to write to
                    patchFile = newPatchFile;
                    break;
                }
                counter++;
            }
        }
        Files.write(patchFile, patchLines);
        return patchFile;
    }
}
