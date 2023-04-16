package com.pawelzabczynski.infrastructure

import com.pawelzabczynski.infrastructure.ZIOLogger.MdcKey
import CorrelationId.generateZIO
import sttp.tapir.model.ServerRequest
import sttp.tapir.server.interceptor.{EndpointInterceptor, RequestHandler, RequestInterceptor, Responder}
import zio.{Task, Trace, UIO, ZIO, ZIOAspect}

object CorrelationId {
  def generateZIO(): UIO[String] = {
    def randomUpperCaseChar(): UIO[Char] = ZIO.random
      .flatMap(random => random.nextIntBetween(65, 91))
      .map(_.toChar)
    def segment() = {
      def loop(cnt: Int, acc: List[Char]): UIO[String] = {
        if (cnt < 3) randomUpperCaseChar().flatMap(c => loop(cnt + 1, c :: acc))
        else ZIO.succeed(acc.mkString)
      }

      loop(0, List.empty)
    }

    for {
      s1 <- segment()
      s2 <- segment()
      s3 <- segment()
    } yield s"$s1-$s2-$s3"
  }
}

object LogAspect {
  val HeaderName: String = "X-Correlation-ID"

  def logAnnotateCorrelationId(
      request: ServerRequest
  ): ZIOAspect[Nothing, Any, Nothing, Any, Nothing, Any] =
    new ZIOAspect[Nothing, Any, Nothing, Any, Nothing, Any] {
      override def apply[R, E, A](
          zio: ZIO[R, E, A]
      )(implicit trace: Trace): ZIO[R, E, A] =
        getCorrelationId(request).flatMap(id => ZIO.logAnnotate(MdcKey.CorrelationId, id)(zio))

      def getCorrelationId(req: ServerRequest): UIO[String] = {
        req
          .header(HeaderName)
          .fold(generateZIO())(cid => ZIO.succeed(cid))
      }
    }
}

object CorrelationIdInterceptor extends RequestInterceptor[Task] {
  override def apply[R, B](
      responder: Responder[Task, B],
      requestHandler: EndpointInterceptor[Task] => RequestHandler[Task, R, B]
  ): RequestHandler[Task, R, B] = RequestHandler.from { case (request, endpoints, monad) =>
    requestHandler(EndpointInterceptor.noop)(request, endpoints)(monad) @@ LogAspect.logAnnotateCorrelationId(request)
  }
}
