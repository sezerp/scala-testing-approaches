package com.pawelzabczynski

import com.pawelzabczynski.account.AccountService
import com.pawelzabczynski.http.{Http, HttpApi}
import com.pawelzabczynski.metrics.MetricsApi
import com.pawelzabczynski.security.apiKey.{ApiKey, ApiKeyAuthOps, ApiKeyService}
import com.pawelzabczynski.security.auth.Auth
import com.pawelzabczynski.test.scalatest.{TestClock, TestConfig}
import com.pawelzabczynski.test.zio_base.TestZioDB
import com.pawelzabczynski.user.{UserApi, UserService}
import com.pawelzabczynski.util.IdGenerator
import io.prometheus.client.CollectorRegistry
import zio.{URLayer, ZLayer}

object ApplicationTestFixtures {
  private def createHttpApi(db: TestZioDB): HttpApi = {
    val idGenerator    = IdGenerator.default
    val clock          = TestClock
    val http           = new Http()
    val apiKeyAuthOps  = new ApiKeyAuthOps
    val apiKeyAuth     = new Auth[ApiKey](apiKeyAuthOps, clock, db.xa)
    val registry       = CollectorRegistry.defaultRegistry
    val accountService = new AccountService(idGenerator, clock)
    val apiKeyService  = new ApiKeyService(idGenerator, clock)
    val userService =
      new UserService(TestConfig.userService, accountService, apiKeyService, idGenerator, clock, db.xa)
    val userApi    = new UserApi(userService, apiKeyAuth, http, db.xa)
    val metricsApi = new MetricsApi(http, registry)
    val endpoints  = userApi.endpoints ++ metricsApi.endpoints

    new HttpApi(http, endpoints, TestConfig.api, registry)
  }

  val testApplicationLayer: URLayer[TestZioDB, HttpApi] = ZLayer.fromFunction(createHttpApi _)
}
