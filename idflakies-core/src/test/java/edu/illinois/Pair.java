package edu.illinois;

import java.util.Objects;

public class Pair {
    private String x;
    private String y;

    Pair(String x, String y) {
        this.x = x;
        this.y = y;
    }

    public String toString() {
        return "(" + this.x + ", " + this.y + ")";
    }

    @Override
    public boolean equals(Object rhs) {
        if (rhs instanceof Pair) {
            Pair o = (Pair) rhs;
            // if (this.x == o.x && this.y == o.y) {
            //     System.out.println(o.x + "    " + this.x);
            //     System.out.println(o.y + "    " + this.y);
            //     System.out.println();
            // }
            return this.x == o.x && this.y == o.y;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    // public Pair reverse() {
    //     Pair reversedPair = new Pair(this.y, this.x);
    //     return reversedPair;
    // }
}