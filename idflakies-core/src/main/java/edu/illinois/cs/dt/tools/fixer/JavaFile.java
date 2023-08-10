package edu.illinois.cs.dt.tools.fixer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.google.common.base.Preconditions;

import edu.illinois.cs.dt.tools.utility.PathManager;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A basic Java file which supports compiling/removing methods.
 */
public class JavaFile {
    public static JavaFile loadFile(final Path path, final String classpath, final Path compiledOutputDir) throws IOException {
        return new JavaFile(path, classpath, compiledOutputDir).loadClassList();
    }

    public static int countDiagnostics(final DiagnosticCollector<JavaFileObject> diagnostics,
                                        final Diagnostic.Kind kind) {
        return Math.toIntExact(diagnostics.getDiagnostics().stream().filter(diag -> diag.getKind().equals(kind)).count());
    }

    private CompilationUnit compilationUnit;
    private List<ClassOrInterfaceDeclaration> classList = new ArrayList<>();
    private Map<ClassOrInterfaceDeclaration, List<MethodDeclaration>> classToMethods = new HashMap<>();
    private Map<ClassOrInterfaceDeclaration, List<MethodDeclaration>> classToTestMethods = new HashMap<>();
    private final Path path;
    private final String classPath;
    private final Path compiledOutputDir;

    private JavaFile(final Path path, final String classPath, final Path compiledOutputDir) {
        this.path = path;
        this.classPath = classPath;
        this.compiledOutputDir = compiledOutputDir;
    }

    public List<Diagnostic<? extends JavaFileObject>> compile() throws Exception {
        final DiagnosticCollector<JavaFileObject> diagnostics = tryCompile();

        return diagnostics.getDiagnostics().stream()
                .filter(diag -> diag.getKind().equals(Diagnostic.Kind.ERROR))
                .collect(Collectors.toList());
    }

    public Path path() {
        return path;
    }

    public CompilationUnit compilationUnit() {
        return compilationUnit;
    }

    public Path compiledOutputPath() {
        return compiledOutputDir;
    }

    /**
     * Finds all classes/interfaces in the file and saves them.
     */
    private JavaFile loadClassList() throws IOException {
        compilationUnit = JavaParser.parse(path);

        classList.clear();
        classList.addAll(compilationUnit.findAll(ClassOrInterfaceDeclaration.class));

        classToMethods.clear();
        for (final ClassOrInterfaceDeclaration classDec : classList) {
           final List<MethodDeclaration> methods = classDec.findAll(MethodDeclaration.class);
           classToMethods.put(classDec, methods);
        }

        classToTestMethods.clear();
        classToMethods.forEach((classDec, methods) -> {
            final List<MethodDeclaration> testMethods = new ArrayList<>();
            for (final MethodDeclaration method : methods) {
                if (areJUnitAnnotations(method.getAnnotations()) ||
                        method.getNameAsString().startsWith("test")) {
                    testMethods.add(method);
                }
            }
            if (!testMethods.isEmpty()) {
                classToTestMethods.put(classDec, testMethods);
            }
        });

        return this;
    }

    private boolean areJUnitAnnotations(final List<AnnotationExpr> annotations) {
        return annotations.stream().anyMatch(expr -> expr.getNameAsString().startsWith("Test"));
    }

    public List<String> getTestListAsString() {
        final List<String> retList = new ArrayList<>();
        for (final ClassOrInterfaceDeclaration classDec : classToTestMethods.keySet()) {
            for (final MethodDeclaration method : classToTestMethods.get(classDec)) {
                retList.add(getFullyQualifiedMethodName(method, classDec));
            }
        }
        return retList;
    }

    private void writeFile() throws IOException {
        Files.write(path(), compilationUnit.toString().getBytes());
    }

    public void writeAndReloadCompilationUnit() throws IOException {
        writeFile();
        loadClassList();
    }

