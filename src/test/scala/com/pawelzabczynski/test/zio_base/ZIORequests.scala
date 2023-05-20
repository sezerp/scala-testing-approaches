package com.pawelzabczynski.test.zio_base

import com.pawelzabczynski.test.scalatest.{CreateUserResult, TestConfig}
import com.pawelzabczynski.user.{
  UserGetResponse,
  UserLoginRequest,
  UserLoginResponse,
  UserPatchRequest,
  UserRegisterRequest,
  UserRegisterResponse
}
import sttp.client3.{SttpBackend, UriContext, basicRequest}
import io.circe.syntax.EncoderOps
import com.pawelzabczynski.infrastructure.JsonSupport._
import com.pawelzabczynski.security.apiKey.ApiKey.ApiKeyId
import com.pawelzabczynski.util.ErrorOps
import zio.{Task, ZIO}

import java.util.UUID

object ZIORequests {

  val basePath: String = s"http://localhost:${TestConfig.api.port}/api/v1"
  def createUser(backend: SttpBackend[Task, Any]): Task[CreateUserResult] = {
    val login       = s"user_${UUID.randomUUID()}"
    val email       = s"user-$login@email.com"
    val password    = UUID.randomUUID().toString
    val accountName = s"account-${UUID.randomUUID()}"
    val entity      = UserRegisterRequest(accountName, login, email, password)

    userRegister(entity)(backend)
      .map { user =>
        CreateUserResult(email, login, password, user.apiKey)
      }
  }

  def userRegister(entity: UserRegisterRequest)(backend: SttpBackend[Task, Any]): Task[UserRegisterResponse] = {
    basicRequest
      .post(uri"$basePath/user/register")
      .body(entity.asJson.noSpaces)
      .send(backend)
      .tapError(err => ZIO.logErrorCause("Error occurred when try register user.", err.toCause))
      .map(_.body.shouldDeserializeTo[UserRegisterResponse])
  }

  def userLogin(entity: UserLoginRequest)(backend: SttpBackend[Task, Any]): Task[UserLoginResponse] = {
    basicRequest
      .post(uri"$basePath/user/login")
      .body(entity.asJson.noSpaces)
      .send(backend)
      .tapError(err => ZIO.logErrorCause("Error occurred when try login user.", err.toCause))
      .map(_.body.shouldDeserializeTo[UserLoginResponse])
  }

  def userGet(apiKey: ApiKeyId)(backend: SttpBackend[Task, Any]): Task[UserGetResponse] = {
    basicRequest
      .get(uri"$basePath/user")
      .header("Authorization", s"Bearer $apiKey")
      .send(backend)
      .tapError(err => ZIO.logErrorCause("Error occurred when try get user.", err.toCause))
      .map(_.body.shouldDeserializeTo[UserGetResponse])
  }

  def userPatch(apiKey: ApiKeyId)(backend: SttpBackend[Task, Any]): Task[UserPatchRequest] = {
    basicRequest
      .get(uri"$basePath/user")
      .header("Authorization", s"Bearer $apiKey")
      .send(backend)
      .tapError(err => ZIO.logErrorCause("Error occurred when try patch user.", err.toCause))
      .map(_.body.shouldDeserializeTo[UserPatchRequest])
  }

  def userPatch(entity: UserPatchRequest, apiKey: ApiKeyId)(backend: SttpBackend[Task, Any]): Task[UserPatchRequest] = {
    basicRequest
      .patch(uri"$basePath/user")
      .header("Authorization", s"Bearer $apiKey")
      .body(entity.asJson.noSpaces)
      .send(backend)
      .tapError(err => ZIO.logErrorCause("Error occurred when try patch user with data.", err.toCause))
      .map(_.body.shouldDeserializeTo[UserPatchRequest])
  }
}
