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

package com.learningobjects.cpxp.service.domain

import java.io.Serializable
import java.lang.{Boolean as JBoolean, Long as JLong}
import java.time.{DateTimeException, ZoneId}
import java.util.Date

import com.learningobjects.cpxp.IdType
import com.learningobjects.cpxp.service.attachment.ResourceDTO

import scala.beans.BeanProperty

/** A data transfer object for domains.
  */
case class DomainDTO(
  id: Long,
  @BeanProperty domainId: String,
  @BeanProperty name: String,
  @BeanProperty shortName: String,
  @BeanProperty hostName: String,
  @BeanProperty `type`: String,
  @BeanProperty locale: String,
  @BeanProperty timeZone: String,
  @BeanProperty loginRequired: JBoolean,
  @BeanProperty securityLevel: SecurityLevel,
  @BeanProperty rememberTimeout: JLong,
  @BeanProperty membershipLimit: JLong,
  @BeanProperty enrollmentLimit: JLong,
  @BeanProperty startDate: Date,
  @BeanProperty endDate: Date,
  @BeanProperty maximumFileSize: JLong,
  @BeanProperty state: DomainState,
  @BeanProperty message: String,
  @BeanProperty userUrlFormat: String,
  @BeanProperty groupUrlFormat: String,
  @BeanProperty licenseRequired: JBoolean,
  @BeanProperty googleAnalyticsAccount: String,
  @BeanProperty favicon: ResourceDTO,
  @BeanProperty image: ResourceDTO,
  @BeanProperty logo: ResourceDTO,
  @BeanProperty logo2: ResourceDTO,
  @BeanProperty css: ResourceDTO,
) extends IdType
    with Serializable:

  override def getId: JLong = Long box id

  override def getItemType: String = "Domain"
  // TODO: KILLME: The following are used by lohtml... Fix lof/admin/ov̈erlorde then remove 'em.
  // logo/favicon/css/googleAnalyticsAccount

  def timeZoneId: ZoneId =
    try ZoneId.of(timeZone)
    catch case _: DateTimeException => ZoneId.systemDefault()
end DomainDTO

object DomainDTO:
  def apply(d: DomainFacade): DomainDTO = new DomainDTO(
    id = d.getId,
    domainId = d.getDomainId,
    name = d.getName,
    shortName = d.getShortName,
    hostName = d.getPrimaryHostName,
    `type` = d.getType,
    locale = d.getLocale,
    timeZone = d.getTimeZone,
    loginRequired = d.getLoginRequired,
    securityLevel = d.getSecurityLevel,
    rememberTimeout = d.getRememberTimeout,
    membershipLimit = d.getMembershipLimit,
    enrollmentLimit = d.getEnrollmentLimit,
    startDate = d.getStartDate,
    endDate = d.getEndDate,
    maximumFileSize = d.getMaximumFileSize,
    state = d.getState,
    message = d.getMessage,
    userUrlFormat = d.getUserUrlFormat,
    groupUrlFormat = d.getGroupUrlFormat,
    licenseRequired = d.getLicenseRequired,
    googleAnalyticsAccount = d.getGoogleAnalyticsAccount,
    Option(d.getFavicon).map(ResourceDTO.apply).orNull,
    Option(d.getImage).map(ResourceDTO.apply).orNull,
    Option(d.getLogo).map(ResourceDTO.apply).orNull,
    Option(d.getLogo2).map(ResourceDTO.apply).orNull,
    Option(d.getCss).map(ResourceDTO.apply).orNull
  )

  import language.implicitConversions
  implicit def unprovide(prov: () => DomainDTO): DomainDTO                  = prov()
  implicit def unprovideImplicit(implicit prov: () => DomainDTO): DomainDTO = prov()
end DomainDTO
