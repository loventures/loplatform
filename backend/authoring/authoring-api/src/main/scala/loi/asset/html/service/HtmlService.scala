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

package loi.asset.html.service

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.util.FileInfo
import loi.asset.html.model.Html
import loi.authoring.asset.Asset
import loi.authoring.blob.BlobRef
import loi.authoring.workspace.AttachedReadWorkspace
import scalaz.\/

import scala.util.Try

@Service
trait HtmlService:

  /** Converts an html graph (html -> web dependency -> css/js) into a single html string. Styles will be inserted into
    * the <head> section of html and the javascript will be inserted at the end of the document.
    *
    * @param html
    *   - the html asset to convert
    * @param ws
    *   - the read work space
    * @param edit
    *   - optional edited overlay to render instead
    * @return
    *   [[RenderedHtmlDto]]
    */
  def createHtml(
    html: Asset[Html],
    ws: AttachedReadWorkspace,
    edit: Option[FileInfo \/ BlobRef] = None,
    useCdn: Boolean = false,
  ): Try[RenderedHtmlDto]

  /** Used to render the effective HTML (including defaults) for an unsaved asset on the front-end. */
  def createHtml(
    edit: FileInfo,
    ws: AttachedReadWorkspace,
  ): RenderedHtmlDto

  /** For rendering print-friendly Lessons
    */
  def renderPrintFriendlyHtml(htmls: Seq[Asset[Html]], title: String, ws: AttachedReadWorkspace): Try[String]
end HtmlService
