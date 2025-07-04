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

package loi.authoring.validate

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.de.web.UncheckedMessageException
import loi.asset.blob.SourceProperty
import loi.authoring.AssetType
import loi.authoring.asset.service.exception.{AssetCreateValidationException, AssetUpdateValidationException}
import loi.authoring.blob.BlobService
import loi.authoring.edge.Group

import scala.util.{Success, Try}

@Service
class ValidationService(
  blobService: BlobService,
):

  /** Create-time validation for nodes
    *
    * @param exemptBlobName
    *   the blob name that the `data.source` can use to avoid blob name validation. This is like a pre-validated blob
    *   name from the far past before we prefixed all object keys with "authoring/".
    */
  def createValidate[A](data: A, exemptBlobName: Option[String], opIndex: Option[Int])(implicit
    assetType: AssetType[A]
  ): Try[Unit] =

    val validated = assetType.validate(data)

    for
      _ <- AssetCreateValidationException.asTryFromMsg(validated.swap.toOption.map(_.toList.mkString("; ")), opIndex)
      _ <- checkSourceBlob(data, assetType, exemptBlobName)
    yield ()
  end createValidate

  def createValidateE[A: AssetType](
    data: A,
    exemptBlobName: Option[String],
    opIndex: Option[Int]
  ): Either[UncheckedMessageException, Unit] =
    toUmeEither(createValidate(data, exemptBlobName, opIndex))

  private def checkSourceBlob(data: Any, assetType: AssetType[?], exemptBlobName: Option[String]): Try[Unit] =
    SourceProperty
      .fromData(data)
      .map(src =>
        for
          provider <- Try(blobService.getProvider(src.provider))
          _        <- blobService.validateBlobData(provider, src.name, src.contentType, assetType, exemptBlobName)
        yield ()
      )
      .getOrElse(Success(()))

  /** Update-time validation for nodes
    */
  def updateValidate[A](oldData: Any, newData: A, groupSizes: => Map[Group, Int], opIndex: Option[Int])(implicit
    assetType: AssetType[A]
  ): Try[Unit] =

    for
      _ <- checkNodeBasedUpdateValidate(newData, groupSizes, opIndex)
      _ <- checkSourceBlob(newData, assetType, SourceProperty.fromData(oldData).map(_.name))
    yield ()

  def updateValidateE[A: AssetType](
    oldData: Any,
    newData: A,
    groupSizes: => Map[Group, Int],
    opIndex: Option[Int]
  ): Either[UncheckedMessageException, Unit] =
    toUmeEither(updateValidate(oldData, newData, groupSizes, opIndex))

  private def checkNodeBasedUpdateValidate[A](newData: A, groupSizes: => Map[Group, Int], opIndex: Option[Int])(implicit
    assetType: AssetType[A]
  ): Try[Unit] =

    val validated = assetType.updateValidate(newData, groupSizes)

    AssetUpdateValidationException.asTryFromMsg(validated.swap.toOption.map(_.toList.mkString("; ")), opIndex)

  private def toUmeEither(t: Try[Unit]): Either[UncheckedMessageException, Unit] = t.toEither.left.map {
    case ume: UncheckedMessageException => ume
    case e                              => throw e
  }
end ValidationService
