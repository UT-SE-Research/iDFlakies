package edu.illinois.cs.testrunner.util

import scala.util.{Failure, Success, Try}

case class autoClose[A <: java.lang.AutoCloseable,B](a: A) extends ((A => B) => Try[B]) {
    def autoCloseTry(closeable: A)(fun: A => B): Try[B] = {
        Try(fun(closeable)).transform(
            result => {
                closeable.close()
                Success(result)
            },
            funT => {
                Try(closeable.close()).transform(
                    _ ⇒ Failure(funT),
                    closeT ⇒ {
                        funT.addSuppressed(closeT)
                        Failure(funT)
                    }
                )
            }
        )
    }

    override def apply(f: A => B): Try[B] = autoCloseTry(a)(f)
}
