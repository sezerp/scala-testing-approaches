package com.pawelzabczynski.user

import cats.implicits.catsSyntaxOptionId
import com.pawelzabczynski.test.{CreateUserResult, TestBase, TestSupport}
import org.scalatest.concurrent.Eventually
import sttp.model.StatusCode
import com.pawelzabczynski.infrastructure.JsonSupport._
import com.pawelzabczynski.security.AdminRole

class UserApiTest extends TestBase with TestSupport with Eventually {

  "[POST] register endpoint" should "return apiKey" in {
    val request  = UserRegisterRequest("account name", "user_login", "test@email.com", "some_password_123")
    val response = requests.userRegister(request)

    response.code shouldBe StatusCode.Ok
    response.body.shouldDeserializeTo[UserRegisterResponse]
  }

  "[POST] login endpoint" should "successfully process form and return api key" in {
    val CreateUserResult(_, login, password, _) = requests.createUser()
    val request                                 = UserLoginRequest(login, password)
    val response                                = requests.userLogin(request)

    response.code shouldBe StatusCode.Ok
    response.body.shouldDeserializeTo[UserLoginResponse]
  }

  "[GET] user endpoint" should "successfully return basic user information" in {
    val CreateUserResult(_, _, _, apiKey) = requests.createUser()
    val response                          = requests.userGet(apiKey)

    response.code shouldBe StatusCode.Ok
    response.body.shouldDeserializeTo[UserGetResponse]
    response.body.shouldDeserializeTo[UserGetResponse].role shouldBe AdminRole
  }

  "[PATCH] user endpoint" should "successfully update login and email" in {
    val CreateUserResult(_, _, _, apiKey) = requests.createUser()
    val entity                            = UserPatchRequest("new_login".some, "new_email@email.com".some)
    val response                          = requests.userPatch(entity, apiKey)

    response.code shouldBe StatusCode.Ok
  }
}
