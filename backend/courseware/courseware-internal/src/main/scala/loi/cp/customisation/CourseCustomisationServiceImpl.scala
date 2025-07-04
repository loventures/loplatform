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

import argonaut.CodecJson
import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.course.lightweight.Lwc
import loi.cp.storage.{CourseStorageService, CourseStoreable}
import scalaz.Endo

@Service
class CourseCustomisationServiceImpl(
  storageService: CourseStorageService,
) extends CourseCustomisationService:
  import CourseCustomisationService.*
  import CourseCustomisationServiceImpl.*

  override def loadCustomisation(lwc: Lwc): Customisation =
    storageService.get[StoragedCustomisation](lwc).value

  override def updateCustomisation(lwc: Lwc, f: CustomisationChange): Customisation =
    storageService
      .modify[StoragedCustomisation](lwc)({ case StoragedCustomisation(customisation) =>
        StoragedCustomisation(f(customisation))
      })
      .value

  override def copyCustomisation(dst: Lwc, src: Lwc): Unit =
    val source = loadCustomisation(src)
    updateCustomisation(dst, Endo(_ => source))
end CourseCustomisationServiceImpl

object CourseCustomisationServiceImpl:
  final case class StoragedCustomisation(value: Customisation)

  implicit val codec: CodecJson[StoragedCustomisation]             =
    CodecJson.derived[Customisation].xmap(StoragedCustomisation.apply)(_.value)
  implicit val storageable: CourseStoreable[StoragedCustomisation] =
    CourseStoreable("customisation")(StoragedCustomisation(Customisation.empty))
