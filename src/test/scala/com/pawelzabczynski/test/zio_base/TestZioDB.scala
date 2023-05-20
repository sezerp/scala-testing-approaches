package com.pawelzabczynski.test.zio_base

import com.pawelzabczynski.infrastructure.DbConfig
import com.pawelzabczynski.infrastructure.Doobie.Transactor
import com.typesafe.scalalogging.StrictLogging
import com.zaxxer.hikari.HikariDataSource
import doobie.implicits.toSqlInterpolator
import org.flywaydb.core.Flyway
import zio.{Cause, Task, ZIO}
import com.pawelzabczynski.infrastructure.Doobie._

import zio.interop.catz._

class TestZioDB(val xa: Transactor[Task], dataSource: HikariDataSource, config: DbConfig) {
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

object TestZioDB extends StrictLogging {}
