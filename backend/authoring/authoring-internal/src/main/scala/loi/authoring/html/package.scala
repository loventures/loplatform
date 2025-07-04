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

package loi.authoring

import loi.asset.contentpart.{BlockPart, HtmlPart}
import loi.authoring.asset.Asset
import org.apache.commons.text.StringEscapeUtils

import java.util.zip.{ZipEntry, ZipFile}

package object html:
  type IndexedAsset = (Asset[?], Int)

  implicit class StringOps(private val self: String) extends AnyVal:
    def html4: String = StringEscapeUtils.escapeHtml4(self)

  implicit class ZipFileOps(private val self: ZipFile) extends AnyVal:
    def entry(name: String): Option[ZipEntry] = Option(self.getEntry(name))

  implicit class BlockStringOps(private val self: String) extends AnyVal:
    def blockPart: BlockPart = BlockPart(HtmlPart(self) :: Nil)
end html
