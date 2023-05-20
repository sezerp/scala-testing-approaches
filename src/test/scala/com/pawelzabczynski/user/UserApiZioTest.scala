package com.pawelzabczynski.user

import com.pawelzabczynski.test.zio_base.{TestApplication, TestZioEmbeddedPostgres, ZIORequests, identityLayer}
import zio.{Scope, Task, ULayer, ZIO}
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault}
import zio.test._
import sttp.client3.SttpBackend

object UserApiZioTest extends ZIOSpecDefault {
  val testLive: ULayer[TestZioEmbeddedPostgres with SttpBackend[Task, Any]] =
    TestZioEmbeddedPostgres.testLive >>> (identityLayer[TestZioEmbeddedPostgres] ++ TestApplication.testLive)
  override def spec: Spec[TestEnvironment with Scope, Any] = (suite("UserApiZioTest")(
    test("[POST] register endpoint") {
      for {
        backend <- ZIO.service[SttpBackend[Task, Any]]
        request = UserRegisterRequest("account name", "user_login", "test@email.com", "some_password_123")
        _ <- ZIORequests.userRegister(request)(backend)
      } yield assertTrue(true)
    },
    test("[POST] login endpoint") {
      for {
        backend <- ZIO.service[SttpBackend[Task, Any]]
        user    <- ZIORequests.createUser(backend)
        request = UserLoginRequest(user.login, user.password)
        _ <- ZIORequests.userLogin(request)(backend)
      } yield assertTrue(true)
    }
  ) @@
    TestAspect.before(TestZioEmbeddedPostgres.beforeEach()) @@
    TestAspect.after(TestZioEmbeddedPostgres.afterEach()) @@
    TestAspect.afterAll(TestZioEmbeddedPostgres.afterAll()) @@
    TestAspect.sequential).provideLayerShared(testLive)
}
