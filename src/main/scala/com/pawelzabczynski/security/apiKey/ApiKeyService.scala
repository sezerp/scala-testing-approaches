package com.pawelzabczynski.security.apiKey

import cats.implicits.catsSyntaxApplicativeId
import com.pawelzabczynski.infrastructure.Doobie._
import com.pawelzabczynski.user.User.UserId
import com.pawelzabczynski.util.{Clock, IdGenerator}
import doobie.ConnectionIO
import zio.{URLayer, ZLayer}

import scala.concurrent.duration.DurationInt

class ApiKeyService(idGenerator: IdGenerator, clock: Clock) {
  def create(userId: UserId): ConnectionIO[ApiKey] = {
    for {
      apiKey <- createApiKey(userId).pure[ConnectionIO]
      _      <- ApiKeyModel.insert(apiKey)
    } yield apiKey
  }

  private def createApiKey(userId: UserId): ApiKey = {
    val id         = idGenerator.unsafeNext[ApiKey]
    val ts         = clock.unsafeNow
    val validUntil = ts.plusSeconds(1.day.toSeconds)

    ApiKey(id, validUntil, userId, ts)
  }
}

object ApiKeyService {
  def unsafeCreate(idGenerator: IdGenerator, clock: Clock): ApiKeyService = {
    new ApiKeyService(idGenerator, clock)
  }

  val live: URLayer[IdGenerator with Clock, ApiKeyService] = ZLayer.fromFunction(unsafeCreate _)
}
