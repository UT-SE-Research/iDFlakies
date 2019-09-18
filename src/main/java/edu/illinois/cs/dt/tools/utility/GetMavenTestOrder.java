package edu.illinois.cs.dt.tools.utility;

import com.reedoei.eunomia.util.StandardMain;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import edu.illinois.cs.testrunner.configuration.Configuration;

public class GetMavenTestOrder extends StandardMain {

    private boolean mvnTestMustPass = Boolean.parseBoolean(Configuration.config().getProperty("dt.mvn_test.must_pass","true"));
    
    @Override
    protected void run() throws Exception {
        final List<String> classOrder = getClassOrder(mvnTestLog.toFile());

        TreeMap<Long, List<TestClassData>> timeToTestClass = testClassDataMap();

        StringBuilder sb = new StringBuilder();
        for (Long time : timeToTestClass.keySet()) {
            sb.append(time);
            sb.append(" :\n");
            List<TestClassData> dataList = timeToTestClass.get(time);

            if (dataList.size() > 1) {
                TreeMap<Integer, List<TestClassData>> indexToTestClass = new TreeMap<>();

                for (TestClassData data : dataList) {
                    int index = classOrder.indexOf(data.className);
                    List<TestClassData> currentList = indexToTestClass.get(index);
                    if (currentList == null) {
                        currentList = new ArrayList<>();
                    }
                    currentList.add(data);

                    indexToTestClass.put(index, currentList);
                }

                for (Integer index : indexToTestClass.keySet()) {
                    List<TestClassData> indexList = indexToTestClass.get(index);
                    if (indexList.size() > 1) {
                        for (TestClassData data : indexList) {
                            setStringBuilderTestClassData(data, sb);
                        }
                    } else {
                        setStringBuilderTestClassData(indexList.get(0), sb);
                    }
                }

            } else {
                setStringBuilderTestClassData(dataList.get(0), sb);
            }
        }
    }

    private TreeMap<Long, List<TestClassData>> testClassDataMap() throws IOException, ParserConfigurationException, SAXException {
        final List<Path> allResultsFolders = Files.walk(sureFireDirectory)
                .filter(path -> path.toString().contains("TEST-"))
                .collect(Collectors.toList());

        TreeMap<Long, List<TestClassData>> timeToTestClass = new TreeMap<>();
        for (final Path p : allResultsFolders) {
            File f = p.toFile();
            long time = f.lastModified();

            List<TestClassData> currentList = timeToTestClass.get(time);
            if (currentList == null) {
                currentList = new ArrayList<>();
            }
            currentList.add(parseXML(f));

            timeToTestClass.put(time, currentList);
        }
        return timeToTestClass;
    }

    public List<TestClassData> testClassDataList() throws IOException, ParserConfigurationException, SAXException {
        final List<String> classOrder = getClassOrder(mvnTestLog.toFile());

        final TreeMap<Long, List<TestClassData>> timeToTestClass = testClassDataMap();

        final List<TestClassData> result = new ArrayList<>();

        for (Long time : timeToTestClass.keySet()) {
            List<TestClassData> dataList = timeToTestClass.get(time);

            if (dataList.size() > 1) {
                TreeMap<Integer, List<TestClassData>> indexToTestClass = new TreeMap<>();

                for (TestClassData data : dataList) {
                    int index = classOrder.indexOf(data.className);
                    List<TestClassData> currentList = indexToTestClass.get(index);
                    if (currentList == null) {
                        currentList = new ArrayList<>();
                    }
                    currentList.add(data);

                    indexToTestClass.put(index, currentList);
                }

                for (Integer index : indexToTestClass.keySet()) {
                    List<TestClassData> indexList = indexToTestClass.get(index);
                    if (indexList.size() > 1) {
                        result.addAll(indexList);
                    } else {
                        result.add(indexList.get(0));
                    }
                }

            } else {
                result.add(dataList.get(0));
            }
        }

        return result;
    }

    private void setStringBuilderTestClassData(TestClassData data, StringBuilder sb) {
        sb.append("  ");
        sb.append(data.className);
        sb.append(" : ");
        sb.append(data.testNames);
        System.out.println(sb.toString());
        sb.setLength(0);
    }

    private List<String> getClassOrder(File f) {
        List<String> classNames = new ArrayList<>();
        try {
            FileReader fileReader = new FileReader(f);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.trim().startsWith("Running ")) {
                    String className = line.trim().split(" ")[1];
                    classNames.add(className);
                }
            }
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classNames;
    }

    private TestClassData parseXML(File xmlFile) throws IOException, SAXException, ParserConfigurationException {
        List<String> testNames = new ArrayList<>();
        String className = "";
        double testTime = 0;

        DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);

        Element rootElement = doc.getDocumentElement();
        rootElement.normalize();

        int errors = Integer.parseInt(rootElement.getAttribute("errors"));
        int failures = Integer.parseInt(rootElement.getAttribute("failures"));

	if (mvnTestMustPass){
	    if (errors != 0 || failures != 0) {
		// errors/failures found in the test suite from running mvn test.
		// this test suite should not proceed to use detectors
		throw new RuntimeException("Failures or errors occurred in mvn test");
	    }
	}

        className = rootElement.getAttribute("name");
        testTime = Double.parseDouble(rootElement.getAttribute("time"));

        NodeList nList = doc.getElementsByTagName("testcase");
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);

            if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                Element eElement = (Element) nNode;

                if (eElement.getElementsByTagName("skipped").getLength() != 0) {
                    // this test case was marked as skip and therefore should not be ran by us
                    // TODO
                    continue;
                }

                String testName = eElement.getAttribute("name");
                testNames.add(testName);
            }
        }

        return new TestClassData(className, testNames, testTime);
    }

    private final Path mvnTestLog;
    private final Path sureFireDirectory;

    public GetMavenTestOrder(final Path sureFireDirectory, final Path mvnTestLog) {
        super(new String[0]);

        this.sureFireDirectory = sureFireDirectory;
        this.mvnTestLog = mvnTestLog;
    }

    private GetMavenTestOrder(final String[] args) {
        super(args);

        this.sureFireDirectory = Paths.get(getArgRequired("sureFireDirectory")).toAbsolutePath();
        this.mvnTestLog = Paths.get(getArgRequired("mvnTestLog")).toAbsolutePath();
    }

    public static void main(final String[] args) {
        try {
            new GetMavenTestOrder(args).run();

            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.exit(1);
    }
}
