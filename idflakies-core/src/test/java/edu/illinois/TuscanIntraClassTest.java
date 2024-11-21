package edu.illinois;

import java.util.*;

import org.junit.Test;
import org.junit.Assert;

import edu.illinois.cs.dt.tools.detection.TestShuffler;
import edu.illinois.cs.dt.tools.detection.detectors.TuscanIntraClassDetector;

public class TuscanIntraClassTest {
    @Test
    public void test() throws Exception {
        String[] testArray = {
            "cn.edu.hfut.dmic.webcollector.util.CharsetDetectorTest.testGuessEncodingByMozilla",
            "cn.edu.hfut.dmic.webcollector.util.CharsetDetectorTest.testGuessEncoding",
            "cn.edu.hfut.dmic.webcollector.util.CrawlDatumTest.testKey",
            "cn.edu.hfut.dmic.webcollector.util.DBManagerTest.testBerkeleyDBInjector",
            "cn.edu.hfut.dmic.webcollector.util.DBManagerTest.testRocksDBInjector",
            "cn.edu.hfut.dmic.webcollector.util.CrawlDatumsTest.testAdd",
            "cn.edu.hfut.dmic.webcollector.util.CrawlDatumsTest.testAddAndReturn",
            "cn.edu.hfut.dmic.webcollector.util.MetaTest.testMetaSetterAndGetter",
            "cn.edu.hfut.dmic.webcollector.util.OkHttpRequesterTest.testHttpCode"
        };
        List<String> tests = Arrays.asList(testArray);
        TestShuffler testShuffler = new TestShuffler("", 0, tests, null);
        HashMap<String, List<String>> classToMethods = generateClassToMethods(tests);

        int maxMethodSize = TuscanIntraClassDetector.findMaxMethodSize(classToMethods);
        if (maxMethodSize == 3 || maxMethodSize == 5) {
            maxMethodSize++;
        }
        int rounds;
        int classSize = classToMethods.keySet().size();
        if (classSize == 3 || classSize == 5) {
            classSize++;
        }
        if (classSize > maxMethodSize) {
            rounds = classSize;
        } else {
            rounds = maxMethodSize;
        }

        // Each List inside a set is basically a pair 
        Set<List<String>> allClassPairs = generateAllPairs(tests, false);
        Set<List<String>> tuscanCoveredClassPairs = tuscanClassPairs(rounds, tests, testShuffler);

        Assert.assertEquals(allClassPairs, tuscanCoveredClassPairs);

        Set<List<String>> allMethodPairs = generateAllPairs(tests, true);
        Set<List<String>> tuscanCoveredMethodPairs = tuscanMethodPairs(rounds, tests, testShuffler);

        Assert.assertEquals(allMethodPairs, tuscanCoveredMethodPairs);
    }    

    public static HashMap<String, List<String>> generateClassToMethods(List<String> tests) {
        HashMap<String, List<String>> classToMethods = new HashMap<>();
        for (final String test : tests) {
            final String className = TestShuffler.className(test);
            if (!classToMethods.containsKey(className)) {
                classToMethods.put(className, new ArrayList<>());
            }
            classToMethods.get(className).add(test);
        }
        return classToMethods;
    }

    public static Set<List<String>> generateAllPairs(List<String> tests) {
        Set<List<String>> allPairs = new LinkedHashSet<List<String>>();
        for (int i = 0; i < tests.size(); i++) {
            for (int j = 0; j < tests.size(); j++) {
                if (!tests.get(i).equals(tests.get(j))) {
                    List<String> newPair = new ArrayList<String>();
                    newPair.add(tests.get(i));
                    newPair.add(tests.get(j));
                    allPairs.add(newPair);
                }
            }
        }
        return allPairs;
    }

    private static Set<List<String>> generateAllPairs(List<String> tests, boolean isMethods) {
        // Generates all pairs with a naive algorithm for comparison with tuscan-intra-class method
        Set<List<String>> allPairs = new LinkedHashSet<List<String>>();
        for (int i = 0; i < tests.size(); i++) {
            for (int j = 0; j < tests.size(); j++) {
                String className1 = TestShuffler.className(tests.get(i));
                String className2 = TestShuffler.className(tests.get(j));
                List<String> newPair = new ArrayList<String>();
                if (!isMethods && !className1.equals(className2)) {
                    newPair.add(className1);
                    newPair.add(className2);
                    allPairs.add(newPair);
                } else if (isMethods && className1.equals(className2) && !tests.get(i).equals(tests.get(j))) {
                    newPair.add(tests.get(i));
                    newPair.add(tests.get(j));
                    allPairs.add(newPair);
                }
            }
        }
        return allPairs;
    }

    private static Set<List<String>> tuscanClassPairs(int rounds, List<String> tests, TestShuffler testShuffler) {
        // Generates the class pairs using tuscan-intra-class algorithm
        Set<List<String>> visitedClassPairs = new LinkedHashSet<List<String>>();
        for (int i = 0; i < rounds; i++) {
            List<String> currentOrder = testShuffler.tuscanIntraClassOrder(i);
            for (int j = 0; j < currentOrder.size() - 1; j++) {
                String className1 = TestShuffler.className(currentOrder.get(j));
                String className2 = TestShuffler.className(currentOrder.get(j + 1));
                List<String> newPair = new ArrayList<String>();
                if (!className1.equals(className2)) {
                    newPair.add(className1);
                    newPair.add(className2);
                    visitedClassPairs.add(newPair);
                }
            }
        }
        return visitedClassPairs;
    }

    private static Set<List<String>> tuscanMethodPairs(int rounds, List<String> tests, TestShuffler testShuffler) {
        // Generates the intra-class method pairs using tuscan-intra-class algorithm
        Set<List<String>> visitedMethodPairs = new LinkedHashSet<List<String>>();
        for (int i = 0; i < rounds; i++) {
            List<String> currentOrder = testShuffler.tuscanIntraClassOrder(i);
            for (int j = 0; j < currentOrder.size() - 1; j++) {
                String class1 = TestShuffler.className(currentOrder.get(j));
                String class2 = TestShuffler.className(currentOrder.get(j + 1));
                if (class1.equals(class2)) {
                    List<String> newPair = new ArrayList<String>();
                    newPair.add(currentOrder.get(j));
                    newPair.add(currentOrder.get(j + 1));
                    visitedMethodPairs.add(newPair);
                }
            }
        }
        return visitedMethodPairs;
    }
}