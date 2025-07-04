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

import java.io.Serializable
import java.lang as jl

import argonaut.Argonaut.*
import argonaut.*
import com.fasterxml.jackson.annotation.JsonIgnore
import com.learningobjects.cpxp.IdType
import com.learningobjects.cpxp.scala.json.JEnumCodec.*
import com.learningobjects.cpxp.service.attachment.ResourceDTO

import scala.beans.BeanProperty

/** User DTO.
  */
final case class UserDTO(
  id: Long,
  @BeanProperty url: String,
  @BeanProperty userName: String,
  @BeanProperty givenName: String,
  @BeanProperty middleName: String,
  @BeanProperty familyName: String,
  @BeanProperty emailAddress: String,
  @BeanProperty externalId: Option[String],
  @BeanProperty disabled: jl.Boolean,
  @BeanProperty userType: UserType,
  @BeanProperty image: ResourceDTO,
  @BeanProperty subtenantId: Option[jl.Long]
) extends IdType
    with Serializable:

  override def getId: jl.Long      = Long box id
  override def getItemType: String = "User"

  @JsonIgnore def isAnonymous: Boolean = userType == UserType.Anonymous
end UserDTO

object UserDTO:
  def apply(user: UserFacade): UserDTO =
    new UserDTO(
      id = user.getId,
      url = user.getUrl,
      userName = user.getUserName,
      givenName = user.getGivenName,
      middleName = user.getMiddleName,
      familyName = user.getFamilyName,
      emailAddress = user.getEmailAddress,
      externalId = Option(user.getUserExternalId.orElse(null)),
      disabled = user.getDisabled,
      userType = user.getUserType,
      image = Option(user.getImage).map(ResourceDTO.apply).orNull,
      subtenantId = Option(user.getSubtenant.orElse(null)).map(_.getId.longValue)
    )

  import language.implicitConversions

  extension (self: UserDTO) implicit def userId: UserId = UserId(self.id)

  implicit def unprovide(prov: () => UserDTO): UserDTO                  = prov()
  implicit def unprovideImplicit(implicit prov: () => UserDTO): UserDTO = prov()

  implicit final val encodeJsonForUserDTO: EncodeJson[UserDTO] = EncodeJson(u =>
    Json(
      "id"          := u.id,
      ("url", if u.url == null then jNull else jString(u.url)),
      ("userName", if u.userName == null then jNull else jString(u.userName)),
      ("givenName", if u.givenName == null then jNull else jString(u.givenName)),
      ("middleName", if u.middleName == null then jNull else jString(u.middleName)),
      ("familyName", if u.familyName == null then jNull else jString(u.familyName)),
      ("emailAddress", if u.emailAddress == null then jNull else jString(u.emailAddress)),
      "externalId"  := u.externalId,
      ("disabled", if u.disabled == null then jNull else jBool(u.disabled)),
      "userType"    := u.userType,
      ("image", if u.image == null then jNull else u.image.asJson),
      "subtenantId" := u.subtenantId
    )
  )
end UserDTO
