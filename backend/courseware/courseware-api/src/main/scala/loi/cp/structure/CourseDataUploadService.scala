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

import software.amazon.awssdk.services.s3.S3Client
import com.learningobjects.cpxp.component.annotation.Service

import java.util.Date
import scala.util.Try

@Service
trait CourseDataUploadService:
  def getActiveUploadConfigs: List[CourseDataUploadConfig]

  def buildS3Client(config: CourseDataUploadConfig): S3Client

  def uploadDataToS3(data: String, courseId: String, fileName: String, timeStamp: Date)(
    s3Client: S3Client,
    config: CourseDataUploadConfig
  ): Try[Unit]
end CourseDataUploadService

final case class CourseDataUploadConfig(
  name: String,
  region: String,
  bucket: String,
  prefix: Option[String],
  useInstanceProfile: Boolean,
  useAssumeRole: Boolean,
  assumeRoleArn: String,
  accessKeyId: String,
  secretAccessKey: String,
  customEndpointUrl: String
)
