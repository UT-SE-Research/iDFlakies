package edu.illinois.cs.dt.tools.detection;

import com.google.common.collect.Lists;
import com.google.common.math.IntMath;
import com.google.gson.Gson;
import com.reedoei.eunomia.collections.ListUtil;
import com.reedoei.eunomia.io.files.FileUtil;
import edu.illinois.cs.dt.tools.runner.RunnerPathManager;
import edu.illinois.cs.dt.tools.utility.Level;
import edu.illinois.cs.dt.tools.utility.Logger;
import edu.illinois.cs.dt.tools.utility.MD5;
import edu.illinois.cs.dt.tools.utility.PathManager;
import edu.illinois.cs.dt.tools.utility.Tuscan;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.data.results.TestRunResult;

import static org.junit.Assert.assertNotSame;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.InterceptionType;

public class TestShuffler {
    public static String className(final String testName) {
        return testName.substring(0, testName.lastIndexOf('.'));
    }

    private final HashMap<String, List<String>> classToMethods;
    private final String type;
    private final List<String> tests;
    private final Set<String> alreadySeenOrders = new HashSet<>();
    private final File baseDir;
    // For tuscanInterClass
    private static int interClassRound = 0; // which class permutation to choose
    private static int interCurrentMethodRound = 0; // first class of pair
    private static int interNextMethodRound = 0; // second class of pair
    private static int i1 = 0; // current class
    private static int i2 = 1; // next class
    private static boolean isNewOrdering = false; // To change the permutation of classes

    private final Random random;

    public TestShuffler(final String type, final int rounds, final List<String> tests, final File baseDir) {
        this.type = type;
        this.tests = tests;
        this.baseDir = baseDir;

        classToMethods = new HashMap<>();

        for (final String test : tests) {
            final String className = className(test);

            if (!classToMethods.containsKey(className)) {
                classToMethods.put(className, new ArrayList<>());
            }

            classToMethods.get(className).add(test);
        }

        // Set up Random instance using passed in seed, if available
        int seed = 42;
        try {
            seed = Integer.parseInt(Configuration.config().getProperty("dt.seed", "42"));
        } catch (NumberFormatException nfe) {
            Logger.getGlobal().log(Level.INFO, "dt.seed needs to be an integer, using default seed " + seed);
        }
        this.random = new Random(seed);
    }

    private String historicalType() {
        if (type.equals("random")) {
            return Configuration.config().getProperty("detector.random.historical_type", "random-class");
        } else {
            return Configuration.config().getProperty("detector.random.historical_type", "random");
        }
    }

    public List<String> shuffledOrder(final int i,
                                      final TestRunResult lastRandomResult,
                                      final boolean useRevPassing) {
        if (useRevPassing) {
            return shuffledOrder(i);
        } else {
            List<String> revPassingOrder = Lists.reverse(lastRandomResult.testOrder());
            String md5 = MD5.md5(String.join("", revPassingOrder));
            if (alreadySeenOrders.contains(md5)) {
                return shuffledOrder(i);
            } else {
                alreadySeenOrders.add(md5);
                return revPassingOrder;
            }
        }
    }

    public List<String> shuffledOrder(final int i) {
        if (type.startsWith("reverse")) {
            return reverseOrder();
        }

        final Path historicalRun = PathManager.detectionRoundPath(historicalType(), i);

        try {
            // look up whether a previous execution of the plugin generated orders for this round already
            // if so, then run the same revealed order as before
            if (Files.exists(historicalRun)) {
                return generateHistorical(readHistorical(historicalRun));
            }
        } catch (IOException ignored) {}

        return generateShuffled();
    }

    private List<String> reverseOrder() {
        if ("reverse-class".equals(type)) {
            final List<String> reversedClassNames =
                    Lists.reverse(ListUtil.map(TestShuffler::className, tests).stream().distinct().collect(Collectors.toList()));

            return reversedClassNames.stream().flatMap(c -> classToMethods.get(c).stream()).collect(Collectors.toList());
        } else {
            return Lists.reverse(tests);
        }
    }

    private List<String> readHistorical(final Path historicalRun) throws IOException {
        final DetectionRound detectionRound = new Gson().fromJson(FileUtil.readFile(historicalRun), DetectionRound.class);

        return detectionRound.testRunIds().stream()
                .flatMap(n -> RunnerPathManager.resultFor(n))
                .findFirst()
                .map(TestRunResult::testOrder)
                .orElse(new ArrayList<>());
    }

