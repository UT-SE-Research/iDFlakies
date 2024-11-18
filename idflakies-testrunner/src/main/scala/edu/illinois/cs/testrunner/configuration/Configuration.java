package edu.illinois.cs.testrunner.configuration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

public class Configuration {
    public static Configuration reloadConfig(final Path path) throws IOException {
        return config = new Configuration(path).loadProperties();
    }

    private static final Path CONFIG_PATH = Paths.get("testplugin.properties");

    private static Configuration config = new Configuration(CONFIG_PATH);

    public static Configuration config() {
        return config;
    }

    private final Properties properties = new Properties();
    private final Path configPath;
    private boolean loaded = false;

    public Configuration(final Path configPath) {
        this.configPath = configPath;
    }

    public Configuration loadProperties() throws IOException {
        if (!loaded) {
            loaded = true;
            return loadProperties(configPath);
        } else {
            return this;
        }
    }

    public Properties properties() {
        if (!loaded) {
            try {
                loadProperties();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return properties;
    }

    public Path configPath() {
        return configPath;
    }

    public void setDefault(final String property, final String value) {
        if (properties().getProperty(property) == null) {
            properties().setProperty(property, value);
        }
    }

    private Configuration loadProperties(final Path path) {
        try (final InputStream fileStream = new FileInputStream(path.toFile())) {
            properties.load(fileStream);
        } catch (IOException ignored) { }

        return this;
    }

    public String getProperty(final String s, final String def) {
        if (properties().getProperty(s) == null) {
            try {
                loadProperties();
            } catch (IOException ignored) {}
        }

        return properties().getProperty(s, def);
    }

    public String getProperty(final String s) {
        return Optional.ofNullable(properties().getProperty(s))
                .orElseThrow(() -> new IllegalArgumentException("Property " + s + " does not exit!"));
    }

    public double getDoubleProperty(final String s) {
        return Double.parseDouble(getProperty(s, "0.0"));
    }

    public double getDoubleProperty(final String s, final double def) {
        return getProperty(s, def);
    }

    public double getProperty(final String s, final double def) {
        return Double.parseDouble(getProperty(s, String.valueOf(def)));
    }

    public int getIntProperty(final String s) {
        return Integer.parseInt(getProperty(s, "0"));
    }

    public int getIntProperty(final String s, final int def) {
        return getProperty(s, def);
    }

    public int getProperty(final String s, final int def) {
        return Integer.parseInt(getProperty(s, String.valueOf(def)));
    }

    public boolean getProperty(final String s, final boolean b) {
        return Boolean.parseBoolean(getProperty(s, String.valueOf(b)));
    }
}
