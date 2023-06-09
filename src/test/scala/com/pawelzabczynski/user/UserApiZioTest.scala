package com.pawelzabczynski.user

import cats.implicits.catsSyntaxOptionId
import com.pawelzabczynski.security.AdminRole
import com.pawelzabczynski.test.zio_base.{TestApplication, TestZioEmbeddedPostgres, ZIORequests, identityLayer}
import zio.{Scope, Task, ULayer, ZIO}
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault}
import zio.test._
import sttp.client3.SttpBackend
import sttp.model.StatusCode

object UserApiZioTest extends ZIOSpecDefault {
  val testLive: ULayer[TestZioEmbeddedPostgres with SttpBackend[Task, Any]] =
    TestZioEmbeddedPostgres.testLive >>> (identityLayer[TestZioEmbeddedPostgres] ++ TestApplication.testLive)
  override def spec: Spec[TestEnvironment with Scope, Any] = (suite("UserApiZioTest")(
    test("[POST] register endpoint should successfully process form and return api key") {
      for {
        backend <- ZIO.service[SttpBackend[Task, Any]]
        request = UserRegisterRequest("account name", "user_login", "test@email.com", "some_password_123")
        _ <- ZIORequests.userRegister(request)(backend)
      } yield assertTrue(true)
    },
    test("[POST] login endpoint should successfully return basic user information") {
      for {
        backend <- ZIO.service[SttpBackend[Task, Any]]
        user    <- ZIORequests.createUser(backend)
        request = UserLoginRequest(user.login, user.password)
        _ <- ZIORequests.userLogin(request)(backend)
      } yield assertTrue(true)
    },
    test("[GET] user endpoint should successfully return basic user information") {
      for {
        backend  <- ZIO.service[SttpBackend[Task, Any]]
        user     <- ZIORequests.createUser(backend)
        response <- ZIORequests.userGet(user.apiKey)(backend)
      } yield assertTrue(response.role == AdminRole)
    },
    test("[PATCH] user endpoint should successfully update login and email") {
      for {
        backend <- ZIO.service[SttpBackend[Task, Any]]
        user    <- ZIORequests.createUser(backend)
        entity = UserPatchRequest("new_login".some, "new_email@email.com".some)
        status      <- ZIORequests.userPatch(entity, user.apiKey)(backend)
        updatedUser <- ZIORequests.userGet(user.apiKey)(backend)
      } yield assertTrue(status == StatusCode.Ok) && assertTrue(updatedUser.login == "new_login") && assertTrue(
        updatedUser.email == "new_email@email.com"
      )
    }
  ) @@
    TestAspect.before(TestZioEmbeddedPostgres.beforeEach()) @@
    TestAspect.after(TestZioEmbeddedPostgres.afterEach()) @@
    TestAspect.afterAll(TestZioEmbeddedPostgres.afterAll()) @@
    TestAspect.sequential).provideLayerShared(testLive)
}
