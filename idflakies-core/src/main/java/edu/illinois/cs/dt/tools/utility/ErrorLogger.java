package edu.illinois.cs.dt.tools.utility;

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

    public <T> Optional<T> runAndLogError(final Callable<T> callable) {
        try {
            return Optional.ofNullable(callable.call());
        } catch (Throwable th) {
            writeError(th);
        } finally {
            System.out.println("TRY_COPY_ALL_FAILING_TEST_OUTPUT");
            try {
                Files.list(PathManager.modulePath()).forEach(path -> {
                    if (path.getFileName().toString().startsWith("failing-test-output")) {
                        try {
                            System.out.println("COPY_FAILING_TEST_OUTPUT: " + path.toAbsolutePath());

                            final Path dest = PathManager.cachePath().resolve(path.getFileName());

                            if (Files.exists(dest)) {
                                Files.copy(path, PathManager.cachePath().resolve(path.getFileName()),
                                    StandardCopyOption.REPLACE_EXISTING);
                            } else {
                                Files.copy(path, PathManager.cachePath().resolve(path.getFileName()));
                            }
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    }
                });
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        return Optional.empty();
    }

    public void writeError(final Throwable th) {
        try {
            System.out.println("---------------------------------------------------");
            th.printStackTrace();

            th.printStackTrace(new PrintStream(new FileOutputStream(String.valueOf(PathManager.errorPath()))));
        } catch (IOException ioe) {
            System.out.println("ERROR (FAIL_OUTPUT_ERR_THROWABLE): Failed to output error!");
            ioe.printStackTrace();
            System.out.println("---------------------------------------------------");
            th.printStackTrace();
        }
    }

    public void writeError(final String msg) {
        try {
            System.out.println("---------------------------------------------------");
            System.out.println("Message was:");
            System.out.println(msg);

            Files.write(PathManager.errorPath(), (msg + "\n").getBytes());
        } catch (IOException ioe) {
            System.out.println("ERROR (FAIL_OUTPUT_ERR_STRING): Failed to output error!");
            ioe.printStackTrace();
            System.out.println("---------------------------------------------------");
            System.out.println("Message was:");
            System.out.println(msg);
        }
    }
}
