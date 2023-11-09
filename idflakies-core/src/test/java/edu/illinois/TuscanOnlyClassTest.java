package edu.illinois;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.illinois.cs.dt.tools.detection.TestShuffler;
import edu.illinois.cs.dt.tools.detection.detectors.TuscanOnlyClassDetector;

public class TuscanOnlyClassTest {
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
        int n = TuscanOnlyClassDetector.getClassesSize(tests);
        int rounds = n;
        if (n == 3 || n == 5) {
            rounds++;
        }

        // Each List inside a set is basically a pair 
        Set<List<String>> allClassPairs = generateAllPairs(tests);
        Set<List<String>> tuscanCoveredClassPairs = tuscanOnlyClassPairs(rounds, tests, testShuffler);

        Assert.assertEquals(allClassPairs, tuscanCoveredClassPairs);
    }    

    public static Set<List<String>> generateAllPairs(List<String> tests) {
        // Generates all pairs using a naive algorithm for comparison with tuscan-only-class method
        Set<List<String>> allPairs = new LinkedHashSet<List<String>>();
        for (int i = 0; i < tests.size(); i++) {
            for (int j = 0; j < tests.size(); j++) {
                String className1 = TestShuffler.className(tests.get(i));
                String className2 = TestShuffler.className(tests.get(j));
                if (!className1.equals(className2)) {
                    List<String> newPair = new ArrayList<String>();
                    newPair.add(className1);
                    newPair.add(className2);
                    allPairs.add(newPair);
                }
            }
        }
        return allPairs;
    }

    private static Set<List<String>> tuscanOnlyClassPairs(int rounds, List<String> tests, TestShuffler testShuffler) {
        // Generates all class pairs using tuscan-only-class method
        Set<List<String>> visitedClassPairs = new LinkedHashSet<List<String>>();
        for (int i = 0; i < rounds; i++) {
            List<String> currentOrder = testShuffler.alphabeticalAndTuscanOrder(i, true);
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
}
