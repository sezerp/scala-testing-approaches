package com.pawelzabczynski.http

import com.pawelzabczynski.Fail
import com.pawelzabczynski.infrastructure.JsonSupport._
import Fail.{IncorrectInput, Unauthorized}
import com.pawelzabczynski.util.{Id, idDecoder}
import sttp.tapir.{Codec, Endpoint, EndpointOutput, PublicEndpoint, Schema, Tapir}
import sttp.tapir.json.circe.TapirJsonCirce
import io.circe.Printer
import sttp.model.StatusCode
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.generic.auto._
import zio.{Task, ZIO, ZLayer}
import com.softwaremill.tagging._

class Http() extends Tapir with TapirJsonCirce with TapirSchemas {
  val jsonErrorOutOutput: EndpointOutput[ErrorOut]                          = jsonBody[ErrorOut]
  val failOutput: EndpointOutput[(StatusCode, ErrorOut)]                    = statusCode.and(jsonErrorOutOutput)
  val baseEndpoint: PublicEndpoint[Unit, (StatusCode, ErrorOut), Unit, Any] = endpoint.errorOut(failOutput)
  val secureEndpoint: Endpoint[Id, Unit, (StatusCode, ErrorOut), Unit, Any] =
    baseEndpoint.securityIn(auth.bearer[String]().mapDecode[Id](idDecoder)(_.toString))

  private val failToResponseData: Fail => (StatusCode, String) = {
    case IncorrectInput(msg) => (StatusCode.BadRequest, msg)
    case Unauthorized        => (StatusCode.Unauthorized, "Unauthorized")
    case _                   => (StatusCode.InternalServerError, "Internal server error")
  }

  override def jsonPrinter: Printer = noNullsPrinter

  implicit class TaskOut[T](f: Task[T]) {
    def toTaskEither: Task[Either[(StatusCode, ErrorOut), T]] = {
      f.map(t => Right(t)).catchSome { case f: Fail =>
        val (statusCode, errorMsg) = failToResponseData(f)
        ZIO.succeed(Left((statusCode, ErrorOut(errorMsg))))
      }
    }
  }
}

object Http {

  def create(): Http = {
    new Http()
  }
  val live: ZLayer[Any, Nothing, Http] = ZLayer.fromFunction(Http.create _)
}

trait TapirSchemas {
  implicit val idPlainCodec: PlainCodec[Id] = Codec.uuid.map(_.asInstanceOf[Id])(identity)
  implicit def taggedPlainCodec[U, T](implicit uc: PlainCodec[U]): PlainCodec[U @@ T] =
    uc.map(_.taggedWith[T])(identity)

  implicit val schemaForId: Schema[Id] = Schema.schemaForUUID.asInstanceOf[Schema[Id]]

  implicit def schemaForTagged[T]: Schema[Id @@ T] = schemaForId.asInstanceOf[Schema[Id @@ T]]
}
