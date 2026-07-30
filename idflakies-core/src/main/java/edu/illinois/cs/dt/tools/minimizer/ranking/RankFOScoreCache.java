package edu.illinois.cs.dt.tools.minimizer.ranking;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

// Invalidated when test-runs/results/ mtime advances past the cache file.
class RankFOScoreCache {

    private static final Gson GSON = new GsonBuilder().create();
    static final String SUBDIR = "rankfo-scores";

    static List<ScoredCandidate> load(Path dtDir, String targetTest,
                                       HeuristicType heuristic, OdType odType) {
        Path cacheFile = cacheFile(dtDir, targetTest, heuristic, odType);
        Path resultsDir = resultsDir(dtDir);

        if (!Files.exists(cacheFile)) return null;
        if (!Files.exists(resultsDir)) return null;

        try {
            FileTime cacheTime   = Files.getLastModifiedTime(cacheFile);
            FileTime resultsTime = Files.getLastModifiedTime(resultsDir);
            if (resultsTime.compareTo(cacheTime) > 0) return null;

            String json = new String(Files.readAllBytes(cacheFile));
            CacheEntry entry = GSON.fromJson(json, CacheEntry.class);
            if (entry == null || entry.candidates == null) return null;
            return entry.candidates;
        } catch (Exception e) {
            return null;
        }
    }

    static void save(Path dtDir, String targetTest,
                     HeuristicType heuristic, OdType odType,
                     List<ScoredCandidate> candidates) {
        Path cacheFile = cacheFile(dtDir, targetTest, heuristic, odType);
        try {
            Files.createDirectories(cacheFile.getParent());
            CacheEntry entry = new CacheEntry(
                    targetTest, heuristic.name(), odType.name(), candidates);
            Files.write(cacheFile, GSON.toJson(entry).getBytes());
        } catch (IOException ignored) {
            // Non-fatal: next call will recompute
        }
    }

    static Path cacheFile(Path dtDir, String targetTest,
                           HeuristicType heuristic, OdType odType) {
        String safeName = targetTest.replace('#', '_')
                + "-" + heuristic.name()
                + "-" + odType.name()
                + ".json";
        return dtDir.resolve(SUBDIR).resolve(safeName);
    }

    static Path resultsDir(Path dtDir) {
        return dtDir.resolve("test-runs").resolve("results");
    }

    private static final class CacheEntry {
        String targetTest;
        String heuristic;
        String odType;
        List<ScoredCandidate> candidates;

        CacheEntry(String targetTest, String heuristic, String odType,
                   List<ScoredCandidate> candidates) {
            this.targetTest = targetTest;
            this.heuristic  = heuristic;
            this.odType     = odType;
            this.candidates = candidates;
        }
    }
}
