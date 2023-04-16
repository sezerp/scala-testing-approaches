package com.pawelzabczynski

import com.pawelzabczynski.config.{Config, Sensitive, UserServiceConfig}
import com.pawelzabczynski.http.HttpConfig
import com.pawelzabczynski.infrastructure.DbConfig
import zio.{Duration, Task, Unsafe}
import zio._

package object test {
  val TestConfig: Config = Config(
    HttpConfig("localhost", 8080),
    DbConfig("postgres", Sensitive(""), "", true, "org.postgresql.Driver", 4),
    UserServiceConfig(11)
  )

  implicit class TestUnsafeOps[A](t: Task[A]) {

    def runUnsafe(): A = {
      runUnsafe(5 minutes)
    }
    def runUnsafe(timeout: Duration): A = {
      Unsafe
        .unsafe { implicit unsafe =>
          zio.Runtime.default.unsafe
            .run(
              t.timeoutFail(new RuntimeException("The given task exceed timeout"))(timeout)
                .tapError(e => ZIO.succeed(e.printStackTrace()))
            )
            .getOrThrowFiberFailure()
        }
        .ensuring(true)
    }
  }

}
