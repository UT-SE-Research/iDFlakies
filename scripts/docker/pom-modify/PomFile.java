import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.nio.file.FileVisitResult;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import static java.nio.file.FileVisitResult.CONTINUE;

public class PomFile {

    private String pom;
    private String fullPath;
    private String groupId;
    private String artifactId;
    private String srcDir;
    private String testDir;
    private String outputDir;
    private List<String> srcClasses = new ArrayList<String>();
    private List<String> testClasses = new ArrayList<String>();
    private List<String> dependencyIds = new ArrayList<String>();

    public PomFile(String pom) {
        this.pom = pom;
        try {
            this.fullPath = new File(pom).getParentFile().getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Parse document for basic fields
        parseFields(pom);

        // Find classes (source and test) associated with this pom project
        setClasses();
    }

    private void parseFields(String pom) {
        File pomFile = new File(pom);
    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory
            .newInstance();
        dbFactory.setNamespaceAware(false);
        dbFactory.setValidating(false);
    	DocumentBuilder dBuilder;

        try {

            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(pomFile);

            // First find groupId by looking at parent
            if (doc.getElementsByTagName("parent").getLength() == 1) {
                Node parent = doc.getElementsByTagName("parent").item(0);
                NodeList parentChildren = parent.getChildNodes();
                for (int i = 0; i < parentChildren.getLength(); i++) {
                    Node n = parentChildren.item(i);
                    if (n.getNodeName().equals("groupId")) {
                        this.groupId = n.getTextContent();
                    }
                }
            }

            // Find high-level groupId and artifact Id
            Node project = doc.getElementsByTagName("project").item(0);
            if (project == null) {
                return;
            }
            NodeList projectChildren = project.getChildNodes();
            for (int i = 0; i < projectChildren.getLength(); i++) {
                Node n = projectChildren.item(i);
                if (n.getNodeName().equals("groupId")) {
                    this.groupId = n.getTextContent();
                }
                if (n.getNodeName().equals("artifactId")) {
                    this.artifactId = n.getTextContent();
                }
            }

            // Search for relevant tags in build
            if (doc.getElementsByTagName("build").getLength() == 1) {
                Node build = doc.getElementsByTagName("build").item(0); // Should only be one <build> structure
                NodeList buildChildren = build.getChildNodes();
                for (int i = 0; i < buildChildren.getLength(); i++) {
                    Node n = buildChildren.item(i);

                    // Find source directory
                    if (n.getNodeName().equals("sourceDirectory")) {
                        this.srcDir = n.getTextContent();
                    }

                    // Find test source directory
                    if (n.getNodeName().equals("testSourceDirectory")) {
                        this.testDir = n.getTextContent();
                    }

                    // Find output directory
                    if (n.getNodeName().equals("outputDirectory")) {
                        this.outputDir = n.getTextContent();
                    }
                }
            }

            // Search for relevant tags in dependencies
            this.dependencyIds = new ArrayList<String>();
            if (doc.getElementsByTagName("dependencies").getLength() > 0) {
                for (int k = 0; k < doc.getElementsByTagName("dependencies").getLength(); k++) {
                    Node dependencies = doc.getElementsByTagName("dependencies").item(k);
                    NodeList dependenciesChildren = dependencies.getChildNodes();
                    for (int i = 0; i < dependenciesChildren.getLength(); i++) {
                        Node dependency = dependenciesChildren.item(i);
                        if (dependency.getNodeName().equals("dependency")) {
                            String local_groupId = "";
                            String local_artifactId = "";
                            NodeList dependencyChildren = dependency.getChildNodes();
                            for (int j = 0; j < dependencyChildren.getLength(); j++) {
                                Node n = dependencyChildren.item(j);
                                if (n.getNodeName().equals("groupId")) {
                                    local_groupId = n.getTextContent();
                                }
                                if (n.getNodeName().equals("artifactId")) {
                                    local_artifactId = n.getTextContent();
                                }
                            }
                            if ((local_groupId.equals(this.groupId) || local_groupId.equals("${project.groupId}")) && !local_artifactId.equals("")) {
                                this.dependencyIds.add(local_artifactId);
                            }
                        }
                    }
                }
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.out.println("File does not exit: " + pom);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Set defaults for directories if they were not found
        if (this.srcDir == null) {
            this.srcDir = "src/main/java";
        }
        if (this.testDir == null) {
            this.testDir = "src/test/java";
        }
        if (this.outputDir == null) {
            this.outputDir = "target/classes";
        }
    }

    private void setClasses() {
        String fullSrcDir = this.fullPath + "/" + this.srcDir;
        String fullTestDir = this.fullPath + "/" + this.testDir;

        try {
            List<String> srcClassPaths = new ArrayList<String>();
            if (new File(fullSrcDir).exists()) {
                Finder finder = new Finder("*.java");
                Files.walkFileTree(Paths.get(fullSrcDir), finder);
                srcClassPaths.addAll(finder.getMatches());
            }

            List<String> testClassPaths = new ArrayList<String>();
            if (new File(fullTestDir).exists()) {
                Finder finder = new Finder("*.java");
                Files.walkFileTree(Paths.get(fullTestDir), finder);
                testClassPaths.addAll(finder.getMatches());
            }
            this.srcClasses = convertToClasses(srcClassPaths, this.srcDir);
            this.testClasses = convertToClasses(testClassPaths, this.testDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<String> convertToClasses(List<String> classPaths, String srcPath) {
        List<String> classes = new ArrayList<String>();
        for (String classPath : classPaths) {
            if (srcPath.charAt(srcPath.length()-1) == '/') {
                classes.add(classPath.replace(this.fullPath + "/" + srcPath, "").replace(".java","").replace("/", "."));
            }
            else {
                classes.add(classPath.replace(this.fullPath + "/" + srcPath + "/", "").replace(".java","").replace("/", "."));
            }
        }

        return classes;
    }

    // Rewrite contents of own pom.xml, augmented with information about dependency srcs and dependency outputs
    public void rewrite(Set<String> dependencySrcs, Set<String> outputDirectories) {
        File pomFile = new File(this.pom);
    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory
            .newInstance();
        dbFactory.setNamespaceAware(false);
        dbFactory.setValidating(false);
    	DocumentBuilder dBuilder;

        try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(pomFile);

            // Get high-level project node to find <build> tag
            Node project = doc.getElementsByTagName("project").item(0);
            NodeList projectChildren = project.getChildNodes();

            // Check if <build> tag exists; if not have to make one
            Node build = null;
            for (int i = 0; i < projectChildren.getLength(); i++) {
                if (projectChildren.item(i).getNodeName().equals("build")) {
                    build = projectChildren.item(i);
                    break;
                }
            }
            if (build == null) {
                build = doc.createElement("build");
                project.appendChild(build);
            }

            NodeList buildChildren = build.getChildNodes();


            // Search for <plugins>
            Node plugins = null;
            for (int i = 0; i < buildChildren.getLength(); i++) {
                if (buildChildren.item(i).getNodeName().equals("plugins")) {
                    plugins = buildChildren.item(i);
                    break;
                }
            }
            // Add new <plugins> if non-existant
            if (plugins == null) {
                plugins = doc.createElement("plugins");
                build.appendChild(plugins);
            }

            // Construct <plugin> for PIT
            {
                Node plugin = doc.createElement("plugin");
                {
                    Node groupId = doc.createElement("groupId");
                    groupId.setTextContent("com.reedoei");
                    plugin.appendChild(groupId);
                }
                {
                    Node artifactId = doc.createElement("artifactId");
                    artifactId.setTextContent("testrunner-maven-plugin");
                    plugin.appendChild(artifactId);
                }
                {
                    Node version = doc.createElement("version");
                    version.setTextContent("0.1-SNAPSHOT");
                    plugin.appendChild(version);
                }
                {
                    Node dependencies = doc.createElement("dependencies");
                    {
                        Node dependency = doc.createElement("dependency");
                        {
                            Node depGroupId = doc.createElement("groupId");
                            depGroupId.setTextContent("edu.illinois.cs");
                            dependency.appendChild(depGroupId);

                            Node depArtifactId = doc.createElement("artifactId");
                            depArtifactId.setTextContent("idflakies");
                            dependency.appendChild(depArtifactId);

                            Node depVersion = doc.createElement("version");
                            depVersion.setTextContent("1.0.0-SNAPSHOT");
                            dependency.appendChild(depVersion);
                        }
                        dependencies.appendChild(dependency);
                    }
                    plugin.appendChild(dependencies);
                }
                {
                    Node configuration = doc.createElement("configuration");
                    {
                        Node className = doc.createElement("className");
                        className.setTextContent("edu.illinois.cs.dt.tools.detection.DetectorPlugin");
                        configuration.appendChild(className);
                    }
                    plugin.appendChild(configuration);
                }
                plugins.appendChild(plugin);
            }

            // Construct string representation of the file
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            String output = writer.getBuffer().toString();

            // Rewrite the pom file with this string
            PrintWriter filewriter = new PrintWriter(this.pom);
            filewriter.println(output);
            filewriter.close();

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.out.println("File does not exit: " + this.pom);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    // Accessors
    public String getFullPath() {
        return this.fullPath;
    }

    public String getArtifactId() {
        return this.artifactId;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public String getSrcDir() {
        return this.srcDir;
    }

    public String getTestDir() {
        return this.testDir;
    }

    public String getOutputDir() {
        return this.outputDir;
    }

    public List<String> getSrcClasses() {
        return this.srcClasses;
    }

    public List<String> getTestClasses() {
        return this.testClasses;
    }

    public List<String> getDependencyIds() {
        return this.dependencyIds;
    }

    // Helper class to implement find functionality in Java
    public static class Finder extends SimpleFileVisitor<Path> {

        private final PathMatcher matcher;
        private List<String> matches;

        public Finder(String pattern) {
            this.matches = new ArrayList<String>();
            this.matcher = FileSystems.getDefault()
                .getPathMatcher("glob:" + pattern);
        }

        // Compares the glob pattern against
        // the file or directory name.
        public void find(Path file) {
            Path name = file.getFileName();
            if (name != null && matcher.matches(name)) {
                this.matches.add(file.toAbsolutePath().normalize().toString());
            }
        }

        // Invoke the pattern matching
        // method on each file.
        @Override
        public FileVisitResult visitFile(Path file,
                BasicFileAttributes attrs) {
            find(file);
            return CONTINUE;
        }

        // Invoke the pattern matching
        // method on each directory.
        @Override
        public FileVisitResult preVisitDirectory(Path dir,
                BasicFileAttributes attrs) {
            find(dir);
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file,
                IOException exc) {
            System.err.println(exc);
            return CONTINUE;
        }

        public List<String> getMatches() {
            return this.matches;
        }
    }

    public static void main(String[] args) {
        InputStreamReader isReader = new InputStreamReader(System.in);
        BufferedReader bufReader = new BufferedReader(isReader);
        Map<String, PomFile> mapping = new HashMap<String, PomFile>();
        String input;
        try {
            // First create objects out of all the pom.xml files passed in
            while ((input = bufReader.readLine()) != null) {
                PomFile p = new PomFile(input);
                mapping.put(p.getArtifactId(), p);
            }

            // Go through all the objects and have them rewrite themselves using information from dependencies
            for (Map.Entry<String,PomFile> entry : mapping.entrySet()) {
                final Set<String> checkedMappings = new HashSet<>();

                PomFile p = entry.getValue();

                System.out.println(p.fullPath);

                // Obtain information (src classes, output directories) from all of its dependencies
                Set<String> dependency_srcs = new HashSet<String>();
                Set<String> output_dirs = new HashSet<String>();
                List<String> allDependencies = p.getDependencyIds();
                while (!allDependencies.isEmpty()) {
                    String dependency = allDependencies.remove(0);
                    PomFile o = mapping.get(dependency);
                    if (o != null){
                        if (!checkedMappings.contains(o.fullPath)) {
                            checkedMappings.add(o.fullPath);

                            dependency_srcs.addAll(o.getSrcClasses());
                            output_dirs.add(o.getFullPath() + "/" + o.getOutputDir());

                            allDependencies.addAll(o.getDependencyIds());   // Get transitive dependencies
                        }
                    }
                }

                // Have the object rewrite itself (the pom) with pit stuff
                p.rewrite(dependency_srcs, output_dirs);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

    }
}
