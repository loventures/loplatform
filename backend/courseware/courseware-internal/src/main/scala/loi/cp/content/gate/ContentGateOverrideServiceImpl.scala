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

package loi.cp.content
package gate

import argonaut.CodecJson
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.util.Misc.tryMonad
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.context.ContextId
import loi.cp.course.lightweight.{LightweightCourseService, Lwc}
import loi.cp.reference.EdgePath
import loi.cp.storage.{CourseStorageService, CourseStoreable}
import scalaz.syntax.functor.*
import scalaz.syntax.std.option.*

import scala.util.Try

@Service
class ContentGateOverrideServiceImpl(
  lwcs: LightweightCourseService,
  storageService: CourseStorageService,
) extends ContentGateOverrideService:
  import Accommodations.*
  import ContentGateOverrideServiceImpl.*

  def loadOverrides(course: Lwc): Try[GateOverrides] =
    Try(storageService.get[StoragedOverrides](course)).map(_.value)

  def updateOverrides(course: Lwc)(changes: GateOverrides.Changes): Try[Unit] =
    Try(storageService.modify[StoragedOverrides](course) { case StoragedOverrides(oldOverrides) =>
      val newOverrides = oldOverrides ~~ changes
      logger.info(s"changing gate overrides for $course from $oldOverrides to $newOverrides")
      // TODO: should we just do this in the storage service?
      lwcs.incrementGeneration(course)
      StoragedOverrides(newOverrides)
    }).void

  def loadAccommodations(course: ContextId, user: UserId): Try[Accommodations] =
    Try(storageService.get[StoragedAccommodations](course, user)).map(_.value)

  def updateAccommodations(
    course: ContextId,
    user: UserId,
    edgePath: EdgePath,
    accommodation: Option[Long]
  ): Try[Unit] =
    Try(storageService.modify[StoragedAccommodations](course, user) { case StoragedAccommodations(accommodations) =>
      StoragedAccommodations(
        accommodation.cata(
          minutes => accommodations.updated(edgePath, minutes),
          accommodations - edgePath
        )
      )
    }).void
end ContentGateOverrideServiceImpl

object ContentGateOverrideServiceImpl:
  private val logger = org.log4s.getLogger

final case class StoragedOverrides(value: GateOverrides)

object StoragedOverrides:
  implicit val codec: CodecJson[StoragedOverrides] =
    GateOverrides.codec.xmap(StoragedOverrides.apply)(_.value)

  implicit val storageable: CourseStoreable[StoragedOverrides] =
    CourseStoreable("gateOverrides")(StoragedOverrides(GateOverrides.empty))

final case class StoragedAccommodations(value: Accommodations.Accommodations)

object StoragedAccommodations:
  implicit val storagedCodec: CodecJson[StoragedAccommodations] =
    Accommodations.codec.xmap(StoragedAccommodations.apply)(_.value)

  implicit val accommodations: CourseStoreable[StoragedAccommodations] =
    CourseStoreable("accommodations")(StoragedAccommodations(Accommodations.empty))
