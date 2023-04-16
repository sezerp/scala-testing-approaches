package com.pawelzabczynski.security

import cats.Show
import cats.syntax.show._
import doobie.Meta
import io.circe.{Decoder, Encoder, Json}

sealed trait Role
case object AdminRole extends Role
case object UserRole  extends Role

object Role {
  implicit val roleShow: Show[Role] = Show.fromToString[Role]

  val from: PartialFunction[String, Role] = {
    case "AdminRole" => AdminRole
    case "UserRole"  => UserRole
  }

  trait RoleJsonSupport {
    implicit val roleEncoder: Encoder[Role] = Encoder.instance(v => Json.fromString(v.toString))
    implicit val roleDecoder: Decoder[Role] = Decoder.instance(_.as[String].map(from))
  }

  trait RoleDoobieSupport {
    implicit val roleMeta: Meta[Role] = Meta[String].timap(from)(_.show)
  }
}
