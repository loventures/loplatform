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

package loi.cp.slack

import com.fasterxml.jackson.annotation.{JsonProperty, JsonValue}
import com.google.common.base.Throwables
import enumeratum.*
import org.apache.commons.lang3.exception.ExceptionUtils
import scalaz.std.string.*
import scaloi.syntax.OptionOps.*

import scala.annotation.meta.field
import scala.jdk.CollectionConverters.*

/** An attachment to a Slack message. */
final case class SlackAttachment(
  /** The text of the message. */
  text: String,
  /** Short fallback text for display in limited space. */
  fallback: String,
  /** A title for the message. */
  title: Option[String] = None,
  /** The severity of this message. */
  @JsonProperty("color")
  severity: SlackSeverity = SlackSeverity.Good
)

object SlackAttachment:

  /** Construct a severe Slack attachment from an exception. */
  def fromError(err: Throwable): SlackAttachment =
    val cause: Throwable = Throwables
      .getCausalChain(err)
      .asScala
      .find {
        case (_: java.lang.reflect.InvocationTargetException) | (_: java.lang.reflect.UndeclaredThrowableException) =>
          false
        case _                                                                                                      => true
      }
      .getOrElse(err)
    SlackAttachment(
      title = Some(s"${cause.getClass.getName}${OptionNZ(err.getMessage).fold("")(m => s": $m")}"),
      text = ExceptionUtils.getStackTrace(err),
      fallback = err.getMessage,
      severity = SlackSeverity.Danger
    )
  end fromError
end SlackAttachment

/** A severity level for Slack messages. */
sealed abstract class SlackSeverity(@(JsonValue @field) val color: String) extends EnumEntry
object SlackSeverity                                                       extends Enum[SlackSeverity]:
  val values = findValues
  case object Good    extends SlackSeverity("good")
  case object Warning extends SlackSeverity("warning")
  case object Danger  extends SlackSeverity("danger")
