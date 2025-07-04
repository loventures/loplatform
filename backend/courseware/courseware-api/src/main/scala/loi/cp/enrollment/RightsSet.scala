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

package loi.cp.enrollment

import argonaut.*
import Argonaut.*
import loi.cp.right.Right

/** A collection of granted and denied rights for a context. This can be queries as to whether the holder of the rights
  * has a given right. To have a right, the holder must be granted the right or a super right thereof and must not have
  * any denials
  */
case class RightsSet(enrollmentRights: Set[EnrollmentRight]):
  private val allowedRights: Set[Class[? <: Right]] =
    enrollmentRights.collect({ case GrantRight(granted) =>
      granted
    })

  private val deniedRights: Set[Class[? <: Right]] =
    enrollmentRights.collect({ case DenyRight(denied) =>
      denied
    })

  /** Returns whether these rights grant the given right. The rights must contain the given right or super right and
    * must not have any denials of the given right or any super right.
    *
    * @param right
    *   the right to check for
    * @return
    *   whether the these rights grant the given right
    */
  def hasRight(right: Class[? <: Right]): Boolean =
    // This sorts [Child, Parent, Grandparent] if analogous to sorting by 'youngest'
    val mostSpecificGrant: Option[Class[? <: Right]] =
      allowedRights
        .filter(allowed => isEqualOrSuperior(allowed, right))
        .toList
        .sortWith(moreSpecificThan)
        .headOption

    val mostSpecificDenial: Option[Class[? <: Right]] =
      deniedRights
        .filter(denied => isEqualOrSuperior(denied, right))
        .toList
        .sortWith(moreSpecificThan)
        .headOption

    (mostSpecificGrant, mostSpecificDenial) match
      case (Some(_), None)           => true                          // Any grant for the right without denial
      case (Some(grant), Some(deny)) => moreSpecificThan(grant, deny) // Child grant will trump Parent deny
      case (None, _)                 => false                         // No grants for right given
  end hasRight

  private def isEqualOrSuperior(right: Class[? <: Right], subright: Class[? <: Right]): Boolean =
    right.isAssignableFrom(subright)

  // Super rights (supertypes) are LESS specific
  private def moreSpecificThan(lhs: Class[? <: Right], rhs: Class[? <: Right]): Boolean =
    isEqualOrSuperior(rhs, lhs) && !lhs.equals(rhs) // rhs is a supertype

  /** Returns whether these rights grant any of the given right. This must contain at least one of the given rights such
    * that the given right or a super right is granted and must not have any denials of the given right or any super
    * right. Denial of one of the given {{rights}} does not deny the other given rights.
    *
    * @param rights
    *   the rights to check for
    * @return
    *   whether one or more of the given rights is granted
    */
  def hasAnyRight(rights: Class[? <: Right]*): Boolean =
    rights.exists(hasRight)
end RightsSet

object RightsSet:
  def of(rights: EnrollmentRight*): RightsSet = RightsSet(Set(rights*))

  val empty: RightsSet = RightsSet(Set.empty)

/** A grant or denial of a right. When combined together, these determine if you have a certain right depending on how
  * specific the relevant grants and denials are.
  */
sealed abstract class EnrollmentRight

/** Grants the user a right or sub right.
  *
  * @param right
  *   the right to grant
  */
case class GrantRight(right: Class[? <: Right]) extends EnrollmentRight

object GrantRight:
  def apply(rights: Class[? <: Right]*): Seq[GrantRight] =
    rights.map(GrantRight.apply)

/** Denies the user a right, unless a more specific grant is given.
  *
  * @param right
  *   the right to deny
  */
case class DenyRight(right: Class[? <: Right]) extends EnrollmentRight

object DenyRight:
  def apply(rights: Class[? <: Right]*): Seq[DenyRight] =
    rights.map(DenyRight.apply)

object EnrollmentRight:

  implicit val encodeJson: EncodeJson[EnrollmentRight] =
    EncodeJson({
      case GrantRight(granted) => granted.getName.asJson
      case DenyRight(denied)   => ("-" + denied.getName).asJson
    })

  implicit val decodeJson: DecodeJson[EnrollmentRight] =
    DecodeJson(cursor =>
      cursor
        .as[String]
        .flatMap(str =>
          val isDeny: JsonBoolean = str.startsWith("-")
          val className: String   =
            if isDeny then str.substring(1)
            else str

          getClass(className)
            .map(rightClass =>
              val enrollmentRight: EnrollmentRight =
                if isDeny then DenyRight(rightClass)
                else GrantRight(rightClass)

              DecodeResult.ok(enrollmentRight)
            )
            .getOrElse(DecodeResult.fail(s"Failure to decode Right class from: $str", cursor.history))
        )
    )

  private def getClass(className: String): Option[Class[? <: Right]] =
    try
      val cls: Class[?] = Class.forName(className)
      if classOf[Right].isAssignableFrom(cls) then // Am I a right?
        Some(cls.asInstanceOf[Class[? <: Right]])
      else None
    catch case _: ClassNotFoundException => None
end EnrollmentRight
