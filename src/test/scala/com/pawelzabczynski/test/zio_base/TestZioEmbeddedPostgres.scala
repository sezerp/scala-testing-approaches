package com.pawelzabczynski.test.zio_base

import com.pawelzabczynski.config.{Config, Sensitive}
import com.pawelzabczynski.infrastructure.DbConfig
import com.pawelzabczynski.infrastructure.Doobie.Transactor
import com.typesafe.scalalogging.StrictLogging
import com.zaxxer.hikari.HikariDataSource
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.flywaydb.core.Flyway
import zio.{Cause, RIO, Task, ULayer, URIO, ZIO, ZLayer}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import zio.interop.catz._
import com.pawelzabczynski.infrastructure.Doobie._
import com.pawelzabczynski.util.ErrorOps
import com.pawelzabczynski.test.scalatest.TestConfig

class TestZioEmbeddedPostgres(
    val postgres: EmbeddedPostgres,
    val config: DbConfig,
    val xa: Transactor[Task],
    val dataSource: HikariDataSource
) {
  private val flyway = {
    Flyway
      .configure()
      .cleanDisabled(false)
      .dataSource(config.url, config.username, config.password.value)
      .load()
  }

  def migrate(): Task[Unit] = {
    if (config.migrateOnStart) {
      ZIO.logInfo("Start migrate database.") *>
        ZIO.attempt(flyway.migrate()).unit *>
        ZIO.logInfo("Database has been successfully migrated.")
    } else ZIO.logInfo("Migration skipped")
  }

  def clean(): Task[Unit] = {
    ZIO.logInfo("Start cleaning DB.") *> ZIO.attempt(flyway.clean()) *> ZIO.logInfo("Db has been cleaned.")
  }

  def testConnection(): Task[Unit] = {
    ZIO.logInfo("Performing test connection") *> sql"select 1"
      .query[Int]
      .unique
      .transact(xa)
      .unit
      .tapError(e => ZIO.logErrorCause("Exception occurred when try test DB connection", Cause.fail(e))) *> ZIO.logInfo(
      "Test DB successful."
    )
  }

  def close(): Task[Unit] = ZIO.logInfo("Start closing DB.") *> ZIO.attempt {
    dataSource.close()
  } *> ZIO.logInfo("Db has been closed.")
}

object TestZioEmbeddedPostgres extends StrictLogging {
  def beforeEach(): RIO[TestZioEmbeddedPostgres, Unit] = {
    ZIO.service[TestZioEmbeddedPostgres].flatMap(_.migrate())
  }

  def afterEach(): RIO[TestZioEmbeddedPostgres, Unit] = {
    ZIO.service[TestZioEmbeddedPostgres].flatMap(_.clean())
  }
  def afterAll(): URIO[TestZioEmbeddedPostgres, Unit] = {
    ZIO
      .service[TestZioEmbeddedPostgres]
      .flatMap(_.close())
      .tapError(e => ZIO.logErrorCause("Error occurred when try close db.", e.toCause))
      .catchAll(_ => ZIO.unit)
  }
  private def createTransactor(config: DbConfig): (Transactor[Task], HikariDataSource) = {
    val ec        = Executors.newFixedThreadPool(config.connectThreadPoolSize)
    val connectEC = ExecutionContext.fromExecutor(ec)
    Class.forName(config.driver) // check if driver exist
    val ds = new HikariDataSource
    ds.setDriverClassName(config.driver)
    ds.setJdbcUrl(config.url)
    ds.setUsername(config.username)
    ds.setPassword(config.password.value)

    val xa = Transactor.fromDataSource[Task](ds, connectEC)

    (xa, ds)
  }

  private def unsafeCrate(config: Config): TestZioEmbeddedPostgres = {
    try {
      val postgresDb = EmbeddedPostgres.start()
      val url        = postgresDb.getJdbcUrl("postgres", "postgres")
      val updatedConfig: DbConfig = config.db.copy(
        username = "postgres",
        password = Sensitive(""),
        url = url,
        migrateOnStart = true
      )
      val (xa, ds) = createTransactor(updatedConfig)

      new TestZioEmbeddedPostgres(postgresDb, updatedConfig, xa, ds)
    } catch {
      case ex: Exception =>
        logger.error("Error when try start ", ex)
        throw ex
    }
  }

  val testLive: ULayer[TestZioEmbeddedPostgres] =
    ZLayer.succeed(TestConfig) >>> ZLayer.fromFunction(unsafeCrate _)

}
