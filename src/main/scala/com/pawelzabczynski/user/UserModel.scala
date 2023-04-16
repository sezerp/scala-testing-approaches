package com.pawelzabczynski.user

import doobie.ConnectionIO
import com.pawelzabczynski.infrastructure.Doobie._

import java.time.Instant
import cats.syntax.functor._
import com.pawelzabczynski.account.Account.AccountId
import com.pawelzabczynski.security.Role
import com.pawelzabczynski.user.User.UserId
import com.pawelzabczynski.util.Id
import com.softwaremill.tagging.@@

object UserModel {
  def insert(user: User): ConnectionIO[Unit] = {
    sql"""INSERT INTO users (id, account_id, login, "role", email_lowercase, login_lowercase, password, created_on)
         VALUES (${user.id}, ${user.accountId}, ${user.login}, ${user.role}, ${user.emailLowercase}, ${user.loginLowercase}, ${user.password}, ${user.createdAt})
       """.stripMargin.update.run.void
  }

  def login(emailOrLogin: String): ConnectionIO[Option[User]] = {
    val emailOrLoginLowercase = emailOrLogin.toLowerCase
    findBy(fr"WHERE email_lowercase = $emailOrLoginLowercase OR login_lowercase = $emailOrLoginLowercase")
  }

  def findById(id: UserId): ConnectionIO[Option[User]] = {
    findBy(fr"WHERE id = $id")
  }

  def findByEmail(email: String): ConnectionIO[Option[User]] = {
    val emailLowercase = email.toLowerCase
    findBy(fr"WHERE email_lowercase = $emailLowercase")
  }

  def findByLogin(login: String): ConnectionIO[Option[User]] = {
    val loginLoweCase = login.toLowerCase
    findBy(fr"WHERE login_lowercase = $loginLoweCase")
  }

  def updatePassword(id: UserId, password: String): ConnectionIO[Unit] = {
    sql"""
         UPDATE users
            SET
                password = $password
         WHERE id = $id
       """.update.run.void
  }

  def update(user: User): ConnectionIO[Unit] = {
    import user._
    sql"""
         UPDATE users
            SET
                email_lowercase = $emailLowercase,
                login_lowercase = $loginLowercase,
                login = ${user.login},
                password = $password,
                role = $role
       """.update.run.void
  }

  private def findBy(where: Fragment): ConnectionIO[Option[User]] = {
    (fr"""
        SELECT id, account_id, login, "role", email_lowercase, login_lowercase, password, created_on
        FROM users""".stripMargin ++ where)
      .query[User]
      .option
  }
}

case class User(
    id: UserId,
    accountId: AccountId,
    login: String,
    role: Role,
    emailLowercase: String,
    loginLowercase: String,
    password: String,
    createdAt: Instant
)

object User {
  import com.github.t3hnar.bcrypt._
  type UserId = Id @@ User

  def patch(user: User, request: UserPatchRequest): User = {
    val emailLowercase = request.email.fold(user.emailLowercase)(_.trim.toLowerCase)
    val login          = request.login.fold(user.loginLowercase)(_.trim)
    val loginLowerCase = request.login.fold(user.loginLowercase)(_.trim.toLowerCase)
    user.copy(
      login = login,
      loginLowercase = loginLowerCase,
      emailLowercase = emailLowercase
    )
  }

  def hash(password: String): String = {
    password.bcryptBounded(11)
  }

  def verifyPw(pw: String, pwHash: String): Boolean = {
    pw.isBcryptedBounded(pwHash)
  }
}
