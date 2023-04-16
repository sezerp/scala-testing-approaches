package com.pawelzabczynski.security.apiKey

import cats.implicits.toFunctorOps
import com.pawelzabczynski.security.apiKey.ApiKey.ApiKeyId
import com.pawelzabczynski.user.User.UserId
import com.pawelzabczynski.util.Id
import com.pawelzabczynski.infrastructure.Doobie._
import com.softwaremill.tagging.@@
import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator

import java.time.Instant

object ApiKeyModel {
  def insert(apiKey: ApiKey): ConnectionIO[Unit] = {
    import apiKey._
    sql"""INSERT INTO api_keys (id, valid_until, user_id, created_on)
         VALUES ($id, $validUntil, $userId, $createdOn)""".stripMargin.update.run.void
  }

  def findById(id: ApiKeyId): ConnectionIO[Option[ApiKey]] = {
    sql"""
         SELECT id, valid_until, user_id, created_on
         FROM api_keys
         WHERE id = $id
       """.stripMargin.query[ApiKey].option
  }

  def delete(id: ApiKeyId): ConnectionIO[Unit] = {
    sql"""DELETE FROM api_keys WHERE id = $id""".stripMargin.update.run.void
  }
}

case class ApiKey(
    id: ApiKeyId,
    validUntil: Instant,
    userId: UserId,
    createdOn: Instant
)

object ApiKey {
  type ApiKeyId = Id @@ ApiKey
}
