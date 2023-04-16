package com.pawelzabczynski.security

import com.pawelzabczynski.Fail
import com.pawelzabczynski.user.User
import com.pawelzabczynski.util.Id
import zio.IO

package object auth {
  object AuthService {
    trait Service {
      def auth(id: Id): IO[AuthError, User]
    }

    sealed trait AuthError
    case object ExpiredToken      extends AuthError
    case object NotExists         extends AuthError
    case object AuthInternalError extends AuthError

    object AuthError {
      def toThrowable(error: AuthError): Throwable = {
        error match {
          case AuthInternalError => Fail.InternalServerError
          case _                 => Fail.Unauthorized
        }
      }
    }
  }
}
