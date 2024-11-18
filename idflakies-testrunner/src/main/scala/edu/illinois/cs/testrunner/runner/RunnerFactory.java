package edu.illinois.cs.testrunner.runner;

import edu.illinois.cs.testrunner.data.framework.TestFramework;
import edu.illinois.cs.testrunner.util.ProjectClassLoader;

import edu.illinois.cs.testrunner.util.ProjectWrapper;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import scala.Option;

public class RunnerFactory {
    public static Option<Runner> from(final ProjectWrapper project) {
        return TestFramework.testFramework(project)
                .map(framework -> create(framework, new ProjectClassLoader(project).classpath(),
                        project.surefireEnvironment(), project.getBasedir().toPath()));
    }

    public static List<Runner> allFrom(final ProjectWrapper project) {
        return TestFramework.getListOfFrameworks(project).stream()
                .map(framework ->
                        create(framework, new ProjectClassLoader(project).classpath(),
                               project.surefireEnvironment(), project.getBasedir().toPath()))
                .collect(Collectors.toList());
    }

    public static Runner create(final TestFramework framework, final String classpath,
                                final Map<String, String> environment, final Path outputPath) {
        return SmartRunner.withFramework(framework, classpath, environment, outputPath);
    }

    // Needed for backward compatibility
    public static Option<Runner> from(final MavenProject project) {
        return TestFramework.testFramework(project)
                .map(framework -> create(framework, new ProjectClassLoader(project).classpath(),
                        surefireEnvironment(project), project.getBasedir().toPath()));
    }

    public static List<Runner> allFrom(final MavenProject project) {
        return TestFramework.getListOfFrameworks(project).stream()
                .map(framework ->
                        create(framework, new ProjectClassLoader(project).classpath(),
                               surefireEnvironment(project), project.getBasedir().toPath()))
                .collect(Collectors.toList());
    }

    private static <T> Stream<T> emptyIfNull(final T t) {
        return t == null ? Stream.empty() : Stream.of(t);
    }

    public static Map<String, String> surefireEnvironment(final MavenProject project) {
        return project.getBuildPlugins().stream()
                .filter(p -> p.getArtifactId().equals("maven-surefire-plugin"))
                .flatMap(p -> emptyIfNull(p.getConfiguration()))
                .flatMap(conf -> {
                    if (conf instanceof Xpp3Dom) {
                        return Stream.of((Xpp3Dom)conf);
                    } else {
                        return Stream.empty();
                    }
                })
                .flatMap(conf -> emptyIfNull(conf.getChild("environmentVariables")))
                .flatMap(envVars -> emptyIfNull(envVars.getChildren()))
                .flatMap(Arrays::stream)
                .collect(Collectors.toMap(Xpp3Dom::getName, v -> v.getValue() == null ? "" : v.getValue()));
    }
}
