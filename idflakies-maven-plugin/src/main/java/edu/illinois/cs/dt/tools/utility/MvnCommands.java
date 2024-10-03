package edu.illinois.cs.dt.tools.utility;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamHandler;

import edu.illinois.cs.dt.tools.utility.Level;
import edu.illinois.cs.dt.tools.utility.Logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Properties;

public class MvnCommands extends BuildCommands {

    private MavenProject project;
    private boolean suppressOutput;

    public MvnCommands(MavenProject project, boolean suppressOutput) {
        this.project = project;
        this.suppressOutput = suppressOutput;
    }

    // Running mvn install, just to build and compile code (no running tests)
    @Override
    public void install() {
	     // Check which argument is provided
        boolean detectUnitTest = Boolean.parseBoolean(System.getProperty("detectUnitTest", "true"));
        boolean detectITTest = Boolean.parseBoolean(System.getProperty("detectITTest", "false"));

        // Choose the corresponding goal based on the argument
        String mojoGoal = "install"; // default

        if (detectUnitTest) {
            // Use the goal for the unit test-specific Mojo
            mojoGoal = "detect"; 
        } else if (detectITTest) {
            // Use the goal for the IT test-specific Mojo
            mojoGoal = "detect-it"; 
        }

        // TODO: Maybe support custom command lines/options?
        final InvocationRequest request = new DefaultInvocationRequest();
        //request.setGoals(Arrays.asList("install"));
	request.setGoals(Arrays.asList(mojoGoal));
	request.setPomFile(project.getFile());
        request.setProperties(new Properties());
        request.getProperties().setProperty("skipTests", "true");
        request.getProperties().setProperty("rat.skip", "true");
        request.getProperties().setProperty("dependency-check.skip", "true");
        request.getProperties().setProperty("enforcer.skip", "true");
        request.getProperties().setProperty("checkstyle.skip", "true");
        request.getProperties().setProperty("maven.javadoc.skip", "true");
        request.getProperties().setProperty("maven.source.skip", "true");
        request.getProperties().setProperty("gpg.skip", "true");
        request.setUpdateSnapshots(false);

        ByteArrayOutputStream baosOutput = new ByteArrayOutputStream();
        PrintStream outputStream = new PrintStream(baosOutput);
        request.setOutputHandler(new PrintStreamHandler(outputStream, true));
        ByteArrayOutputStream baosError = new ByteArrayOutputStream();
        PrintStream errorStream = new PrintStream(baosError);
        request.setErrorHandler(new PrintStreamHandler(errorStream, true));

        try {
            final Invoker invoker = new DefaultInvoker();
            final InvocationResult result = invoker.execute(request);

            if (result.getExitCode() != 0) {
                // Print out the contents of the output/error streamed out during evocation, if not suppressed
                if (!suppressOutput) {
                    Logger.getGlobal().log(Level.SEVERE, baosOutput.toString());
                    Logger.getGlobal().log(Level.SEVERE, baosError.toString());
                }

                if (result.getExecutionException() == null) {
                    throw new RuntimeException("Compilation failed with exit code " + result.getExitCode() + " for an unknown reason");
                } else {
                    throw new RuntimeException(result.getExecutionException());
                }
            }
        } catch (MavenInvocationException mie) {
            throw new RuntimeException(mie);
        }
    }
}
