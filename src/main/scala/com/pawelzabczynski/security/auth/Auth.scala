package com.pawelzabczynski.security.auth

import cats.implicits.toFunctorOps
import com.pawelzabczynski.infrastructure.Doobie._
import com.pawelzabczynski.security.apiKey.ApiKey
import com.pawelzabczynski.security.auth.AuthService.{AuthError, AuthInternalError, ExpiredToken, NotExists, Service}
import com.pawelzabczynski.user.{User, UserModel}
import com.pawelzabczynski.util.{Clock, ErrorOps, Id}
import doobie.util.transactor.Transactor
import zio.interop.catz._
import zio.{IO, Task, UIO, URLayer, ZIO, ZLayer}

class Auth[A](authOps: AuthOps[A], clock: Clock, xa: Transactor[Task]) extends Service {
  override def auth(id: Id): IO[AuthService.AuthError, User] = {
    for {
      maybeToken <- authOps
        .findById(id.asId[A])
        .transact(xa)
        .tapError(e => ZIO.logErrorCause(s"Error occurred when try find token for ID: `$id`", e.toCause))
        .mapError(_ => NotExists)
      token <- ZIO.getOrFailWith(NotExists)(maybeToken)
      _     <- isValidToken(token)
      userId = authOps.userId(token)
      maybeUser <- UserModel
        .findById(userId)
        .transact(xa)
        .tapError(e => ZIO.logErrorCause("Error occurred when try fetch user information", e.toCause))
        .mapError(_ => AuthInternalError)
      user <- ZIO.getOrFailWith(NotExists)(maybeUser)
    } yield user
  }

  private def isValidToken(token: A): IO[AuthError, Unit] = {
    clock.now[UIO].flatMap { ts =>
      if (ts.isAfter(authOps.validUntil(token)))
        authOps
          .delete(token)
          .void
          .transact(xa)
          .tapError(e => ZIO.logErrorCause(s"Error occurred when try delete token.", e.toCause))
          .mapError(_ => ExpiredToken)
      else if (authOps.deleteWhenValid)
        authOps
          .delete(token)
          .transact(xa)
          .tapError(e => ZIO.logErrorCause(s"Error occurred when try delete token.", e.toCause))
          .catchAll(_ => ZIO.unit)
      else ZIO.unit
    }
  }
}

object Auth {
  def unsafeCreate[A](authOps: AuthOps[A], clock: Clock, xa: Transactor[Task]): Auth[A] = {
    new Auth[A](authOps, clock, xa)
  }

  val liveApiKeyAuth: URLayer[AuthOps[ApiKey] with Clock with Transactor[Task], Auth[ApiKey]] =
    ZLayer.fromFunction(unsafeCreate[ApiKey] _)
}