    public MethodDeclaration findMethodAt(final long line) {
        for (final ClassOrInterfaceDeclaration classDeclaration : classList) {
            for (final BodyDeclaration bodyDeclaration : classDeclaration.getMembers()) {
                if (bodyDeclaration instanceof MethodDeclaration) {
                    final MethodDeclaration method = (MethodDeclaration)bodyDeclaration;
                    final Position begin = method.getBegin().orElseThrow(() -> new RuntimeException("Cannot get start line for " + method.getSignature()));
                    final Position end = method.getEnd().orElseThrow(() -> new RuntimeException("Cannot get end line for " + method.getSignature()));

                    if (begin.line <= line && end.line >= line) {
                        return method;
                    }
                }
            }
        }

        return null;
    }

    public MethodDeclaration findMethodDeclaration(final String name) {
        for (final ClassOrInterfaceDeclaration classDeclaration : classList) {
            for (final BodyDeclaration bodyDeclaration : classDeclaration.getMembers()) {
                if (bodyDeclaration instanceof MethodDeclaration) {
                    final MethodDeclaration method = (MethodDeclaration)bodyDeclaration;
                    final String fullMethodName = getFullyQualifiedMethodName(method, classDeclaration);
                    if (fullMethodName.equalsIgnoreCase(name)) {
                        return method;
                    }
                }
            }
        }

        return null;
    }

    public List<MethodDeclaration> findMethodsWithAnnotation(String annotation) {
        List<MethodDeclaration> methods = new ArrayList<>();
        for (final ClassOrInterfaceDeclaration classDeclaration : classList) {
            for (final BodyDeclaration bodyDeclaration : classDeclaration.getMembers()) {
                if (bodyDeclaration instanceof MethodDeclaration) {
                    final MethodDeclaration method = (MethodDeclaration)bodyDeclaration;
                    for (AnnotationExpr annotationExpr : method.getAnnotations()) {
                        if (annotationExpr.toString().equals(annotation)) {
                            methods.add(method);
                            break;
                        }
                    }
                }
            }
        }
        return methods;
    }

    private String getFullyQualifiedMethodName(MethodDeclaration method, ClassOrInterfaceDeclaration classDec) {
        final Optional<PackageDeclaration> packageDec = compilationUnit.getPackageDeclaration();

        Preconditions.checkArgument(packageDec.isPresent(), "No package declaration found for class: " + classDec.getNameAsString());

        return String.format("%s.%s.%s",
                packageDec.get().getName().toString(),
                classDec.getNameAsString(),
                method.getName().getIdentifier());
    }


    /**
     * Attempts to remove the method declaration.
     * @param method The method declaration to remove.
     * @return A String of the fully qualified name of the method removed if found, null otherwise.
     */
    public String removeMethod(final MethodDeclaration method) {
        for (final ClassOrInterfaceDeclaration classDeclaration : classList) {
            final MethodRemoverVisitor remover = new MethodRemoverVisitor(method);
            classDeclaration.accept(remover, null);

            final Optional<PackageDeclaration> packageDec = compilationUnit.getPackageDeclaration();

            if (remover.succeeded()) {
                return createRemovedMethodString(packageDec.map(PackageDeclaration::getNameAsString).orElse(""), classDeclaration, method);
            }
        }

        return null;
    }

    // Adding a void, no parameter method (annotated with @Test just to overcome error-prone)
    public MethodDeclaration addMethod(final String method) {
        // First check if method exists, and don't do anything if it already does
        MethodDeclaration existingMethod = findMethodDeclaration(method);
        if (existingMethod != null) {
            return existingMethod;
        }

        // Search for the class that this one should belong to
        String className = method.substring(0, method.lastIndexOf('.'));
        className = className.substring(className.lastIndexOf('.') + 1);
        String methodName = method.substring(method.lastIndexOf('.') + 1);
        for (final ClassOrInterfaceDeclaration classDeclaration : classList) {
            if (classDeclaration.getNameAsString().equals(className)) {
                return classDeclaration.addMethod(methodName, Modifier.PUBLIC);
            }
        }
        return null;
    }

    public MethodDeclaration addMethod(final String method, final String annotation) {
        // First check if method exists, and don't do anything if it already does
        MethodDeclaration existingMethod = findMethodDeclaration(method);
        if (existingMethod != null) {
            return existingMethod;
        }

        // Search for the class that this one should belong to
        String className = method.substring(0, method.lastIndexOf('.'));
        className = className.substring(className.lastIndexOf('.') + 1);
        String methodName = method.substring(method.lastIndexOf('.') + 1);
        for (final ClassOrInterfaceDeclaration classDeclaration : classList) {
            if (classDeclaration.getNameAsString().equals(className)) {
                MethodDeclaration newMethod = classDeclaration.addMethod(methodName, Modifier.PUBLIC);
                newMethod.addAnnotation(annotation);
                return newMethod;
            }
        }
        return null;
    }

