package com.pawelzabczynski.util

import cats.Applicative
import cats.implicits.catsSyntaxApplicativeId
import com.softwaremill.tagging.@@
import zio.{UIO, ULayer, ZIO, ZLayer}

import java.util.UUID

trait IdGenerator {
  def next[T]: UIO[Id @@ T]                  = ZIO.succeed(unsafeNext[T])
  def next[F[_]: Applicative, T]: F[Id @@ T] = unsafeNext[T].pure[F]
  def unsafeNext[T]: Id @@ T
}

object IdGenerator {
  val default: IdGenerator = {
    new IdGenerator {
      override def unsafeNext[T]: Id @@ T = UUID.randomUUID().asId[T]
    }
  }

  val live: ULayer[IdGenerator] = ZLayer.succeed(default)
}
