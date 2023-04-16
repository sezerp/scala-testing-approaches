package com.pawelzabczynski.test

import com.pawelzabczynski.config.Sensitive
import com.pawelzabczynski.infrastructure.DbConfig
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.postgresql.jdbc.PgConnection
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait TestEmbeddedPostgres extends BeforeAndAfterEach with BeforeAndAfterAll { self: Suite =>
  private var postgres: EmbeddedPostgres = _
  private var currentDbConfig: DbConfig  = _
  var currentDb: TestDb                  = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    postgres = EmbeddedPostgres.builder().start()
    val url = postgres.getJdbcUrl("postgres", "postgres")
    postgres.getPostgresDatabase().getConnection.asInstanceOf[PgConnection].setPrepareThreshold(100)
    currentDbConfig = TestConfig.db.copy(
      username = "postgres",
      password = Sensitive(""),
      url = url,
      migrateOnStart = true
    )
    currentDb = new TestDb(currentDbConfig)
    currentDb.testConnection()
  }

  override protected def afterAll(): Unit = {
    postgres.close()
    currentDb.close()
    super.afterAll()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    currentDb.migrate()
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    currentDb.clean()
  }
}
