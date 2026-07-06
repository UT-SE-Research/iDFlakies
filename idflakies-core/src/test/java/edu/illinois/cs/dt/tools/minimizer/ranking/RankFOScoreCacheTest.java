package edu.illinois.cs.dt.tools.minimizer.ranking;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.*;

public class RankFOScoreCacheTest {

    private Path tmpDir;
    private Path resultsDir;

    @Before
    public void setUp() throws IOException {
        tmpDir = Files.createTempDirectory("rankfo-cache-test");
        resultsDir = tmpDir.resolve("test-runs").resolve("results");
        Files.createDirectories(resultsDir);
        // Make detection results appear OLD so cached entries are treated as fresh
        Files.setLastModifiedTime(resultsDir, FileTime.fromMillis(System.currentTimeMillis() - 10_000));
    }

    @After
    public void tearDown() throws IOException {
        deleteRecursive(tmpDir);
    }

    // ── cache miss ────────────────────────────────────────────────────────

    @Test
    public void cacheMiss_whenNoCacheFileExists() {
        List<ScoredCandidate> result = RankFOScoreCache.load(
                tmpDir, "com.example.VictimTest#test",
                HeuristicType.DISTANCE, OdType.VICTIM_POLLUTER);

        assertNull("Should return null when no cache file exists", result);
    }

    @Test
    public void cacheMiss_whenDetectionResultsDirIsNewerThanCacheFile() throws IOException {
        List<ScoredCandidate> candidates = singleCandidate("com.example.A#test", 1.0);
        RankFOScoreCache.save(tmpDir, "com.example.V#test",
                HeuristicType.PLUS_ONE, OdType.VICTIM_POLLUTER, candidates);

        // Advance results dir mtime past the cache file
        Files.setLastModifiedTime(resultsDir,
                FileTime.fromMillis(System.currentTimeMillis() + 5_000));

        List<ScoredCandidate> result = RankFOScoreCache.load(
                tmpDir, "com.example.V#test",
                HeuristicType.PLUS_ONE, OdType.VICTIM_POLLUTER);

        assertNull("Cache should be invalid when detection results are newer", result);
    }

    // ── cache hit ─────────────────────────────────────────────────────────

    @Test
    public void cacheHit_returnsSavedCandidatesInOrder() throws IOException {
        List<ScoredCandidate> saved = Arrays.asList(
                new ScoredCandidate("com.example.A#test", 2.5, -1.0),
                new ScoredCandidate("com.example.B#test", 1.0, -0.5));

        RankFOScoreCache.save(tmpDir, "com.example.VictimTest#test",
                HeuristicType.DISTANCE, OdType.VICTIM_POLLUTER, saved);

        List<ScoredCandidate> loaded = RankFOScoreCache.load(
                tmpDir, "com.example.VictimTest#test",
                HeuristicType.DISTANCE, OdType.VICTIM_POLLUTER);

        assertNotNull("Should hit cache when detection results are older", loaded);
        assertEquals(2, loaded.size());
        assertEquals("com.example.A#test", loaded.get(0).getTestName());
        assertEquals(2.5, loaded.get(0).getPolluterScore(), 1e-6);
        assertEquals("com.example.B#test", loaded.get(1).getTestName());
        assertEquals(1.0, loaded.get(1).getPolluterScore(), 1e-6);
    }

    @Test
    public void cacheHit_preservesNonPolluterScore() throws IOException {
        List<ScoredCandidate> saved = singleCandidate("com.example.A#test", 3.0, -7.5);
        RankFOScoreCache.save(tmpDir, "com.example.V#test",
                HeuristicType.METHODS, OdType.BRITTLE_STATESETTER, saved);

        List<ScoredCandidate> loaded = RankFOScoreCache.load(
                tmpDir, "com.example.V#test",
                HeuristicType.METHODS, OdType.BRITTLE_STATESETTER);

        assertNotNull(loaded);
        assertEquals(-7.5, loaded.get(0).getNonPolluterScore(), 1e-6);
    }

    // ── isolation across key dimensions ──────────────────────────────────

    @Test
    public void differentHeuristics_useSeparateCacheFiles() throws IOException {
        List<ScoredCandidate> svCandidates = singleCandidate("com.example.A#t", 1.0);
        List<ScoredCandidate> dvCandidates = singleCandidate("com.example.B#t", 2.0);

        RankFOScoreCache.save(tmpDir, "com.example.V#t",
                HeuristicType.PLUS_ONE, OdType.VICTIM_POLLUTER, svCandidates);
        RankFOScoreCache.save(tmpDir, "com.example.V#t",
                HeuristicType.DISTANCE, OdType.VICTIM_POLLUTER, dvCandidates);

        List<ScoredCandidate> sv = RankFOScoreCache.load(
                tmpDir, "com.example.V#t",
                HeuristicType.PLUS_ONE, OdType.VICTIM_POLLUTER);
        List<ScoredCandidate> dv = RankFOScoreCache.load(
                tmpDir, "com.example.V#t",
                HeuristicType.DISTANCE, OdType.VICTIM_POLLUTER);

        assertNotNull(sv);
        assertNotNull(dv);
        assertEquals("com.example.A#t", sv.get(0).getTestName());
        assertEquals("com.example.B#t", dv.get(0).getTestName());
    }

    @Test
    public void differentOdTypes_useSeparateCacheFiles() throws IOException {
        List<ScoredCandidate> vpCandidates = singleCandidate("com.example.A#t", 1.0);
        List<ScoredCandidate> bssCandidates = singleCandidate("com.example.B#t", 2.0);

        RankFOScoreCache.save(tmpDir, "com.example.V#t",
                HeuristicType.PLUS_ONE, OdType.VICTIM_POLLUTER, vpCandidates);
        RankFOScoreCache.save(tmpDir, "com.example.V#t",
                HeuristicType.PLUS_ONE, OdType.BRITTLE_STATESETTER, bssCandidates);

        List<ScoredCandidate> vp = RankFOScoreCache.load(
                tmpDir, "com.example.V#t",
                HeuristicType.PLUS_ONE, OdType.VICTIM_POLLUTER);
        List<ScoredCandidate> bss = RankFOScoreCache.load(
                tmpDir, "com.example.V#t",
                HeuristicType.PLUS_ONE, OdType.BRITTLE_STATESETTER);

        assertNotNull(vp);
        assertNotNull(bss);
        assertEquals("com.example.A#t", vp.get(0).getTestName());
        assertEquals("com.example.B#t", bss.get(0).getTestName());
    }

    @Test
    public void save_createsCacheFileOnDisk() throws IOException {
        RankFOScoreCache.save(tmpDir, "com.example.V#test",
                HeuristicType.DISTANCE, OdType.VICTIM_POLLUTER,
                singleCandidate("com.example.A#test", 1.0));

        Path cacheDir = tmpDir.resolve("rankfo-scores");
        assertTrue("rankfo-scores/ directory should be created", Files.isDirectory(cacheDir));
        long fileCount = Files.list(cacheDir).count();
        assertEquals("Exactly one cache file should exist", 1, fileCount);
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private static List<ScoredCandidate> singleCandidate(String name, double polluter) {
        return Arrays.asList(new ScoredCandidate(name, polluter, -polluter));
    }

    private static List<ScoredCandidate> singleCandidate(String name, double polluter, double nonPolluter) {
        return Arrays.asList(new ScoredCandidate(name, polluter, nonPolluter));
    }

    private static void deleteRecursive(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walk(dir)
             .sorted(Comparator.reverseOrder())
             .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
    }
}
