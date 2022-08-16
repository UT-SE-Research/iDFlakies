package edu.illinois;

import java.util.*;

import javax.annotation.security.PermitAll;

import org.junit.Assert;
import org.junit.Test;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.illinois.cs.dt.tools.detection.TestShuffler;
import edu.illinois.cs.dt.tools.detection.detectors.TuscanInterClassDetector;

public class TuscanInterClassTest {
    @Test
    public void test() throws Exception {
        String[] testArray = { "cn.edu.hfut.dmic.webcollector.util.CharsetDetectorTest.testGuessEncodingByMozilla",
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
        Set<List<String>> visitedMethodPairs = new LinkedHashSet<List<String>>();
        Set<List<String>> finalPairsMethods= new LinkedHashSet<List<String>>();
        for (int i = 0; i < rounds; i++) {
            List<String> currentRoundPermutation = testShuffler.tuscanInterClass(i);
            List<String> permutatedClasses = new ArrayList<String>();
            HashMap<String, List<String>> classToPermutations = new HashMap<String, List<String>>();
            for (String testName : currentRoundPermutation) {
                String className = TestShuffler.className(testName);
                if (!permutatedClasses.contains(className)) {
                    permutatedClasses.add(className);
                }
                if (!classes.contains(className)) {
                    classes.add(className);
                }
                if (!classToPermutations.containsKey(className)) {
                    classToPermutations.put(className, new ArrayList<>());
                }
                classToPermutations.get(className).add(testName);
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
            // Now we will explore the inter-class method pairs
            for (int j = 0; j < permutatedClasses.size() - 1; j++) {
                String currentClass = permutatedClasses.get(j);
                List<String> currentMethods = classToPermutations.get(currentClass);
                String lastMethod = currentMethods.get(currentMethods.size() - 1);
                String nextClass = permutatedClasses.get(j + 1);
                List<String> nextMethods = classToPermutations.get(nextClass);
                String firstMethod = nextMethods.get(0);

                List<List<String>> allMethodPairs = generateAllMethodPairs(currentMethods, nextMethods);

                List<String> newPair = new ArrayList<String>();
                newPair.add(lastMethod);
                newPair.add(firstMethod);
                if (!allMethodPairs.contains(newPair)) {
                    System.out.println(newPair);
                    throw new Exception("Doents contain all method paris");
                }
                if (!visitedMethodPairs.contains(newPair)) {
                    visitedMethodPairs.add(newPair);
                }
                finalPairsMethods.addAll(allMethodPairs);
            }
        }
        Assert.assertEquals(finalPairs, visitedClassPairs);
        Assert.assertEquals(finalPairsMethods, visitedMethodPairs);
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

    public static List<List<String>> generateAllMethodPairs(List<String> currentMethods, List<String> nextMethods) {
        List<List<String>> allPairs = new ArrayList<>();
        for (int i = 0; i < currentMethods.size(); i++) {
            for (int j = 0; j < nextMethods.size(); j++) {
                List<String> newPair = new ArrayList<String>();
                newPair.add(currentMethods.get(i));
                newPair.add(nextMethods.get(j));
                allPairs.add(newPair);
            }
        }
        return allPairs;
    }
}