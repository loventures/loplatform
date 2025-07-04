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

package loi.asset.contentpart

import com.fasterxml.jackson.annotation.JsonTypeInfo.{As, Id}
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonSubTypes, JsonTypeInfo}
import com.learningobjects.cpxp.util.HtmlUtils
import loi.asset.file.image.model.Image
import loi.authoring.asset.Asset
import loi.authoring.render.LoEdgeIdUrl

import java.util.UUID

/** A content part holds various kinds of asset content.
  */
@JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "partType")
@JsonSubTypes(
  value = Array(
    new JsonSubTypes.Type(value = classOf[BlockPart], name = "block"),
    new JsonSubTypes.Type(value = classOf[EmbeddablePart], name = "embeddable"),
    new JsonSubTypes.Type(value = classOf[ImagePart], name = "image"),
    new JsonSubTypes.Type(value = classOf[MediaGalleryPart], name = "mediaGallery"),
    new JsonSubTypes.Type(value = classOf[HtmlPart], name = "html")
  )
)
sealed trait ContentPart:

  def partType: ContentPartType

  def renderedHtml: Option[String]

  def edgeIds: Set[UUID]

  def render(targets: Map[UUID, Asset[?]]): ContentPart
end ContentPart

/** Embeddable content parts are for representing the content of another asset in the data of an asset. This feature is
  * not fully baked.
  */
case class EmbeddablePart(
  edgeId: UUID,
  title: String,
  renderedHtml: Option[String] = None
) extends ContentPart:

  override val partType: ContentPartType = ContentPartType.Embeddable

  @JsonIgnore
  override val edgeIds: Set[UUID] = Set.empty

  override def render(targets: Map[UUID, Asset[?]]): EmbeddablePart =
    // just copying the old code, seems embeddable parts didn't participate
    // in this render business
    this.copy(renderedHtml = Some(""))
end EmbeddablePart

/** An image content part is for referring to another image asset in the data of an asset.
  */
case class ImagePart(
  edgeId: UUID,
  renderedHtml: Option[String] = None,
  altText: Option[String] = None,
  title: Option[String] = None,
  caption: Option[String] = None
) extends ContentPart:

  override val partType: ContentPartType = ContentPartType.Image

  @JsonIgnore
  override lazy val edgeIds: Set[UUID] = Set(edgeId)

  override def render(targets: Map[UUID, Asset[?]]): ImagePart = this.copy(renderedHtml = Some(""))
end ImagePart

/** A media gallery content part is for organizing references to many images in one gallery.
  */
case class MediaGalleryPart(
  title: String,
  description: String,
  parts: Seq[ImagePart],
  renderedHtml: Option[String] = None
) extends ContentPart:

  override val partType: ContentPartType = ContentPartType.MediaGallery

  @JsonIgnore
  override lazy val edgeIds: Set[UUID] = parts.flatMap(_.edgeIds).toSet

  override def render(targets: Map[UUID, Asset[?]]): MediaGalleryPart =
    val renderedImageParts = parts.map(part =>
      val serveUrl = LoEdgeIdUrl.serveUrl(targets.get(part.edgeId))

      val altText: Option[String] = targets
        .get(part.edgeId)
        .map({
          case Image.Asset(img) => img.data.altText.getOrElse("")
          case _                => ""
        })

      val title: Option[String] = targets
        .get(part.edgeId)
        .map({
          case Image.Asset(img) => img.data.title
          case _                => ""
        })

      val caption: Option[String] = targets
        .get(part.edgeId)
        .map({
          case Image.Asset(img) => img.data.caption.getOrElse("")
          case _                => ""
        })

      part.copy(renderedHtml = Some(serveUrl), altText = altText, title = title, caption = caption)
    )
    val imageHtmls         = renderedImageParts.flatMap(_.renderedHtml)
    val html               = imageHtmls.mkString("<div>", "&nbsp", "</div>")
    this.copy(renderedHtml = Some(html), parts = renderedImageParts)
  end render
end MediaGalleryPart

/** An HTML part can refer to other assets in img tags and anchor tags. The `src` attribute of each is a url in the
  * form: loEdgeGuid://edgeGuid where edgeGuid is the edgeGuid of the edge that targets the desired asset.
  */
case class HtmlPart(
  html: String = "",
  renderedHtml: Option[String] = None
) extends ContentPart:

  override val partType: ContentPartType = ContentPartType.Html

  @JsonIgnore
  lazy val plainText: String = HtmlUtils.toPlaintext(html)

  @JsonIgnore
  override lazy val edgeIds: Set[UUID] = LoEdgeIdUrl.parseAll(html)

  override def render(targets: Map[UUID, Asset[?]]): HtmlPart =
    val renderedHtml = LoEdgeIdUrl.replaceAll(html, targets) // could DataIds.render this but authoring doesn't
    this.copy(renderedHtml = Some(renderedHtml))
end HtmlPart

/** An aggregation of content parts
  *
  * @param parts
  *   the parts
  */
case class BlockPart(
  parts: Seq[ContentPart] = Seq.empty,
  renderedHtml: Option[String] = None
) extends ContentPart:

  override val partType: ContentPartType = ContentPartType.Block

  @JsonIgnore
  override lazy val edgeIds: Set[UUID] = parts.flatMap(_.edgeIds).toSet

  override def render(targets: Map[UUID, Asset[?]]): BlockPart =
    val renderedParts = parts.map(_.render(targets))
    this.copy(renderedHtml = Some(renderedParts.flatMap(_.renderedHtml).mkString("&nbsp;")), parts = renderedParts)
end BlockPart
