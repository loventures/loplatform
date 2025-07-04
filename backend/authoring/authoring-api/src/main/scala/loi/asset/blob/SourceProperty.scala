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

package loi.asset.blob

import loi.asset.file.audio.model.Audio
import loi.asset.file.file.model.File
import loi.asset.file.fileBundle.model.FileBundle
import loi.asset.file.image.model.Image
import loi.asset.file.pdf.model.Pdf
import loi.asset.file.video.model.Video
import loi.asset.file.videoCaption.model.VideoCaption
import loi.asset.html.model.*
import loi.authoring.asset.Asset
import loi.authoring.blob.BlobRef

// a pseudo-typeclass for nodes that have a "source" property that represents a blob
object SourceProperty:

  def fromNode(node: Asset[?]): Option[BlobRef] = fromData(node.data)

  def fromData(data: Any): Option[BlobRef] =
    data match
      case data: Audio        => data.source
      case data: File         => data.source
      case data: FileBundle   => data.source
      case data: Html         => data.source
      case data: Scorm        => data.source
      case data: Image        => data.source
      case data: Javascript   => data.source
      case data: Pdf          => data.source
      case data: Stylesheet   => data.source
      case data: Video        => data.source
      case data: VideoCaption => data.source
      case _                  => None

  // *shudder* If you're ambitious, YOU look into scalaz prisms and lenses.
  def putSource[A](data: A, source: Option[BlobRef]): A =
    data match
      case data: Audio        => data.copy(source = source).asInstanceOf[A]
      case data: File         => data.copy(source = source).asInstanceOf[A]
      case data: FileBundle   => data.copy(source = source).asInstanceOf[A]
      case data: Html         => data.copy(source = source).asInstanceOf[A]
      case data: Scorm        => data.copy(source = source).asInstanceOf[A]
      case data: Image        => data.copy(source = source).asInstanceOf[A]
      case data: Javascript   => data.copy(source = source).asInstanceOf[A]
      case data: Pdf          => data.copy(source = source).asInstanceOf[A]
      case data: Stylesheet   => data.copy(source = source).asInstanceOf[A]
      case data: Video        => data.copy(source = source).asInstanceOf[A]
      case data: VideoCaption => data.copy(source = source).asInstanceOf[A]
      case _                  => data
end SourceProperty
