package edu.illinois.cs.dt.tools.minimizer.ranking;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

/**
 * Disk cache for RankFO scored candidates.
 *
 * Cache files live in {@code <dtDir>/rankfo-scores/}.
 * A cache entry is valid when the detection results directory has NOT been
 * modified since the entry was written — i.e., no new {@code detect} run has
 * produced new data. If it has been modified, the entry is treated as stale
 * and the caller must recompute.
 *
 * Each (targetTest, heuristic, odType) triple gets its own file so that
 * switching heuristics or re-detecting does not cross-contaminate entries.
 */
class RankFOScoreCache {

    private static final Gson GSON = new GsonBuilder().create();
    static final String SUBDIR = "rankfo-scores";

    /**
     * Returns the cached ranking for the given key, or {@code null} on cache miss.
     * A miss occurs when: the cache file doesn't exist, the detection results
     * directory is newer than the cache file, or the file can't be parsed.
     */
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

    /**
     * Persists the ranked candidates to disk. Silently no-ops on I/O failure
     * (the caller will just recompute next time).
     */
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

    // ── inner DTO ─────────────────────────────────────────────────────────

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
