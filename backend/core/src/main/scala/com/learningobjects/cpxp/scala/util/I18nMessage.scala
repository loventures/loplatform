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

package com.learningobjects.cpxp.scala.util

import com.learningobjects.cpxp.component.ComponentDescriptor
import com.learningobjects.cpxp.component.util.ComponentUtils
import com.learningobjects.cpxp.util.InternationalizationUtils
import org.apache.commons.text.StringEscapeUtils
import scaloi.syntax.any.*

import java.text.{DateFormat, SimpleDateFormat}
import java.time.Instant
import java.util.{Collections, Date}
import scala.jdk.CollectionConverters.*
import scala.util.Try

/** An internationalizable message. This type is used to support discoverable messages within the platform.
  * @param key
  *   the message key
  * @param message
  *   the untranslated error message
  * @param html
  *   if the message is html
  */
case class I18nMessage(key: Option[String], message: String, html: Boolean):
  import I18nMessage.*

  /** Returns the translation of this message.
    * @param cd
    *   component scope for translation
    * @return
    *   the translation
    */
  def i18n(implicit cd: ComponentDescriptor): String =
    i18n(Collections.emptyMap[String, Any])

  /** Returns the translation of this message.
    * @param variable
    *   a variable binding
    * @param rest
    *   additional variable bindings
    * @param cd
    *   component scope for translation
    * @return
    *   the translation
    */
  def i18n(variable: (String, Any), rest: (String, Any)*)(implicit cd: ComponentDescriptor): String =
    i18n((variable +: rest).toMap.asJava)

  /** Returns the translation of this message.
    * @param variables
    *   the variable bindings
    * @param cd
    *   component scope for translation
    * @return
    *   the translation
    */
  def i18n(variables: Seq[(String, Any)])(implicit cd: ComponentDescriptor): String = i18n(variables.toMap.asJava)

  /** Returns the translation of this message.
    * @param variables
    *   the variable bindings; either a map or an object
    * @param cd
    *   component scope for translation
    * @return
    *   the translation
    */
  def i18n(variables: AnyRef)(implicit cd: ComponentDescriptor): String =
    TokenRe.replaceAllIn(
      translated,
      { m =>
        val token = m.group(1)
        Try(
          Option(ComponentUtils.dereference(variables, token.split('.')*))
            .map(value => formatReplacement(value, m.group(2), m.group(3)))
            .fold(throw new Exception("Evaluated to null")) { v =>
              if html then StringEscapeUtils.escapeHtml4(v) else v
            }
        ).recover { case e: Exception =>
          logger.warn(e)(s"Error evaluating $token")
          s"???$token???"
        }.get
      }
    )

  private def formatReplacement(value: AnyRef, formatter: String, arg: String): String = formatter match
    case null     => formatDate(value)
    case "choice" => formatChoice(value.toString, arg).getOrElse("")
    case "time"   => dateFormatter(arg).format(toDate(value))

  private def toDate(value: AnyRef) = value match
    case i: Instant => Date `from` i
    case o          => o.asInstanceOf[Date]

  private def dateFormatter(fmt: String): SimpleDateFormat =
    new SimpleDateFormat(fmt, InternationalizationUtils.getLocale) <| { fmt =>
      fmt.setTimeZone(InternationalizationUtils.getTimeZone)
    }

  /* Date#toString is pretty unfriendly */
  private def formatDate(arg: AnyRef): String = arg match
    case d: Date    => DateFormat.getInstance().format(d)
    case i: Instant => DateFormat.getInstance().format(Date `from` i)
    case o          => o.toString
    /* wryyy */

  private def formatChoice(token: String, choices: String): Option[String] =
    choices.split('|') collectFirst {
      case NHash(n, s) if n.toInt == token.toInt => s
      case NLess(n, s) if n.toInt < token.toInt  => s
    }

  /** Return the translated but unexpanded message.
    */
  private def translated(implicit cd: ComponentDescriptor): String =
    key.fold(ComponentUtils.i18n(message, cd)) { k =>
      ComponentUtils.getMessage(k, message, cd)
    }

  /** Converts the message to a regex string.
    * @return
    *   the message as a regex
    */
  def toRegexString: String = TokenRe.replaceAllIn(message, ".*")
end I18nMessage

/** Internationalizable message singleton.
  */
object I18nMessage:

  /** Construct an i18n message from a message.
    * @param message
    *   the message
    * @return
    *   the I18n message
    */
  def apply(message: String): I18nMessage = I18nMessage(None, message, html = false)

  /** Construct an i18n message from a key and message. If the key ends in "_html" the message will be treated as HTML.
    * @param key
    *   the message key
    * @param message
    *   the message
    * @return
    *   the I18n message
    */
  def apply(key: String, message: String): I18nMessage = I18nMessage(Option(key), message, key.endsWith("_html"))

  def key(key: String) = I18nMessage(Some(key), message = null, html = false)

  /** The _logger. */
  private val logger = org.log4s.getLogger

  /** Matches variable expansions of the form {name} and {user.fullName}. */
  private val TokenRe = """\{([.\w]*)(?:,(\w*),([^}]*))?\}""".r

  private val NHash = """(\d+)#(.*)""".r
  private val NLess = """(\d+)<(.*)""".r
end I18nMessage
