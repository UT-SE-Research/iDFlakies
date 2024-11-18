package edu.illinois.cs.testrunner.util

import java.io.{File, FileOutputStream}
import java.nio.file.{Files, Path, StandardOpenOption}
import java.util.Properties

import scala.util.Try

object TempFiles {
    /**
      * Calls the function passed in with the path of a blank temporary file.
      * The file will no longer exist after this function ends
      */
    def withTempFile[A](f: Path => A): Try[A] = {
        val path = File.createTempFile("temp", null).toPath

        val result = Try(f(path))

        Files.deleteIfExists(path)

        result
    }

    def withSeq[S[_] <: Traversable[_], A, B](seq: S[A])(f: Path => B): Try[B] = {
        withTempFile(path => {
            // Don't need to clear file because withTempFile will always return a blank file
            for (s <- seq) {
                Files.write(path, (s.toString + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND)
            }

            f(path)
        })
    }

    def withProperties[B](properties: Properties)(f: Path => B): Try[B] = {
        withTempFile(path => autoClose(new FileOutputStream(path.toFile))(os => {
            properties.store(os, "")
            f(path)
        })).flatten
    }
}
