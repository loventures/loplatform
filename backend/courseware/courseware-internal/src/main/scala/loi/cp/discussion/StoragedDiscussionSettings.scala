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

package loi.cp.discussion
import loi.cp.reference.EdgePath
import loi.cp.storage.CourseStoreable

final case class DiscussionSettings(discussionBoardSettings: Map[EdgePath, DiscussionSetting])
final case class DiscussionSetting(closed: Boolean)

final case class StoragedDiscussionSettings(settings: DiscussionSettings)

object StoragedDiscussionSettings:
  import argonaut.*

  final val empty: DiscussionSettings = DiscussionSettings(Map.empty)

  implicit val discussionSettingCodec: CodecJson[DiscussionSetting] =
    CodecJson.derive[DiscussionSetting]

  implicit val discussionSettingsCodec: CodecJson[DiscussionSettings] =
    CodecJson.derive[DiscussionSettings]

  implicit val storagedDiscussionSettingsCodec: CodecJson[StoragedDiscussionSettings] =
    discussionSettingsCodec.xmap(StoragedDiscussionSettings(_))(_.settings)

  implicit val storageable: CourseStoreable[StoragedDiscussionSettings] =
    CourseStoreable("discussionSettings")(StoragedDiscussionSettings(empty))
end StoragedDiscussionSettings

object DiscussionSetting:

  val default: DiscussionSetting = DiscussionSetting(false)
