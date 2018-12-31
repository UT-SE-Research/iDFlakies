package edu.illinois.cs.dt.tools.runner.data;

import com.reedoei.eunomia.io.files.FileUtil;
import com.reedoei.eunomia.util.Util;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.AnyTypePermission;
import edu.illinois.cs.dt.tools.diagnosis.DiffContainer;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class TestResult {
    private static Map<String,DiffContainer> readXml() throws IOException, DocumentException {
        // TODO: Make this nicer... It really is safe though
        try {
            return (Map<String, DiffContainer>) getXStreamInstance().fromXML(FileUtil.readFile(Paths.get("state-diff.xml")));
        } catch (Exception e) {
            // Probably some issue with xstream deserialization.
            // Just grab the names of the fields, that'll be good enough.
            return makeDiffs(Paths.get("state-diff.xml"));
        }
    }

    private static Map<String,DiffContainer> makeDiffs(final Path path)
            throws MalformedURLException, DocumentException {
        final Map<String, DiffContainer> diffMap = new HashMap<>();

        final Document document = Util.readXmlDoc(path.toFile());

        for (final Element entry : document.getRootElement().elements("entry")) {
            final String testName = entry.elementText("string");

            final Map<String, String> before = new HashMap<>();
            final Map<String, String> after = new HashMap<>();

            for (final Node node : entry.selectNodes("edu.illinois.cs.dt.tools.diagnosis.DiffContainer/diffs/entry/string")) {
                // Dummy strings so we're guaranteed a diff on every key. This is fine because the map
                // in the XML file should only contain keys that differ anyway.
                before.put(node.getText(), "before");
                after.put(node.getText(), "after");
            }

            diffMap.put(testName, new DiffContainer(testName, before, after));
        }

        return diffMap;
    }

    public static XStream getXStreamInstance() {
        XStream xstream = new XStream(); // new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);
        xstream.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        // Set fields to be omitted during serialization
        xstream.omitField(java.lang.ref.SoftReference.class, "timestamp");
        xstream.omitField(java.lang.ref.SoftReference.class, "referent");
        xstream.omitField(java.lang.ref.Reference.class, "referent");

        return xstream;
    }
}
