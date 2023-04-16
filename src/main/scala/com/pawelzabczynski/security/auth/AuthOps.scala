package com.pawelzabczynski.security.auth

import com.pawelzabczynski.user.User.UserId
import com.pawelzabczynski.util.Id
import com.softwaremill.tagging.@@
import doobie.ConnectionIO

import java.time.Instant

trait AuthOps[A] {
  def tokenName: String

  def findById(id: Id @@ A): ConnectionIO[Option[A]]

  def delete(token: A): ConnectionIO[Unit]

  def userId(token: A): UserId

  def validUntil(token: A): Instant

  def deleteWhenValid: Boolean
}
