package com.pawelzabczynski.account

import com.pawelzabczynski.account.Account.AccountId
import com.pawelzabczynski.util.Id
import com.softwaremill.tagging.@@
import doobie.ConnectionIO
import cats.syntax.functor._
import com.pawelzabczynski.infrastructure.Doobie._

import java.time.Instant

private[account] object AccountModel {
  def insert(account: Account): ConnectionIO[Unit] = {
    sql"""INSERT INTO accounts (id, name, created_at)
         VALUES (${account.id}, ${account.name}, ${account.createdAt})
         """.stripMargin.update.run.void
  }
}

case class Account(id: AccountId, name: String, createdAt: Instant)

object Account {
  type AccountId = Id @@ Account
}
