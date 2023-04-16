package com.pawelzabczynski.test

import com.pawelzabczynski.infrastructure.DbConfig
import com.typesafe.scalalogging.StrictLogging
import doobie.Transactor
import org.flywaydb.core.Flyway
import zio.Task
import zio.interop.catz._
import zio._
import com.pawelzabczynski.infrastructure.Doobie._
import com.zaxxer.hikari.HikariDataSource

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

class TestDb(config: DbConfig) extends StrictLogging {
  var xa: Transactor[Task]                 = _
  private var dataSource: HikariDataSource = _

  {
    val (ds, transactor) = (for {
      ec        <- ZIO.attempt(Executors.newFixedThreadPool(config.connectThreadPoolSize))
      connectEC <- ZIO.attempt(ExecutionContext.fromExecutor(ec))
      _         <- ZIO.attempt(Class.forName(config.driver)) // check if driver exist
      ds        <- ZIO.attempt(new HikariDataSource)
      _ <- ZIO.attempt {
        ds.setDriverClassName(config.driver)
        ds.setJdbcUrl(config.url)
        ds.setUsername(config.username)
        ds.setPassword(config.password.value)
      }
      t <- ZIO.attempt(Transactor.fromDataSource[Task](ds, connectEC))
    } yield (ds, t)).runUnsafe(1.minute)

    dataSource = ds
    xa = transactor
  }

  private val flyway = {
    Flyway
      .configure()
      .cleanDisabled(false)
      .dataSource(config.url, config.username, config.password.value)
      .load()
  }

  def migrate(): Unit = {
    if (config.migrateOnStart) {
      logger.info("Start migrate database.")
      flyway.migrate()
      logger.info("Database has been successfully migrated.")
      ()
    }
  }

  def clean(): Unit = {
    flyway.clean()
  }

  def testConnection(): Unit = {
    (ZIO.logInfo("Performing test connection") *> sql"select 1"
      .query[Int]
      .unique
      .transact(xa)
      .unit
      .tapError(e => ZIO.logErrorCause("Exception occurred when try test DB connection", Cause.fail(e))) *> ZIO.logInfo(
      "Test DB successful."
    )).runUnsafe(1.minute)
  }

  def close(): Unit = {
    logger.info("Start closing DB")
    dataSource.close()
  }
}
