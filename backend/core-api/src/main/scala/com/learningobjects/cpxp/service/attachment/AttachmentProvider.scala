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

package com.learningobjects.cpxp.service.attachment

import java.io.File

import software.amazon.awssdk.services.s3.S3Client
import com.google.common.io.ByteSource
import com.learningobjects.cpxp.service.blob.BlobPutLocation
import com.learningobjects.cpxp.util.BlobInfo
import com.learningobjects.de.web.MediaType
import org.jclouds.blobstore.BlobStore
import org.jclouds.blobstore.domain.Blob

import scala.util.Try

trait AttachmentProvider:

  def name: String

  def providerType: String

  def blobStore: BlobStore

  def getBlob(path: String): Blob

  def blobExists(path: String): Boolean

  def putBlob(path: String, source: ByteSource): Unit

  def putBlob(path: String, file: File): Unit

  def container: String

  def getNameFor(domain: String, digest: String): String

  def getDirectUrl(
    blob: BlobInfo,
    method: String,
    disposition: String,
    fileName: String,
    mimeType: String,
    expires: Long,
  ): Try[String]

  def isS3: Boolean

  def identity: String

  def credential: String

  def s3Client: S3Client

  def buildPutUrl(
    path: String,
    contentType: MediaType,
    contentLength: Long,
    expires: Long
  ): BlobPutLocation
end AttachmentProvider
