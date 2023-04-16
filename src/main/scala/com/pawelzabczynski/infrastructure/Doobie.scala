package com.pawelzabczynski.infrastructure

import com.pawelzabczynski.security.Role.RoleDoobieSupport
import com.pawelzabczynski.util.Id
import com.softwaremill.tagging.@@
import com.typesafe.scalalogging.StrictLogging

import java.util.UUID

object Doobie
    extends doobie.Aliases
    with doobie.hi.Modules
    with doobie.free.Modules
    with doobie.free.Types
    with doobie.postgres.Instances
    with doobie.util.meta.LegacyInstantMetaInstance
    with doobie.free.Instances
    with doobie.syntax.AllSyntax
    with RoleDoobieSupport
    with StrictLogging {

  implicit def idType: Meta[Id]                   = Meta.Advanced.other[UUID]("uuid").asInstanceOf[Meta[Id]]
  implicit def taggedUUIDType[U]: Meta[UUID @@ U] = Meta.Advanced.other[UUID]("uuid").asInstanceOf[Meta[UUID @@ U]]
  implicit def taggedIdType[U]: Meta[Id @@ U]     = Meta.Advanced.other[UUID]("uuid").asInstanceOf[Meta[Id @@ U]]
}
