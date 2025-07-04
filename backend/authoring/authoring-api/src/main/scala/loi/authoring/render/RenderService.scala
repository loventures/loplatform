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

import com.learningobjects.cpxp.component.annotation.Service
import loi.asset.contentpart.{BlockPart, ContentPart, HtmlPart}
import loi.authoring.asset.Asset
import loi.authoring.edge.AssetEdge
import loi.authoring.workspace.{AttachedReadWorkspace, ReadWorkspace}
import org.apache.commons.codec.digest.DigestUtils

import scala.util.Try

@Service
trait RenderService:

  /** Replaces each loEdgeId:// URL in the data of `asset` with browser markup for the referenced asset.
    */
  def render[A](ws: ReadWorkspace, asset: Asset[A]): Asset[A]

  /** Replaces each loEdgeId:// URL in the data of `assets` and vertices of `edges` with browser markup for the
    * referenced asset. The returned assets and edges are in the same order as the respective parameters and have the
    * same size.
    */
  def render(
    ws: ReadWorkspace,
    assets: List[Asset[?]],
    edges: List[AssetEdge[?, ?]]
  ): (List[Asset[?]], List[AssetEdge[?, ?]])

  /** Replaces each loEdgeId:// URL in the data of `assets` with browser markup for the referenced asset. The returned
    * assets are in the same order as the parameter and have the same size.
    */
  final def render(ws: ReadWorkspace, assets: List[Asset[?]]): List[Asset[?]] = render(ws, assets, Nil)._1

  /** Obviously has nothing to do with loEdgeId:// URLs, that's why we named this one `getRenderedAsset` instead of
    * `render`.
    *
    * Builds an HTML document for the subgraph rooted at `asset`. Of course `asset` is a content designer or a certain
    * enclosure of one.
    *
    * @param asset
    *   - the asset to get the rendered value for
    * @param commit
    *   - the head commit where the asset lives.
    * @param bypassCache
    *   - whether to bypass the render cache
    * @param useCdn
    *   - whether to use the external CDN when rendering
    * @return
    *   the result of attempting to render the asset otherwise.
    * @throws RenderFailures.UnrenderableAssetType
    *   if the asset type is of a type unamenable to rendering
    */
  def getRenderedAsset(
    asset: Asset[?],
    ws: AttachedReadWorkspace,
    bypassCache: Boolean = false,
    useCdn: Boolean = false,
  ): Try[String]

  def getPrintFriendlyRenderedAsset(asset: Asset[?], ws: AttachedReadWorkspace): Try[String]

  /** Finds the actual lti://tool/path?parameters URL in an HTML asset or an activity asset's instructions by the
    * digested link token used in an LTI launch URL. Return the three parts: tool name, url suffix, query parameters.
    *
    * The rendering flags should preferentially match those used during actual rendering to avoid unnecessary rendering
    * computations.
    */
  def findLtiUrlByRenderedDigest(
    asset: Asset[?],
    instructions: Boolean,
    ws: AttachedReadWorkspace,
    token: String,
    bypassCache: Boolean,
    useCdn: Boolean,
  ): Try[Option[LtiUrl]];

  def findLtiUrlByRenderedDigest(html: String, token: String): Option[LtiUrl];
end RenderService

final case class LtiUrl(
  name: String,
  suffix: String,
  query: String
)

/** Support for rendering lti:// URLs into real URLs for LTI launch. */
object LtiLinkRenderer:
  private[render] final val LtiUrlRE = """lti://(?<name>[^"'/?]+)(?<suffix>[^"'?]*)(?<query>[^"']*)""".r

  def rewriteLtiUrls(html: String, baseUrl: String): String =
    LtiUrlRE.replaceAllIn(html, s => s"${baseUrl}lti/${DigestUtils.md5Hex(s.matched)}")

  /** Rewrite any lti:// URLs in this content part. */
  def rewriteContentPart[A <: ContentPart](content: A, courseAssetUrl: String): A = content match
    case p: BlockPart =>
      p.copy(parts = p.parts.map(rewriteContentPart(_, courseAssetUrl))).asInstanceOf[A]
    case p: HtmlPart  =>
      p.copy(renderedHtml = p.renderedHtml.map(rewriteLtiUrls(_, courseAssetUrl))).asInstanceOf[A]
    case p            => p
end LtiLinkRenderer
