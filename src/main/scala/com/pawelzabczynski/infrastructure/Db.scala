package com.pawelzabczynski.infrastructure

import zio.{Scope, Task, ZIO, ZLayer, durationInt}
import doobie.hikari.HikariTransactor
import doobie.Transactor
import com.pawelzabczynski.infrastructure.Doobie._
import cats.effect.kernel.Resource
import com.pawelzabczynski.util.ErrorOps
import doobie.util.ExecutionContexts
import org.flywaydb.core.Flyway
import zio.interop.catz._

class Db(config: DbConfig, xa: Transactor[Task]) {
  private val flyway = {
    Flyway
      .configure()
      .dataSource(config.url, config.username, config.password.value)
      .load()
  }

  private def migrate(): Task[Unit] = {
    if (config.migrateOnStart) ZIO.attempt(flyway.migrate()).unit
    else ZIO.unit
  }

  private def testConnection(xa: Transactor[Task]): Task[Unit] = {
    sql"select 1"
      .query[Int]
      .unique
      .transact(xa)
      .unit
      .catchSome { case e: Exception =>
        ZIO.logWarningCause("Database not available, waiting 5 seconds to retry...", e.toCause) *> ZIO.sleep(
          5.second
        ) *> testConnection(xa)
      }
  }

  def checkAndMigrate(): Task[Unit] = {
    testConnection(xa) *> migrate() *> ZIO.logInfo("Database connection check and migration completed.")
  }
}

object Db {

  type Env = DbConfig with HikariTransactor[Task]

  def create(config: DbConfig, xa: HikariTransactor[Task]): Db = {
    new Db(config, xa)
  }

  val live: ZLayer[Env, Throwable, Db] = ZLayer.fromFunction(create _)
}

object DbTransactor {
  private def transactorResources(config: DbConfig): Resource[Task, HikariTransactor[Task]] = {
    for {
      ec <- ExecutionContexts.fixedThreadPool[Task](config.connectThreadPoolSize)
      xa <- HikariTransactor.newHikariTransactor[Task](
        config.driver,
        config.url,
        config.username,
        config.password.value,
        ec
      )
    } yield xa
  }

  private def transactorScoped: ZIO[DbConfig with Scope, Throwable, HikariTransactor[Task]] = for {
    config <- ZIO.service[DbConfig]
    xa     <- transactorResources(config).toScopedZIO
  } yield xa

  val live: ZLayer[DbConfig with Scope, Throwable, HikariTransactor[Task]] = ZLayer.fromZIO(transactorScoped)
}
