package edu.illinois.cs.dt.tools.plugin;

import edu.illinois.cs.dt.tools.fixer.CleanerFixer;
import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.dt.tools.utility.Level;
import edu.illinois.cs.dt.tools.utility.Logger;
import edu.illinois.cs.dt.tools.utility.MvnCommands;
import edu.illinois.cs.testrunner.runner.Runner;
import edu.illinois.cs.testrunner.runner.RunnerFactory;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import scala.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Mojo(name = "fix", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class CleanerFixerMojo extends AbstractIDFlakiesMojo {

    private InstrumentingSmartRunner runner;
    private MvnCommands mvnCommands;

    // Find where the test source files are
    private List<Path> testSources() throws IOException {
        final List<Path> testFiles = new ArrayList<>();
        try (final Stream<Path> paths = Files.walk(Paths.get(this.mavenProject.getBuild().getTestSourceDirectory()))) {
            paths.filter(Files::isRegularFile)
                    .forEach(testFiles::add);
        }
        return testFiles;
    }

    // Compute the classpath
    private String classpath() throws DependencyResolutionRequiredException {
        final List<String> elements = new ArrayList<>(this.mavenProject.getCompileClasspathElements());
        elements.addAll(this.mavenProject.getRuntimeClasspathElements());
        elements.addAll(this.mavenProject.getTestClasspathElements());

        return String.join(File.pathSeparator, elements);
    }

    @Override
    public void execute() {
        super.execute();

        final Option<Runner> runnerOption = RunnerFactory.from(this.mavenProject);

        if (runnerOption.isDefined()) {
            this.runner = InstrumentingSmartRunner.fromRunner(runnerOption.get(), this.mavenProject.getBasedir());
            this.mvnCommands = new MvnCommands(this.mavenProject, false);

            // Invoke the main fixing class to do the fixing based off of cleaners
            try {
                CleanerFixer fixer = new CleanerFixer(runner, testSources(), classpath(), mvnCommands);
                fixer.fix();
            } catch (IOException ioe) {
                Logger.getGlobal().log(Level.INFO, "Cannot read test source files");
            } catch (DependencyResolutionRequiredException drre) {
                Logger.getGlobal().log(Level.INFO, "Cannot resolve the classpath");
            }
        } else {
            final String errorMsg = "Module is not using a supported test framework (probably not JUnit).";
            Logger.getGlobal().log(Level.INFO, errorMsg);
        }
    }
}
