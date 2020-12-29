package edu.illinois.cs.dt.tools.utility;

import java.io.PrintStream;

public class Logger {

    private static final Logger INSTANCE = new Logger();
    private PrintStream out = System.out;
    private Level level = Level.CONFIG;

    public void setLoggingLevel(Level level) {
        this.level = level;
    }

    public Level getLoggingLevel() {
        return this.level;
    }

    public static Logger getGlobal() {
        return Logger.INSTANCE;
    }

    public void log(Level lev, String msg, Throwable thr) {
        if (lev.intValue() < this.level.intValue()) {
            return;
        }
        this.out.println(lev.toString() + ": " + msg);
        this.out.println(thr);
    }

    public void log(Level lev, String msg) {
        if (lev.intValue() < this.level.intValue()) {
            return;
        }
        this.out.println(lev.toString() + ": " + msg);
    }
}
