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

package loi.authoring.render
import net.htmlparser.jericho.*
import org.apache.commons.codec.digest.DigestUtils

import scala.collection.mutable
import scala.util.control.NonFatal

object DataIds:
  private final val logger = org.log4s.getLogger

  private final val IdableTagNames = locally {
    import HTMLElementName.*
    Set(P, LI, DT, DD, H1, H2, H3, H4, H5, H6, TH, TD, IMG, PRE)
  }

  def render(html: String): String =
    try
      val seen = mutable.Set.empty[String]
      val sb   = new StringBuilder(html.length)
      import scala.jdk.CollectionConverters.*
      val tags = new net.htmlparser.jericho.Source(html)
      tags.fullSequentialParse()
      tags.getNodeIterator.asScala foreach {
        case tag: StartTag if isRewritable(tag) =>
          sb.append('<').append(tag.getName)
          tag.getAttributes.asScala foreach { attribute =>
            sb.append(' ').append(attribute.getName);
            if attribute.getValue ne null then
              sb.append("=\"");
              sb.append(CharacterReference.encode(attribute.getValue));
              sb.append('"');
          }
          sb.append(" data-id=\"")
          val str = tag.getElement.toString
          val md5 = Iterator
            .iterate(0)(_ + 1)
            .map(i => DigestUtils.md5Hex(if i == 0 then str else s"$str-$i").substring(0, 8))
            .find(seen.add)
            .get
          sb.append(md5)
          sb.append('"')
          if (tag.getElement.getEndTag eq null) && !HTMLElements.getEndTagOptionalElementNames.contains(tag.getName)
          then sb.append(" />");
          else sb.append('>');

        case segment =>
          sb.append(segment.toString)
      }
      sb.toString
    catch
      case NonFatal(e) =>
        logger.warn(e)("Error adding ids to html")
        html

  private def isRewritable(tag: StartTag) =
    IdableTagNames.contains(tag.getName) && tag.getTagType == StartTagType.NORMAL && !tag.getAttributes.contains(
      "data-id"
    )
end DataIds
