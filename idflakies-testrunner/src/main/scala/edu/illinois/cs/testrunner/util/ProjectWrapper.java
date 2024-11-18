package edu.illinois.cs.testrunner.util;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface ProjectWrapper {
    public void debug(String str);
    public void info(String str);
    public void error(String str);
    public void error(Throwable t);
    public ProjectWrapper getParent();
    public File getBasedir();
    public String getGroupId();
    public String getArtifactId();
    public String getVersion();
    public String getBuildDirectory();
    public List<String> getBuildTestOutputDirectories();
    public boolean containJunit4();
    public boolean containJunit5();
    public List<String> getClasspathElements();
    public Map<String, String> surefireEnvironment();
}
