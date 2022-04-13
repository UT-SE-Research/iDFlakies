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
        } catch (Throwable t) {
            writeError(t);
        } finally {
            System.out.println("TRY_COPY_ALL_FAILING_TEST_OUTPUT");
            try {
                Files.list(PathManager.modulePath()).forEach(path -> {
                    if (path.getFileName().toString().startsWith("failing-test-output")) {
                        try {
                            System.out.println("COPY_FAILING_TEST_OUTPUT: " + path.toAbsolutePath());

                            final Path dest = PathManager.cachePath().resolve(path.getFileName());

                            if (Files.exists(dest)) {
                                Files.copy(path, PathManager.cachePath().resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                            } else {
                                Files.copy(path, PathManager.cachePath().resolve(path.getFileName()));
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
            t.printStackTrace();

            t.printStackTrace(new PrintStream(new FileOutputStream(String.valueOf(PathManager.errorPath()))));
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
            System.out.println("Message was:");
            System.out.println(msg);

            Files.write(PathManager.errorPath(), (msg + "\n").getBytes());
        } catch (IOException e) {
            System.out.println("ERROR (FAIL_OUTPUT_ERR_STRING): Failed to output error!");
            e.printStackTrace();
            System.out.println("---------------------------------------------------");
            System.out.println("Message was:");
            System.out.println(msg);
        }
    }
}
