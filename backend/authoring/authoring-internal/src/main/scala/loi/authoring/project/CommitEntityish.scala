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

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.user.UserFinder

import java.time.LocalDateTime

/** unified assetcommit and authoringprojectcommit entity type */
// kill after migration
// another thing that, if done earlier, could have saved code changes
trait CommitEntityish:

  def id: java.lang.Long

  def created: LocalDateTime

  def createdById: java.lang.Long

  def createdByFfs(is: ItemService): UserFinder

  def parent: CommitEntityish

  def ops: JsonNode
end CommitEntityish
