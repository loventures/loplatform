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

package loi.cp.i18n

import java.text.MessageFormat
import java.util.{MissingResourceException, ResourceBundle}
import javax.inject.Inject

import com.learningobjects.cpxp.util.InternationalizationUtils

/** A Message service that provides internationalized messages for [[BundleMessage]] s with a given [[ResourceBundle]]
  */
final class ResourceBundleTranslator(@Inject defaultBundle: ResourceBundle) extends Translatable[BundleMessage]:

  private def attemptGetResource(bundle: ResourceBundle, key: String): Option[String] = try
    Option(bundle.getString(key))
  catch case ex: MissingResourceException => None

  override def pattern(message: BundleMessage): Option[String] = message match
    case bundle: ResourceBundleMessage => attemptGetResource(bundle.bundle, bundle.key)
    case _                             => Option.empty

  override def translate(message: BundleMessage): String =
    Option(InternationalizationUtils.getMessages.getMessage(message.key))
      // "because this contains the admin-installed language pack and the browser-requested language.
      // this could just use one of the language services but we need to know what language to use."
      // - a wise wizard
      .orElse(pattern(message))
      .orElse(attemptGetResource(defaultBundle, message.key))
      .map(pattern => MessageFormat.format(pattern, message.args*))
      .getOrElse(message.key)
end ResourceBundleTranslator

final class ThrowableTranslator extends Translatable[Throwable]:
  override def pattern(message: Throwable): Option[String] = None

  override def translate(message: Throwable): String =
    Option(
      message.getLocalizedMessage
    ) getOrElse s"${message.getClass.getName} at:\n${message.getStackTrace take 16 mkString "\n"}"
