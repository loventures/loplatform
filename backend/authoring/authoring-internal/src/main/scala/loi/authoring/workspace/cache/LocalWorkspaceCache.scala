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

import java.lang as jl

import com.learningobjects.cpxp.util.cache
import scalaz.syntax.std.option.*
import scaloi.syntax.AnyOps.*

import scala.concurrent.duration.*
import scala.ref.SoftReference

class LocalWorkspaceCache
    extends cache.BucketGenerationalCache[
      jl.Long,
      SoftReference[LocalWorkspaceData],
      LocalWorkspaceCache.Entry
    ](
      itemAware = false,
      replicated = false,
      timeout = 5.minutes,
    ):
  import LocalWorkspaceCache.*

  def getOrLoad(commit: Long, load: () => LocalWorkspaceData): LocalWorkspaceData =
    get(commit).flatMap(_.get) | load() <| { data =>
      put(commit, data)
    }

  def get_?(commit: Long): Option[LocalWorkspaceData] =
    get(commit).flatMap(_.get)

  def put(commit: Long, data: LocalWorkspaceData): Unit =
    put(new Entry(commit, SoftReference(data)))
end LocalWorkspaceCache

object LocalWorkspaceCache:
  final class Entry(commitId: Long, data: SoftReference[LocalWorkspaceData])
      extends cache.Entry[jl.Long, SoftReference[LocalWorkspaceData]](commitId, data)
