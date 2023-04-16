package com.pawelzabczynski.infrastructure

import com.typesafe.scalalogging.LazyLogging
import zio.{Cause, FiberId, FiberRefs, LogLevel, LogSpan, Trace, ZLogger}
import org.slf4j.MDC

object ZIOLogger extends LazyLogging {
  object MdcKey {
    val CorrelationId = "correlation_id"
  }

  def make: ZLogger[String, Unit] = (
      trace: Trace,
      fiberId: FiberId,
      logLevel: LogLevel,
      message: () => String,
      cause: Cause[Any],
      context: FiberRefs,
      spans: List[LogSpan],
      annotations: Map[String, String]
  ) => {
    withMdc(annotations) {
      if (cause.isEmpty) log(logLevel, message)
      else
        cause.find { case f: Cause.Fail[Throwable] => f.value } match {
          case Some(e) => logWithCause(logLevel, message, e)
          case None    => log(logLevel, message)
        }
    }
  }

  private def logWithCause(logLevel: LogLevel, message: () => String, cause: Throwable): Unit = {
    logLevel match {
      case LogLevel.All     => logger.trace(message(), cause)
      case LogLevel.Trace   => logger.trace(message(), cause)
      case LogLevel.Info    => logger.info(message(), cause)
      case LogLevel.Debug   => logger.debug(message(), cause)
      case LogLevel.Warning => logger.warn(message(), cause)
      case LogLevel.Error   => logger.error(message(), cause)
      case _                => logger.error(message())
    }
  }

  private def log(logLevel: LogLevel, message: () => String): Unit = {
    logLevel match {
      case LogLevel.All   => logger.trace(message())
      case LogLevel.Trace => logger.trace(message())
      case LogLevel.Info  => logger.info(message())
      case LogLevel.Debug => logger.debug(message())
      case LogLevel.Error => logger.error(message())
      case _              => logger.error(message())
    }
  }

  private def withMdc[T](annotations: Map[String, String])(fn: => T): T = {
    annotations.get(MdcKey.CorrelationId).foreach(cid => MDC.put(MdcKey.CorrelationId, cid))
    try fn
    finally MDC.remove(MdcKey.CorrelationId)
  }
}
