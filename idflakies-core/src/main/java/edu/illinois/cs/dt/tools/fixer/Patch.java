package edu.illinois.cs.dt.tools.fixer;

import edu.illinois.cs.dt.tools.utility.PathManager;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class Patch {
    private JavaMethod methodToPatch;   // Method where patch is applied (prepended or appended)
    private BlockStmt patchedBlock;     // The block to patch into the method
    private boolean prepend;            // Whether to prepend or append to the method
    private JavaMethod cleanerMethod;   // Cleaner method where the patch came from

    // Information that is mainly used in case applying patch cannot inline, needs combination
    private List<Path> testFiles;
    private String classpath;
    private boolean inlineSuccessful;

    private JavaMethod victimMethod;    // Keep around just for bookkeeping

    public Patch(JavaMethod methodToPatch, BlockStmt patchedBlock, boolean prepend,
                 JavaMethod cleanerMethod,
                 JavaMethod victimMethod,
                 List<Path> testFiles, String classpath,
                 boolean inlineSuccessful) {
        this.methodToPatch = methodToPatch;
        this.patchedBlock = patchedBlock;
        this.prepend = prepend;
        this.cleanerMethod = cleanerMethod;
        this.victimMethod = victimMethod;
        this.testFiles = testFiles;
        this.classpath = classpath;
        this.inlineSuccessful = inlineSuccessful;
    }

    public JavaMethod methodToPatch() {
        return this.methodToPatch;
    }

    public BlockStmt patchedBlock() {
        return this.patchedBlock;
    }

    public boolean prepend() {
        return this.prepend;
    }

    public JavaMethod cleanerMethod() {
        return this.cleanerMethod;
    }

    public JavaMethod victimMethod() {
        return this.victimMethod;
    }

    public List<Path> testFiles() {
        return this.testFiles;
    }

    public String classpath() {
        return this.classpath;
    }

    public boolean inlineSuccessful() {
        return this.inlineSuccessful;
    }

    public void applyPatch() throws Exception {
        // If able to inline, then only need to put patch into the method to patch
        if (this.inlineSuccessful) {
            if (this.prepend) {
                this.methodToPatch.prepend(this.patchedBlock.getStatements());
            } else {
                this.methodToPatch.append(this.patchedBlock.getStatements());
            }
            this.methodToPatch.javaFile().writeAndReloadCompilationUnit();
        } else {
            // Otherwise, need to add the statements into fresh cleanerHelper in cleaner method's file, and call to it
            // The modification is to modify the cleaner class to add a helper, then have the other method call the helper
            ExpressionStmt helperCallStmt = getHelperCallStmt(cleanerMethod);
            if (this.prepend) {
                this.methodToPatch.prepend(NodeList.nodeList(helperCallStmt));
            } else {
                this.methodToPatch.append(NodeList.nodeList(helperCallStmt));
            }
            this.methodToPatch.javaFile().writeAndReloadCompilationUnit();

            String helperName = this.cleanerMethod.getClassName() + ".cleanerHelper";
            this.cleanerMethod = JavaMethod.find(this.cleanerMethod.methodName(), this.testFiles, this.classpath).get();
            this.cleanerMethod.javaFile().addMethod(helperName);
            this.cleanerMethod.javaFile().writeAndReloadCompilationUnit();
            JavaMethod helperMethod = JavaMethod.find(helperName, this.testFiles, this.classpath).get();
            helperMethod.javaFile().writeAndReloadCompilationUnit();

            this.methodToPatch = JavaMethod.find(this.methodToPatch.methodName(), this.testFiles, this.classpath).get();
        }
    }

    public void restore() throws Exception {
        Path path = PathManager.backupPath(this.methodToPatch.javaFile().path());
        Files.copy(path, this.methodToPatch.javaFile().path(), StandardCopyOption.REPLACE_EXISTING);
        if (!inlineSuccessful) {
            path = PathManager.backupPath(this.cleanerMethod.javaFile().path());
            Files.copy(path, this.cleanerMethod.javaFile().path(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private ExpressionStmt getHelperCallStmt(JavaMethod cleanerMethod) {
        Expression objectCreation = new ObjectCreationExpr(null,
            new ClassOrInterfaceType(null, cleanerMethod.getClassName()), NodeList.nodeList());
        Expression helperCall = new MethodCallExpr(objectCreation, "cleanerHelper");
        ExpressionStmt helperCallStmt = new ExpressionStmt(helperCall);
        return helperCallStmt;
    }
}
