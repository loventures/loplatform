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

package loi.authoring.project

import argonaut.StringWrap.*
import argonaut.{EncodeJson, Json}
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import loi.cp.config.ConfigurationKey

import java.time.LocalDate
import java.util.UUID

trait Project:
  def id: Long
  def name: String
  def homeNodeName: UUID // kill; use `homeName`, a hill i choose to die on
  def rootNodeName: UUID // kill; use `rootName`, a hill i choose to die on
  def ownedBy: Long
  def contributedBy: Map[Long, Option[String]]
  def archived: Boolean
  def code: Option[String]
  def productType: Option[String]
  def category: Option[String]
  def subCategory: Option[String]
  def revision: Option[Int]
  def launchDate: Option[LocalDate]
  def liveVersion: Option[String]
  def s3: Option[String]
  def configuration: Option[JsonNode]
  def maintenance: Boolean

  @JsonIgnore
  def userIds: Set[Long]

  @JsonIgnore
  def homeName: UUID

  @JsonIgnore
  def rootName: UUID

  final def getRawConfigJson[A](key: ConfigurationKey[A]): JsonNode =
    val config = for
      config <- configuration
      json   <- Option(config.get(ConfigurationKey.configBinding(key.getClass).value()))
    yield json
    config getOrElse JsonNodeFactory.instance.objectNode()
end Project

object Project:

  implicit final val encodeJsonForProject: EncodeJson[Project] = EncodeJson(p =>
    Json(
      "id" := p.id,
//    "name":= p.name,
//    "archived" := p.archived,
//    "code" := p.code,
//    "productType" := p.productType,
//    "category" := p.category,
//    "subCategory" := p.subCategory,
//    "revision" := p.revision,
//    "launchDate" := p.launchDate,
//    "liveVersion" := p.liveVersion,
//    "s3" := p.s3

    )
  )
end Project
