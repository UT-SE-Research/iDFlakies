package edu.illinois;

import java.util.*;

import org.junit.Test;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.illinois.cs.dt.tools.detection.TestShuffler;
import edu.illinois.cs.dt.tools.detection.detectors.TuscanInterClassDetector;


public class TuscanInterClassTest {
    @Test
    public void test() throws Exception {
        String[] testArray = {"cn.edu.hfut.dmic.webcollector.util.CharsetDetectorTest.testGuessEncodingByMozilla",
        "cn.edu.hfut.dmic.webcollector.util.CharsetDetectorTest.testGuessEncoding",
        "cn.edu.hfut.dmic.webcollector.util.CrawlDatumTest.testKey",
        "cn.edu.hfut.dmic.webcollector.util.CrawlDatumTest.testKey1",
        "cn.edu.hfut.dmic.webcollector.util.DBManagerTest.testBerkeleyDBInjector",
        "cn.edu.hfut.dmic.webcollector.util.DBManagerTest.testRocksDBInjector",
        "cn.edu.hfut.dmic.webcollector.util.CrawlDatumsTest.testAdd",
        "cn.edu.hfut.dmic.webcollector.util.CrawlDatumsTest.testAddAndReturn",
        "cn.edu.hfut.dmic.webcollector.util.MetaTest.testMetaSetterAndGetter",
        "cn.edu.hfut.dmic.webcollector.util.MetaTest.testMock1",
        "cn.edu.hfut.dmic.webcollector.util.OkHttpRequesterTest.testHttpCode"};
        List<String> tests = Arrays.asList(testArray);
        HashMap<String, List<String>> tempClassToMethods = TuscanIntraClassTest.generateClassToMethods(tests);
        int rounds = TuscanInterClassDetector.findNumberOfRounds(tempClassToMethods);
        TestShuffler testShuffler = new TestShuffler("", 0, tests, null);
        List<String> classes = new ArrayList<String>();
        Set<List<String>> finalPairs = new LinkedHashSet<List<String>>();
        Set<List<String>> visitedClassPairs = new LinkedHashSet<List<String>>();
        for (int i = 0; i < rounds; i++) {
           List<String> currentRoundPermutation = testShuffler.tuscanInterClass(i);
           List<String> permutatedClasses = new ArrayList<String>();
           for (String testName : currentRoundPermutation) {
                String className = TestShuffler.className(testName);
                if (!permutatedClasses.contains(className)) {
                    permutatedClasses.add(className);
                }
                if (!classes.contains(className)) {
                    classes.add(className);
                }
           }
           List<List<String>> allClassPairs = generateAllPairs(classes);
           for (int j = 0; j < permutatedClasses.size() - 1; j++) {
                List<String> newPair = new ArrayList<String>();
                newPair.add(permutatedClasses.get(j));
                newPair.add(permutatedClasses.get(j + 1));
                // System.out.println(newPair);
                if (!(allClassPairs.contains(newPair))) {
                    throw new Exception("Not included pair");
                }
                if (!visitedClassPairs.contains(newPair)) {
                    visitedClassPairs.add(newPair);
                }
           }
           finalPairs.addAll(allClassPairs);
        }
        System.out.println(finalPairs.size());
        System.out.println(visitedClassPairs.size());
        if (finalPairs.size() != visitedClassPairs.size()) {
            printDifference(visitedClassPairs, finalPairs);
        }
    }

    public static void printDifference(Set<List<String>> visited, Set<List<String>> expected) {
        System.out.println("Difference: ");
        for (List<String> item : expected) {
            if (expected.contains(item) && !visited.contains(item)) {
                System.out.println(item);
            }
        }
    }

    public static List<List<String>> generateAllPairs(List<String> tests) {
        List<List<String>> allPairs = new ArrayList<>();
        for (int i = 0; i < tests.size(); i++) {
            for (int j = 0; j < tests.size(); j++) {
                if (tests.get(i) != tests.get(j)) {
                    List<String> newPair = new ArrayList<String>();
                    newPair.add(tests.get(i));
                    newPair.add(tests.get(j));
                    allPairs.add(newPair);
                }
            }
        }
        return allPairs;
    }
}