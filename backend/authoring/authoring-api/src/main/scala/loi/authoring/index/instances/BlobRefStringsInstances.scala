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

package loi.authoring.index
package instances

import loi.authoring.blob.{BlobRef, BlobService}

/** [[Strings]] evidence for [[BlobRef]]. */
trait BlobRefStringsInstances:

  implicit def blobRefStrings(implicit blobService: BlobService): Strings[BlobRef] = new Strings[BlobRef]:
    override def strings(ref: BlobRef): List[String] = BlobExtractor.blobStrings(ref)
    override def htmls(ref: BlobRef): List[String]   = BlobExtractor.blobHtml(ref).toList
