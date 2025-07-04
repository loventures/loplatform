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

package loi.cp.structure

import argonaut.Argonaut.*
import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.course.lightweight.Lwc
import scaloi.syntax.disjunction.*
import scalaz.syntax.traverse.*
import scalaz.std.list.*
import scaloi.misc.TimeSource
import scaloi.misc.TryInstances.*

@Service
trait CourseStructureUploadService:
  def pushOfferingToS3(offering: Lwc): Unit

@Service
class CourseStructureUploadServiceImpl(
  courseDataUploadService: CourseDataUploadService,
  courseStructureService: CourseStructureService,
  ts: => TimeSource
) extends CourseStructureUploadService:
  import CourseStructureUploadServiceImpl.*

  override def pushOfferingToS3(offering: Lwc): Unit =
    for
      structure <- courseStructureService.getOfferingStructure(offering.groupId).toTry(StructureException.apply)
      json       = structure.asJson.nospaces
      configs    = courseDataUploadService.getActiveUploadConfigs
      _         <- configs.traverse(config =>
                     val s3Client = courseDataUploadService.buildS3Client(config)
                     try
                       courseDataUploadService.uploadDataToS3(json, offering.groupId, Filename, ts.date)(s3Client, config)
                     finally s3Client.close()
                   )
    yield ()
end CourseStructureUploadServiceImpl

object CourseStructureUploadServiceImpl:
  private final val Filename = "Course.json"
