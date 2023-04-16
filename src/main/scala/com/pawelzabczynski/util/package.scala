package com.pawelzabczynski

import sttp.tapir.server.ServerEndpoint
import com.softwaremill.tagging._
import sttp.tapir.DecodeResult
import zio.{Cause, Task}

import java.util.UUID
import scala.util.{Failure, Success, Try}

package object util {
  type Id <: UUID
  type HttpEndpoints = List[ServerEndpoint[Any, Task]]

  def idDecoder(v: String): DecodeResult[Id] = {
    Try(UUID.fromString(v)) match {
      case Success(id) => DecodeResult.Value(id.asInstanceOf[Id])
      case Failure(_)  => DecodeResult.Error(v, new RuntimeException(s"Invalid api key format."))
    }
  }

  implicit class ErrorOps[E <: Throwable](e: E) {
    implicit def toCause: Cause[E] = Cause.fail(e)
  }

  implicit class IdOps(val s: UUID) extends AnyVal {
    def asId[T]: Id @@ T = s.asInstanceOf[Id @@ T]
  }

}
