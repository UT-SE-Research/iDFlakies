package edu.illinois.cs.dt.tools.detection;

import com.google.common.collect.Lists;
import com.google.common.math.IntMath;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import com.reedoei.eunomia.collections.ListUtil;
import com.reedoei.eunomia.collections.RandomList;
import com.reedoei.eunomia.io.files.FileUtil;
import edu.illinois.cs.dt.tools.runner.RunnerPathManager;
import edu.illinois.cs.dt.tools.utility.MD5;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.data.results.TestRunResult;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class TestShuffler {
    public static String className(final String testName) {
        return testName.substring(0, testName.lastIndexOf('.'));
    }

    private final HashMap<String, List<String>> classToMethods;

    private final String type;
    private final List<String> tests;
    private final Set<String> alreadySeenOrders = new HashSet<>();
    private final Set<String> newTestsRan = new HashSet<>();
    private boolean overwritten = false;

    //Variable to keep track of which new tests have been processed
    private int processedIndex = 0;

    public TestShuffler(final String type, final int rounds, final List<String> tests) {
        this.type = type;
        this.tests = tests;

        classToMethods = new HashMap<>();

        for (final String test : tests) {
            final String className = className(test);

            if (!classToMethods.containsKey(className)) {
                classToMethods.put(className, new ArrayList<>());
            }

            classToMethods.get(className).add(test);
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
        if (type.equals("incremental")) {
            // *** check if any new tests were run ***
            // if yes, run test at front and back
            // if no, run tests in new order
            System.out.println("***Using incremental detector and shuffling for a round***");
            try{
                JsonReader getLocalJsonFile = new JsonReader(new FileReader(DetectorPathManager.PREVIOUS_TESTS.toString()));
//                Type mapTokenType = new TypeToken<Map<String, Map>>(){}.getType();
                Type mapTokenType = new TypeToken<List<String>>(){}.getType();
                // Map<String, String[]> jsonMap = new Gson().fromJson(getLocalJsonFile, mapTokenType);
                List<String> jsonMap = new Gson().fromJson(getLocalJsonFile, mapTokenType);

                // check is there are any new tests
                List<String> newTests = new ArrayList<>();
                for(int j=0;j<tests.size();j++){
                    String test = tests.get(j);
//                    if(!jsonMap.containsValue(test)){
//                        newTests.add(test);
//                    }
                    if(!jsonMap.contains(test)){
                        System.out.println("***Found a new test***");
                        newTests.add(test);
                    }
                }

                // if no new tests just sent new shuffled order
                // need to make this more sophisticated to account for multiple new tests at once
                if(newTests.isEmpty()){
                    System.out.println("***No new tests found, just shuffling tests***");
                    return generateShuffled();
                } else {
                    System.out.println("***New tests were found***");
                    // if there are new tests, run them at the front and back
                    List<String> testOrder = new ArrayList<>();
                     if(!newTestsRan.contains("Front")){
                        if(newTests.size() > 1) {
                            //Put one of the tests at the front
                            testOrder.add(newTests.get(processedIndex));
                            newTests.remove(processedIndex);

                            testOrder.addAll(newTests); //TODO randomize the other ones
                        }
                        else {
                            testOrder.addAll(newTests);
                        }
//                    testOrder.addAll(jsonMap.keySet());
                        testOrder.addAll(jsonMap);
                         newTestsRan.add("Front");
                    } else if(newTestsRan.contains("Front") && !newTestsRan.contains("Back")){
                        testOrder.addAll(jsonMap);
                        testOrder.add(newTests.get(processedIndex));
                        newTests.remove(processedIndex);
                        testOrder.addAll(newTests);
                        processedIndex++;
                        newTestsRan.add("Back");
                    } else{
                         return generateShuffled();
                     }

                    // write new test order to file
                    Gson gson = new Gson();
                    Type gsonType = new TypeToken<List>(){}.getType();
                    String gsonString = gson.toJson(testOrder,gsonType);
                    if(!overwritten){
                        Files.write(DetectorPathManager.NEWTEST_TESTORDER, gsonString.getBytes());
                        overwritten = true;
                    } else{
                        Files.write(DetectorPathManager.NEWTEST_TESTORDER, gsonString.getBytes(), StandardOpenOption.APPEND);
                    }

                    return testOrder;
                }
            } catch(IOException e){
                System.out.println(e);
            }
        }

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
        return generateWithClassOrder(new RandomList<>(classToMethods.keySet()).shuffled());
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
}