    /**
     * @return The fully qualified name of this method: packageName.className.methodName(paramNames)
     */
    private static String createRemovedMethodString(final String packageName,
                                                    final ClassOrInterfaceDeclaration classDeclaration,
                                                    final MethodDeclaration method) {
        // Not using .getDeclarationAsString because it includes the return type, which wouldn't work with concatting below
        final String methodName = method.getName() + "(" + getParametersAsString(method) + ")";

        return packageName + classDeclaration.getName() + "." + methodName;
    }

    /**
     * Ex: For int f(int a, int b), returns "int a, int b"
     * @return A string containing the parameters separated by commas.
     */
    private static String getParametersAsString(final MethodDeclaration method) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < method.getParameters().size(); i++) {
            final Parameter parameter = method.getParameters().get(i);

            result.append(parameter.toString());

            if (i != (method.getParameters().size() - 1)) {
                result.append(", ");
            }
        }

        return result.toString();
    }

    /**
     * Writes the file to the output path, then tries to compile the output file.
     */
    private DiagnosticCollector<JavaFileObject> tryCompile() throws IOException {
        writeAndReloadCompilationUnit();
        return runCompilation();
    }

    private DiagnosticCollector<JavaFileObject> runCompilation() throws IOException {
        final File file = Objects.requireNonNull(path()).toFile();

        final JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        final List<String> compilerOptions =
                new ArrayList<>(Arrays.asList("-classpath", classPath));

        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (final StandardJavaFileManager fileManager =
                     javaCompiler.getStandardFileManager(diagnostics, null, null)) {
            final Iterable<? extends JavaFileObject> fileObjects =
                    fileManager.getJavaFileObjectsFromFiles(Collections.singletonList(file));

            final JavaCompiler.CompilationTask compilationTask =
                    javaCompiler.getTask(null, fileManager, diagnostics, compilerOptions, null, fileObjects);
            compilationTask.call();

            // Move to compile output path
            final Path compiledPath = PathManager.changeExtension(path(), "class");
            final Path outputPath = PathManager.compiledPath(path());

            System.out.println("[INFO] Compiling to " + outputPath);
            Files.move(compiledPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
        }

        return diagnostics;
    }

    public List<MethodDeclaration> getMethodsWithErrors(DiagnosticCollector<JavaFileObject> diagnostics)
            throws Exception {
        final List<MethodDeclaration> methodsWithErrors = new ArrayList<>();

        for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                final MethodDeclaration method = findMethodAt(diagnostic.getLineNumber());
                if (method != null) {
                    if (!methodsWithErrors.contains(method)) {
                        methodsWithErrors.add(method);
                    }
                } else {
                    throw new Exception("An error has occurred in an unknown method:\n"
                                                + " Message: " + diagnostic.getMessage(Locale.getDefault()) + "\n"
                                                + " Source: " + diagnostic.getSource() + "\n"
                                                + " Code: " + diagnostic.getCode() + "\n"
                                                + " Kind: " + diagnostic.getKind() + "\n"
                                                + " Line: " + diagnostic.getLineNumber()
                                                + " Position: " + diagnostic.getPosition() + "\n");
                }
            } else {
                System.out.println("Ignoring the following diagnostic: "
                                           + diagnostic.getMessage(Locale.getDefault()));
            }
        }

        return methodsWithErrors;
    }


    private final class MethodRemoverVisitor extends ModifierVisitor<Void> {
        private final MethodDeclaration method;

        private boolean found = false;

        MethodRemoverVisitor(final MethodDeclaration method) {
            this.method = method;
        }

        @Override
        public Visitable visit(MethodDeclaration n, Void arg) {
            if (n.getSignature().equals(method.getSignature())) {
                this.found = true;

                return null;
            }

            return super.visit(n, arg);
        }

        boolean succeeded() {
            return found;
        }
    }
}
