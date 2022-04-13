package edu.illinois.cs.dt.tools.plugin;


import edu.illinois.cs.dt.tools.utility.PathManager;
import edu.illinois.cs.dt.tools.detection.MavenDetectorPathManager;
import edu.illinois.cs.testrunner.configuration.ConfigProps;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.util.ProjectWrapper;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@Execute(phase = LifecyclePhase.TEST_COMPILE)
public abstract class AbstractIDFlakiesMojo extends AbstractMojo {

    // Generic properties
    @Parameter(property = "project")
    protected MavenProject mavenProject;

    @Parameter(property = "propertiesPath", defaultValue = "")
    protected String propertiesPath = "";

    private String pluginCp;
    private List<URL> pluginCpURLs;

    private void generate() {
        // TODO: When upgrading past Java 8, this will probably no longer work
        // (cannot cast any ClassLoader to URLClassLoader)
        URLClassLoader pluginClassloader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
        pluginCpURLs = Arrays.asList(pluginClassloader.getURLs());
        pluginCp = String.join(File.pathSeparator,
                               pluginCpURLs.stream().map((PluginCpUrl) -> PluginCpUrl.getPath()).toArray(String[]::new));
    }

    private List<URL> pluginClasspathUrls() {
        if (pluginCpURLs == null) {
            generate();
        }

        return pluginCpURLs;
    }

    private String pluginClasspath() {
        if (pluginCp == null) {
            generate();
        }

        return pluginCp;
    }

    private void configJavaAgentPath() {
        pluginClasspathUrls().stream().filter((url) -> url.toString().contains("idflakies-maven-plugin") ||
                                              url.toString().contains("testrunner-gradle-plugin")).forEach((url) -> Configuration.config().setDefault("testplugin.javaagent", url.toString()));
    }

    private void setDefaults(Configuration configuration) {
        configuration.setDefault(ConfigProps.CAPTURE_STATE, String.valueOf(false));
        configuration.setDefault(ConfigProps.UNIVERSAL_TIMEOUT, String.valueOf(-1));
        configuration.setDefault(ConfigProps.SMARTRUNNER_DEFAULT_TIMEOUT, String.valueOf(6 * 3600));
        configuration.setDefault("testplugin.runner.smart.timeout.multiplier", String.valueOf(4));
        configuration.setDefault("testplugin.runner.smart.timeout.offset", String.valueOf(5));
        configuration.setDefault("testplugin.runner.smart.timeout.pertest", String.valueOf(2));
        configuration.setDefault("testplugin.classpath", pluginClasspath());

        configJavaAgentPath();
    }

    public void setConfigs(String propertiesPath) throws IOException {
        PathManager.setInstance(new MavenDetectorPathManager(mavenProject));

        System.getProperties()
            .forEach((key, value) -> Configuration.config().properties().setProperty(key.toString(), value.toString()));

        if (propertiesPath != null && !propertiesPath.isEmpty()) {
            Configuration.reloadConfig(Paths.get(propertiesPath));
        }

        setDefaults(Configuration.config());
    }
    
    @Override
    public void execute() {
        try {
            setConfigs(propertiesPath);
        } catch (IOException ignored) {}
    }

}
