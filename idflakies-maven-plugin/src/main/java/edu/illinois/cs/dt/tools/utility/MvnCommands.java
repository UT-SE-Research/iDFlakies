package edu.illinois.cs.dt.tools.utility;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
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

public class MvnCommands {

    // Running mvn install, just to build and compile code (no running tests)
    public static boolean runMvnInstall(MavenProject project, boolean suppressOutput) throws MavenInvocationException {
        // TODO: Maybe support custom command lines/options?
        final InvocationRequest request = new DefaultInvocationRequest();
        request.setGoals(Arrays.asList("install"));
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

        return true;
    }
}
