package edu.illinois.cs.dt.tools.poldet;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import edu.illinois.cs.dt.tools.poldet.instrumentation.MainAgent;
import edu.illinois.cs.testrunner.mavenplugin.TestPluginPlugin;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.io.BufferedWriter;
import java.io.CharArrayReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.NodeDetail;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class StateCapture {

    private static LinkedHashMap<String, Object> beforeMapping = new LinkedHashMap<>();
    private static LinkedHashMap<String, Object> afterMapping = new LinkedHashMap<>();
    private static Set<String> roots = new HashSet<>();

    private static Set<String> polluters = new HashSet<String>();

    private static DocumentBuilder dBuilder;

    static {
        try {
            dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
        }
    }

    public static Set<String> getPolluters() {
        return polluters;
    }

    public static Set<String> getRoots() {
        return roots;
    }

    public static void captureBefore(String testname) {
        // Don't bother with one of our fake tests
        if (testname.contains("StateCaptureFakeTest")) {
            return;
        }
        beforeMapping = capture();
    }

    public static void captureAfter(String testname) {
        // Don't bother with one of our fake tests
        if (testname.contains("StateCaptureFakeTest")) {
            return;
        }
        afterMapping = capture();
        recordDiff(testname);
    }

    public static boolean ignoreClass(String className) {
        if (className.startsWith("java.")
            || className.startsWith("javax.")
            || className.startsWith("scala.")
            || className.startsWith("sun.")
            || className.startsWith("com.sun.")
            || className.startsWith("jdk.")
            || className.startsWith("org.junit.") // Skip JUnit stuff
            || className.startsWith("com.thoughtworks.xstream") // Skip XStream stuff
            || className.startsWith("ch.qos.logback")   // Skip logging stuff
            || className.startsWith("org.slf4j.impl")   // Skip logging stuff
            || className.startsWith("org.mockito.internal.progress.SequenceNumber") // Skip some mockito stuff
            || className.startsWith("[")    // Skip array stuff
            || className.startsWith("edu.illinois.cs")) {
            return true;
        }
        return false;
    }

    private static LinkedHashMap<String, Object> capture() {
        if (MainAgent.getInstrumentation() == null) {
            System.out.println("NO INSTRUMENTATION");
            return new LinkedHashMap<>();
        }

        LinkedHashMap<String, Object> nameToInstance = new LinkedHashMap<String, Object>();;

        List<String> classes = new ArrayList<String>();
        Class[] loadedClasses = MainAgent.getInstrumentation().getAllLoadedClasses();
        for (Class clz : loadedClasses) {
            // Skip if one of our classes or top-level stuff
            String className = clz.getName();
            if (ignoreClass(className)) {
                continue;
            }

            Set<Field> allFields = new HashSet<Field>();
            try {
                Field[] declaredFields = clz.getDeclaredFields();
                Field[] fields = clz.getFields();
                allFields.addAll(Arrays.asList(declaredFields));
                allFields.addAll(Arrays.asList(fields));
            } catch (NoClassDefFoundError ex) {
                //continue;
            }

            for (Field f : allFields) {
                String fieldName = getFieldFQN(f);

                // If a field is final and has a primitive type there's no point to capture it.
                if (Modifier.isStatic(f.getModifiers()) 
                    && !(Modifier.isFinal(f.getModifiers()) &&  f.getType().isPrimitive())) {
                    try {
                        f.setAccessible(true);
                        nameToInstance.put(fieldName, f.get(null));
                    } catch (NoClassDefFoundError e) {
                        // Case of reflection not being able to find class, is in external library?
                        continue;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return nameToInstance;
    }

    private static Document stringToXmlDocument(String str) {
        try {           
            CharArrayReader rdr = new CharArrayReader(str.toCharArray());
            InputSource is = new InputSource(rdr);
            Document doc = dBuilder.parse(is);
            //cleanupDocument(doc);
            return doc;

        } catch(Exception ex) {
            ex.printStackTrace();
            return null;
        }

    }

    private static String documentToString(Document doc) {
        try{
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");        
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, result);
            String str = sw.toString();
            str = str.trim();
            str = str.substring(str.indexOf('\n') + 1);
            return str;
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    /**
     * Takes in a string and removes problematic characters.
     *
     * @param  in  the input string to be filtered
     * @return     the input string with the unparsable characters removed
     */
    public static String sanitizeXmlChars(String in) {
        in = in.replaceAll("&#", "&amp;#");
        StringBuilder out = new StringBuilder();
        char current;

        if (in == null || ("".equals(in))) 
            return "";
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i);
            if ((current == 0x9) ||
                (current == 0xA) ||
                (current == 0xD) ||
                ((current >= 0x20) && (current <= 0xD7FF)) ||
                ((current >= 0xE000) && (current <= 0xFFFD)) ||
                ((current >= 0x10000) && (current <= 0x10FFFF)))
                out.append(current);
        }
        return out.toString();
    }

    /**
     * This is the method that calls XStream to serialize the state map into a string.
     *
     * @param  state  the string to object map representing the roots of the state
     * @return        string representing the serialized input state
     */
    private static String serializeRoots(Map<String, Object> state) {
        XStream xstream = getXStreamInstance();
        String s = "";
        try {
            Map<String, Object> stateToSerialize = new LinkedHashMap<>();
            for (String k : state.keySet()) {
                Map<String, Object> tmp = new LinkedHashMap<>();
                tmp.put(k, state.get(k));
                try {
                    xstream.toXML(tmp);
                } catch (Exception e) {
                    System.out.println("KEY " + k + " IS BROKEN");
                    continue;
                }
                stateToSerialize.put(k, state.get(k));
            }
            s = xstream.toXML(stateToSerialize);
            s = sanitizeXmlChars(s); 
        } catch (Exception e) {
            // In case serialization fails, mark the StateCapture for this test
            // as dirty, meaning it should be ignored
            //dirty = true;
            //throw e;
            e.printStackTrace();
        }
        return s;
    }

    private static XStream getXStreamInstance() {
        XStream xstream = new XStream(new DomDriver());
        xstream.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        // Set fields to be omitted during serialization
        xstream.omitField(java.lang.ref.SoftReference.class, "timestamp");
        xstream.omitField(java.lang.ref.SoftReference.class, "referent");
        xstream.omitField(java.lang.ref.Reference.class, "referent");

        return xstream;
    }

    // If there are any extra fields in after not in before, add them
    private static String checkAdded(String beforeState, Set<String> beforeRoots, 
                              String afterState, Set<String> afterRoots) {
        Set<String> rootsDifference = new HashSet<String>(afterRoots);
        rootsDifference.removeAll(beforeRoots);

        if (rootsDifference.isEmpty()) {
            return afterState;
        }

        Document after = stringToXmlDocument(afterState);
        Element root = after.getDocumentElement();
        NodeList ls = root.getChildNodes();
        for (int i = 0; i < ls.getLength(); i++) {
            Node n = ls.item(i);
            if (n.getNodeName().equals("entry")) {
                Node keyNode = n.getChildNodes().item(1);
                if (rootsDifference.contains(keyNode.getTextContent())) {
                    Node tmp = n.getPreviousSibling();
                    root.removeChild(n);
                    root.removeChild(tmp);
                    i = i - 2;
                }
            }
        }

        if (ls.getLength() == 1) {
            root.removeChild(ls.item(0));
        }
        
        return documentToString(after);
    }

    // Finds the differences, makes the report in sb, returns the roots for convenience
    private static Set<String> makeDifferenceReport(Difference difference, String xmlDoc, StringBuilder sb) {
        NodeDetail controlNode = difference.getControlNodeDetail();
        NodeDetail afterNode = difference.getTestNodeDetail();
              
        Set<String> roots = new HashSet<String>();

        String diffXpath = controlNode.getXpathLocation();
        if (diffXpath == null) {
            diffXpath = afterNode.getXpathLocation();
            if (diffXpath == null) {
                sb.append("NULL xpath\n");
                return roots;
            }
        }
        String[] elems = diffXpath.split("/");
        if (elems.length >= 3) {
            diffXpath = "/" + elems[1] + "/" + elems[2];
            try {                 
                XPath xPath =  XPathFactory.newInstance().newXPath();
                Node n = (Node) xPath.compile(diffXpath).evaluate(stringToXmlDocument(xmlDoc), XPathConstants.NODE);
                n = n.getChildNodes().item(1);
                if (n != null) {
                    roots.add(n.getTextContent());

                    sb.append("Static root: ");
                    sb.append(n.getTextContent());
                    sb.append("\n");
                    sb.append(controlNode.getXpathLocation());
                    sb.append("\n");
                    sb.append(afterNode.getXpathLocation());
                    sb.append("\n");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return roots;
    }        

    private static void recordDiff(String testname) {
        // Serialize everything
        String beforeState = serializeRoots(beforeMapping);
        Set<String> beforeRoots = new HashSet<String>(beforeMapping.keySet());
        String afterState = serializeRoots(afterMapping);
        Set<String> afterRoots = new HashSet<String>(afterMapping.keySet());

        // Returns a new afterState only having the roots that are common with the beforeState
        afterState = checkAdded(beforeState, beforeRoots, afterState, afterRoots);

        try {
            boolean statesAreSame = beforeState.equals(afterState);

            // create a string builder
            StringBuilder sb = new StringBuilder();
            sb.append(statesAreSame);
            sb.append("\n");

            Diff diff = new Diff(beforeState, afterState);
            DetailedDiff detDiff = new DetailedDiff(diff);
            List differences = detDiff.getAllDifferences();
            Collections.sort(differences, new Comparator() {
                    public int compare(Object o1, Object o2) {
                        Difference d1 = (Difference)o1;
                        Difference d2 = (Difference)o2;
                        // Sort based on id, which should represent order in the XML
                        if (d1.getId() < d2.getId()) {
                            return -1;
                        }
                        else if (d1.getId() == d2.getId()) {
                            return 0;
                        }
                        else {
                            return 1;
                        }
                    }
                });

            roots = new HashSet<String>();
            for (Object object : differences) {
                Difference difference = (Difference)object;
                
                sb.append("***********************\n");
                roots.addAll(makeDifferenceReport(difference, beforeState, sb));
                sb.append("\n~~~~\n");
                sb.append(difference);
                sb.append("***********************\n");
            }

            if (differences.size() > 0) {
                polluters.add(testname);
                // Output roots into file for the test
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(".dtfixingtools/" + testname));
                    //writer.write(roots.toString());
                    writer.write(sb.toString());
                    writer.write("\n");
                    writer.close();
                } catch (IOException ex) {
                }
            }
            //System.out.println("TEST " + testname + " ROOTS " + roots);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static String getFieldFQN(Field f) {
        String clz = f.getDeclaringClass().getName();
        String fld = f.getName();
        return clz + "." + fld;
    }
}
