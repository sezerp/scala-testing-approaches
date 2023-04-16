package com.pawelzabczynski.infrastructure

import com.pawelzabczynski.config.{Config, Sensitive}
import zio.ZLayer

case class DbConfig(
    username: String,
    password: Sensitive,
    url: String,
    migrateOnStart: Boolean,
    driver: String,
    connectThreadPoolSize: Int
)

object DbConfig {
  type Env = Config

  def fromConfig(config: Config): DbConfig = {
    config.db
  }

  val live: ZLayer[Env, Nothing, DbConfig] = ZLayer.fromFunction(fromConfig _)
}
