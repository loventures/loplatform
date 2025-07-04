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

package com.learningobjects.cpxp.service.user

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import com.learningobjects.cpxp.postgresql.{JsonNodeUserType, TSVectorUserType}
import com.learningobjects.cpxp.service.attachment.{AttachmentFinder, ResourceDTO}
import com.learningobjects.cpxp.service.data.DataFormat
import com.learningobjects.cpxp.service.subtenant.SubtenantFinder
import jakarta.persistence.*
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{ColumnTransformer, JdbcType, Type, Cache as HCache}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import java.util.Date
import java.lang as jl

@Entity
@HCache(usage = READ_WRITE)
class UserFinder extends PeerEntity:
  import UserFinder.*

  @Column(columnDefinition = "JSONB")
  @DataType(DATA_TYPE_CONFIGURATION_BLOB)
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var configuration: JsonNode = scala.compiletime.uninitialized

  @Column
  @FunctionalIndex(byParent = true, nonDeleted = true, function = IndexType.LCASELIKE)
  var emailAddress: String = scala.compiletime.uninitialized

  @Column
  @FunctionalIndex(byParent = true, nonDeleted = true, function = IndexType.LCASELIKE)
  var externalId: String = scala.compiletime.uninitialized

  @Column
  var familyName: String = scala.compiletime.uninitialized

  @Column(columnDefinition = "TSVECTOR")
  @FinderDataDef(DataFormat.tsvector)
  @ColumnTransformer(write = "to_tsvector('simple',LOWER(?))")
  @Type(classOf[TSVectorUserType])
  @FunctionalIndex(byParent = false, nonDeleted = false, function = IndexType.GIN)
  var fullName: String = scala.compiletime.uninitialized

  @Column
  var givenName: String = scala.compiletime.uninitialized

  @Column
  var licenseAccepted: jl.Boolean = scala.compiletime.uninitialized

  @Column
  var middleName: String = scala.compiletime.uninitialized

  @Column
  var password: String = scala.compiletime.uninitialized

  @Column
  var rssPassword: String = scala.compiletime.uninitialized

  @Column
  @FunctionalIndex(byParent = true, nonDeleted = true, function = IndexType.LCASE)
  var rssUsername: String = scala.compiletime.uninitialized

  @Column
  var state: String = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  @FunctionalIndex(byParent = true, nonDeleted = true, function = IndexType.NORMAL)
  var subtenant: SubtenantFinder = scala.compiletime.uninitialized

  @Column
  var title: String = scala.compiletime.uninitialized

  @Column
  @FriendlyName
  @FunctionalIndex(byParent = true, nonDeleted = true, function = IndexType.LCASELIKE)
  var userName: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_CREATE_TIME)
  var createTime: Date = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_DISABLED)
  var disabled: jl.Boolean = scala.compiletime.uninitialized

  @DataType(DATA_TYPE_IMAGE)
  @ManyToOne(fetch = FetchType.LAZY)
  var image: AttachmentFinder = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_IN_DIRECTORY)
  var inDirectory: jl.Boolean = scala.compiletime.uninitialized

  @DataType(DATA_TYPE_LOGO)
  @ManyToOne(fetch = FetchType.LAZY)
  var logo: AttachmentFinder = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_URL)
  var url: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_USER_TYPE)
  var utype: String = scala.compiletime.uninitialized
end UserFinder

object UserFinder:
  final val ITEM_TYPE_USER               = "User"
  final val DATA_TYPE_MIDDLE_NAME        = "User.middleName"
  final val DATA_TYPE_LICENSE_ACCEPTED   = "User.licenseAccepted"
  final val DATA_TYPE_RSS_USERNAME       = "User.rssUsername"
  final val DATA_TYPE_URL                = "url"
  final val DATA_TYPE_EXTERNAL_ID        = "User.externalId"
  final val DATA_TYPE_IMAGE              = "image"
  final val DATA_TYPE_CREATE_TIME        = "createTime"
  final val DATA_TYPE_GIVEN_NAME         = "User.givenName"
  final val DATA_TYPE_USER_SUBTENANT     = "User.subtenant"
  final val DATA_TYPE_FULL_NAME          = "User.fullName"
  final val DATA_TYPE_USER_TITLE         = "User.title"
  final val DATA_TYPE_CONFIGURATION_BLOB = "Configuration.configuration"
  final val DATA_TYPE_RSS_PASSWORD       = "User.rssPassword"
  final val DATA_TYPE_USER_TYPE          = "User.type"
  final val DATA_TYPE_USER_NAME          = "User.userName"
  final val DATA_TYPE_LOGO               = "logo"
  final val DATA_TYPE_FAMILY_NAME        = "User.familyName"
  final val DATA_TYPE_PASSWORD           = "User.password"
  final val DATA_TYPE_EMAIL_ADDRESS      = "User.emailAddress"
  final val DATA_TYPE_DISABLED           = "disabled"
  final val DATA_TYPE_USER_STATE         = "User.state"
  final val DATA_TYPE_IN_DIRECTORY       = "inDirectory"

  implicit class toDtoOps(entity: UserFinder):
    def loadDto: UserDTO =
      // This does loads of the image, that's kinda bad
      UserDTO(
        entity.getId,
        entity.url,
        entity.userName,
        entity.givenName,
        entity.middleName,
        entity.familyName,
        entity.emailAddress,
        Option(entity.externalId),
        entity.disabled,
        UserType.valueOf(entity.utype),
        Option(entity.image).map(img => ResourceDTO(img.url)).orNull,
        Option(entity.subtenant).map(_.getId)
      )

    // forsake (unused) image accuracy for performance
    def loadDtoNoInit: UserDTO = UserDTO(
      entity.getId,
      entity.url,
      entity.userName,
      entity.givenName,
      entity.middleName,
      entity.familyName,
      entity.emailAddress,
      Option(entity.externalId),
      entity.disabled,
      UserType.valueOf(entity.utype),
      null, // inaccurate
      Option(entity.subtenant).map(_.getId)
    )
  end toDtoOps
end UserFinder
