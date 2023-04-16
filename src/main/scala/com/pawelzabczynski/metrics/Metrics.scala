package com.pawelzabczynski.metrics

import io.prometheus.client.{CollectorRegistry, Counter, hotspot}
import zio.{ULayer, ZLayer}

object Metrics {

  val Prefix: String = "tapir_http_template"

  def init(): Unit = {
    hotspot.DefaultExports.initialize()
  }

  object UserMetrics {
    val registeredUsers: Counter = Counter
      .build()
      .name(s"${Prefix}_number_users")
      .help("Total registered users.")
      .register()
  }

  val live: ULayer[CollectorRegistry] = ZLayer.succeed(CollectorRegistry.defaultRegistry)
}
