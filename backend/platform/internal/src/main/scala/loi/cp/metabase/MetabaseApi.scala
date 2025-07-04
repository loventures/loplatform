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

package loi.cp.metabase

import com.fasterxml.jackson.databind.JsonNode
import loi.cp.config.ConfigurationService
import loi.cp.admin.right.ReportingAdminRight
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.cpxp.component.annotation.{Component, Controller, PathVariable, RequestBody, RequestMapping}
import com.learningobjects.de.authorization.Secured

import java.time.Clock
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}

@Component
@Controller(value = "metabase", root = true)
@Secured(Array(classOf[ReportingAdminRight]))
class MetabaseApi(ci: ComponentInstance)(implicit
  cs: ConfigurationService
) extends BaseComponent(ci)
    with ApiRootComponent:
  implicit val clock: Clock = Clock.systemUTC

  @RequestMapping(path = "metabase/{embedType}/{id}", method = Method.POST)
  def getEmbedUrl(
    @PathVariable("embedType") embedType: String,
    @PathVariable("id") id: Long,
    @RequestBody params: JsonNode
  ): String =
    val claim = JwtClaim({
      raw"""{"resource":{"$embedType": $id}, "params": $params}"""
    }).issuedNow.expiresIn(10 * 60)

    val token: String = Jwt.encode(
      claim,
      MetabaseSettings.Key.getDomain.secretKey,
      JwtAlgorithm.HS256
    )

    return s"${MetabaseSettings.Key.getDomain.siteUrl}/embed/$embedType/$token#theme=transparent&bordered=false&titled=false"
  end getEmbedUrl
end MetabaseApi
