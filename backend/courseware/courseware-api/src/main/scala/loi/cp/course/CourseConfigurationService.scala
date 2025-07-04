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

package loi.cp.course

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.config.ConfigurationKey
import loi.cp.config.ConfigurationService.{ConfigDetail, SetResult}
import loi.cp.context.ContextId

/** Specialization of ConfigurationService where the chain of configuration for CourseComponent items includes offerings
  * and projects:
  *
  *   - preview section: Overlord + Domain + Project + Section
  *   - test section: Overlord + Domain + Section (same chain of config as ConfigurationService)
  *     - publish actions set Project on Section (total overwrite)
  *   - course section: Overlord + Domain + Offering + Section
  *     - publish actions set Project on Offering (total overwrite)
  */
@Service
trait CourseConfigurationService:

  /** Gets the merged `key` config for `group`
    */
  final def getGroupConfig[A](key: ConfigurationKey[A], group: CourseComponent): A = getGroupDetail(key, group).value

  final def getGroupConfig[A](key: ConfigurationKey[A], group: CourseSection): A = getGroupDetail(key, group).value

  final def getGroupConfig[A](key: ConfigurationKey[A], group: ContextId): A = getGroupDetail(key, group).value

  def getGroupDetail[A](key: ConfigurationKey[A], group: CourseComponent): ConfigDetail[A]

  def getGroupDetail[A](key: ConfigurationKey[A], group: CourseSection): ConfigDetail[A]

  def getGroupDetail[A](key: ConfigurationKey[A], group: ContextId): ConfigDetail[A]

  def getProjectDetail[A](key: ConfigurationKey[A], projectId: Long): ConfigDetail[A]

  /** Sets `config` for `key` on the project. If None, then the `key` is removed from the project (parent `key` remains)
    */
  def setProjectConfig[A](key: ConfigurationKey[A], projectId: Long, config: Option[JsonNode]): SetResult[A]

  /** Sets `config` for `key` on the group. If None then the `key` is removed from the project (parent `key` remains)
    */
  def setGroupConfig[A](key: ConfigurationKey[A], group: CourseComponent, config: Option[JsonNode]): SetResult[A]

  /** Set group's config at `key` to group + patch
    */
  def patchGroupConfig[A](key: ConfigurationKey[A], group: CourseComponent, patch: JsonNode): SetResult[A]

  /** Set group's config at `key` to projectId's config at `key`
    */
  def copyConfig[A](key: ConfigurationKey[A], srcProjectId: Long, tgtGroup: CourseComponent): SetResult[A]

  /** Set group's config at `key` to projectId-config + group
    */
  final def copyConfigOrThrow[A](key: ConfigurationKey[A], srcProjectId: Long, tgtGroup: CourseComponent): A =
    copyConfig(key, srcProjectId, tgtGroup).valueOr(e =>
      throw new RuntimeException(
        s"failed to set ${tgtGroup.id} config from $srcProjectId config: ${e.concreteStreamableCharSequenceForJvmClients}"
      )
    )
end CourseConfigurationService
