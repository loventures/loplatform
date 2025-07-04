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

import com.learningobjects.cpxp.component.ComponentEnvironment
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.util.ComponentUtils.resourceUrl
import loi.asset.contentpart.{BlockPart, ContentPart, HtmlPart}
import loi.asset.html.model.Html
import loi.asset.html.service.HtmlService
import loi.asset.lesson.model.Lesson
import loi.asset.util.Assex.*
import loi.authoring.asset.Asset
import loi.authoring.edge.{AssetEdge, EdgeService, Group, TraverseGraph}
import loi.authoring.render.RenderFailures.AssetCannotBePrinted
import loi.authoring.workspace.{AttachedReadWorkspace, ReadWorkspace}
import org.apache.commons.codec.digest.DigestUtils
import scalaz.std.string.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.syntax.any.*
import scaloi.syntax.collection.*
import scaloi.syntax.map.*
import scaloi.syntax.option.*

import java.util.UUID
import scala.util.{Failure, Try}

@Service
class BaseRenderService(
  edgeService: EdgeService,
  htmlService: HtmlService,
  renderedContentLoadingCache: RenderedContentLoadingCache,
  environment: () => ComponentEnvironment
) extends RenderService:

  import LtiLinkRenderer.*

  // cries. why can't this one use the List overload and still keep A type
  override def render[A](ws: ReadWorkspace, asset: Asset[A]): Asset[A] =
    if asset.edgeIds.isEmpty then asset.render(Map.empty)
    else
      val targets = edgeService
        .loadOutEdges(ws, asset, Group.Resources)
        .groupMapUniq(_.edgeId)(_.target)
      asset.render(targets)

  override def render(
    ws: ReadWorkspace,
    assets: List[Asset[?]],
    edges: List[AssetEdge[?, ?]]
  ): (List[Asset[?]], List[AssetEdge[?, ?]]) =

    val rendees = (assets ++ edges.map(_.source) ++ edges.map(_.target)).distinct

    val targets =
      if rendees.exists(_.edgeIds.nonEmpty) then
        // then there are loEdgeId:// to replace, fetch the possible replacements
        edgeService
          .loadOutEdges(ws, rendees) // supposed to be only Group.Resource but I dare not change it now
          .groupBy(_.source.info.id)
          .mapValuesEagerly(outEdges => outEdges.groupMapUniq(_.edgeId)(_.target))
      else
        // then there are no loEdgeId:// to replace. But we still render to initialize
        // the rendered forms of the Asset.
        // For example, HtmlPart's `renderedHtml: Option[String]` needs to become a Some of
        // the `html: String` property even if the `html: String` property contains no
        // loEdgeId:// URLs.
        Map.empty[Long, Map[UUID, Asset[?]]]

    val rendereds = rendees
      .map(rendee =>
        val tgts = targets.getOrElse(rendee.info.id, Map.empty)
        rendee.render(tgts)
      )
      .groupUniqBy(_.info.id)

    val renderedAssets = assets.map(asset => rendereds.getOrElse(asset.info.id, asset))
    val renderedEdges  = edges.map(edge =>
      edge.copy(
        source = rendereds.getOrElse(edge.source.info.id, edge.source),
        target = rendereds.getOrElse(edge.target.info.id, edge.target)
      )
    )

    (renderedAssets, renderedEdges)
  end render

  override def getRenderedAsset(
    asset: Asset[?],
    ws: AttachedReadWorkspace,
    bypassCache: Boolean = false,
    useCdn: Boolean = false,
  ): Try[String] =
    rawRenderedAsset(asset, ws, bypassCache, useCdn).map(interpolateHtml)

  private def rawRenderedAsset(
    asset: Asset[?],
    ws: AttachedReadWorkspace,
    bypassCache: Boolean = false,
    useCdn: Boolean = false,
  ): Try[String] =
    asset match
      case Html.Asset(html) =>
        lazy val render = htmlService.createHtml(html, ws, None, useCdn).map(_.html)
        val suffix      = s"""${useCdn ?? "-cdn"}"""
        if bypassCache then render
        else Try(renderedContentLoadingCache.getOrLoad(html, ws.commitId, suffix, () => render.get))

      case unrenderable =>
        Failure(RenderFailures.UnrenderableAssetType(unrenderable))

  override def getPrintFriendlyRenderedAsset(asset: Asset[?], ws: AttachedReadWorkspace): Try[String] =
    asset match
      case Html.Asset(html)     =>
        htmlService.renderPrintFriendlyHtml(Seq(html), html.data.title, ws).map(interpolateHtml)
      case Lesson.Asset(lesson) =>
        val htmls = edgeService
          .stravaigeOutGraphs(TraverseGraph.fromSource(lesson.info.name).traverse(Group.Elements).noFurther :: Nil, ws)
          .targetsInGroupOfType[Html](lesson, Group.Elements)
        htmlService
          .renderPrintFriendlyHtml(htmls, lesson.data.title, ws)
          .map(interpolateHtml)
      case _                    => Failure(AssetCannotBePrinted(asset))

  private def interpolateHtml(html: String): String =
    html |> interpolateCdn |> interpolateLti

  /* Instead of HtmlFilter or other more sophisticated methods, we just grab the base URL for the front-component
   * and find/replace $$cdn/ in the rendered html. */
  private def interpolateCdn(html: String): String =
    Option(environment()).cata(
      env =>
        val component  = env.getComponent(loi.authoring.authoringComponentIdentifier)
        val baseCdnUrl = resourceUrl("", component)
        html.replace("$$cdn/", baseCdnUrl)
      ,
      html
    )

  // Interpolates lti:// URLs into relative URLs assuming a render web controller base URL
  private def interpolateLti(html: String): String = rewriteLtiUrls(html, "")

  override def findLtiUrlByRenderedDigest(
    asset: Asset[?],
    instructions: Boolean,
    ws: AttachedReadWorkspace,
    token: String,
    bypassCache: Boolean = false,
    useCdn: Boolean = false,
  ): Try[Option[LtiUrl]] =
    for html <-
        if instructions then instructionsHtml(asset) else rawRenderedAsset(asset, ws, bypassCache, useCdn)
    yield findLtiUrlByRenderedDigest(html, token)

  override def findLtiUrlByRenderedDigest(html: String, token: String): Option[LtiUrl] =
    LtiUrlRE
      .findAllMatchIn(html)
      .find(m => token == DigestUtils.md5Hex(m.matched))
      .map(m => LtiUrl(m.group(1), m.group(2), m.group(3)))

  private def instructionsHtml(asset: Asset[?]): Try[String] =
    for instructions <- asset.instructions <@~* RenderFailures.UnrenderableAssetType(asset)
    yield partHtmls(instructions).mkString

  private def partHtmls(content: ContentPart): Seq[String] = content match
    case p: BlockPart => p.parts.flatMap(partHtmls)
    case p: HtmlPart  => p.renderedHtml.toSeq
    case _            => Seq.empty
end BaseRenderService
