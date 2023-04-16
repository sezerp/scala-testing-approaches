package com.pawelzabczynski.user

import cats.implicits.catsSyntaxApplicativeId
import com.pawelzabczynski.Fail
import com.pawelzabczynski.account.Account.AccountId
import com.pawelzabczynski.infrastructure.Doobie._
import com.pawelzabczynski.account.{Account, AccountService}
import com.pawelzabczynski.config.{Config, UserServiceConfig}
import com.pawelzabczynski.security.AdminRole
import com.pawelzabczynski.security.apiKey.{ApiKey, ApiKeyService}
import com.pawelzabczynski.user.UserService.{
  IncorrectAuthData,
  RegistrationError,
  UserRegistration,
  UserServiceError,
  UserServiceInternalError
}
import com.pawelzabczynski.util.{Clock, ErrorOps, IdGenerator}
import doobie.ConnectionIO
import doobie.util.transactor.Transactor
import zio.{IO, Task, URLayer, ZIO, ZLayer}
import zio.interop.catz._

class UserService(
    config: UserServiceConfig,
    accountService: AccountService,
    apiKeyService: ApiKeyService,
    idGenerator: IdGenerator,
    clock: Clock,
    xa: Transactor[Task]
) {
  def register(data: UserRegisterRequest): IO[UserServiceError, UserRegistration] = {
    val transaction = for {
      account <- accountService.crate(data.accountName)
      user    <- createUser(data, account.id)
      _       <- UserModel.insert(user)
      apiKey  <- apiKeyService.create(user.id)
    } yield UserRegistration(user, account, apiKey)

    checkIfUserExist(data.login, data.email) *>
      validateUserPasswordLength(data.password) *>
      transaction
        .transact(xa)
        .tapError(e => ZIO.logErrorCause("Error occurred when try register new user", e.toCause))
        .mapError(_ => UserServiceInternalError)
  }

  def login(request: UserLoginRequest): IO[UserServiceError, ApiKey] = {
    for {
      maybeUser <- UserModel
        .login(request.loginOrEmail)
        .transact(xa)
        .tapError(e => ZIO.logErrorCause(s"Error occurred when try find user ${request.loginOrEmail}.", e.toCause))
        .mapError(_ => UserServiceInternalError)
      user <- ZIO.getOrFailWith(IncorrectAuthData)(maybeUser)
      _    <- verifyPassword(user, request.password)
      apiKey <- apiKeyService
        .create(user.id)
        .transact(xa)
        .tapError(e => ZIO.logErrorCause(s"Error occurred when try generate api key.", e.toCause))
        .mapError(_ => UserServiceInternalError)
    } yield apiKey
  }

  def changePassword(user: User, request: UserChangePasswordRequest): IO[UserServiceError, Unit] = {
    for {
      _ <- verifyPassword(user, request.currentPassword)
      _ <- validateUserPasswordLength(request.newPassword)
      pwHash = User.hash(request.newPassword)
      _ <- UserModel
        .updatePassword(user.id, pwHash)
        .transact(xa)
        .tapError(e => ZIO.logErrorCause(s"Error occurred when try generate api key.", e.toCause))
        .mapError(_ => UserServiceInternalError)
    } yield ()
  }

  def patchUser(user: User, request: UserPatchRequest): IO[UserServiceError, Unit] = {
    (for {
      updatedUser <- User.patch(user, request).pure[ConnectionIO]
      _           <- UserModel.update(updatedUser)
    } yield ())
      .transact(xa)
      .tapError(e => ZIO.logErrorCause(s"Error occurred when try patch user entity.", e.toCause))
      .mapError(_ => UserServiceInternalError)
  }

  private def verifyPassword(user: User, pw: String): IO[UserServiceError, Unit] = {
    if (User.verifyPw(pw, user.password)) ZIO.unit
    else ZIO.fail(IncorrectAuthData)
  }

  private def validateUserPasswordLength(pw: String): IO[UserServiceError, Unit] = {
    if (pw.length >= config.minPasswordLength) ZIO.unit
    else ZIO.fail(RegistrationError(s"The min password length is ${config.minPasswordLength}."))
  }

  private def checkIfUserExist(login: String, email: String): IO[UserServiceError, Unit] = {
    for {
      maybeByEmailUser <- UserModel
        .findByEmail(email)
        .transact(xa)
        .tapError(e => ZIO.logErrorCause(s"Error occurred when check if user exist by email", e.toCause))
        .mapError(_ => UserServiceInternalError)
      _ <- ZIO.noneOrFailWith(maybeByEmailUser)(_ => RegistrationError("Email already exists."))
      maybeByLoginUser <- UserModel
        .findByLogin(login)
        .transact(xa)
        .tapError(e => ZIO.logErrorCause("Error occurred when check user exist by login", e.toCause))
        .mapError(_ => UserServiceInternalError)
      _ <- ZIO.noneOrFailWith(maybeByLoginUser)(_ => RegistrationError("Login already exists."))
    } yield ()
  }

  private def createUser(data: UserRegisterRequest, accountId: AccountId): ConnectionIO[User] = {
    for {
      userId <- idGenerator.next[ConnectionIO, User]
      ts     <- clock.now[ConnectionIO]
      hashedPassword = User.hash(data.password)
    } yield User(
      userId,
      accountId,
      data.login,
      AdminRole,
      data.email.toLowerCase,
      data.login.toLowerCase,
      hashedPassword,
      ts
    )
  }
}

object UserService {
  type Env = Config with AccountService with ApiKeyService with IdGenerator with Clock with Transactor[Task]

  case class UserRegistration(user: User, account: Account, apiKey: ApiKey)

  def unsafeCreate(
      config: Config,
      accountService: AccountService,
      apiKeyService: ApiKeyService,
      idGenerator: IdGenerator,
      clock: Clock,
      xa: Transactor[Task]
  ): UserService = {
    new UserService(config.userService, accountService, apiKeyService, idGenerator, clock, xa)
  }

  sealed trait UserServiceError
  case class RegistrationError(msg: String) extends UserServiceError
  case object IncorrectAuthData             extends UserServiceError
  case object UserServiceInternalError      extends UserServiceError

  object UserServiceError {
    def toThrowable(ue: UserServiceError): Throwable = {
      ue match {
        case RegistrationError(msg)   => Fail.IncorrectInput(msg)
        case IncorrectAuthData        => Fail.Unauthorized
        case UserServiceInternalError => Fail.InternalServerError
      }
    }
  }

  val live: URLayer[Env, UserService] = ZLayer.fromFunction(unsafeCreate _)
}
