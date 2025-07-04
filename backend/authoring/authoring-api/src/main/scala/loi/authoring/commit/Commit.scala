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

package loi.authoring.commit

import argonaut.StringWrap.*
import argonaut.{EncodeJson, Json}
import scaloi.json.ArgoExtras.*

import java.util.{Date, UUID}

/** @param parentId
  *   the id of the parent commit. The first commit of the master branch is the only time the value is None
  */
case class Commit(
  id: Long,
  createTime: Date,
  createUser: Long,
  rootNodes: Map[UUID, Long],
  parentId: Option[Long],
  rootId: Long
)

object Commit:

  implicit final val encodeJsonForCommit: EncodeJson[Commit] = EncodeJson(c =>
    Json(
      "id"         := c.id,
      "createTime" := c.createTime,
      "createUser" := c.createUser,
      "rootNodes"  := c.rootNodes,
      "parentId"   := c.parentId,
      "rootId"     := c.rootId,
    )
  )
end Commit
