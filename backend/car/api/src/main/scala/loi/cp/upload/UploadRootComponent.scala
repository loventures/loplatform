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

package loi.cp.upload

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method, WebResponse}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.AdminRight

import java.lang as jl

/** This controller is used for staging uploads that will be consumed by subsequent API calls. Uploads will be destroyed
  * once consumed, explicitly deleted or your session expires.
  */
@Controller(value = "uploads", root = true)
@RequestMapping(path = "uploads")
trait UploadRootComponent extends ApiRootComponent:

  /** Upload a new file. The body (if singlepart) or first body part (if multipart) will be stored in the session upload
    * area. Metadata about the upload are returned.
    */
  @RequestMapping(method = Method.POST)
  def upload(@RequestBody upload: UploadInfo): Upload

  /** Fetches a file from a remote URL and stages it as if an upload. Metadata about the upload are returned.
    */
  @RequestMapping(path = "fetch", method = Method.POST)
  @Secured(Array(classOf[AdminRight]))
  def fetch(@QueryParam("url") url: String): Upload

  /** Download an uploaded file.
    * @param guid
    *   the upload guid
    * @return
    *   the downloaded attachment
    */
  @RequestMapping(path = "{guid}", method = Method.GET, csrf = false)
  def download(@PathVariable("guid") guid: String, @QueryParam(required = false) download: jl.Boolean): WebResponse

  /** Delete an uploaded file
    * @param guid
    *   the upload guid
    */
  @RequestMapping(path = "{guid}", method = Method.DELETE)
  def delete(@PathVariable("guid") guid: String): Unit
end UploadRootComponent

/** Metadata about an upload.
  * @param guid
  *   the upload guid
  * @param fileName
  *   the filename
  * @param size
  *   the file size
  * @param mimeType
  *   the MIME type of the uploda
  * @param width
  *   the width of the upload, if an image
  * @param height
  *   the height of the upload, if an image
  */
case class Upload(guid: String, fileName: String, size: Long, mimeType: String, width: Option[Int], height: Option[Int])
