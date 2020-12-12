import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.HashMap;
import java.util.Map;

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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

public class PomFile {

    private String pom;
    private String fullPath;
    private String artifactId;
    private String srcDir;
    private String testDir;
    private String outputDir;
    private static String ARTIFACT_ID = "ifixflakies";
    private static String CONFIGURATION_CLASS = "edu.illinois.cs.dt.tools.fixer.CleanerFixerPlugin";
    private static String ARTIFACT_VERSION = "1.0.0-SNAPSHOT";

    public PomFile(String pom) {
        this.pom = pom;
        try {
            this.fullPath = new File(pom).getParentFile().getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Parse document for basic fields
        parseFields(pom);
    }

    private void parseFields(String pom) {
        File pomFile = new File(pom);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(false);
        dbFactory.setValidating(false);
        DocumentBuilder dBuilder;

        try {

            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(pomFile);

            // Find high-level groupId and artifact Id
            Node project = doc.getElementsByTagName("project").item(0);
            if (project == null) {
                return;
            }
            NodeList projectChildren = project.getChildNodes();
            for (int i = 0; i < projectChildren.getLength(); i++) {
                Node n = projectChildren.item(i);
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
        } catch (ParserConfigurationException | SAXException e) {
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

    // Rewrite contents of own pom.xml, augmented with information about dependency srcs and dependency outputs
    public void rewrite() {
        File pomFile = new File(this.pom);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
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

            addPlugin(plugins, doc);

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
            PrintWriter fileWriter = new PrintWriter(this.pom);
            fileWriter.println(output);
            fileWriter.close();

        } catch (ParserConfigurationException | SAXException | TransformerException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.out.println("File does not exit: " + this.pom);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addPlugin(Node plugins, Document doc) {
        {
            Node plugin = doc.createElement("plugin");
            {
                Node groupId = doc.createElement("groupId");
                groupId.setTextContent("edu.illinois.cs");
                plugin.appendChild(groupId);
            }
            {
                Node artifactId = doc.createElement("artifactId");
                artifactId.setTextContent("testrunner-maven-plugin");
                plugin.appendChild(artifactId);
            }
            {
                Node version = doc.createElement("version");
                version.setTextContent("1.2");
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
                        depArtifactId.setTextContent(ARTIFACT_ID);
                        dependency.appendChild(depArtifactId);

                        Node depVersion = doc.createElement("version");
                        depVersion.setTextContent(ARTIFACT_VERSION);
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
                    className.setTextContent(CONFIGURATION_CLASS);
                    configuration.appendChild(className);
                }
                plugin.appendChild(configuration);
            }
            plugins.appendChild(plugin);
        }
    }

    private String getArtifactId() {
        return this.artifactId;
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java PomFile <artifact_id> <artifact_version> <configuration_class_name>");
        }

        ARTIFACT_ID = args[0];
        ARTIFACT_VERSION = args[1];
        CONFIGURATION_CLASS  = args[2];

        InputStreamReader isReader = new InputStreamReader(System.in);
        BufferedReader bufReader = new BufferedReader(isReader);
        Map<String, PomFile> mapping = new HashMap<>();
        String input;
        try {
            // First create objects out of all the pom.xml files passed in
            while ((input = bufReader.readLine()) != null) {
                PomFile p = new PomFile(input);
                mapping.put(p.getArtifactId(), p);
            }

            // Go through all the objects and have them rewrite themselves using information from dependencies
            for (Map.Entry<String,PomFile> entry : mapping.entrySet()) {
                PomFile p = entry.getValue();
                System.out.println(p.fullPath);

                // Have the object rewrite itself (the pom)
                p.rewrite();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
