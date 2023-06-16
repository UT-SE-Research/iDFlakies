package edu.illinois.cs.dt.tools.plugin;

import com.google.common.base.Preconditions;
import edu.illinois.cs.dt.tools.utility.ErrorLogger;
import edu.illinois.cs.dt.tools.utility.Level;
import edu.illinois.cs.dt.tools.utility.Logger;
import edu.illinois.cs.dt.tools.utility.PathManager;
import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.data.framework.TestFramework;
import edu.illinois.starts.asm.ClassReader;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.util.Pair;

import org.apache.commons.codec.binary.Hex;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.SurefireExecutionException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static edu.illinois.starts.helpers.ZLCHelper.STAR_FILE;

@Mojo(name = "incdetect", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class IncDetectorMojo extends DetectorMojo {

    protected static String CLASSES = "classes";
    protected static String EQUAL = "=";
    protected static String JAR_CHECKSUMS = "jar-checksums";
    protected static String SF_CLASSPATH = "sf-classpath";
    protected static String TEST_CLASSES = "test-classes";
    private static final String TARGET = "target";

    /**
     * The directory in which to store STARTS artifacts that are needed between runs.
     */
    protected String artifactsDir;

    protected String RTSDir;

    protected ClassLoader loader;

    protected List<Pair> jarCheckSums = null;

    protected Set<String> selectedTests;

    // the dependency map from test classes to their dependencies (classes)
    protected Map<String, Set<String>> transitiveClosure;

    // the dependency map from classes to their dependencies (test classes)
    protected Map<String, Set<String>> reverseTransitiveClosure;

    private Classpath sureFireClassPath;

    protected boolean selectMore;

    protected boolean detectOrNot;

    protected boolean selectAll;

    protected Path ekstaziSelectedTestsPath;

    protected Path startsSelectedTestsPath;

    protected Path startsDependenciesPath;

    protected boolean isEkstazi;

    private Set<String> affectedTestClasses;

    private static Set<String> immutableList;

    @Override
    public void execute() {
        superExecute();

        final ErrorLogger logger = new ErrorLogger();
        this.outputPath = PathManager.detectionResults();
        this.coordinates = mavenProject.getGroupId() + ":" + mavenProject.getArtifactId() + ":" + mavenProject.getVersion();

        try {
            defineSettings(logger, mavenProject);
            loadTestRunners(logger, mavenProject);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (this.runner == null) {
            return;
        }
        long startTime = System.currentTimeMillis();
        try {
            affectedTestClasses = computeAffectedTests(mavenProject);
            if (!detectOrNot) {
                System.out.println("Not detect flaky tests at the first run");
                return;
            }
        } catch (IOException | MojoExecutionException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        timing(startTime);

        startTime = System.currentTimeMillis();
        logger.runAndLogError(() -> detectorExecute(logger, mavenProject, moduleRounds(coordinates)));
        timing(startTime);
    }

    // TODO: make Starts and Ekstazi's deps similar
    private Set<String> computeAffectedTests(final MavenProject project) throws IOException, MojoExecutionException, ClassNotFoundException {
        Set<String> affectedTests = new HashSet<>();
        Set<String> allTests = new HashSet<>(getTestClasses(project, this.runner.framework()));

        selectedTests = getSelectedTests();
        // check if the classpath or jar checksum are changed; if they are changed, STARTs/ekstazi should select all tests
        checkSelectAll();

        affectedTests.addAll(selectedTests);
        if (!selectMore) {
            return affectedTests;
        }

        if (selectAll) {
            return allTests;
        }

        getTransitiveClosure();

        Set<String> additionalTests = new HashSet<>();

        // iter through the affected tests and find what each one depends on
        Set<String> processedClasses = new HashSet<>();

        getImmutableList();
        for (String affectedTest : affectedTests) {
            Set<String> dependencies = transitiveClosure.get(affectedTest);
            if (dependencies == null) {
                continue;
            }
            for (String dependency : dependencies) {
                if (processedClasses.contains(dependency)) {
                    continue;
                }
                try {
                    Class clazz = loader.loadClass(dependency);
                    for (Field field : clazz.getDeclaredFields()) {
                        if (isImmutable(field) && Modifier.isFinal(field.getModifiers())) {
                            continue;
                        }
                        if (Modifier.isStatic(field.getModifiers())) {
                            String upperLevelAffectedClass = clazz.getName();
                            Set<String> upperLevelAffectedTestClasses = reverseTransitiveClosure.get(upperLevelAffectedClass);
                            if (upperLevelAffectedTestClasses != null) {
                                for (String upperLevelAffectedTestClass : upperLevelAffectedTestClasses) {
                                    additionalTests.add(upperLevelAffectedTestClass);
                                }
                            }
                            break;
                        }
                    }
                    processedClasses.add(dependency);
                } catch (ClassNotFoundException | NoClassDefFoundError e)  {
                    e.printStackTrace();
                }
            }
        }

        affectedTests.addAll(additionalTests);
        return affectedTests;
    }

    private Set<String> getSelectedTests() {
        Path path;
        if (isEkstazi) {
            path = ekstaziSelectedTestsPath;
        } else {
            path = startsSelectedTestsPath;
        }
        selectedTests = new HashSet<>();
        try {
            BufferedReader in = Files.newBufferedReader(path, StandardCharsets.UTF_8);
            String str;
            while ((str = in.readLine()) != null) {
                selectedTests.add(str);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return selectedTests;
    }

    private void getTransitiveClosure() {
        if (isEkstazi) {
            try {
                File ekstaziDirFile = new File(RTSDir);
                File[] files = ekstaziDirFile.listFiles();
                for (File file : files) {
                    String fileName = file.toString().substring(file.toString().lastIndexOf(File.separator) + 1);
                    if (fileName.endsWith(".clz")) {
                        String transitiveClosureKey = fileName.substring(0, fileName.indexOf(".clz"));
                        BufferedReader in = Files.newBufferedReader(Paths.get(file.toString()), StandardCharsets.UTF_8);
                        String str;
                        Set<String> transitiveClosureValue = new HashSet<>();
                        while ((str = in.readLine()) != null) {
                            if (!str.contains(".class") || !str.startsWith("file:")) {
                                continue;
                            }
                            int sepIndex = str.lastIndexOf("_");
                            String urlExternalForm = str.substring(0, sepIndex);
                            URL url = new URL(urlExternalForm);
                            String transitiveClosureValueArrayItem = getClassNameFromClassFile(url.getPath());
                            transitiveClosureValue.add(transitiveClosureValueArrayItem);
                        }
                        transitiveClosure.put(transitiveClosureKey, transitiveClosureValue);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            // the dependency map from classes to their dependencies (test classes)
            reverseTransitiveClosure = getReverseClosure(transitiveClosure);
        } else {
            try {
                List<String> zlcLines = Files.readAllLines(startsDependenciesPath, Charset.defaultCharset());
                String firstLine = zlcLines.get(0);
                String space = " ";

                // check whether the first line is for *
                if (firstLine.startsWith(STAR_FILE) || firstLine.equals("PLAIN_TEXT")) {
                    zlcLines.remove(0);
                }

                for (String line : zlcLines) {
                    String[] parts = line.split(space);
                    String stringURL = parts[0];
                    Set<String> tests = parts.length == 3 ? fromCSV(parts[2]) : new HashSet<String>();
                    URL url = new URL(stringURL);
                    String reverseTransitiveClosureKey = getClassNameFromClassFile(url.getPath());
                    reverseTransitiveClosure.put(reverseTransitiveClosureKey, tests);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            // the dependency map from test classes to their dependencies (classes)
            transitiveClosure = getReverseClosure(reverseTransitiveClosure);
        }
    }

    // apply the same logic as STARTS for Ekstazi
    private void checkSelectAll() throws IOException, MojoExecutionException {
        String cpString = Writer.pathToString(sureFireClassPath.getClassPath());
        List<String> sfPathElements = getCleanClassPath(cpString);

        selectAll = false;
        if (!isSameClassPath(sfPathElements) || !hasSameJarChecksum(sfPathElements)) {
            // Force retestAll because classpath changed since last run
            // don't compute changed and non-affected classes
            // Make nonAffected empty so dependencies can be updated
            Writer.writeClassPath(cpString, artifactsDir);
            writeJarChecksums(sfPathElements, artifactsDir, jarCheckSums);
            selectAll = true;
        }
    }

    private ClassLoader createClassLoader(Classpath sfClassPath) {
        long start = System.currentTimeMillis();
        ClassLoader loader = null;
        try {
            loader = sfClassPath.createClassLoader(false, false, "MyRole");
        } catch (SurefireExecutionException see) {
            see.printStackTrace();
        }
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] IncDetectorPlugin(createClassLoader): "
                + Writer.millsToSeconds(end - start));
        return loader;
    }

    private Path relativePath(final Path initial, final Path relative) {
        Preconditions.checkState(!relative.isAbsolute(),
                "PathManager.path(): Cache paths must be relative, not absolute (%s)", relative);

        return initial.resolve(relative);
    }

    @Override
    protected void defineSettings(final ErrorLogger logger, final MavenProject project) throws IOException {
        super.defineSettings(logger, project);

        selectMore = Configuration.config().getProperty("dt.incdetector.selectmore", false);
        selectAll = false;
        detectOrNot = Configuration.config().getProperty("dt.incdetector.detectornot", true);
        isEkstazi = Configuration.config().getProperty("dt.incdetector.ekstazi", false);
        artifactsDir = getArtifactsDir();
        RTSDir = getRTSDir();
        ekstaziSelectedTestsPath = relativePath(PathManager.ekstaziPath(), Paths.get("selected-tests"));
        startsSelectedTestsPath = relativePath(PathManager.startsPath(), Paths.get("selected-tests"));
        startsDependenciesPath = relativePath(PathManager.startsPath(), Paths.get("deps.zlc"));

        transitiveClosure = new HashMap<>();
        reverseTransitiveClosure = new HashMap<>();

        getSureFireClassPath(project);
        loader = createClassLoader(sureFireClassPath);
    }

    private String getArtifactsDir() throws FileNotFoundException {
        if (artifactsDir == null) {
            artifactsDir = PathManager.cachePath().toString();
            File file = new File(artifactsDir);
            if (!file.mkdirs() && !file.exists()) {
                throw new FileNotFoundException("I could not create artifacts dir: " + artifactsDir);
            }
        }
        return artifactsDir;
    }

    private String getRTSDir() throws FileNotFoundException {
        if (RTSDir == null) {
            if (isEkstazi) {
                RTSDir = PathManager.ekstaziPath().toString();
            } else {
                RTSDir = PathManager.startsPath().toString();
            }
            File file = new File(RTSDir);
            if (!file.mkdirs() && !file.exists()) {
                throw new FileNotFoundException("I could not create artifacts dir: " + RTSDir);
            }
        }
        return RTSDir;
    }

    private List<String> getCleanClassPath(String cp) {
        List<String> cpPaths = new ArrayList<>();
        String[] paths = cp.split(File.pathSeparator);
        String classes = File.separator + TARGET + File.separator + CLASSES;
        String testClasses = File.separator + TARGET + File.separator + TEST_CLASSES;
        Set<String> classPathSet = new HashSet<>();
        for (int i = 0; i < paths.length; i++) {
            paths[i].replaceAll(" ", "");
            // TODO: should we also exclude SNAPSHOTS from same project?
            if (paths[i].contains(classes) || paths[i].contains(testClasses)) {
                continue;
            }
            classPathSet.add(paths[i]);
        }
        for (String classPath : classPathSet) {
            cpPaths.add(classPath);
        }
        return cpPaths;
    }

    private Map<String, Set<String>> getReverseClosure(Map<String, Set<String>> transitiveClosure) {
        Map<String, Set<String>> reverseTransitiveClosure = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : transitiveClosure.entrySet()) {
            for (String dep : entry.getValue()) {
                if (reverseTransitiveClosure.containsKey(dep)) {
                    Set<String> reverseDeps = reverseTransitiveClosure.get(dep);
                    reverseDeps.add(entry.getKey());
                }
                else {
                    Set<String> reverseDeps = new HashSet<>();
                    reverseDeps.add(entry.getKey());
                    reverseTransitiveClosure.putIfAbsent(dep, reverseDeps);
                }
            }
        }
        return reverseTransitiveClosure;
    }

    private Classpath getSureFireClassPath(final MavenProject project) {
        long start = System.currentTimeMillis();
        if (sureFireClassPath == null) {
            try {
                sureFireClassPath = new Classpath(project.getTestClasspathElements());
            } catch (DependencyResolutionRequiredException e) {
                e.printStackTrace();
            }
        }
        Logger.getGlobal().log(Level.FINEST, "SF-CLASSPATH: " + sureFireClassPath.getClassPath());
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] IncDetectorPlugin(getSureFireClassPath): "
                + Writer.millsToSeconds(end - start));
        return sureFireClassPath;
    }

    @Override
    protected List<String> getTests(
            final MavenProject project,
            TestFramework testFramework) throws IOException {
        // NOTE: Do we need to re-compute original order, or expect developer to provide already?
        List<String> tests = getOriginalOrder(project, testFramework, true);
        List<String> affectedTests = new ArrayList<>();

        String delimiter = testFramework.getDelimiter();
        for (String test : tests) {
            String clazz = test.substring(0, test.lastIndexOf(delimiter));
            if (affectedTestClasses.contains(clazz)) {
                affectedTests.add(test);
            }
        }
        return affectedTests;
    }

    private List<String> getTestClasses(
            final MavenProject project,
            TestFramework testFramework) throws IOException {
        // NOTE: Do we need to re-compute original order, or expect developer to provide already?
        List<String> tests = getOriginalOrder(project, testFramework, true);

        String delimiter = testFramework.getDelimiter();
        List<String> classes = new ArrayList<>();
        for (String test : tests){
            String clazz = test.substring(0, test.lastIndexOf(delimiter));
            if (!classes.contains(clazz)) {
                classes.add(clazz);
            }
        }

        return classes;
    }

    /**
     * Compute the checksum for the given map and return the jar
     * and the checksum as a string.
     *
     * @param jar  The jar whose checksum we need to compute.
     */
    private Pair<String, String> getJarToChecksumMapping(String jar) {
        Pair<String, String> pair = new Pair<>(jar, "-1");
        byte[] bytes;
        int bufSize = 65536 * 2;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            InputStream is = Files.newInputStream(Paths.get(jar));
            bytes = new byte[bufSize];
            int size = is.read(bytes, 0, bufSize);
            while (size >= 0) {
                md.update(bytes, 0, size);
                size = is.read(bytes, 0, bufSize);
            }
            pair = new Pair<>(jar, Hex.encodeHexString(md.digest()));
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return pair;
    }

    private boolean hasSameJarChecksum(List<String> cleanSfClassPath) throws FileNotFoundException {
        if (cleanSfClassPath.isEmpty()) {
            return true;
        }
        String oldChecksumPathFileName = Paths.get(getArtifactsDir(), JAR_CHECKSUMS).toString();
        if (!new File(oldChecksumPathFileName).exists()) {
            return false;
        }
        boolean noException = true;
        try {
            List<String> lines = Files.readAllLines(Paths.get(oldChecksumPathFileName));
            Map<String, String> checksumMap = new HashMap<>();
            for (String line : lines) {
                String[] elems = line.split(EQUAL);
                checksumMap.put(elems[0], elems[1]);
            }
            jarCheckSums = new ArrayList<>();
            for (String path : cleanSfClassPath) {
                Pair<String, String> pair = getJarToChecksumMapping(path);
                jarCheckSums.add(pair);
                String oldCS = checksumMap.get(pair.getKey());
                noException &= pair.getValue().equals(oldCS);
            }
        } catch (IOException ioe) {
            noException = false;
            // reset to null because we don't know what/when exception happened
            jarCheckSums = null;
            ioe.printStackTrace();
        }
        return noException;
    }

    private boolean isSameClassPath(List<String> sfPathString) throws MojoExecutionException, FileNotFoundException {
        if (sfPathString.isEmpty()) {
            return true;
        }
        String oldSfPathFileName = Paths.get(getArtifactsDir(), SF_CLASSPATH).toString();
        if (!new File(oldSfPathFileName).exists()) {
            return false;
        }
        try {
            List<String> oldClassPathLines = Files.readAllLines(Paths.get(oldSfPathFileName));
            if (oldClassPathLines.size() != 1) {
                throw new MojoExecutionException(SF_CLASSPATH + " is corrupt! Expected only 1 line.");
                // This exception is not correct and need to be modified.
            }
            List<String> oldClassPathelements = getCleanClassPath(oldClassPathLines.get(0));
            // comparing lists and not sets in case order changes
            if (sfPathString.equals(oldClassPathelements)) {
                return true;
            }
            Set<String> sfPathStringSet = new HashSet<>();
            sfPathStringSet.addAll(sfPathString);
            Set<String> oldClassPathelementsSet = new HashSet<>();
            oldClassPathelementsSet.addAll(sfPathString);
            if (sfPathStringSet.equals(oldClassPathelementsSet)) {
                return true;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return false;
    }

    private void getImmutableList() {
        if (immutableList == null) {
            immutableList = new HashSet<>();

            immutableList.add("java.lang.String");
            immutableList.add("java.lang.Enum");
            immutableList.add("java.lang.StackTraceElement");
            immutableList.add("java.math.BigInteger");
            immutableList.add("java.math.BigDecimal");
            immutableList.add("java.io.File");
            immutableList.add("java.awt.Font");
            immutableList.add("java.awt.BasicStroke");
            immutableList.add("java.awt.Color");
            immutableList.add("java.awt.GradientPaint");
            immutableList.add("java.awt.LinearGradientPaint");
            immutableList.add("java.awt.RadialGradientPaint");
            immutableList.add("java.awt.Cursor");
            immutableList.add("java.util.Locale");
            immutableList.add("java.util.UUID");
            immutableList.add("java.util.Collections");
            immutableList.add("java.net.URL");
            immutableList.add("java.net.URI");
            immutableList.add("java.net.Inet4Address");
            immutableList.add("java.net.Inet6Address");
            immutableList.add("java.net.InetSocketAddress");
            immutableList.add("java.awt.BasicStroke");
            immutableList.add("java.awt.Color");
            immutableList.add("java.awt.GradientPaint");
            immutableList.add("java.awt.LinearGradientPaint");
            immutableList.add("java.awt.RadialGradientPaint");
            immutableList.add("java.awt.Cursor");
            immutableList.add("java.util.regex.Pattern");
        }
    }

    private boolean isImmutable(Field field) {
        boolean isFinal = false;
        if (Modifier.isFinal(field.getModifiers())) {
            isFinal = true;
        }

        if ((field.getType().isPrimitive() || field.getDeclaringClass().isEnum()) && isFinal) {
            return true;
        }

        for (String immutableTypeName : immutableList) {
            if ((field.getType().getName().equals(immutableTypeName)) && isFinal) {
                return true;
            }
        }
        return false;
    }

    private void writeJarChecksums(List<String> sfPathString, String artifactsDir, List<Pair> jarCheckSums) {
        String outFilename = Paths.get(artifactsDir, JAR_CHECKSUMS).toString();
        try (BufferedWriter writer = Writer.getWriter(outFilename)) {
            if (jarCheckSums != null) {
                // we already computed the checksums while checking with old version in RunMojo#hasSameJarChecksum()
                for (Pair<String, String> pair : jarCheckSums) {
                    writer.write(pair.toString());
                    writer.write(System.lineSeparator());
                }
            } else {
                for (String path : sfPathString) {
                    if (path.isEmpty()) {
                        continue;
                    }
                    writer.write(getJarToChecksumMapping(path).toString());
                    writer.write(System.lineSeparator());
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private Set<String> fromCSV(String tests) {
        return new HashSet<>(Arrays.asList(tests.split(",")));
    }

    private String getClassNameFromClassFile(String filePath) {
        try {
            byte[] classFileBuffer = Files.readAllBytes(Paths.get(filePath));
            ClassReader classReader = new ClassReader(classFileBuffer);
            return classReader.getClassName().replaceAll("/", ".");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
