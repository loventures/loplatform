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

package loi.cp.language

import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.{ComponentEnvironment, ComponentService}
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.domain.DomainFacade
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.internationalization.VersionedMessageMapCompositor
import com.learningobjects.cpxp.service.language.LanguageService
import com.learningobjects.cpxp.util.message.BaseMessageMap
import com.learningobjects.cpxp.util.{ClassUtils, Encheferize, InternationalizationUtils}
import scalaz.syntax.std.`boolean`.*
import scaloi.syntax.AnyOps.*

import java.util.{Locale, TimeZone}
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

@Service
class LanguageServiceImpl(
  env: ComponentEnvironment,
  sm: ServiceMeta
)(implicit fs: FacadeService, cs: ComponentService)
    extends LanguageService:
  override def getDomainMessages: VersionedMessageMapCompositor =
    // TODO: this should use an normal cache with a long timeout so it respects normal invalidations
    val folder  =
      (Current.getDomain != null).option(LanguageRoot.languageFolder)
    val version = folder.flatMap(f => Option(f.getGeneration)).fold(0L)(_.longValue)
    // get the current messages from the component environment if the i18n version aligns, else create it
    Option(env.getAttribute(classOf[VersionedMessageMapCompositor]))
      .filter(_.getVersion == version) getOrElse {
      try
        val messages = folder.fold(defaultCompositor)(domainCompositor)
        env.setAttribute(classOf[VersionedMessageMapCompositor], messages)
        messages
      catch case NonFatal(_) => defaultCompositor
    }
  end getDomainMessages

  override def setDefaultLanguage(language: String): Unit =
    val localeStr = Locale.forLanguageTag(language).toString // stored as en_US in the domain
    val domain    = fs.getFacade(Current.getDomain, classOf[DomainFacade])
    if localeStr != domain.getLocale then
      domain.setLocale(localeStr)
      invalidateDomainMessages()

  private def defaultCompositor: VersionedMessageMapCompositor =
    InternationalizationUtils.defaultMessages(TimeZone.getDefault)

  // TODO: Refactor the MessageMap API to compose maps rather than concatenate them, and break it away from
  // the Java Map API so it can be more reasonable
  def domainCompositor(folder: LanguageParentFacade): VersionedMessageMapCompositor =
    new VersionedMessageMapCompositor(Option(folder.getGeneration).fold(0L)(_.longValue)) <| { messages =>
      val defaultTimezone = TimeZone.getTimeZone(
        Option(Current.getDomainDTO.timeZone)
          .filterNot(_.isEmpty)
          .getOrElse("US/Eastern")
      )
      // add the stock messages... this means you cannot turn off the stock languages, for now...
      defaultCompositor.getAvailableMessages.values.asScala foreach { mm =>
        messages.addMessageMap(new BaseMessageMap(mm.getLocale, defaultTimezone, mm))
      }
      // apply all installed languages in order of the language name
      folder.getLanguages
        .filterNot(_.isDisabled)
        .sortWith(_.getName.toLowerCase < _.getName.toLowerCase) foreach { language =>
        val locale =
          Locale.of(language.getLanguage, language.getCountry.getOrElse(""))
        Option(messages.getAvailableMessages.get(locale)).fold(
          messages.addMessageMap(new BaseMessageMap(locale, defaultTimezone, language.getMessages.asJava))
        ) { mm =>
          mm.putAll(language.getMessages.asJava)
        }
      }
      if !sm.isProdLike then
        Option(messages.getAvailableMessages.get(Locale.ENGLISH)) foreach { en =>
          en.asScala foreach { tuple =>
            Encheferize.translate(
              tuple._1,
              tuple._2,
              (l, v) => messages.getOrCreateMessageMap(l, defaultTimezone).put(tuple._1, v)
            )
          }
        }
      end if
      messages.setDefaultLocale(
        ClassUtils.parseLocale(Option(Current.getDomainDTO.locale).filterNot(_.isEmpty).getOrElse("en"))
      )
    }

  override def invalidateDomainMessages(): Unit =
    val folder = LanguageRoot.languageFolder
    folder.refresh(true)
    folder.setGeneration(1L + Option(folder.getGeneration).fold(0L)(_.longValue))
    folder.invalidate()
end LanguageServiceImpl
