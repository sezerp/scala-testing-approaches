package com.pawelzabczynski.account

import com.pawelzabczynski.util.{Clock, IdGenerator}
import doobie.ConnectionIO
import zio.{URLayer, ZLayer}

class AccountService(idGenerator: IdGenerator, clock: Clock) {
  def crate(name: String): ConnectionIO[Account] = {
    for {
      id <- idGenerator.next[ConnectionIO, Account]
      ts <- clock.now[ConnectionIO]
      account = Account(id, name, ts)
      _ <- AccountModel.insert(account)
    } yield account
  }
}

object AccountService {
  def unsafeCreate(idGenerator: IdGenerator, clock: Clock): AccountService = {
    new AccountService(idGenerator, clock)
  }

  val live: URLayer[IdGenerator with Clock, AccountService] = ZLayer.fromFunction(unsafeCreate _)
}
