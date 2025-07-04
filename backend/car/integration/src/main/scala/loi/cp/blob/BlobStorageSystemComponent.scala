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

package loi.cp.blob

import com.fasterxml.jackson.annotation.JsonIgnore
import com.learningobjects.cpxp.controller.upload.UploadInfo
import loi.cp.integration.SystemComponent

import java.io.{IOException, InputStream, OutputStream}
import scala.util.Either

trait BlobStorageSystemComponent[C <: BlobStorageSystemComponent[C]] extends SystemComponent[C]:

  /** Tries to return an UploadInfo if it is possible, otherwise None */
  @JsonIgnore
  def writeTo[R](blobName: String, uiTransform: (UploadInfo, R) => R)(cont: OutputStream => R): Either[IOException, R]

  @JsonIgnore
  def readFrom[T](blobName: String)(cont: InputStream => T): Either[IOException, T]
