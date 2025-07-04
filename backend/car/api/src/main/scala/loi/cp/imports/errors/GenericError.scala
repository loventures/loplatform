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

package loi.cp.imports
package errors

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonSubTypes, JsonTypeInfo}
import com.learningobjects.cpxp.component.ComponentDescriptor
import com.learningobjects.cpxp.scala.util.I18nMessage
import loi.cp.Widen
import scalaz.std.list.*
import scalaz.syntax.foldable.*
import scalaz.syntax.semigroup.*
import scalaz.{Foldable, NonEmptyList, Semigroup}

/** Represents an error that occurred during an import. This could be a [[ValidationError]], if validation for the
  * request failed, or a [[PersistError]], if an error occurred while persisting the request. These errors are stored
  * collected in an [[loi.cp.imports.ImportComponent]] via the [[loi.cp.imports.ImportComponent.addFailure]] method.
  */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type", visible = true)
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(classOf[DeserializeError]),
    new JsonSubTypes.Type(classOf[PersistError]),
    new JsonSubTypes.Type(classOf[ValidationError]),
  )
)
sealed abstract class GenericError extends Widen[GenericError]:
  def _type: String
  @JsonIgnore def messages(implicit cd: ComponentDescriptor): Seq[String]
end GenericError

/** An error encountered while mapping JSON to ImportItems */
final case class DeserializeError(
  msg: String
) extends GenericError:
  val _type                                               = "DeserializeError"
  override def messages(implicit cd: ComponentDescriptor) = Nil

/** An error encountered while persisting import data */
final case class PersistError(
  msgs: List[I18nMessage], // but a non-empty one, please
  _type: String = "PersistError"
) extends GenericError:
  override def messages(implicit cd: ComponentDescriptor): Seq[String] =
    msgs.map(msg => msg.i18n)
object PersistError:
  def apply(message: String)                = new PersistError(I18nMessage(message) :: Nil)
  def apply(messages: NonEmptyList[String]) = new PersistError(messages.map(I18nMessage.apply).toList)
  def unexpected(th: Throwable)             = PersistError(s"Unexpected error: ${th.getMessage}")

  implicit val semigroup: Semigroup[PersistError] =
    (l, r) => PersistError(l.msgs |+| r.msgs)

/** An error caused by import data failing semantic validation */
final case class ValidationError(
  violations: Seq[Violation],
  _type: String = "ValidationError"
) extends GenericError:
  override def messages(implicit cd: ComponentDescriptor): Seq[String] =
    violations.map(_.message.i18n)

object ValidationError:
  def apply[F[_]: Foldable](violations: F[Violation]): ValidationError =
    new ValidationError(violations.toList)
