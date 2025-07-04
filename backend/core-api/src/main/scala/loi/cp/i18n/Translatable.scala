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

trait Translatable[Message]:

  /** Construct a (regex?) pattern from the given message.
    */
  def pattern(message: Message): Option[String]

  /** Translate the given message with arguments resulting in a localized String.
    */
  def translate(message: Message): String

object Translatable:

  /** Any type of translatable message, along with a proof of translatability. */
  trait Any:

    /** The type of the translatable message. */
    type Message

    /** The translatable message. */
    val message: Message

    /** The `Translatable` instance for `Message`. */
    val ev: Translatable[Message]

    final def concreteStreamableCharSequenceForJvmClients: String = ev.translate(message)
  end Any

  object Any:

    /** Wrap a message of type `M` along with its `Translatable` instance. */
    def apply[M: Translatable](m: M): Any =
      new Any:
        type Message = M
        val message = m
        val ev      = implicitly[Translatable[Message]]

    /** Destructure a `Translatable.Any`. */
    def unapply(a: Any): Option[(a.Message, Translatable[a.Message])] =
      Some((a.message, a.ev))
  end Any

  /** Mix-in/import this to be able to use raw strings as translatable things. */
  trait RawStrings:
    implicit val rawStringTranslatable: Translatable[String] =
      new Translatable[String]:
        def pattern(message: String)   = None
        def translate(message: String) = message
  object RawStrings extends RawStrings
end Translatable
