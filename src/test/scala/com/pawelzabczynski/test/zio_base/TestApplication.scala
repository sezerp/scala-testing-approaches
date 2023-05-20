package com.pawelzabczynski.test.zio_base

import com.pawelzabczynski.account.AccountService
import com.pawelzabczynski.http.{Http, HttpApi}
import com.pawelzabczynski.metrics.MetricsApi
import com.pawelzabczynski.security.apiKey.{ApiKey, ApiKeyAuthOps, ApiKeyService}
import com.pawelzabczynski.security.auth.Auth
import com.pawelzabczynski.test.scalatest.{TestClock, TestConfig}
import com.pawelzabczynski.user.{UserApi, UserService}
import com.pawelzabczynski.util.IdGenerator
import io.prometheus.client.CollectorRegistry
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import sttp.client3.testing.SttpBackendStub
import sttp.tapir.server.stub.TapirStubInterpreter
import zio.{Task, URLayer, ZLayer}
import zio.interop.catz._

object TestApplication {
  def unsafeCreate(currentDb: TestZioEmbeddedPostgres): SttpBackend[Task, Any] = {
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

    val httpApi                                 = new HttpApi(http, endpoints, TestConfig.api, registry)
    val stubBackend: SttpBackendStub[Task, Any] = AsyncHttpClientFs2Backend.stub[Task]

    TapirStubInterpreter(stubBackend)
      .whenServerEndpointsRunLogic(httpApi.allEndpoints)
      .backend()
  }

  val testLive: URLayer[TestZioEmbeddedPostgres, SttpBackend[Task, Any]] = ZLayer.fromFunction(unsafeCreate _)
}
