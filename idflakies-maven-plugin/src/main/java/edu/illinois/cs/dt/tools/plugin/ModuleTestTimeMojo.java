package edu.illinois.cs.dt.tools.plugin;

import edu.illinois.cs.dt.tools.detection.MavenDetectorPathManager;
import edu.illinois.cs.dt.tools.utility.GetMavenTestOrder;
import edu.illinois.cs.dt.tools.utility.PathManager;
import edu.illinois.cs.dt.tools.utility.TestClassData;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
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

@Mojo(name = "testTime", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class ModuleTestTimeMojo extends AbstractIDFlakiesMojo {
    private String coordinates;

    @Override
    public void execute() {
        super.execute();
        this.coordinates = mavenProject.getGroupId() + ":" + mavenProject.getArtifactId() + ":" + mavenProject.getVersion();

        final Path surefireReportsPath = Paths.get(mavenProject.getBuild().getDirectory()).resolve("surefire-reports");
        final Path mvnTestLog = PathManager.testLog();
        try {
            final Path outputFile = Paths.get(MavenDetectorPathManager.getMavenProjectParent(mavenProject).getBasedir().getAbsolutePath(),
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