    private List<String> generateHistorical(final List<String> historicalOrder) {
        if ("random-class".equals(type)) {
            return generateWithClassOrder(classOrder(historicalOrder));
        } else {
            alreadySeenOrders.add(MD5.md5(String.join("", historicalOrder)));
            return historicalOrder;
        }
    }

    private List<String> generateShuffled() {
        // sort the classes alphabetically, then shuffle as to ensure deterministic randomness
        List<String> classes = new ArrayList<>(classToMethods.keySet());
        Collections.sort(classes);
        Collections.shuffle(classes, random);
        return generateWithClassOrder(classes);
    }

    private List<String> generateWithClassOrder(final List<String> classOrder) {
        final List<String> fullTestOrder = new ArrayList<>();

        for (final String className : classOrder) {
            // random-class only shuffles classes, not methods
            if ("random-class".equals(type)) {
                fullTestOrder.addAll(classToMethods.get(className));
            } else {
                // the standard "random" type, will shuffle both
                // sort the methods alphabetically, then shuffle as to ensure deterministic randomness
                List<String> methods = classToMethods.get(className);
                Collections.sort(methods);
                Collections.shuffle(methods, random);
                fullTestOrder.addAll(methods);
            }
        }

        alreadySeenOrders.add(MD5.md5(String.join("", fullTestOrder)));

        return fullTestOrder;
    }

    private List<String> classOrder(final List<String> historicalOrder) {
        return historicalOrder.stream().map(TestShuffler::className).distinct().collect(Collectors.toList());
    }

    @Deprecated
    private int permutations(final int rounds) {
        return permutations(IntMath.factorial(classToMethods.keySet().size()), classToMethods.values().iterator(), rounds);
    }

    @Deprecated
    private int permutations(final int accum, final Iterator<List<String>> iterator, final int rounds) {
        if (accum > rounds) {
            return accum;
        } else {
            if (iterator.hasNext()) {
                final List<String> testsInMethod = iterator.next();

                return permutations(accum * IntMath.factorial(testsInMethod.size()), iterator, rounds);
            } else {
                return accum;
            }
        }
    }

    public List<String> alphabeticalAndTuscanOrder(int count, boolean isTuscan) {
        List<String> classes = new ArrayList<>(classToMethods.keySet());
        Collections.sort(classes);
        final List<String> fullTestOrder = new ArrayList<>();
        if (isTuscan) {
            int n = classes.size();
            int[][] res = Tuscan.generateTuscanPermutations(n);
            List<String> permClasses = new ArrayList<String>();
            for (int i = 0; i < res[count].length - 1; i++) {
                permClasses.add(classes.get(res[count][i]));
            }
            for (String className : permClasses) {
                fullTestOrder.addAll(classToMethods.get(className));
            }
        } else {
            for (String className : classes) {
                fullTestOrder.addAll(classToMethods.get(className));
            }
        }
        return fullTestOrder;
    }

    public List<String> tuscanIntraClassOrder(int round) {
        List<String> classes = new ArrayList<>(classToMethods.keySet());
        HashMap<String, int[][]> classToPermutations = new HashMap<String, int[][]>();
        Collections.sort(classes);
        final List<String> fullTestOrder = new ArrayList<>();
        int n = classes.size(); // n is number of classes
        int[][] classOrdering = Tuscan.generateTuscanPermutations(n);
        for (String className : classes) {
            if (classToMethods.get(className).size() == 1) {
                int[][] methodPermuation = { 
                    { 0, 0 }
                };
                classToPermutations.put(className, methodPermuation);
            } else {
                int[][] methodPermuation = Tuscan.generateTuscanPermutations(classToMethods.get(className).size());
                classToPermutations.put(className, methodPermuation);
            }
        }
        HashMap<String, List<String>> newClassToMethods = new HashMap<String, List<String>>();
        List<String> permClasses = new ArrayList<String>();
        int classRound = round;
        while ((classOrdering.length - 1) < classRound) {
            classRound -= classOrdering.length;
        }
        for (int i = 0; i < classOrdering[classRound].length - 1; i++) { // {0, 1, 0} {1, 0, 0}
            permClasses.add(classes.get(classOrdering[classRound][i]));
        }
        for (String className : permClasses) {
            List<String> methods = classToMethods.get(className);
            List<String> permMethods = new ArrayList<String>();
            int[][] currMethodOrdering = classToPermutations.get(className);
            n = methods.size();
            int methodRound = round;
            while((currMethodOrdering.length - 1) < methodRound) {
                methodRound -= currMethodOrdering.length;
            }
            for (int i = 0; i < currMethodOrdering[methodRound].length - 1; i++) {
                permMethods.add(methods.get(currMethodOrdering[methodRound][i]));
            }
            newClassToMethods.put(className, permMethods);
        }
        for (String className : permClasses) {
            fullTestOrder.addAll(newClassToMethods.get(className));
        }
        return fullTestOrder;
    }
    
