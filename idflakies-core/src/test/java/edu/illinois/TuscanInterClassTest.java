package edu.illinois;

import java.util.ArrayList;
import java.util.List;
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
        "cn.edu.hfut.dmic.webcollector.util.DBManagerTest.testRocksDBInjector"};
        // "cn.edu.hfut.dmic.webcollector.util.CrawlDatumsTest.testAdd",
        // "cn.edu.hfut.dmic.webcollector.util.CrawlDatumsTest.testAddAndReturn",
        // "cn.edu.hfut.dmic.webcollector.util.MetaTest.testMetaSetterAndGetter",
        // "cn.edu.hfut.dmic.webcollector.util.OkHttpRequesterTest.testHttpCode"};
        List<String> tests = Arrays.asList(testArray);
        HashMap<String, List<String>> tempClassToMethods = TuscanIntraClassTest.generateClassToMethods(tests);
        int rounds = TuscanInterClassDetector.findNumberOfRounds(tempClassToMethods);
        TestShuffler testShuffler = new TestShuffler("", 0, tests, null);
        // int n = TuscanOnlyClassDetector.getClassesSize(tests);
        List<String> tempClasses = new ArrayList<String>(tempClassToMethods.keySet());
        // System.out.println(classes);
        LinkedHashMap<String, List<String>> classToMethods = new LinkedHashMap<String, List<String>>();
        List<Pair> allClassPairs = TuscanIntraClassTest.generateAllPairs(tempClasses);
        List<Pair> allMethodPairs = new ArrayList<Pair>();
        List<Pair> visitedMethodPairs = new ArrayList<Pair>();
        List<Pair> visitedClassPairs = new ArrayList<Pair>();
        List<String> classes = new ArrayList<String>();
        System.out.println("Rounds: " + rounds);
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
            // List<Pair> currentClassPairs = TuscanIntraClassTest.generateAllPairs(classes);
            // allClassPairs.addAll(currentClassPairs);
            for (int j = 0; j < classes.size() - 1; j++) {
                // Check if class pairs are covered
                Pair newPair = new Pair(classes.get(j), classes.get(j + 1));
                if (!allClassPairs.contains(newPair)) {
                    // System.out.println("1" + allClassPairs.get(2) + "1");
                    // if (allClassPairs.get(2).equals(newPair)) {
                    //     System.out.println("HELLO");
                    // } else {
                    //     System.out.println("NO");
                    // }
                    // System.out.println("NEWPAIR " + newPair);
                    // System.out.println("1" + newPair + "1");
                    // throw new Exception("Not included Classes");
                    System.out.println(newPair);
                    System.out.println(allClassPairs);
                    System.out.println("Not included classes");
                }
                if (!visitedClassPairs.contains(newPair)) {
                    visitedClassPairs.add(newPair);
                }
            }
            for (String className : classes) {
                List<String> currentMethodOrdering = classToMethods.get(className);
                List<Pair> currentMethodPairs = TuscanIntraClassTest.generateAllPairs(currentMethodOrdering);
                allMethodPairs.addAll(currentMethodPairs);
                // System.out.println(currentMethodPairs.size());
                for (int j = 0; j < currentMethodOrdering.size() - 1; j++) {
                    Pair newPair = new Pair(currentMethodOrdering.get(j), currentMethodOrdering.get(j + 1));
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
            System.out.println(allClassPairs.size());
            System.out.println(visitedClassPairs.size());
            System.out.println(visitedClassPairs.size());
    }
}