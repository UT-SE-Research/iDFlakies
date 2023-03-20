package edu.illinois.cs.dt.tools.utility.deltadebug;

import java.util.ArrayList;
import java.util.List;

// Utility class for handling general delta debugging
public abstract class DeltaDebugger<T> {

    protected int iterations;   // Keep track of number of iterations the delta debugging went through

    // Core logic for delta debugging, generalized to elements
    public List<T> deltaDebug(final List<T> elements, int n) {
        this.iterations++;

        // If n granularity is greater than number of tests, then finished, simply return passed in tests
        if (elements.size() < n) {
            return elements;
        }

        // Cut the elements into n equal chunks and try each chunk
        int chunkSize = (int)Math.round((double)(elements.size()) / n);
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < elements.size(); i += chunkSize) {
            List<T> chunk = new ArrayList<>();
            List<T> otherChunk = new ArrayList<>();
            // Create chunk starting at this iteration
            int endpoint = Math.min(elements.size(), i + chunkSize);
            chunk.addAll(elements.subList(i, endpoint));

            // Complement chunk are elements before and after this current chunk
            otherChunk.addAll(elements.subList(0, i));
            otherChunk.addAll(elements.subList(endpoint, elements.size()));

            // Try to other, complement chunk first, with theory that valid elements are closer to end
            if (checkValid(otherChunk)) {
                return deltaDebug(otherChunk, 2);   // If works, then delta debug some more the complement chunk
            }
            // Check if running this chunk works
            if (checkValid(chunk)) {
                return deltaDebug(chunk, 2);        // If works, then delta debug some more this chunk
            }
        }
        // If size is equal to number of chunks, we are finished, cannot go down more
        if (elements.size() == n) {
            return elements;
        }
        // If not chunk/complement work, increase granularity and try again
        if (elements.size() < n * 2) {
            return deltaDebug(elements, elements.size());
        } else {
            return deltaDebug(elements, n * 2);
        }
    }

    // Getter method for number of iterations
    public int getIterations() {
        return this.iterations;
    }

    // Method to check if chunks during delta debugging is valid, to be overwritten by subclasses for specific delta debugging tasks
    public abstract boolean checkValid(List<T> elements);
}
