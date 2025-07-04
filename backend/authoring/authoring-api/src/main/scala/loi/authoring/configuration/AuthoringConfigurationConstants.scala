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

package loi.authoring.configuration

import com.learningobjects.de.authorization.Secured
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.cp.admin.right.{AdminRight, ProjectAdminRight}
import loi.cp.config.{ConfigurationKey, ConfigurationKeyBinding}
import loi.cp.i18n.Translatable
import loi.cp.i18n.Translatable.RawStrings.*
import scalaz.\/
import scalaz.syntax.std.boolean.*

object AuthoringConfigurationConstants:

  @ConfigurationKeyBinding(
    value = "authoring",
    read = new Secured(value = Array(classOf[AccessAuthoringAppRight], classOf[ProjectAdminRight])),
    write = new Secured(Array(classOf[AdminRight]))
  )
  object domainAuthoringConfig extends ConfigurationKey[AuthoringConfiguration]:
    override val init: AuthoringConfiguration = defaultAuthoringConfigs.defaultAuthoringConfiguration
    override val schema                       = AuthoringConfiguration.JsonSchema

    override def validate(ac: AuthoringConfiguration): Translatable.Any \/ Unit =
      validateCdnMapping(ac.cdnMapping)

    private final def validateCdnMapping(cdnMapping: String) =
      (cdnMapping.isEmpty || AuthoringConfiguration.CdnMappingsRE.matches(cdnMapping)) either (()) or Translatable.Any(
        s"Invalid CDN mapping: $cdnMapping"
      )
  end domainAuthoringConfig
end AuthoringConfigurationConstants

object defaultAuthoringConfigs:

  /** The properties commented out are set in the component configuration JSON. The configuration UI cannot handle
    * non-primitive types yet
    */
  final val defaultAuthoringConfiguration = AuthoringConfiguration(
    maxFileUploadSizeMegabytes = 50L,
    iframelyApiKey = "",
    injectIframeResizer = true,
    presenceEnabled = true,
    chatEnabled = true,
    synchronousIndexing = false,
    renderCacheKey = "",
    cdnMapping = "",
    eBookSupportEnabled = false,
    injectContentFeedback = true,
    adobeStockApiKey = None,
    injectDataIds = true,
    realTime = true,
    semiRealTime = false,
  )
end defaultAuthoringConfigs
