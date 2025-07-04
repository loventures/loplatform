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

package loi.authoring.html

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.service.CurrentUrlService
import com.learningobjects.cpxp.web.ExportFile
import loi.asset.contentpart.HtmlPart
import loi.asset.html.model.Html
import loi.asset.util.Assex.*
import loi.authoring.asset.Asset
import loi.authoring.blob.BlobService
import loi.authoring.edge.Group.Elements
import loi.authoring.edge.TraversedGraph
import loi.authoring.node.AssetNodeService
import org.apache.commons.io.IOUtils
import scalaz.std.string.*
import scalaz.syntax.std.option.*
import scaloi.syntax.classTag.*
import scaloi.syntax.option.*

import java.nio.charset.StandardCharsets
import scala.reflect.ClassTag
import scala.util.Using

@Service
class HtmlTransferService(implicit
  blobService: BlobService,
  nodeService: AssetNodeService,
  currentUrlService: CurrentUrlService,
):

  /** Return a lazy list of operations needed to export or import the HTML content from a course, rooted at a module,
    * lesson or the course. The list is only really lazy at the module level, all content within a module will be
    * eagerly evaluated.
    */
  def transferOperations(
    root: Asset[?],
    graph: TraversedGraph
  ): LazyList[TransferOp] =
    // This is a top down histomorphism over the asset tree, but it seems so much effort to go there...
    def transferOps(indexedAsset: IndexedAsset, path: String): List[TransferOp] =
      val (asset, index) = indexedAsset
      val dir            = f"$path%s/${index + 1}%02d - ${ExportFile.cleanFilename(asset.title.orZ.trim)}%s"
      val dirOp          = TransferOp.Directory(s"$dir/")
      val dataOp         = asset match
        case Html.Asset(html) =>
          for
            blob    <- blobService.getBlobInfo(html)
            src      = Using.resource(blob.openInputStream())(IOUtils.toString(_, StandardCharsets.UTF_8))
            rendered = CrappyRenderer.render(src, html, graph).trim
          yield TransferOp.Content(s"$dir/index.html", html, rendered)

        case other =>
          for
            blockPart <- other.instructions
            if blockPart.parts.length == 1
            firstPart <- blockPart.parts.headOption
            firstHtml <- implicitly[ClassTag[HtmlPart]].option(firstPart)
            rendered   = CrappyRenderer.render(firstHtml.html, other, graph)
          yield TransferOp.Instructions(s"$dir/instructions.html", other, rendered)

      dirOp :: (dataOp ::? graph
        .targetsInGroup(asset, Elements)
        .zipWithIndex
        .toList
        .flatMap(transferOps(_, dir)))
    end transferOps

    val dir = ExportFile.cleanFilename(root.title | "Untitled")
    LazyList.from(graph.targetsInGroup(root, Elements)).zipWithIndex.flatMap(transferOps(_, dir))
  end transferOperations
end HtmlTransferService

protected[html] sealed trait TransferOp

protected[html] object TransferOp:

  final case class Directory(path: String) extends TransferOp

  final case class Instructions[A](path: String, asset: Asset[A], instructions: String) extends TransferOp

  final case class Content(path: String, asset: Asset[Html], html: String) extends TransferOp
