package edu.illinois.cs.dt.tools.fixer;

import com.google.gson.Gson;
import com.reedoei.eunomia.io.files.FileUtil;
import edu.illinois.cs.dt.tools.utility.OperationTime;
import edu.illinois.cs.dt.tools.utility.PathManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FixerResult {

    private final OperationTime time;
    private final FixStatus status;
    private final String dependentTest;
    private final List<PatchResult> patchResults;

    public static FixerResult fromPath(final Path path) throws IOException {
        return fromString(FileUtil.readFile(path));
    }

    public static FixerResult fromString(final String jsonString) {
        return new Gson().fromJson(jsonString, FixerResult.class);
    }

    public FixerResult(final OperationTime time, final FixStatus status, final String dependentTest, final List<PatchResult> patchResults) {
        this.time = time;
        this.status = status;
        this.dependentTest = dependentTest;
        this.patchResults = patchResults;
    }

    public OperationTime time() {
        return this.time;
    }

    public FixStatus status() {
        return this.status;
    }

    public String dependentTest() {
        return this.dependentTest;
    }

    public List<PatchResult> patchResults() {
        return this.patchResults;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public void save() {
        final Path outputPath = PathManager.fixerPath(dependentTest() + ".json");

        try {
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
