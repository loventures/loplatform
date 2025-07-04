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

package com.learningobjects.cpxp.util

import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.CurrentUrlService
import com.learningobjects.cpxp.service.domain.DomainDTO

@Service
class CurrentUrlServiceImpl(
  domain: () => DomainDTO,
  sm: () => ServiceMeta
) extends CurrentUrlService:
  override def getUrl(path: String): String =
    val prefix = if domain.securityLevel.getIsSecure then "https://" else "http://"
    val port   =
      if sm().isLocal && domain.securityLevel.getIsSecure then ":8181"
      else if sm().isLocal then ":8080"
      else ""
    s"$prefix${domain.hostName}$port$path"
end CurrentUrlServiceImpl
