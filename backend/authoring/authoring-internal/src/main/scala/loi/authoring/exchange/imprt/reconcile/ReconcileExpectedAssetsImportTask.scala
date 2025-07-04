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

package loi.authoring.exchange.imprt.reconcile

import com.learningobjects.de.task.TaskReport
import loi.asset.competency.model.*
import loi.authoring.exchange.imprt.store.ImportReceiptDao
import loi.authoring.exchange.imprt.{ImportReceipt, ImportTask}
import loi.authoring.exchange.model.{ValidatedExchangeManifest, ValidatedNodeExchangeData}
import loi.authoring.node.store.NodeEntity2
import loi.authoring.project.{CommitDocEntity, CommitEntity2}
import loi.authoring.workspace.ReadWorkspace
import loi.cp.i18n.AuthoringBundle
import org.hibernate.Session
import org.hibernate.query.NativeQuery

import java.util.UUID
import scala.jdk.CollectionConverters.*

/** An import task that associates some assets in the exchange to assets that are expected to already exist on the
  * system. Competencies are such assets.
  */
class ReconcileExpectedAssetsImportTask private (
  report: TaskReport,
  receipt: ImportReceipt,
  manifest: ValidatedExchangeManifest,
  readWorkspace: ReadWorkspace
)(
  importReceiptDao: ImportReceiptDao,
  session: Session
) extends ImportTask[Map[String, UUID]](report, receipt)(importReceiptDao):

  private val batchSize = 250

  override protected def run(): Option[Map[String, UUID]] =

    val batches = manifest.expectedAssets.values.toList.grouped(batchSize).toList

    val expectedAssets = batches.foldLeft(Map.empty[String, UUID])({ case (acc, batch) =>
      acc ++ processBatch(batch)
    })

    Some(expectedAssets)

  /** Processes a batch of entries from the import manifest. For each asset in the batch, find its brethren on the
    * server or warn if it cannot be found.
    *
    * @param batch
    *   the assets to process
    * @return
    *   the import entries that have been processed so far, including `batch`
    */
  private def processBatch(
    batch: Seq[ValidatedNodeExchangeData[?]]
  ): Map[String, UUID] =

    val expectedTitles = batch.collect(node =>
      node.data match
        case lvl1: Level1Competency => NodeAndTitle(node, lvl1.title)
        case lvl2: Level2Competency => NodeAndTitle(node, lvl2.title)
        case lvl3: Level3Competency => NodeAndTitle(node, lvl3.title)
    )

    val existingAssetNames = queryNames(expectedTitles)

    val replacements = expectedTitles
      .flatMap({ case NodeAndTitle(node, title) =>
        val typeId               = node.assetType.id.entryName
        val existingAssetName    = existingAssetNames.get((title, typeId))
        val entryWithReplacement = existingAssetName.map((node.id, _))
        if entryWithReplacement.isEmpty then
          report
            .addWarning(AuthoringBundle.message("import.missingExpectedAsset", node.id))
        report.markProgress()
        entryWithReplacement
      })
      .toMap

    replacements
  end processBatch

  type TitleAndTypeId = (String, String)

  private def queryNames(titlesAndTypeIds: Seq[NodeAndTitle]): Map[TitleAndTypeId, UUID] =

    if readWorkspace.rootIds.isEmpty then return Map.empty

    /*
     * We are turning a Seq of Seqs of conditions like this
     *
     * Seq(
     *   Seq(title = 't1', assettypedescriptor_id = 42),
     *   Seq(title = 't2', assettypedescriptor_id = 43)
     * )
     *
     * into QB instructions that will make SQL like this
     *
     * (title = 't1' AND assettypedescriptor_id = 42) OR
     * (title = 't2' AND assettypedescriptor_id = 43)
     *
     * and this already exists as a concept on QueryBuilder
     */
    // qb.addDisjunction doesn't work, no time to investigate, see  CBLPROD-6730 for
    // sys/script to recreate the problem with addDisjunction
    val conjunctions = titlesAndTypeIds.indices.map(i => s"(title=:title$i and typeId=:typeId$i)")
    val disjunction  = conjunctions.mkString("(", " or\n", ")")

    val q =
      session
        .createNativeQuery(s"""WITH kfnode(name, id) AS (
           |  SELECT kfnode.name, CAST(kfnode.id AS BIGINT)
           |  FROM authoringcommit c
           |  JOIN authoringcommitdoc kfdoc on kfdoc.id = c.kfdoc_id
           |  CROSS JOIN jsonb_each(kfdoc.nodes) AS kfnodes(projectid, nodes)
           |  CROSS JOIN jsonb_each_text(kfnodes.nodes) as kfnode(name, id)
           |  WHERE c.id = :commitId
           |), driftnode(name, id) AS (
           |  SELECT driftnode.name, CAST(driftnode.id AS BIGINT)
           |  FROM authoringcommit c
           |  JOIN authoringcommitdoc driftdoc ON driftdoc.id = c.driftdoc_id
           |  CROSS JOIN jsonb_each(driftdoc.nodes) AS driftnodes(projectid, nodes)
           |  CROSS JOIN jsonb_each_text(driftnodes.nodes) AS driftnode(name, id)
           |  WHERE c.id = :commitId
           |), combined(name, id) AS (
           |  SELECT
           |    coalesce(driftnode.name, kfnode.name) AS name,
           |    coalesce(driftnode.id, kfnode.id) AS id
           |  FROM kfnode
           |  FULL OUTER JOIN driftnode ON kfnode.name = driftnode.name
           |)
           |SELECT title, typeid, n.name
           |FROM combined
           |JOIN authoringnode n ON combined.id = n.id
           |WHERE combined.id != -1
           |  AND $disjunction""".stripMargin)
        .unwrap(classOf[NativeQuery[?]])
        .setParameter("commitId", readWorkspace.commitId)
        .addSynchronizedEntityClass(classOf[CommitEntity2])
        .addSynchronizedEntityClass(classOf[CommitDocEntity])
        .addSynchronizedEntityClass(classOf[NodeEntity2])

    titlesAndTypeIds.zipWithIndex.foreach({ case (NodeAndTitle(node, title), i) =>
      q.setParameter(s"title$i", title)
      q.setParameter(s"typeId$i", node.assetType.id.entryName)
    })

    q.getResultList.asScala
      .map({
        case Array(title: String, typeId: String, name: UUID)   => (title, typeId) -> name                  // laird
        case Array(title: String, typeId: String, name: String) => (title, typeId) -> UUID.fromString(name) // branchy
      })
      .toMap
  end queryNames
end ReconcileExpectedAssetsImportTask

object ReconcileExpectedAssetsImportTask:

  def apply(
    receipt: ImportReceipt,
    manifest: ValidatedExchangeManifest,
    readWorkspace: ReadWorkspace
  )(
    importReceiptDao: ImportReceiptDao,
    session: Session
  ): ReconcileExpectedAssetsImportTask =
    val report = receipt.report
      .addChild("Reconciling assets", manifest.expectedAssets.size)
    new ReconcileExpectedAssetsImportTask(report, receipt, manifest, readWorkspace)(
      importReceiptDao,
      session
    )
  end apply
end ReconcileExpectedAssetsImportTask

case class NodeAndTitle(node: ValidatedNodeExchangeData[?], title: String)
