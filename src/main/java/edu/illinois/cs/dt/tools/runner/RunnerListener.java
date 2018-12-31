package edu.illinois.cs.dt.tools.runner;

import com.google.gson.Gson;
import com.reedoei.eunomia.io.files.FileUtil;
import com.reedoei.testrunner.configuration.Configuration;
import com.reedoei.testrunner.execution.JUnitTestRunner;
import edu.illinois.cs.dt.tools.diagnosis.instrumentation.StaticFieldPathManager;
import edu.illinois.cs.dt.tools.diagnosis.instrumentation.StaticTracer;
import edu.illinois.cs.dt.tools.diagnosis.instrumentation.TracerMode;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RunnerListener extends RunListener {
    @Override
    public void testFinished(final Description description) throws Exception {
        final String trackerPath = System.getProperty("statictracer.tracer_path", "");

        final String hash = Configuration.config().getProperty("statictracer.hash", "NOHASHFOUND");

        if (!"".equals(trackerPath)) {
            final Path path = Paths.get(trackerPath).resolve(JUnitTestRunner.fullName(description) + "-" + hash);
            Files.createDirectories(path.getParent());
            StaticTracer.output(String.valueOf(path));
        }
    }

    @Override
    public void testStarted(final Description description) throws Exception {
        // Track every field at the beginning of the test
        // This way, we always have a consistent list of accessed fields for each test, with and without
        // the polluter(s)
        if (StaticTracer.mode() == TracerMode.FIRST_ACCESS) {
            final String testToCheck =
                    Configuration.config().getProperty("statictracer.first_access.test", "none");

            if (testToCheck.equals(JUnitTestRunner.fullName(description))) {
                final String hash = Configuration.config().getProperty("statictracer.hash", "NOHASHFOUND");

                final String trackerPath = System.getProperty("statictracer.tracer_path", "");
                final Path trackPath = Paths.get(trackerPath)
                        .getParent()
                        .resolve("static-field-info-TRACK")
                        .resolve(JUnitTestRunner.fullName(description) + "-" + hash);

                final StaticTracer staticTracer = new Gson().fromJson(FileUtil.readFile(trackPath), StaticTracer.class);

                // Log every field right at the beginning of the test
                for (final String fieldName : staticTracer.staticFields().keySet()) {
                    StaticTracer.logStatic(fieldName);
                }
            }
        }
    }

    @Override
    public void testFailure(final Failure failure) throws Exception {
//        failure.getException().printStackTrace();
    }

    @Override
    public void testAssumptionFailure(final Failure failure) {
//        failure.getException().printStackTrace();
    }
}
