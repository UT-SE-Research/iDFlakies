package edu.illinois;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
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
        // int n = TuscanOnlyClassDetector.getClassesSize(tests);
        HashMap<String, List<String>> tempClassToMethods = generateClassToMethods(tests);
        int maxMethodSize = TuscanIntraClassDetector.findMaxMethodSize(tempClassToMethods);
        if (maxMethodSize == 3 || maxMethodSize == 5) {
            maxMethodSize++;
        }
        int rounds;
        int classSize = tempClassToMethods.keySet().size();
        if (classSize == 3 || classSize == 5) {
            classSize++;
        }
        if (classSize > maxMethodSize) {
            rounds = classSize;
        } else {
            rounds = maxMethodSize;
        }
        List<String> classes = new ArrayList<String>();
        LinkedHashMap<String, List<String>> classToMethods = new LinkedHashMap<String, List<String>>();
        List<List<String>> visitedMethodPairs = new ArrayList<List<String>>();
        Set<List<String>> allMethodPairs = new HashSet<List<String>>();
        for (int i = 0; i < rounds; i++) {
            List<String> currentOrder = testShuffler.tuscanIntraClassOrder(i);
            for (String test : currentOrder) {
                String className = TestShuffler.className(test);
                if (!classes.contains(className)) {
                    classes.add(className);
                }
                if (!classToMethods.containsKey(className)) {
                    classToMethods.put(className, new ArrayList<String>());
                }
                classToMethods.get(className).add(test);
            }
            List<List<String>> currentClassPairs = generateAllPairs(classes);
            for (int j = 0; j < classes.size() - 1; j++) {
                // Check if class pairs are covered
                List<String> newPair = new ArrayList<String>();
                newPair.add(classes.get(j));
                newPair.add(classes.get(j + 1));
                if (!currentClassPairs.contains(newPair)) {
                    throw new Exception("Not included Classes");
                }
            }
            for (String className : classes) {
                List<String> currentMethodOrdering = classToMethods.get(className);
                List<List<String>> currentMethodPairs = generateAllPairs(currentMethodOrdering);
                allMethodPairs.addAll(currentMethodPairs);
                for (int j = 0; j < currentMethodOrdering.size() - 1; j++) {
                    List<String> newPair = new ArrayList<String>();
                    newPair.add(currentMethodOrdering.get(j));
                    newPair.add(currentMethodOrdering.get(j + 1));
                    if(!currentMethodPairs.contains(newPair)) {
                        System.out.println(newPair);
                        throw new Exception("Not included Method Pairs");
                    }
                    if (!visitedMethodPairs.contains(newPair)) {
                        visitedMethodPairs.add(newPair);
                    }
                }
                currentMethodOrdering.clear();
            }
        }
        Assert.assertEquals(allMethodPairs.size(), visitedMethodPairs.size());
    }    

    public static HashMap<String, List<String>> generateClassToMethods (List<String> tests) {
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
