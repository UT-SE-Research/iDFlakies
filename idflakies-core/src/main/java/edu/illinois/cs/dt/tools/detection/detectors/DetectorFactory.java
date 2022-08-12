package edu.illinois.cs.dt.tools.detection.detectors;

import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.testrunner.configuration.Configuration;

import java.io.File;
import java.util.List;

public class DetectorFactory {
    public static String detectorType() {
        return Configuration.config().getProperty("detector.detector_type", "random");
    }

    public static Detector makeDetector(final InstrumentingSmartRunner runner, final File baseDir, final List<String> tests) {
        return makeDetector(runner, baseDir, tests, Configuration.config().getProperty("dt.randomize.rounds", 20));
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
        }  else if (detectorType().equals("tuscan")){
            return new TuscanOnlyClassDetector(runner, baseDir, rounds, detectorType(), tests);
        } else if (detectorType().startsWith("alphabetical")) {
            return new AlphabeticalDetector(runner, baseDir, rounds, detectorType(), tests);
        }
        return new RandomDetector("random", baseDir, runner, rounds, tests);
    }
}
