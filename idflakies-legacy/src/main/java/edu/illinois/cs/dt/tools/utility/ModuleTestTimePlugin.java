package edu.illinois.cs.dt.tools.utility;

import edu.illinois.cs.dt.tools.detection.LegacyDetectorPathManager;
import edu.illinois.cs.testrunner.coreplugin.TestPlugin;
import edu.illinois.cs.testrunner.util.ProjectWrapper;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

public class ModuleTestTimePlugin extends TestPlugin {
    private String coordinates;

    // Don't delete this.
    // This is actually used, provided you call this class via Maven (used by the testrunner plugin)
    public ModuleTestTimePlugin() {
    }

    @Override
    public void execute(final ProjectWrapper project) {
        PathManager.setInstance(new LegacyDetectorPathManager());
        this.coordinates = project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion();

        final Path surefireReportsPath = Paths.get(project.getBuildDirectory()).resolve("surefire-reports");
        final Path mvnTestLog = PathManager.testLog();
        try {
            final Path outputFile = Paths.get(LegacyDetectorPathManager.getProjectParent(project).getBasedir().getAbsolutePath(),
                    "module-test-time.csv");

            final String outputStr = coordinates + "," + timeFrom(surefireReportsPath, mvnTestLog);
            System.out.println(outputStr);
            Files.write(outputFile, Collections.singletonList(outputStr), StandardCharsets.UTF_8,
                    Files.exists(outputFile) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
        } catch (SAXException | ParserConfigurationException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    private double timeFrom(final Path path, final Path mvnTestLog) throws IOException, ParserConfigurationException, SAXException {
        if (Files.exists(path)) {
            return new GetMavenTestOrder(path, mvnTestLog).testClassDataList().stream()
                    .mapToDouble(TestClassData::classTime).sum();
        }
        return 0.0;
    }

}
