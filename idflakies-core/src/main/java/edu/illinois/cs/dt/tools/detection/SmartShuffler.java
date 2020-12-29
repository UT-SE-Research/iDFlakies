package edu.illinois.cs.dt.tools.detection;

import com.reedoei.eunomia.collections.RandomList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SmartShuffler {
    private final RandomList<String> toComeFirst;
    private final RandomList<String> toComeLast;

    private final HashMap<String, RandomList<String>> classToMethods;
    private final HashMap<String, String> methodToClass;

    private final RandomList<String> tests;

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

    public List<String> nextOrder() {
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
}
