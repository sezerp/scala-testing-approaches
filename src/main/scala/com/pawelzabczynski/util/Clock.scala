package com.pawelzabczynski.util

import cats.Applicative
import cats.implicits.catsSyntaxApplicativeId
import zio.{ULayer, ZLayer}

import java.time.Instant
import java.time.temporal.ChronoUnit

trait Clock {
  def now[G[_]: Applicative]: G[Instant] = unsafeNow.pure[G]
  def unsafeNow: Instant
}

class DefaultClock extends Clock {
  private val clock: java.time.Clock = java.time.Clock.systemUTC()

  override def unsafeNow: Instant = clock.instant().truncatedTo(ChronoUnit.MILLIS)
}

object Clock {
  val live: ULayer[Clock] = ZLayer.succeed(new DefaultClock)
}
