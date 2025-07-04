/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.learningobjects.cpxp.postgresql

import argonaut.{Json, Parse}
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType
import scaloi.syntax.either.*

import java.sql.{PreparedStatement, ResultSet, Types}

class ArgonautUserType extends UserType[Json]:
  override def getSqlType: Int = Types.OTHER

  override def returnedClass(): Class[Json] = classOf[Json]

  override def equals(x: Json, y: Json): Boolean = x == y

  override def hashCode(x: Json): Int = x.hashCode()

  override def nullSafeGet(
    rs: ResultSet,
    position: Int,
    session: SharedSessionContractImplementor,
    owner: Any
  ): Json =
    Option(rs.getString(position)).map(parse).orNull

  override def nullSafeSet(
    st: PreparedStatement,
    value: Json,
    index: Int,
    session: SharedSessionContractImplementor
  ): Unit =
    if value eq null then st.setNull(index, Types.OTHER)
    else st.setObject(index, value.nospaces, Types.OTHER)

  override def deepCopy(value: Json): Json = value

  override def isMutable: Boolean = false

  override def disassemble(value: Json): Serializable = value

  override def assemble(cached: Serializable, owner: Any): Json = cached.asInstanceOf[Json]

  private def parse(value: String): Json =
    Parse.parse(value).valueOr(err => throw new IllegalStateException(s"Bad JSON: $err"))
end ArgonautUserType
