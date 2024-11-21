package edu.illinois.cs.dt.tools.fixer;

import edu.illinois.cs.dt.tools.runner.InstrumentingSmartRunner;
import edu.illinois.cs.dt.tools.utility.BuildCommands;
import edu.illinois.cs.dt.tools.utility.Level;
import edu.illinois.cs.dt.tools.utility.Logger;
import edu.illinois.cs.dt.tools.utility.deltadebug.DeltaDebugger;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.Statement;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

public class CleanerFixerDeltaDebugger extends DeltaDebugger<Statement> {

    private final BuildCommands buildCommands;
    private final InstrumentingSmartRunner runner;
    private final JavaMethod methodToModify;
    private final List<String> failingOrder;
    private final boolean prepend;

    public CleanerFixerDeltaDebugger(BuildCommands buildCommands, InstrumentingSmartRunner runner,
                                     JavaMethod methodToModify, List<String> failingOrder,
                                     boolean prepend) {
        this.buildCommands = buildCommands;
        this.runner = runner;
        this.methodToModify = methodToModify;
        this.failingOrder = failingOrder;
        this.prepend = prepend;
    }

    // Cleaner statements are valid if using them leads to order-dependent test to pass
    @Override
    public boolean checkValid(List<Statement> statements) {
        return checkValid(statements, true);
    }

    public boolean checkValid(List<Statement> statements, boolean suppressError) {
        // Converting to NodeList
        NodeList<Statement> cleanerStmts = NodeList.nodeList();
        cleanerStmts.addAll(statements);

        // Giant try-catch block to handle odd case if cannot write to Java file on disk
        try {
            // If want to prepend set to true, then prepend to victim
            if (this.prepend) {
                this.methodToModify.prepend(cleanerStmts);
            } else {
                this.methodToModify.append(cleanerStmts);
            }
            this.methodToModify.javaFile().writeAndReloadCompilationUnit();

            // Rebuild and see if tests run properly
            try {
                this.buildCommands.install();
            } catch (Exception ex) {
                Logger.getGlobal().log(Level.FINE, "Error building the code, passed in cleaner code does not compile");
                // Reset the change
                if (this.prepend) {
                    this.methodToModify.removeFirstBlock();
                } else {
                    this.methodToModify.removeLastBlock();
                }
                this.methodToModify.javaFile().writeAndReloadCompilationUnit();
                return false;
            }
            boolean passInFailingOrder = testOrderPasses(this.failingOrder);

            // Reset the change
            if (this.prepend) {
                this.methodToModify.removeFirstBlock();
            } else {
                this.methodToModify.removeLastBlock();
            }
            this.methodToModify.javaFile().writeAndReloadCompilationUnit();

            return passInFailingOrder;
        } catch (IOException ioe) {
            Logger.getGlobal().log(Level.SEVERE, "Problem with writing to Java file!");
            return false;
        }

        // TODO: Make sure our fix doesn't break any other tests
        //  This block of code could be useful when dealing with a case where we add the necessary
        //  setup to a victim test but our "fix" would actually now cause another test to fail.
        //  We will need some additional logic to deal with this case (i.e., a fix reveals another
        //  dependency) than just this block of code, but it may have its uses later.
        // Check if we pass in the whole test class
        // Should check before fix if class is passing
        //        boolean didClassPass = didTestsPass(victimJavaFile.getTestListAsString());

        //        if (didClassPass) {
        //            boolean didClassPassAfterFix = didTestsPass(victimJavaFile.getTestListAsString());
        //            if (!didClassPassAfterFix) {
        //                System.out.println("Fix was unsuccessful. Fix causes some other test in the class to fail.");
        //                return;
        //            }
        //        }
    }

    // Helper method for determining if a specific test order passes
    private boolean testOrderPasses(final List<String> tests) {
        return new FailingTestDetector(this.runner).notPassingTests(tests).orElse(new HashSet<>()).isEmpty();
    }
}
