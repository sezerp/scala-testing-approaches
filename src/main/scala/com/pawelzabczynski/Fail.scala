package com.pawelzabczynski

abstract class Fail extends Exception

object Fail {
  case object InternalServerError        extends Fail
  case class IncorrectInput(msg: String) extends Fail
  case object Unauthorized               extends Fail
}
