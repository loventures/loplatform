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

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import com.learningobjects.cpxp.postgresql.JsonNodeUserType
import com.learningobjects.cpxp.service.attachment.AttachmentFinder
import com.learningobjects.cpxp.service.item.Item
import jakarta.persistence.*
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{JdbcType, Type, Cache as HCache}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import java.lang as jl
import java.util.Date

@Entity
@HCache(usage = READ_WRITE)
class DomainFinder extends PeerEntity:
  import DomainFinder.*

  @Column(columnDefinition = "JSONB")
  @DataType(DATA_TYPE_CONFIGURATION_BLOB)
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var configuration: JsonNode = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var css: Item = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var cssFile: Item = scala.compiletime.uninitialized

  @Column
  var customColors: String = scala.compiletime.uninitialized

  @Column
  var endDate: Date = scala.compiletime.uninitialized

  @Column
  var enrollmentLimit: jl.Long = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var favicon: Item = scala.compiletime.uninitialized

  @Column
  var googleAnalyticsAccount: String = scala.compiletime.uninitialized

  @Column
  var supportEmail: String = scala.compiletime.uninitialized

  @Column
  var groupLimit: jl.Long = scala.compiletime.uninitialized

  @Column
  var groupUrlFormat: String = scala.compiletime.uninitialized

  @Column
  var guestPolicy: String = scala.compiletime.uninitialized

  @Column
  var hostName: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_DOMAIN_ID)
  var domainId: String = scala.compiletime.uninitialized

  @Column
  var licenseRequired: jl.Boolean = scala.compiletime.uninitialized

  @Column
  var locale: String = scala.compiletime.uninitialized

  @Column
  var loginRequired: jl.Boolean = scala.compiletime.uninitialized

  @Column
  var maximumFileSize: jl.Long = scala.compiletime.uninitialized

  @Column
  var membershipLimit: jl.Long = scala.compiletime.uninitialized

  @Column
  var message: String = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var privacyPolicyHtml: AttachmentFinder = scala.compiletime.uninitialized

  @Column
  var rememberTimeout: jl.Long = scala.compiletime.uninitialized

  @Column
  var securityLevel: String = scala.compiletime.uninitialized

  @Column
  var sessionLimit: jl.Long = scala.compiletime.uninitialized

  @Column
  var sessionTimeout: jl.Long = scala.compiletime.uninitialized

  @Column
  var shortName: String = scala.compiletime.uninitialized

  @Column
  var startDate: Date = scala.compiletime.uninitialized

  @Column
  var state: String = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var termsOfUseHtml: AttachmentFinder = scala.compiletime.uninitialized

  @Column
  var timeZone: String = scala.compiletime.uninitialized

  @Column
  var userLimit: jl.Long = scala.compiletime.uninitialized

  @Column
  var userUrlFormat: String = scala.compiletime.uninitialized

  @DataType(DATA_TYPE_IMAGE)
  @ManyToOne(fetch = FetchType.LAZY)
  var image: AttachmentFinder = scala.compiletime.uninitialized

  @DataType(DATA_TYPE_LOGO)
  @ManyToOne(fetch = FetchType.LAZY)
  var logo: AttachmentFinder = scala.compiletime.uninitialized

  @DataType(DATA_TYPE_LOGO2)
  @ManyToOne(fetch = FetchType.LAZY)
  var logo2: AttachmentFinder = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_NAME)
  var name: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_DESCRIPTION)
  var description: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_URL)
  var url: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_TYPE)
  var xtype: String = scala.compiletime.uninitialized
end DomainFinder

object DomainFinder:
  final val ITEM_TYPE_DOMAIN                     = "Domain"
  final val DATA_TYPE_DOMAIN_SHORT_NAME          = "Domain.shortName"
  final val DATA_TYPE_PRIVACY_POLICY_HTML        = "Domain.privacyPolicyHtml"
  final val DATA_TYPE_NAME                       = "name"
  final val DATA_TYPE_DESCRIPTION                = "description"
  final val DATA_TYPE_FAVICON                    = "Domain.favicon"
  final val DATA_TYPE_USER_URL_FORMAT            = "Domain.userUrlFormat"
  final val DATA_TYPE_DOMAIN_ID                  = "Domain.id"
  final val DATA_TYPE_URL                        = "url"
  final val DATA_TYPE_IMAGE                      = "image"
  final val DATA_TYPE_GROUP_URL_FORMAT           = "Domain.groupUrlFormat"
  final val DATA_TYPE_DOMAIN_TIME_ZONE           = "Domain.timeZone"
  final val DATA_TYPE_GROUPS_LIMIT               = "Domain.groupLimit"
  final val DATA_TYPE_GUEST_POLICY               = "Domain.guestPolicy"
  final val DATA_TYPE_END_DATE                   = "Domain.endDate"
  final val DATA_TYPE_LOCALE                     = "Domain.locale"
  final val DATA_TYPE_SECURITY_LEVEL             = "Domain.securityLevel"
  final val DATA_TYPE_USERS_LIMIT                = "Domain.userLimit"
  final val DATA_TYPE_CONFIGURATION_BLOB         = "Configuration.configuration"
  final val DATA_TYPE_DOMAIN_CSS                 = "Domain.css"
  final val DATA_TYPE_START_DATE                 = "Domain.startDate"
  final val DATA_TYPE_SESSION_LIMIT              = "Domain.sessionLimit"
  final val DATA_TYPE_LOGO                       = "logo"
  final val DATA_TYPE_LOGO2                      = "logo2"
  final val DATA_TYPE_LICENSE_REQUIRED           = "Domain.licenseRequired"
  final val DATA_TYPE_SESSION_TIMEOUT            = "Domain.sessionTimeout"
  final val DATA_TYPE_GOOGLE_ANALYTICS_ACCOUNT   = "Domain.googleAnalyticsAccount"
  final val DATA_TYPE_GOOGLE_SUPPORT_EMAIL       = "Domain.supportEmail"
  final val DATA_TYPE_TERMS_OF_USE_HTML          = "Domain.termsOfUseHtml"
  final val DATA_TYPE_CSS_FILE                   = "Domain.cssFile"
  final val DATA_TYPE_DOMAIN_MESSAGE             = "Domain.message"
  final val DATA_TYPE_MEMBERSHIP_LIMIT           = "Domain.membershipLimit"
  final val DATA_TYPE_DOMAIN_THEME_CUSTOM_COLORS = "Domain.customColors"
  final val DATA_TYPE_DOMAIN_STATE               = "Domain.state"
  final val DATA_TYPE_DOMAIN_HOST_NAME           = "Domain.hostName"
  final val DATA_TYPE_TYPE                       = "type"
  final val DATA_TYPE_ENROLLMENTS_LIMIT          = "Domain.enrollmentLimit"
  final val DATA_TYPE_REMEMBER_TIMEOUT           = "Domain.rememberTimeout"
  final val DATA_TYPE_MAXIMUM_FILE_SIZE          = "Domain.maximumFileSize"
  final val DATA_TYPE_LOGIN_REQUIRED             = "Domain.loginRequired"
end DomainFinder
