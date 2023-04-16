package com.pawelzabczynski.test

import com.pawelzabczynski.user.{UserLoginRequest, UserPatchRequest, UserRegisterRequest, UserRegisterResponse}
import com.pawelzabczynski.infrastructure.JsonSupport._
import com.pawelzabczynski.security.apiKey.ApiKey.ApiKeyId
import io.circe.syntax.EncoderOps
import sttp.client3.Response
import sttp.client3.{UriContext, basicRequest}

import java.util.UUID

trait UserRequests { self: TestRequestSupport with TestSupport =>

  def createUser(): CreateUserResult = {
    val login       = s"user_${UUID.randomUUID()}"
    val email       = s"user-$login@email.com"
    val password    = UUID.randomUUID().toString
    val accountName = s"account-${UUID.randomUUID()}"
    val entity      = UserRegisterRequest(accountName, login, email, password)
    val response    = userRegister(entity).body.shouldDeserializeTo[UserRegisterResponse]

    CreateUserResult(email, login, password, response.apiKey)
  }

  def userRegister(entity: UserRegisterRequest): Response[Either[String, String]] = {
    basicRequest
      .post(uri"$basePath/user/register")
      .body(entity.asJson.noSpaces)
      .send(backend)
      .runUnsafe()
  }

  def userLogin(entity: UserLoginRequest): Response[Either[String, String]] = {
    basicRequest
      .post(uri"$basePath/user/login")
      .body(entity.asJson.noSpaces)
      .send(backend)
      .runUnsafe()
  }

  def userGet(apiKey: ApiKeyId): Response[Either[String, String]] = {
    basicRequest
      .get(uri"$basePath/user")
      .header("Authorization", s"Bearer $apiKey")
      .send(backend)
      .runUnsafe()
  }

  def userPatch(apiKey: ApiKeyId): Response[Either[String, String]] = {
    basicRequest
      .get(uri"$basePath/user")
      .header("Authorization", s"Bearer $apiKey")
      .send(backend)
      .runUnsafe()
  }

  def userPatch(entity: UserPatchRequest, apiKey: ApiKeyId): Response[Either[String, String]] = {
    basicRequest
      .patch(uri"$basePath/user")
      .header("Authorization", s"Bearer $apiKey")
      .body(entity.asJson.noSpaces)
      .send(backend)
      .runUnsafe()
  }
}

final case class CreateUserResult(email: String, login: String, password: String, apiKey: ApiKeyId)
