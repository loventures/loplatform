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

package loi.authoring.asset

import argonaut.*
import argonaut.Argonaut.*
import cats.data.ValidatedNel
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.learningobjects.cpxp.scala.json.JacksonCodecs
import com.learningobjects.cpxp.service.mime.MimeWebService
import loi.authoring.AssetType
import loi.authoring.blob.BlobService
import loi.authoring.edge.Group
import loi.authoring.index.AssetDataDocument
import loi.authoring.json.AssetJsonConverter
import scaloi.json.ArgoExtras.*

import java.util.{Objects, UUID}

/** The asset representation.
  *
  * @param info
  *   immutable metadata about this asset
  * @param data
  *   data of this asset. Some properties will be imbued with special behavior based on their name
  *
  * @see
  *   [[loi.authoring.asset.factory.SpecialPropsConfig]]
  */
@JsonSerialize(converter = classOf[AssetJsonConverter])
final class Asset[A](val info: AssetInfo, val assetType: AssetType[A], val data: A):

  /** Obtains this asset an as Asset[B] if it is a B
    */
  def filter[B](implicit bType: AssetType[B]): Option[Asset[B]] =
    bType.classTag.unapply(data).filter(_ => assetType.id == bType.id).map(_ => this.asInstanceOf[Asset[B]])

  /** Returns true if this is an `Asset[B]`. */
  def is[B](implicit bType: AssetType[B]): Boolean = filter[B].isDefined

  // could this become map[B : AssetType](f: Data => B) what would that meeeeeeeeeeeanunctor
  def map(f: A => A): Asset[A] = new Asset(info, assetType, f(data))

  // is asset a functor
  def as(data: => A): Asset[A] = map(_ => data)

  /** @see
    *   [[AssetType.validate]]
    */
  def validate: ValidatedNel[String, Unit] = assetType.validate(data)

  /** @see
    *   [[AssetType.updateValidate]]
    */
  def updateValidate(groupSizes: => Map[Group, Int]): ValidatedNel[String, Unit] =
    assetType.updateValidate(data, groupSizes)

  /** @see [[AssetType.computeTitle]] */
  def computeTitle: Option[String] = assetType.computeTitle(data)

  /** @see [[AssetType.receiveTitle]] */
  def receiveTitle(title: String): Asset[A] = this.as(assetType.receiveTitle(data, title))

  /** @see [[AssetType.edgeIds]] */
  def edgeIds: Set[UUID] = assetType.edgeIds(data)

  /** @see [[AssetType.render]] */
  def render(targets: Map[UUID, Asset[?]]): Asset[A] = this.as(assetType.render(data, targets))

  /** @see
    *   [[AssetType.index]]
    */
  def index(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument =
    assetType.index(data)

  /** @see
    *   [[AssetType.htmls]]
    */
  def htmls(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
    assetType.htmls(data)

  override def toString: String = s"${info.typeId}(${info.id};${info.name})"

  override def hashCode(): Int = Objects.hash(long2Long(info.id))

  override def equals(obj: Any): Boolean =
    obj match
      case that: Asset[?] => this.info.id == that.info.id
      case _              => false
end Asset

object Asset:

  def apply[A](info: AssetInfo, data: A)(implicit assetType: AssetType[A]) =
    new Asset(info, assetType, data)

  implicit def encodeJsonForAsset[A](implicit encodeJsonForA: EncodeJson[A]): EncodeJson[Asset[A]] =
    EncodeJson(asset =>
      Json(
        "id"        := asset.info.id,
        "name"      := asset.info.name,
        "typeId"    := asset.info.typeId,
        "created"   := asset.info.created,
        "createdBy" := asset.info.createdBy,
        "modified"  := asset.info.modified,
        "data"      := encodeJsonForA.encode(asset.data)
      )
    )
end Asset

object JacksonAssetCodec:
  implicit val assetCodecJson: CodecJson[Asset[?]] = JacksonCodecs.codecFromJackson[Asset[?]]

abstract class AssetExtractor[A: AssetType]:
  def unapply(asset: Asset[?]): Option[Asset[A]] = asset.filter[A]
