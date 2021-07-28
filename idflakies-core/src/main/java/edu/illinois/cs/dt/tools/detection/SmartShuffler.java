package edu.illinois.cs.dt.tools.detection;

import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.dt.tools.utility.Level;
import edu.illinois.cs.dt.tools.utility.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class SmartShuffler {
    private final List<String> toComeFirst;
    private final List<String> toComeLast;

    private final HashMap<String, List<String>> classToMethods;
    private final HashMap<String, String> methodToClass;

    private final List<String> tests;

    private final Random random;

    public SmartShuffler(final List<String> tests) {
        this.tests = new ArrayList<>(tests);

        toComeFirst = new ArrayList<>(tests);
        toComeLast = new ArrayList<>(tests);

        classToMethods = new HashMap<>();
        methodToClass = new HashMap<>();

        for (final String test : tests) {
            final String className = TestShuffler.className(test);

            if (!classToMethods.containsKey(className)) {
                classToMethods.put(className, new ArrayList<>());
            }

            classToMethods.get(className).add(test);
            methodToClass.put(test, className);
        }

        // Set up Random instance using passed in seed, if available
        int seed = 42;
        try {
            seed = Integer.parseInt(Configuration.config().getProperty("dt.seed", "42"));
        } catch (NumberFormatException nfe) {
            Logger.getGlobal().log(Level.INFO, "dt.seed needs to be an integer, using default seed " + seed);
        }
        this.random = new Random(seed);
    }

    private List<String> testSiblings(final String testName) {
        return classToMethods.get(methodToClass.get(testName));
    }

    @SafeVarargs
    private final void addTestMethods(final List<String> order, final Optional<String>... excluding) {
        // Add all classes other than the last one
        for (final List<String> methods : classToMethods.values()) {
            final boolean foundExcludedTest =
                    Arrays.stream(excluding).anyMatch(t -> t.isPresent() && methods.contains(t.get()));

            if (!foundExcludedTest) {
                List<String> methodsShuffled = new ArrayList<>(methods);
                Collections.sort(methods);
                Collections.shuffle(methods, random);
                order.addAll(methodsShuffled);
            }
        }
    }

    public List<String> nextOrder() {
        final Optional<String> first = sample(toComeFirst);
        final Optional<String> last = first.isPresent() ? sample(toComeLast, first.get()) : sample(toComeLast);

        first.ifPresent(toComeFirst::remove);
        last.ifPresent(toComeLast::remove);

        final List<String> order = new ArrayList<>();

        // Add the first class, make sure the first test actually comes first
        if (first.isPresent()) {
            List<String> siblings = new ArrayList<>(testSiblings(first.get()));
            Collections.sort(siblings);
            Collections.shuffle(siblings, random);
            order.addAll(siblings);
            order.remove(first.get());
            order.add(0, first.get());
        }

        addTestMethods(order, first, last);

        // Add all tests from the last class, make sure the last test actually comes last
        if (last.isPresent()) {
            List<String> siblings = new ArrayList<>(testSiblings(last.get()));
            Collections.sort(siblings);
            Collections.shuffle(siblings, random);
            order.addAll(siblings);
            order.remove(last.get());
            order.add(last.get());
        }

        return order;
    }

    private Optional<String> sample(final List<String> from, final String... excluding) {
        return sample(from, Arrays.stream(excluding).collect(Collectors.toSet()));
    }

    private Optional<String> sample(final List<String> from, final Set<String> excluding) {
        List<String> fromShuffled = new ArrayList<>(from);
        Collections.sort(fromShuffled);
        Collections.shuffle(fromShuffled, random);
        for (final String s : fromShuffled) {
            if (!excluding.contains(s)) {
                return Optional.ofNullable(s);
            }
        }

        return Optional.empty();
    }
}
