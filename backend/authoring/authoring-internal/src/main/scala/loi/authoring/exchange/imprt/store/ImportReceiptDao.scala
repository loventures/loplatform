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

package loi.authoring.exchange.imprt.store

import java.util.Date

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.de.task.TaskReport
import loi.authoring.blob.BlobRef
import loi.authoring.exchange.imprt.ImportReceipt
import loi.authoring.exchange.model.ImportedRoot
import loi.cp.asset.exchange.impl.AssetExchangeRequestStatus
import org.hibernate.Session

import scala.jdk.CollectionConverters.*

@Service
class ImportReceiptDao(
  session: => Session,
  domainDto: => DomainDTO
):

  def loadReceipts(limit: Int, offset: Int): Seq[ImportReceiptEntity] =
    session
      .createQuery(
        """
        | FROM ImportReceiptEntity
        | WHERE root = :root
        | ORDER BY createTime DESC
      """.stripMargin,
        classOf[ImportReceiptEntity]
      )
      .setParameter("root", domainDto.id)
      .setMaxResults(limit)
      .setFirstResult(offset)
      .getResultList
      .asScala
      .toSeq

  def loadReceipt(id: Long): Option[ImportReceiptEntity] =
    Option(session.find(classOf[ImportReceiptEntity], id))

  def save(entity: ImportReceiptEntity): ImportReceiptEntity =
    session.persist(entity)
    entity

  // for when `entity` has a real id but is not attached to the session... updates the
  // session with the data on `entity`.
  def merge(entity: ImportReceiptEntity): Unit =
    session.merge(entity)

  def loadReference(receipt: ImportReceipt): ImportReceiptEntity =
    session.getReference(classOf[ImportReceiptEntity], receipt.id)

  def delete(entity: ImportReceiptEntity): Unit =
    session.remove(entity)
end ImportReceiptDao

object ImportReceiptDao:

  private val om = JacksonUtils.getFinatraMapper

  private lazy val listOfImportedRoot = om.getTypeFactory
    .constructCollectionType(classOf[java.util.ArrayList[?]], classOf[ImportedRoot])

  def entityToReceipt(entity: ImportReceiptEntity) =
    new ImportReceipt(
      entity.getId,
      entity.getData,
      om.readValue[java.util.List[ImportedRoot]](om.treeAsTokens(entity.getImportedRoots), listOfImportedRoot)
        .asScala
        .toSeq,
      om.readValue(om.treeAsTokens(entity.getReport), classOf[TaskReport]),
      Option(entity.getAttachmentId).map(Long2long),
      Option(entity.getDownloadFilename),
      AssetExchangeRequestStatus.withName(entity.getStatus),
      entity.getCreateTime,
      Option(entity.getStartTime),
      Option(entity.getEndTime),
      Option(entity.getCreatedBy).map(Long2long),
      entity.getRoot,
      // Option to flatMap of Option is dumb but otherwise we can fail to serialize and get a Some(null) because that makes sense
      Option(entity.getSource).flatMap(src => Option(om.treeToValue[BlobRef](src, classOf[BlobRef]))),
      Option(entity.getUnconvertedSource).flatMap(usrc => Option(om.treeToValue[BlobRef](usrc, classOf[BlobRef])))
    )

  def receiptToEntity(receipt: ImportReceipt) =
    new ImportReceiptEntity(
      receipt.id,
      receipt.data,
      om.valueToTree(receipt.importedRoots),
      om.valueToTree(receipt.report),
      receipt.attachmentId.map(long2Long).orNull,
      receipt.downloadFilename.orNull,
      receipt.status.entryName,
      receipt.createTime,
      receipt.startTime.orNull,
      receipt.endTime.orNull,
      receipt.createdBy.map(long2Long).orNull,
      receipt.domainId,
      om.valueToTree[JsonNode](receipt.source),
      om.valueToTree[JsonNode](receipt.unconvertedSource)
    )

  def newEntity(
    importName: String,
    data: JsonNode,
    downloadFilename: String,
    convertedSource: Option[BlobRef],
    unconvertedSource: Option[BlobRef],
    status: AssetExchangeRequestStatus,
    userId: Long,
    domainId: Long,
    taskReport: TaskReport,
    startTime: Option[Date]
  ): ImportReceiptEntity =
    new ImportReceiptEntity(
      null,
      data,
      JsonNodeFactory.instance.arrayNode(),
      om.valueToTree[JsonNode](taskReport),
      null, // attachmentId
      downloadFilename,
      status.entryName,
      new Date(),
      startTime.orNull,
      null, // end time
      userId,
      domainId,
      convertedSource.map(om.valueToTree[JsonNode]).orNull,
      unconvertedSource.map(om.valueToTree[JsonNode]).orNull
    )
end ImportReceiptDao
