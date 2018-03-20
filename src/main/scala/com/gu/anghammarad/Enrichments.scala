package com.gu.anghammarad

import scala.util.{Success, Try}

object Enrichments {
  implicit class RichList[A](as: List[A]) {
    def traverse[B](f: A => Try[B]): Try[List[B]] = {
      as.foldRight[Try[List[B]]](Success(Nil)) { (a, acc) =>
        for {
          b <- f(a)
          bs <- acc
        } yield b :: bs
      }
    }
  }
}
