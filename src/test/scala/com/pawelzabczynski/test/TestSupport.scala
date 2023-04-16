package com.pawelzabczynski.test

import com.pawelzabczynski.http.ErrorOut
import io.circe.{Decoder, parser}
import com.pawelzabczynski.infrastructure.JsonSupport._

import scala.reflect.ClassTag

trait TestSupport {

  implicit class EitherSupport[L, R](e: Either[L, R]) {
    def get: R = {
      e match {
        case Right(b) => b
        case Left(l)  => throw new NoSuchElementException(s"Either.right.get on Right, $l")
      }
    }

    def getLeft: L = {
      e match {
        case Left(e)  => e
        case Right(r) => throw new NoSuchElementException(s"Either.left.get on Left, $r")
      }
    }
  }

  implicit class RichEiter(r: Either[String, String]) {

    def shouldDeserializeTo[T: Decoder: ClassTag]: T =
      r.flatMap(parser.parse).flatMap(_.as[T]).get

    def shouldDeserializeToError: String = {
      parser.parse(r.getLeft).flatMap(_.as[ErrorOut]).get.error
    }
  }
}
