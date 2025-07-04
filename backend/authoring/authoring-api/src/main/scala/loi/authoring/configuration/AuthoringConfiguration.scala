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

import cats.syntax.option.*
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import loi.cp.config.JsonSchema.*

/** The properties commented out are set in the component configuration JSON. The configuration UI cannot handle
  * non-primitive types yet
  */
@JsonIgnoreProperties(ignoreUnknown = true)
case class AuthoringConfiguration(
  maxFileUploadSizeMegabytes: Long,
  iframelyApiKey: String,
  injectIframeResizer: Boolean,
  presenceEnabled: Boolean,
  chatEnabled: Boolean,
  synchronousIndexing: Boolean,     // synchronous indexing is only intended for use during testing
  renderCacheKey: String,           // a key to invalidate the render cache
  cdnMapping: String,               // example.org,cdn.example.org for doing CDN mapping of html assets
  eBookSupportEnabled: Boolean,     // enable the ebook json parsing widget in html editor
  injectContentFeedback: Boolean,   // inject the content feedback script
  adobeStockApiKey: Option[String], // Adobe stock image API key
  injectDataIds: Boolean,
  realTime: Boolean,
  semiRealTime: Boolean,
)

object AuthoringConfiguration:
  final val JsonSchema = Schema(
    title = "Authoring".some,
    properties = List(
      NumberField("maxFileUploadSizeMegabytes"),
      StringField("iframelyApiKey", "Iframely API Key".some),
      BooleanField("injectIframeResizer"),
      BooleanField("presenceEnabled", "Presence".some),
      BooleanField("chatEnabled", "Chat".some),
      BooleanField("synchronousIndexing"),
      StringField("renderCacheKey"),
      StringField("cdnMapping", "CDN Mapping".some, description = Some("e.g. assets.example.org,cdn.example.org")),
      BooleanField("eBookSupportEnabled", "Ebook Support".some),
      BooleanField("injectContentFeedback"),
      StringField("adobeStockApiKey", "Adobe Stock API Key".some),
      BooleanField("injectDataIds"),
      BooleanField("realTime"),
      BooleanField("semiRealTime"),
    )
  )

  final val CdnMappingRE  = "([^,;]*),([^,;]*)".r
  final val CdnMappingsRE = s"^${CdnMappingRE.pattern}(;${CdnMappingRE.pattern})*$$".r
end AuthoringConfiguration
