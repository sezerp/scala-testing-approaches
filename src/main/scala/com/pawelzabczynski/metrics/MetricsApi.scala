package com.pawelzabczynski.metrics

import com.pawelzabczynski.http.Http
import com.pawelzabczynski.util.{ErrorOps, HttpEndpoints}
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat

import java.io.StringWriter
import sttp.tapir.ztapir.ZServerEndpoint
import zio.{URLayer, ZIO, ZLayer}

class MetricsApi(http: Http, registry: CollectorRegistry) {
  import http._

  private val metricsEndpoint: ZServerEndpoint[Any, Any] = baseEndpoint.get
    .in("metrics")
    .out(stringBody)
    .serverLogic { _ =>
      ZIO
        .attempt {
          val w = new StringWriter
          TextFormat.write004(w, registry.metricFamilySamples())
          w.toString
        }
        .tapError(e => ZIO.logErrorCause("Error occurred when scrap metrics", e.toCause))
        .toTaskEither
    }

  val endpoints: HttpEndpoints = List(metricsEndpoint)
}

object MetricsApi {
  type Env = Http with CollectorRegistry
  def create(http: Http, registry: CollectorRegistry): MetricsApi = {
    new MetricsApi(http, registry)
  }

  val live: URLayer[Env, MetricsApi] = ZLayer.fromFunction(create _)
}
