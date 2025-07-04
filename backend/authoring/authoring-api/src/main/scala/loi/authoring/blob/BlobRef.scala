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

package loi.authoring.blob

import argonaut.CodecJson
import com.learningobjects.de.web.MediaType
import scaloi.json.ArgoExtras

// one would like to call this BlobInfo instead, but that is already taken
/** Information about a jclouds blob (aka S3 object)
  *
  * @param provider
  *   the name of the provider that can access the blob
  * @param name
  *   jclouds blob name (aka s3 object key)
  * @param size
  *   number of bytes
  */
// one of the ways that the provider is important is for prod clones. We don't clone the
// prod s3-bucket, we give the lower environment a read-only key for prod's bucket. Thus
// some blob refs in an environment are not accessed by that environment's default
// provider.
case class BlobRef(
  provider: String,
  name: String,
  filename: String,
  contentType: MediaType,
  size: Long
)

object BlobRef:
  given CodecJson[MediaType] = CodecJson.derived[String].xmap(MediaType.parseMediaType)(_.toString)

  given CodecJson[BlobRef] = CodecJson.casecodec5(BlobRef.apply, ArgoExtras.unapply)(
    "provider",
    "name",
    "filename",
    "contentType",
    "size",
  )
