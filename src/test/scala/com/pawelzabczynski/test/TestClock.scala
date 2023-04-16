package com.pawelzabczynski.test

import com.pawelzabczynski.util.Clock
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

import java.time.Instant
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.atomic.AtomicReference

object TestClock extends Clock {
  private val clock  = java.time.Clock.systemUTC()
  private val offset = new AtomicReference[FiniteDuration](0.seconds)

  override def unsafeNow: Instant = {
    val currentOffset = offset.get()
    if (currentOffset.toMillis >= 0) clock.instant().plusMillis(currentOffset.toMillis)
    else clock.instant().minusMillis(-currentOffset.toMillis)
  }

  def moveForward(t: FiniteDuration): Unit = {
    offset.set(t)
  }

  def moveBackward(t: FiniteDuration): Unit = {
    offset.set(t)
  }
}
