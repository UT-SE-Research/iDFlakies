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
        // System.out.println(tests);
        TestShuffler testShuffler = new TestShuffler("", 0, tests, null);
        int n = TuscanOnlyClassDetector.getClassesSize(tests);
        int rounds = n;
        if (n == 3 || n == 5) {
            rounds++;
        }
        Set<List<String>> finalPairs = new LinkedHashSet<List<String>>();
        Set<List<String>> visitedPairs = new LinkedHashSet<List<String>>();

        for (int i = 0; i < rounds; i++) {
            List<String> currentRoundPermutation = testShuffler.alphabeticalAndTuscanOrder(i, true);
            List<String> permutatedClasses = new ArrayList<String>();
            for (String test : currentRoundPermutation) {
                String className = TestShuffler.className(test);
                // classes.add(TestShuffler.className(currentOrder.get(j)));
                if (!permutatedClasses.contains(className)) {
                    permutatedClasses.add(className);
                }
            }
            List<List<String>> allPairs = generateAllPairs(permutatedClasses);
            for (int j = 0; j < permutatedClasses.size() - 1; j++) {
                List<String> newPair = new ArrayList<String>();
                newPair.add(permutatedClasses.get(j));
                newPair.add(permutatedClasses.get(j + 1));
                if (!allPairs.contains(newPair)) {
                    throw new Exception("Class pair is not covered");
                }
                visitedPairs.add(newPair);
            }
            finalPairs.addAll(allPairs);
        }
        int count = finalPairs.size();
        Assert.assertEquals(count, n * (n - 1));
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
