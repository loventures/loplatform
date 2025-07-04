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

package loi.cp.internationalization

import java.util.Locale

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{CacheOptions, ErrorResponse, Lazy}
import com.learningobjects.cpxp.component.{
  ComponentDescriptor,
  ComponentImplementation,
  ComponentInstance,
  ComponentSupport
}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.internationalization.VersionedMessageMapCompositor
import com.learningobjects.cpxp.service.language.LanguageService
import com.learningobjects.cpxp.util.Out
import com.learningobjects.cpxp.util.message.{MessageMap, MessageMapCompositor}
import loi.cp.language.{LanguageRoot, LocaleInfo}
import scalaz.\/
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*

import scala.jdk.CollectionConverters.*
import scala.collection.mutable

@Component
class InternationalizationRoot(
  val componentInstance: ComponentInstance,
  i18nSvc: LanguageService,
  implicit val fs: FacadeService
) extends InternationalizationRootComponent
    with ComponentImplementation:

  import InternationalizationRoot.*

  private final val MessageRef = "``([^`]+)``".r

  override def i18nMeta(localeStr: String, component: String): ErrorResponse \/ I18nMeta =
    for cd <- getComponent(component)
    yield i18nResult(localeStr, cd, toI18nMeta)

  override def i18nMessages(
    localeStr: String,
    component: String,
    cacheOptions: Out[CacheOptions]
  ): ErrorResponse \/ Lazy[Map[String, String]] =
    for cd <- getComponent(component)
    yield
      cacheOptions.set(CacheOptions(LanguageRoot.languageFolder, cd.getArchive.getIdentifier))
      Lazy { i18nResult(localeStr, cd, toI18nMessages) }

  private def getComponent(
    component: String
  ): ErrorResponse \/ ComponentDescriptor = // s"Unknown component: $component"
    Option(ComponentSupport.getComponentDescriptor(component))
      .toRightDisjunction(ErrorResponse.notFound)

  private def toI18nMessages: Internationalizer[Map[String, String]] = (cd, messages, locales, localeMaps) =>
    // Concatenate the message maps
    val concatenated = localeMaps.flatMap(_._2.toMap.toSeq).toMap
    concatenated.transform { (key, value) =>
      MessageRef.replaceAllIn(value, m => concatenated.getOrElse(m.group(1), m.toString))
    }

  private def toI18nMeta: Internationalizer[I18nMeta] = (cd, messages, locales, localeMaps) =>
    // Find the most specific locale in the resulting list
    val locale    = localeMaps.lastOption.fold(Locale.getDefault)(_._1)
    // Determine available locales in the domain and component
    val available =
      (messages.getAvailableMessages.keySet.asScala ++ cd.getAvailableMessages.keySet.asScala).toList
        .map(LocaleInfo(_, locale))
    I18nMeta(cd.getIdentifier, LocaleInfo(locale, locale), available)

  private def i18nResult[T](localeStr: String, cd: ComponentDescriptor, f: Internationalizer[T]): T =
    val messages   = i18nSvc.getDomainMessages
    // Compute our locale search path, least to most-specific: en, en-US, fr, fr-FR
    val locales    =
      localeSearch(Locale.forLanguageTag(localeStr), messages.getDefaultLocale)
    // Determine all supported message maps for the locale search
    val localeMaps = locales.flatMap(localeMessages(messages, cd))
    f(cd, messages, locales, localeMaps)

  // Given the domain messages and a component, for a given locale, returns any messages defined in that component
  // for that exact locale, and any messages defined in the domain for that exact component and locale.
  // Remove the namespaces as well.
  //  component -> domain-nonamespace -> global-componentnamespace-override
  private def localeMessages(mmc: MessageMapCompositor, cd: ComponentDescriptor)(l: Locale): Seq[LocaleMessages] =
    List(
      Option(cd.getAvailableMessages.get(l)).map(_.asScala),
      Option(mmc.getAvailableMessages.get(l))
        .map(mm => global(mm))
        .filterNot(_.isEmpty),
      Option(mmc.getAvailableMessages.get(l))
        .map(mm => slice(mm, cd.getIdentifier.concat("/")))
        .filterNot(_.isEmpty)
    ).flatMap(optMap => optMap.map(m => (l, m)))

  // Returns the slice of messages matching a given prefix, with the prefix removed
  private def slice(mm: MessageMap, prefix: String): mutable.Map[String, String] =
    mm.asScala collect {
      case (key, value) if key.startsWith(prefix) =>
        (key.substring(prefix.length), value)
    }

  // TODO: Split up messagemaps so that we calculate the namespacing once.
  private def global(mm: MessageMap): mutable.Map[String, String] =
    mm.asScala collect {
      case (key, value) if !key.contains('/') => (key, value)
    }

  // returns a hierarchy, from least specific to most specific, of locales to search starting
  // with the default hierarchy followed by the requested locale
  private def localeSearch(locale: Locale, defaultLocale: Locale): Seq[Locale] =
    (noFallback(locale).fold(List.empty, localeHierarchy(defaultLocale)) ++ localeHierarchy(locale)).distinct

  private def noFallback(locale: Locale): Boolean = // fr-fr-x-nf means french without fallback to english
    Option(locale.getExtension(Locale.PRIVATE_USE_EXTENSION)).contains(NoFallback)

  // TODO: always include "Default"? "en"?

  // For en-US returns en, en-US. does not support variant or other minutia
  private def localeHierarchy(l: Locale): Seq[Locale] =
    List(Locale.of(l.getLanguage)) ++ l.getCountry.nonEmpty.option(Locale.of(l.getLanguage, l.getCountry))

  private final val NoFallback = "nf"
end InternationalizationRoot

object InternationalizationRoot:
  type LocaleMessages = (Locale, mutable.Map[String, String])

  type Internationalizer[T] =
    (ComponentDescriptor, VersionedMessageMapCompositor, Seq[Locale], Seq[LocaleMessages]) => T
