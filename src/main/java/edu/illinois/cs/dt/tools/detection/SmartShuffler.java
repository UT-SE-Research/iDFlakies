package edu.illinois.cs.dt.tools.detection;

import com.google.common.math.IntMath;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.reedoei.eunomia.collections.RandomList;
import edu.illinois.cs.dt.tools.utility.MD5;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class SmartShuffler {
    private final RandomList<String> toComeFirst;
    private final RandomList<String> toComeLast;

    private final HashMap<String, RandomList<String>> classToMethods;
    private final HashMap<String, String> methodToClass;

    private final RandomList<String> tests;
    private Set<String> alreadySeenOrders = new HashSet<>();

    public SmartShuffler(final List<String> tests) {
        this.tests = new RandomList<>(tests);

        toComeFirst = new RandomList<>(tests);
        toComeLast = new RandomList<>(tests);

        classToMethods = new HashMap<>();
        methodToClass = new HashMap<>();

        for (final String test : tests) {
            final String className = TestShuffler.className(test);

            if (!classToMethods.containsKey(className)) {
                classToMethods.put(className, new RandomList<>());
            }

            classToMethods.get(className).add(test);
            methodToClass.put(test, className);
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

    private RandomList<String> testSiblings(final String testName) {
        return classToMethods.get(methodToClass.get(testName));
    }

    @SafeVarargs
    private final void addTestMethods(final List<String> order, final Optional<String>... excluding) {
        // Add all classes other than the last one
        for (final RandomList<String> methods : classToMethods.values()) {
            final boolean foundExcludedTest =
                    Arrays.stream(excluding).anyMatch(t -> t.isPresent() && methods.contains(t.get()));

            if (!foundExcludedTest) {
                order.addAll(methods.shuffled());
            }
        }
    }

    public List<String> generateOrder(){
        List<String> nextOrder = nextOrder();
        String hashedOrder = MD5.md5(String.join("", nextOrder));
        while(alreadySeenOrders.contains(hashedOrder)){
            nextOrder = nextOrder();
            hashedOrder = MD5.md5(String.join("", nextOrder));
        }

        alreadySeenOrders.add(hashedOrder);
        if(toComeFirst.isEmpty() && toComeLast.isEmpty()) {
            saveOrders();
        }
        return nextOrder;
    }

    private List<String> nextOrder() {
        final Optional<String> first = sample(toComeFirst);
        final Optional<String> last = first.isPresent() ? sample(toComeLast, first.get()) : sample(toComeLast);

        first.ifPresent(toComeFirst::remove);
        last.ifPresent(toComeLast::remove);

        final List<String> order = new ArrayList<>();

        // Add the first class, make sure the first test actually comes first
        if (first.isPresent()) {
            order.addAll(testSiblings(first.get()).shuffled());
            order.remove(first.get());
            order.add(0, first.get());
        }

        addTestMethods(order, first, last);

        // Add all tests from the last class, make sure the last test actually comes last
        if (last.isPresent()) {
            order.addAll(testSiblings(last.get()).shuffled());
            order.remove(last.get());
            order.add(last.get());
        }
        return order;
    }

    private Optional<String> sample(final RandomList<String> from, final String... excluding) {
        return sample(from, Arrays.stream(excluding).collect(Collectors.toSet()));
    }

    private Optional<String> sample(final RandomList<String> from, final Set<String> excluding) {
        for (final String s : from.shuffled()) {
            if (!excluding.contains(s)) {
                return Optional.ofNullable(s);
            }
        }

        return Optional.empty();
    }

    public int ordersTried(){
        return alreadySeenOrders.size();
    }

    public void saveOrders(){
        Gson gson = new Gson();
        Type gsonType = new TypeToken<Set>(){}.getType();
        String gsonString = gson.toJson(alreadySeenOrders, gsonType);
        try {
            Files.write(DetectorPathManager.previousOrders(), gsonString.getBytes(),
                    Files.exists(DetectorPathManager.previousOrders()) ? StandardOpenOption.WRITE : StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long calculatePermutations() {
        int numClasses = 0;
        long numPermutations = 1;
        Iterator<String> it = classToMethods.keySet().iterator();
        while(it.hasNext()){
            String className = it.next();
            int numTests = classToMethods.get(className).size();
            numPermutations *= IntMath.factorial(numTests);
            numClasses++;
        }

        // formula
        long maxPermutations = IntMath.factorial(numClasses) * numPermutations;
        return maxPermutations;
    }
}
