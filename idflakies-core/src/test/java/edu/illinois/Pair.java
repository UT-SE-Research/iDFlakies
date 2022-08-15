package edu.illinois;

import java.util.Objects;

public class Pair<T> {
    private T x;
    private T y;

    Pair(T x, T y) {
        this.x = x;
        this.y = y;
    }

    public String toString() {
        return "(" + this.x + ", " + this.y + ")";
    }

    @Override
    public boolean equals(Object rhs) {
        if (rhs instanceof Pair<?>) {
            Pair<?> o = (Pair<?>)rhs;
            return this.x == o.x && this.y == o.y;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}