    public List<String> tuscanInterClass(int round) {
        List<String> classes = new ArrayList<>(classToMethods.keySet());
        HashMap<String, int[][]> classToPermutations = new HashMap<String, int[][]>();
        Collections.sort(classes);
        final List<String> fullTestOrder = new ArrayList<>();
        int n = classes.size(); // n is number of classes
        int[][] classOrdering = Tuscan.generateTuscanPermutations(n);

        for (String className : classes) {
            int methodSize = classToMethods.get(className).size();
            int[][] result;
            if (methodSize == 3) {
                int[][] methodPermuation = {
                    { 0, 1, 2, 0 },
                    { 1, 2, 0, 0 },
                    { 2, 0, 1, 0 },
                };
                result = methodPermuation;
            } else if (methodSize == 5) {
                int[][] methodPermuation = {
                    { 0, 1, 2, 3, 4, 0 },
                    { 1, 0, 2, 4, 3, 0 },
                    { 2, 4, 0, 3, 1, 0 },
                    { 3, 1, 0, 4, 2, 0 },
                    { 4, 1, 2, 3, 0, 0 },
                };
                result = methodPermuation;
            } else {
                int[][] methodPermuation = Tuscan.generateTuscanPermutations(methodSize);
                result = methodPermuation;
            }
            classToPermutations.put(className, result);
        }
        HashMap<String, List<String>> newClassToMethods = new HashMap<String, List<String>>(); // class to permutated methods
        List<String> permClasses = new ArrayList<String>();
        if (isNewOrdering) {
            // When we reach end of a permutation for classes only
            i1 = 0;
            i2 = 1;
            interNextMethodRound = 0;
            interCurrentMethodRound = 0;
            interClassRound++;
            isNewOrdering = false;
        }
        for (int i = 0; i < classOrdering[interClassRound].length - 1; i++) {
            permClasses.add(classes.get(classOrdering[interClassRound][i]));
        }
        String currentClass = permClasses.get(i1), nextClass = permClasses.get(i2);
        int currentClassMethodSize = classToMethods.get(currentClass).size();
        int nextClassMethodSize = classToMethods.get(nextClass).size();
        if (currentClassMethodSize == interCurrentMethodRound && nextClassMethodSize == (interNextMethodRound + 1)) {
            // To change the pair so we change i1 & i2
            i1++;
            i2++;
            interNextMethodRound = 0;
            interCurrentMethodRound = 0;
        }
        else if (currentClassMethodSize == (interCurrentMethodRound)) {
            // To change the *next* class methods
            interNextMethodRound++;
            interCurrentMethodRound = 0;
        }
        int[] currentClassTuscan = classToPermutations.get(currentClass)[interCurrentMethodRound];
        int[] nextClassTuscan = classToPermutations.get(nextClass)[interNextMethodRound];
        for (String className : permClasses) {
            List<String> methods = classToMethods.get(className);
            List<String> permMethods = new ArrayList<String>();
            if (className == currentClass) {
                for (int i = 0; i < currentClassTuscan.length - 1; i++) {
                    permMethods.add(methods.get(currentClassTuscan[i]));
                }
            }
            else if (className == nextClass) {
                for (int i = 0; i < nextClassTuscan.length - 1; i++) {
                    permMethods.add(methods.get(nextClassTuscan[i]));
                }
            } else {
                // We don't care about this classes permutations yet
                for (int i = 0; i < nextClassTuscan.length; i++) {
                    permMethods = methods;
                }
            }
            newClassToMethods.put(className, permMethods);
        }
        for (String className : permClasses) {
            fullTestOrder.addAll(newClassToMethods.get(className));
        }
        interCurrentMethodRound++;
        if (nextClass == permClasses.get(permClasses.size() - 1) && currentClassMethodSize == interCurrentMethodRound && nextClassMethodSize == (interNextMethodRound + 1)) {
            // if the *next class* is our last class then there is no pair so change to the next order
            isNewOrdering = true;
        }
        return fullTestOrder;
    }
}