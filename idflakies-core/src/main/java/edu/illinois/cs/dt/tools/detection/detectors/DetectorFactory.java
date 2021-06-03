package edu.illinois.cs.dt.tools.detection.detectors;

import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.testrunner.configuration.Configuration;

import java.io.File;
import java.util.List;

public class DetectorFactory {
    public static String detectorType() {
        return System.getProperty("detector.detector_type", "random");
    }

    public static Detector makeDetector(final InstrumentingSmartRunner runner, final File baseDir, final List<String> tests) {
        return makeDetector(runner, baseDir, tests, Integer.parseInt(System.getProperty("dt.randomize.rounds", "20")));
    }

    public static Detector makeDetector(final InstrumentingSmartRunner runner, final File baseDir, final List<String> tests, final int rounds) {
        if (detectorType().startsWith("random")) {
            return new RandomDetector(detectorType(), baseDir, runner, rounds, tests);
        } else if (detectorType().startsWith("reverse")) {
            return new ReverseDetector(runner, baseDir, rounds, detectorType(), tests);
        } else if (detectorType().equals("original")) {
            return new OriginalDetector(runner, baseDir, rounds, tests);
        } else if (detectorType().equals("smart-shuffle")) {
            return new SmartShuffleDetector(runner, baseDir, rounds, tests, detectorType());
        }

        return new RandomDetector("random", baseDir, runner, rounds, tests);
    }
}
