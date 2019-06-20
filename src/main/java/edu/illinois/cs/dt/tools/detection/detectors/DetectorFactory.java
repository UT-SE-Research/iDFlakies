package edu.illinois.cs.dt.tools.detection.detectors;

import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.testrunner.configuration.Configuration;

import java.util.List;

public class DetectorFactory {
    public static String detectorType() {
        return Configuration.config().getProperty("detector.detector_type", "random");
    }

    public static Detector makeDetector(final InstrumentingSmartRunner runner, final List<String> tests) {
        return makeDetector(runner, tests, Configuration.config().getProperty("dt.randomize.rounds", 20));
    }

    public static Detector makeDetector(final InstrumentingSmartRunner runner, final List<String> tests, final int rounds) {
        if (detectorType().startsWith("random")) {
            return new RandomDetector(detectorType(), runner, rounds, tests);
        } else if (detectorType().startsWith("reverse")) {
            return new ReverseDetector(runner, rounds, detectorType(), tests);
        } else if (detectorType().equals("original")) {
            return new OriginalDetector(runner, rounds, tests);
        } else if (detectorType().equals("smart-shuffle")) {
            return new SmartShuffleDetector(runner, rounds, tests, detectorType());
        }

        return new RandomDetector("random", runner, rounds, tests);
    }
}
