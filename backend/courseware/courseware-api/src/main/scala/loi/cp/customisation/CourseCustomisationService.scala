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

package loi.cp.customisation

import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.course.lightweight.Lwc
import loi.cp.reference.EdgePath
import scalaz.Endo
import scalaz.std.list.*
import scalaz.std.set.*
import scalaz.syntax.foldable.*
import scaloi.syntax.OptionOps.*

@Service
trait CourseCustomisationService:
  import CourseCustomisationService.*

  def loadCustomisation(lwc: Lwc): Customisation

  def updateCustomisation(lwc: Lwc, f: CustomisationChange): Customisation

  def updateCustomisation(lwc: Lwc, path: EdgePath, f: OverlayChange): Customisation =
    updateCustomisation(lwc, CustomiseEdgePath(path, f))

  def copyCustomisation(dst: Lwc, src: Lwc): Unit
end CourseCustomisationService

object CourseCustomisationService:
  type CustomisationChange = Endo[Customisation]
  type OverlayChange       = Endo[ContentOverlay]

  def ResetCustomisation: CustomisationChange =
    Endo(_ => Customisation.empty)

  def BulkUpdateOverlay(updates: Map[EdgePath, ContentOverlayUpdate]): CustomisationChange =
    updates.toList.foldMap { case (path, update) =>
      CustomiseEdgePath(path, UpdateOverlay(update))
    }

  def CustomiseEdgePath(path: EdgePath, f: OverlayChange): CustomisationChange =
    Endo(customisation => customisation.withOverlay(path, f(customisation(path))))

  def ResetEdgePath(path: EdgePath): CustomisationChange =
    Endo(customisation => customisation.copy(overlays = customisation.overlays - path))

  def ResetContents: OverlayChange =
    Endo(overlay => overlay.copy(hide = None, order = None))

  def HideContent(name: EdgePath): OverlayChange =
    Endo(overlay => overlay.copy(hide = Some(overlay.hide.orZ + name)))

  def ShowContent(name: EdgePath): OverlayChange =
    Endo(overlay => overlay.copy(hide = Some(overlay.hide.orZ - name).filterNZ))

  def SetOrder(order: List[EdgePath]): OverlayChange =
    Endo(overlay => overlay.copy(order = Some(order)))

  def ResetOrder: OverlayChange =
    Endo(overlay => overlay.copy(order = None))

  def SetTitle(title: String): OverlayChange =
    Endo(overlay => overlay.copy(title = Some(title)))

  def ResetTitle: OverlayChange =
    Endo(overlay => overlay.copy(title = None))

  def UpdateOverlay(update: ContentOverlayUpdate): OverlayChange =
    Endo(overlay => overlay |: update)
end CourseCustomisationService
