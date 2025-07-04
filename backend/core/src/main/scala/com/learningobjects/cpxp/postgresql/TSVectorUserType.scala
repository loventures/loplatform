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

import com.learningobjects.cpxp.util.entity.TSVector
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType

import java.sql.{PreparedStatement, ResultSet, Types}

/** JPA support for TSVertor entity values.
  *
  * Formerly we decoded TSVector types inside the entity classes; for example, public void setFullName(String fullName)
  * { this.fullName = TSVector.fromTsVector(fullName); } however this led to Hibernate occasionally thinking the user *
  * dirty, because the original property value from the database did not match the property in the entity. This user
  * type pushes that decoding down beneath Hibernate so that the entities no longer seem as dirty to Hibernate.
  *
  * The hypersistence has a tsvector type but I don't understand it and it offers no control over the vector language.
  */
class TSVectorUserType extends UserType[String]:
  override def getSqlType: Int = Types.OTHER

  override def returnedClass(): Class[String] = classOf[String]

  override def equals(x: String, y: String): Boolean = x == y

  override def hashCode(x: String): Int = x.hashCode()

  override def nullSafeGet(
    rs: ResultSet,
    position: Int,
    session: SharedSessionContractImplementor,
    owner: Any
  ): String =
    TSVector.fromTsVector(rs.getString(position))

  override def nullSafeSet(
    st: PreparedStatement,
    value: String,
    index: Int,
    session: SharedSessionContractImplementor
  ): Unit =
    if value eq null then st.setNull(index, Types.OTHER)
    else st.setObject(index, value, Types.OTHER)

  override def deepCopy(value: String): String = value

  override def isMutable: Boolean = false

  override def disassemble(value: String): Serializable = value

  override def assemble(cached: Serializable, owner: Any): String = cached.asInstanceOf[String]
end TSVectorUserType
