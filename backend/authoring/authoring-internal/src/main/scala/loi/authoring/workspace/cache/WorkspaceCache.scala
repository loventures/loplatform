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

package loi.authoring.workspace
package cache

import com.learningobjects.cpxp.component.annotation.Service

/** I assume this will ultimately be further genercized i.e. RedisLoadingCache[T], but we'll cross that bridge when we
  * get there
  */
@Service
trait WorkspaceCache:

  def getOrLoad(commitId: Long, load: () => LocalWorkspaceData): LocalWorkspaceData

  def get(commitId: Long): Option[LocalWorkspaceData]

  def put(commitId: Long, workspace: LocalWorkspaceData): Boolean
