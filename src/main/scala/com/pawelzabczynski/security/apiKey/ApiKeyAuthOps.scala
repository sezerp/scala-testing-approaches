package com.pawelzabczynski.security.apiKey

import com.pawelzabczynski.security.auth.AuthOps
import com.pawelzabczynski.user.User.UserId
import com.pawelzabczynski.util.Id
import com.softwaremill.tagging.@@
import doobie.ConnectionIO
import zio.{ULayer, ZLayer}

import java.time.Instant

class ApiKeyAuthOps extends AuthOps[ApiKey] {
  override def tokenName: String = "ApiKeyAuth"

  override def findById(id: Id @@ ApiKey): ConnectionIO[Option[ApiKey]] = ApiKeyModel.findById(id)

  override def delete(token: ApiKey): ConnectionIO[Unit] = ApiKeyModel.delete(token.id)

  override def userId(token: ApiKey): UserId = token.userId

  override def validUntil(token: ApiKey): Instant = token.validUntil

  override def deleteWhenValid: Boolean = false
}

object ApiKeyAuthOps {
  def unsafeCreate(): AuthOps[ApiKey] = {
    new ApiKeyAuthOps()
  }

  val live: ULayer[AuthOps[ApiKey]] = ZLayer.fromFunction(unsafeCreate _)
}
