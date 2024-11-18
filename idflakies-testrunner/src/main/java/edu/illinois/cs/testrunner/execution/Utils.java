package edu.illinois.cs.testrunner.execution;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private static final String junit5BasicRegex = "\\[class:([\\w.]+).*\\[method:([\\w().]+)";
    private static final Pattern junit5BasicPattern = Pattern.compile(junit5BasicRegex);
    private static final String junit5NestedRegex = "\\[nested-class:([\\w.]+)\\]";
    private static final Pattern junit5NestedPattern = Pattern.compile(junit5NestedRegex);
    private static final String junit4Regex = "\\[test:(\\w+)\\(([\\w.]+)\\)";
    private static final Pattern junit4Pattern = Pattern.compile(junit4Regex);

    /**
     * Turn the uniqueId from identifier into fully qualified method name.
     *
     * For JUnit 5:
     * uniqueId: [engine:junit-jupiter]/[class:com.luojl.demo.JUnit5DemoTest]/[method:TestC()]
     * full qualified name: com.luojl.demo.JUnit5DemoTest#TestC()
     *
     * For JUnit 5 nested test:
     * uniqueId: [engine:junit-jupiter]/[class:com.luojl.demo.InheritedTest]/[nested-class:NestedTest]/[method:NestedTestB()]
     * fully qualified name: com.luojl.demo.InheritedTest$NestedTest#NestedTestB()
     *
     * For JUnit 4:
     * uniqueId: [engine:junit-vintage]/[runner:com.luojl.demo.JUnit4DemoTest]/[test:TestA4(com.luojl.demo.JUnit4DemoTest)]
     * full qualified name: com.luojl.demo.JUnit4DemoTest#TestA4
     */
    public static String toFullyQualifiedName(String identifierUniqueId) {
        Matcher matcher = junit5BasicPattern.matcher(identifierUniqueId);
        if (matcher.find()) {
            // found JUnit 5 basic pattern: class + method
            StringBuilder sb = new StringBuilder();
            sb.append(matcher.group(1));  // com.package.ClassName

            Matcher nestedMatcher = junit5NestedPattern.matcher(identifierUniqueId);
            while (nestedMatcher.find()) {
                // found nested class
                // may nest multiple layers
                sb.append("$");
                sb.append(nestedMatcher.group(1));
            }

            sb.append("#");
            sb.append(matcher.group(2));  // method
            return sb.toString();
        }
        // fall back to JUnit 4
        matcher = junit4Pattern.matcher(identifierUniqueId);
        if (matcher.find()) {
            return matcher.group(2) + "#" + matcher.group(1);
        }
        throw new IllegalStateException(
            "Fail to parse identifierUniqueId: " + identifierUniqueId);
    }
}
