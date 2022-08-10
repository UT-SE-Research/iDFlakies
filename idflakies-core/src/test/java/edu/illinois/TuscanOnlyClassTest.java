package edu.illinois;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.illinois.cs.dt.tools.detection.TestShuffler;
import edu.illinois.cs.dt.tools.detection.detectors.TuscanOnlyClassDetector;
import scala.collection.mutable.ArrayBuilder.ofBoolean;

public class TuscanOnlyClassTest {
    @Test
    public void test() throws Exception {
        String[] testArray = {"cn.edu.hfut.dmic.webcollector.util.CharsetDetectorTest.testGuessEncodingByMozilla",
        "cn.edu.hfut.dmic.webcollector.util.CharsetDetectorTest.testGuessEncoding",
        "cn.edu.hfut.dmic.webcollector.util.CrawlDatumTest.testKey",
        "cn.edu.hfut.dmic.webcollector.util.DBManagerTest.testBerkeleyDBInjector",
        "cn.edu.hfut.dmic.webcollector.util.DBManagerTest.testRocksDBInjector",
        "cn.edu.hfut.dmic.webcollector.util.CrawlDatumsTest.testAdd",
        "cn.edu.hfut.dmic.webcollector.util.CrawlDatumsTest.testAddAndReturn",
        "cn.edu.hfut.dmic.webcollector.util.MetaTest.testMetaSetterAndGetter",
        "cn.edu.hfut.dmic.webcollector.util.OkHttpRequesterTest.testHttpCode"};
        List<String> tests = Arrays.asList(testArray);
        // System.out.println(tests);
        TestShuffler testShuffler = new TestShuffler("", 0, tests, null);
        int n = TuscanOnlyClassDetector.getClassesSize(tests);
        int rounds = n;
        if (n == 3 || n == 5) {
            rounds++;
        }
        List<Pair> listOfPairs = new ArrayList<Pair>();
        for (int i = 0; i < rounds; i++) {
            List<String> currentOrder = testShuffler.alphabeticalAndTuscanOrder(i, true);
            Set<String> classes = new LinkedHashSet<String>();
            // System.out.println("currentORder.size()" + currentOrder.size());
            for (int j = 0; j < currentOrder.size(); j++) {
                classes.add(TestShuffler.className(currentOrder.get(j)));
            }
            // System.out.println("classes.size()" + classes.size());
            String[] classesArray = classes.toArray(new String[classes.size()]);
            for (int j = 0; j < classesArray.length - 1; j++) {
                Pair newPair = new Pair(classesArray[j], classesArray[j + 1]);
                if (listOfPairs.contains(newPair)) {
                    throw new Exception("new");
                }
                listOfPairs.add(newPair);
            }
        }
        int count = listOfPairs.size();
        if (n == 5) {
            Assert.assertEquals(count, 21);
        } else {
            Assert.assertEquals(count, n * (n - 1));
        }
    }    
}