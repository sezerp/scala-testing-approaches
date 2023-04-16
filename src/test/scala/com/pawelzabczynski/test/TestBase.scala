package com.pawelzabczynski.test

import com.pawelzabczynski.account.AccountService
import com.pawelzabczynski.http.{Http, HttpApi}
import com.pawelzabczynski.metrics.MetricsApi
import com.pawelzabczynski.security.apiKey.{ApiKey, ApiKeyAuthOps, ApiKeyService}
import com.pawelzabczynski.security.auth.Auth
import com.pawelzabczynski.user.{UserApi, UserService}
import com.pawelzabczynski.util.IdGenerator
import com.typesafe.scalalogging.StrictLogging
import io.prometheus.client.CollectorRegistry
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import sttp.client3.testing.SttpBackendStub
import sttp.tapir.server.stub.TapirStubInterpreter
import zio.interop.catz._
import zio.Task

class TestBase extends AnyFlatSpec with Matchers with BeforeAndAfterAll with TestEmbeddedPostgres with StrictLogging {
  Thread.setDefaultUncaughtExceptionHandler((t, e) => logger.error("Uncaught exception in thread: " + t, e))

  private var httpApi: HttpApi = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val idGenerator    = IdGenerator.default
    val clock          = TestClock
    val http           = new Http()
    val apiKeyAuthOps  = new ApiKeyAuthOps
    val apiKeyAuth     = new Auth[ApiKey](apiKeyAuthOps, clock, currentDb.xa)
    val registry       = CollectorRegistry.defaultRegistry
    val accountService = new AccountService(idGenerator, clock)
    val apiKeyService  = new ApiKeyService(idGenerator, clock)
    val userService =
      new UserService(TestConfig.userService, accountService, apiKeyService, idGenerator, clock, currentDb.xa)
    val userApi    = new UserApi(userService, apiKeyAuth, http, currentDb.xa)
    val metricsApi = new MetricsApi(http, registry)
    val endpoints  = userApi.endpoints ++ metricsApi.endpoints

    httpApi = new HttpApi(http, endpoints, TestConfig.api, registry)
  }

  private val stubBackend: SttpBackendStub[Task, Any] = AsyncHttpClientFs2Backend.stub[Task]
  private lazy val serverStub: SttpBackend[Task, Any] = TapirStubInterpreter(stubBackend)
    .whenServerEndpointsRunLogic(httpApi.allEndpoints)
    .backend()

  lazy val requests = new Requests(serverStub)
}
