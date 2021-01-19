package edu.illinois.cs.dt.tools.detection;

import com.google.common.collect.Lists;
import com.google.common.math.IntMath;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.reedoei.eunomia.collections.ListUtil;
import com.reedoei.eunomia.collections.RandomList;
import com.reedoei.eunomia.io.files.FileUtil;
import edu.illinois.cs.dt.tools.runner.RunnerPathManager;
import edu.illinois.cs.dt.tools.utility.MD5;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.data.results.TestRunResult;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TestShuffler {
    public static String className(final String testName) {
        return testName.substring(0, testName.lastIndexOf('.'));
    }

    private final HashMap<String, List<String>> classToMethods;

    private final String type;
    private final List<String> tests;
    private Set<String> alreadySeenOrders = new HashSet<>();
    private int roundsRemaining;

    public TestShuffler(final String type, final int rounds, final List<String> tests) {
        this.type = type;
        this.tests = tests;
        this.roundsRemaining = rounds;

        classToMethods = new HashMap<>();

        for (final String test : tests) {
            final String className = className(test);

            if (!classToMethods.containsKey(className)) {
                classToMethods.put(className, new ArrayList<>());
            }

            classToMethods.get(className).add(test);
        }

        //Load all previous orders into alreadySeenOrders
        try {
            JsonReader getLocalJsonFile = new JsonReader(new FileReader(DetectorPathManager.previousOrders().toString()));
            Type mapTokenType = new TypeToken<Set<String>>(){}.getType();
            Set<String> jsonMapOrders = new Gson().fromJson(getLocalJsonFile, mapTokenType);
            if(jsonMapOrders != null) {
                alreadySeenOrders = jsonMapOrders;
            }
        } catch (FileNotFoundException e) {
            System.out.println("No previous orders tracked");
        }
    }

    private String historicalType() {
        if (type.equals("random")) {
            return Configuration.config().getProperty("detector.random.historical_type", "random-class");
        } else {

            return Configuration.config().getProperty("detector.random.historical_type", "random");
        }
    }

    public List<String> shuffledOrder(final int i) {
        if (type.startsWith("reverse")) {
            return reverseOrder();
        }

        final Path historicalRun = DetectorPathManager.detectionRoundPath(historicalType(), i);

        try {
            if (Files.exists(historicalRun)) {
                    return generateHistorical(readHistorical(historicalRun));
            }
        } catch (IOException ignored) {}

        return generateShuffled();
    }

    public long calculatePermutations() {
        if (type.equals("random-class-method")) {
            int numClasses = 0;
            long numPermutations = 1;
            Iterator<String> it = classToMethods.keySet().iterator();
            while (it.hasNext()){
                String className = it.next();
                int numTests = classToMethods.get(className).size();
                numPermutations *= IntMath.factorial(numTests);
                numClasses++;
            }

            // formula
            long maxPermutations = IntMath.factorial(numClasses) * numPermutations;
            return maxPermutations;
        }
        else if (type.equals("random-class")) {
            int numClasses = 0;
            Iterator<String> it = classToMethods.keySet().iterator();
            while (it.hasNext()){
                numClasses++;
            }
            return (long) IntMath.factorial(numClasses);
        }

        return 0;
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
                .flatMap(RunnerPathManager::resultFor)
                .findFirst()
                .map(TestRunResult::testOrder)
                .orElse(new ArrayList<>());
    }

    private List<String> generateHistorical(final List<String> historicalOrder) {
        if ("random-class".equals(type)) {
            return generateWithClassOrder(classOrder(historicalOrder));
        } else {
            return historicalOrder;
        }
    }

    private List<String> generateShuffled() {
        List<String> shuffledOrder = generateWithClassOrder(new RandomList<>(classToMethods.keySet()).shuffled());
        String hashedOrder = MD5.md5(String.join("", shuffledOrder));
        while(alreadySeenOrders.contains(hashedOrder)){
            shuffledOrder = generateWithClassOrder(new RandomList<>(classToMethods.keySet()).shuffled());
            hashedOrder = MD5.md5(String.join("", shuffledOrder));
        }

        alreadySeenOrders.add(hashedOrder);
        roundsRemaining--;
        if(roundsRemaining <= 0) {
            saveOrders();
        }

        return shuffledOrder;
        //return generateWithClassOrder(new RandomList<>(classToMethods.keySet()).shuffled());
    }

    private List<String> generateWithClassOrder(final List<String> classOrder) {
        final List<String> fullTestOrder = new ArrayList<>();

        for (final String className : classOrder) {
            // random-class only shuffles classes, not methods
            if ("random-class".equals(type)) {
                fullTestOrder.addAll(classToMethods.get(className));
            } else {
                // the standard "random" type, will shuffle both
                fullTestOrder.addAll(new RandomList<>(classToMethods.get(className)).shuffled());
            }
        }

        return fullTestOrder;
    }

    private List<String> classOrder(final List<String> historicalOrder) {
        return historicalOrder.stream().map(TestShuffler::className).distinct().collect(Collectors.toList());
    }

    public int ordersTried(){
        return alreadySeenOrders.size();
    }

    public void saveOrders(){
        Gson gson = new Gson();
        Type gsonType = new TypeToken<Set>(){}.getType();
        String gsonString = gson.toJson(alreadySeenOrders, gsonType);
        try {
            System.out.println("Printing orders to file");
            Files.write(DetectorPathManager.previousOrders(), gsonString.getBytes(),
                    Files.exists(DetectorPathManager.previousOrders()) ? StandardOpenOption.WRITE : StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
}
