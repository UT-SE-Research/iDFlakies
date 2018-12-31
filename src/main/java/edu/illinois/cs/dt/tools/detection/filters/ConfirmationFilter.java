package edu.illinois.cs.dt.tools.detection.filters;

import com.google.gson.Gson;
import com.reedoei.eunomia.io.files.FileUtil;
import edu.illinois.cs.dt.tools.detection.DetectionRound;
import edu.illinois.cs.dt.tools.detection.DetectorPathManager;
import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.dt.tools.runner.data.DependentTest;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ConfirmationFilter implements Filter {
    private static final double DEPENDENT_CONFIRMATION_SAMPLING_RATE = 0.2;
    private static final double FLAKY_CONFIRMATION_SAMPLING_RATE = 0.2;

    private final Set<String> knownFlaky;
    private final Set<String> knownDep;
    private final String detectorType;
    private final InstrumentingSmartRunner runner;

    public ConfirmationFilter(final String detectorType,
                              final List<String> tests,
                              final InstrumentingSmartRunner runner) {
        this.detectorType = detectorType;
        this.runner = runner;
        this.knownFlaky = new HashSet<>();
        knownDep = new HashSet<>();

        for (final String test : tests) {
            if (runner.info().isFlaky(test)) {
                knownFlaky.add(test);
            }
        }

        // Load results from the directory if we can
        try {
            Files.list(DetectorPathManager.detectionResults().resolve("flaky")).forEach(p -> {
                try {
                    final DetectionRound round = new Gson().fromJson(FileUtil.readFile(p), DetectionRound.class);

                    for (final DependentTest dependentTest : round.filteredTests().dts()) {
                        knownFlaky.add(dependentTest.name());
                    }
                } catch (Exception ignored) {}
            });
        } catch (IOException ignored) {}
    }

    @Override
    public boolean keep(final DependentTest dependentTest, final int absoluteRound) {
        if (knownFlaky.contains(dependentTest.name())) {
            if (new Random().nextDouble() < FLAKY_CONFIRMATION_SAMPLING_RATE) {
                return confirmation(true, "confirmation-sampling", dependentTest, absoluteRound);
            }

            // This test is known to be flaky, so it should never make it past the filter
            return false;
        } else if (knownDep.contains(dependentTest.name())) {
            if (new Random().nextDouble() < DEPENDENT_CONFIRMATION_SAMPLING_RATE) {
                return confirmation(false, "confirmation-sampling", dependentTest, absoluteRound);
            }

            // Known dependent, so return true
            return true;
        } else {
            return confirmation(false, "verify", dependentTest, absoluteRound);
        }
    }

    private boolean confirmation(final boolean isFlaky, final String verifyType,
                                 final DependentTest dependentTest, final int absoluteRound) {
        final boolean confirmed = verify(verifyType, dependentTest, absoluteRound);

        // if it's flaky, we shouldn't put it back into the dep set
        if (!isFlaky) {
            if (confirmed) {
                knownDep.add(dependentTest.name());
            } else {
                knownFlaky.add(dependentTest.name());
                knownDep.remove(dependentTest.name());
            }
        }

        // Should never keep flaky tests (keeping mean it's dependent in this case)
        if (isFlaky) {
            return false;
        }

        return confirmed;
    }

    private boolean verify(final String verifyType, final DependentTest dependentTest, final int absoluteRound) {
        return dependentTest.verify(runner, DetectorPathManager.filterPath(detectorType, verifyType, absoluteRound));
    }
}
