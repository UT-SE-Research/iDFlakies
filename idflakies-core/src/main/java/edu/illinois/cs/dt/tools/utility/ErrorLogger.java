package edu.illinois.cs.dt.tools.utility;

import edu.illinois.cs.dt.tools.detection.DetectorPathManager;
import edu.illinois.cs.testrunner.util.ProjectWrapper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.Callable;

public class ErrorLogger {
    private final String coordinates;
    private final ProjectWrapper project;

    public ErrorLogger(final ProjectWrapper project) {
        this.project = project;
        this.coordinates = project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion();
    }

    public String coordinates() {
        return coordinates;
    }

    private String subjectName(final ProjectWrapper project) {
        final Path relativePath =
                PathManager.parentPath(project).getParent().toAbsolutePath()
                        .relativize(project.getBasedir().toPath().toAbsolutePath());

        return relativePath.toString().replace("/", "-");
    }

    public <T> Optional<T> runAndLogError(final Callable<T> callable) {
        try {
            return Optional.ofNullable(callable.call());
        } catch (Throwable t) {
            writeError(t);
        } finally {
            System.out.println("TRY_COPY_ALL_FAILING_TEST_OUTPUT");
            try {
                Files.list(project.getBasedir().toPath()).forEach(path -> {
                    if (path.getFileName().toString().startsWith("failing-test-output")) {
                        try {
                            System.out.println("COPY_FAILING_TEST_OUTPUT: " + path.toAbsolutePath());

                            final Path dest = PathManager.cachePath(project.getBasedir()).resolve(path.getFileName());

                            if (Files.exists(dest)) {
                                Files.copy(path, PathManager.cachePath(project.getBasedir()).resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                            } else {
                                Files.copy(path, PathManager.cachePath(project.getBasedir()).resolve(path.getFileName()));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return Optional.empty();
    }

    public void writeError(final Throwable t) {
        try {
            System.out.println("---------------------------------------------------");
            System.out.println("ERROR (WRITE_ERROR_STDOUT_THROWABLE): " + coordinates);
            t.printStackTrace();

            t.printStackTrace(new PrintStream(new FileOutputStream(String.valueOf(DetectorPathManager.errorPath(project.getBasedir())))));
            Files.write(DetectorPathManager.errorPath(project.getBasedir()), ("\n" + coordinates).getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("ERROR (FAIL_OUTPUT_ERR_THROWABLE): Failed to output error!");
            e.printStackTrace();
            System.out.println("---------------------------------------------------");
            t.printStackTrace();
        }
    }

    public void writeError(final String msg) {
        try {
            System.out.println("---------------------------------------------------");
            System.out.println("ERROR (WRITE_ERROR_STDOUT_STRING): " + coordinates);
            System.out.println("Message was:");
            System.out.println(msg);

            Files.write(DetectorPathManager.errorPath(project.getBasedir()), (msg + "\n").getBytes());
            Files.write(DetectorPathManager.errorPath(project.getBasedir()), ("\n" + coordinates).getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("ERROR (FAIL_OUTPUT_ERR_STRING): Failed to output error!");
            e.printStackTrace();
            System.out.println("---------------------------------------------------");
            System.out.println("Message was:");
            System.out.println(msg);
        }
    }
}
