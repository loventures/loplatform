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

import loi.asset.contentpart.*
import loi.authoring.syntax.index.*

/** [[Strings]] evidence for the spaghettification of content parts. */
trait ContentPartStringsInstances:
  import HtmlExtractor.*

  implicit val embeddablePartStrings: Strings[EmbeddablePart] =
    Strings.plaintext(a => a.title :: Nil) // not supported/used?

  implicit val imagePartStrings: Strings[ImagePart] = new Strings[ImagePart]:
    override def strings(a: ImagePart): List[String] = List(a.altText, a.title, a.caption.map(fromHtml)).flatten
    override def htmls(a: ImagePart): List[String]   = a.caption.toList

  implicit val mediaGalleryPartStrings: Strings[MediaGalleryPart] = new Strings[MediaGalleryPart]:
    override def strings(a: MediaGalleryPart): List[String] = a.title :: a.description :: a.parts.strings
    override def htmls(a: MediaGalleryPart): List[String]   = a.parts.htmls

  implicit val htmlPartStrings: Strings[HtmlPart] = new Strings[HtmlPart]:
    override def strings(a: HtmlPart): List[String] = fromHtml(a.html).strings
    override def htmls(a: HtmlPart): List[String]   = a.html :: Nil

  implicit val blockPartStrings: Strings[BlockPart] = new Strings[BlockPart]:
    override def strings(a: BlockPart): List[String] =
      a.parts.toList flatMap {
        case p: EmbeddablePart   => p.strings
        case p: ImagePart        => p.strings
        case p: MediaGalleryPart => p.strings
        case p: HtmlPart         => p.strings
        case p: BlockPart        => strings(a)
      }

    override def htmls(a: BlockPart): List[String] =
      a.parts.toList flatMap {
        case p: EmbeddablePart   => p.htmls
        case p: ImagePart        => p.htmls
        case p: MediaGalleryPart => p.htmls
        case p: HtmlPart         => p.htmls
        case p: BlockPart        => htmls(a)
      }
end ContentPartStringsInstances
