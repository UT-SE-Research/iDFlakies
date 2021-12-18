package edu.illinois.cs.dt.tools.plugin;

import edu.illinois.cs.testrunner.configuration.ConfigProps;
import edu.illinois.cs.testrunner.configuration.Configuration;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

@Execute(phase = LifecyclePhase.TEST_COMPILE)
public abstract class AbstractIDFlakiesMojo extends AbstractMojo {

    // Generic properties
    @Parameter(property = "project")
    protected MavenProject mavenProject;

    private String pluginCp;

    @Override
    public void execute() {
        //setDefaults(Configuration.config());        TODO: see if this alters tests.
    }

    private void setDefaults(Configuration configuration) {
        configuration.setDefault(ConfigProps.CAPTURE_STATE, "false");
        configuration.setDefault(ConfigProps.UNIVERSAL_TIMEOUT, "-1");
        configuration.setDefault(ConfigProps.SMARTRUNNER_DEFAULT_TIMEOUT, Integer.toString(6 * 3600));
        configuration.setDefault("testplugin.runner.smart.timeout.multiplier", "4");
        configuration.setDefault("testplugin.runner.smart.timeout.offset", "5");
        configuration.setDefault("testplugin.runner.smart.timeout.pertest", "2");
        configuration.setDefault("testplugin.classpath", pluginClasspath());
    }

    private String pluginClasspath() {
        if (pluginCp == null) {
            generate();
        }
        return this.pluginCp;
    }

    private void generate() {
        // TODO: When upgrading past Java 8, this will probably no longer work
        // (cannot cast any ClassLoader to URLClassLoader)
        URLClassLoader pluginClassloader = (URLClassLoader)(Thread.currentThread().getContextClassLoader());
        List<String> pluginCpURLs = new ArrayList<>();
        for (URL url : pluginClassloader.getURLs()) {
            pluginCpURLs.add(url.getPath());
        }
        this.pluginCp = String.join(File.pathSeparator, pluginCpURLs);
    }
}
