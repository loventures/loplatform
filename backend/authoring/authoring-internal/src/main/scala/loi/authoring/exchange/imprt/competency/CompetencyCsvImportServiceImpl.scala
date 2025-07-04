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

package loi.authoring.exchange.imprt.competency

import java.util.UUID

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.de.task.TaskReport
import loi.authoring.AssetType
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.Group
import loi.authoring.exchange.imprt.NodeExchangeBuilder
import loi.authoring.exchange.model.{EdgeExchangeData, ExchangeManifest, NodeExchangeData}
import loi.cp.asset.edge.EdgeData
import loi.cp.i18n.{AuthoringBundle, BundleMessage}
import scaloi.syntax.StringOps.*

import scala.util.Success

@Service
class CompetencyCsvImportServiceImpl extends CompetencyCsvImportService:
  import CompetencyCsvImportServiceImpl.*

  override def parseCsv(rawRows: List[Map[String, String]], report: TaskReport): Option[Seq[CompetencyCsvImportRow]] =
    report.markStart()

    validateCsvFile(rawRows, report)

    val rowOpts: Seq[Option[CompetencyCsvImportRow]] = rawRows.zipWithIndex
      .map({ case (rawRow: Map[String, String], index: Int) =>
        // handle header row and make 1 indexed
        val rowNum: Long = index.toLong + 2L

        // removes empty strings so 'get' calls on 'Map' returns 'None' instead of 'Some("")'
        val row = rawRow.filterNot(_._2.equalsIgnoreCase(""))

        validateRow(row, rowNum, report)
      })

    val validFormat: Boolean = rowOpts.forall(_.isDefined)
    if validFormat then validateRowData(rowOpts.map(_.get), report)

    report.markComplete()
    if !report.hasErrors then Some(rowOpts.map(_.get))
    else None
  end parseCsv

  override def generateManifest(rows: Seq[CompetencyCsvImportRow]): ExchangeManifest =

    val usages: Map[Long, CompetencyCsvImportUsages] = generateUsagesMap(rows)

    val assetExchange: Seq[NodeExchangeData] = rows.map(row =>

      val builder = NodeExchangeBuilder.builder(row.index.toString, row.typeId.entryName)

      builder
        .title(row.title)
        .description(row.description)

      if competencyTypes.contains(row.typeId) then builder.keywords(row.keywords)

      usages
        .get(row.index)
        .foreach(assetImportUsages =>
          val usageExchangeDatas: Seq[EdgeExchangeData] = assetImportUsages.children.zipWithIndex
            .map(child =>
              EdgeExchangeData(
                group = assetImportUsages.group,
                target = child._1.toString,
                position = child._2.toLong,
                traverse = true,
                UUID.randomUUID(),
                EdgeData.empty
              )
            )
          builder.edges(usageExchangeDatas)
        )

      builder.build()
    )
    ExchangeManifest.empty.copy(nodes = assetExchange)
  end generateManifest

  // this depends on the validation already passing
  private def generateUsagesMap(rows: Seq[CompetencyCsvImportRow]): Map[Long, CompetencyCsvImportUsages] =
    val assets: Map[Long, CompetencyCsvImportRow] = rows
      .groupBy(_.index)
      .view
      .mapValues(_.headOption)
      .filter(_._2.isDefined)
      .mapValues(_.get)
      .toMap

    rows
      .filter(_.parent.isDefined)
      .groupBy(_.parent.get)
      .map({ case (parent: Long, usageRows: Seq[CompetencyCsvImportRow]) =>
        val childrenIds: Seq[Long] = usageRows.map(_.index)
        val usageGroup             = assetTypeRelationshipConstraints(assets(parent).typeId).usageGroup.get
        parent -> CompetencyCsvImportUsages(usageGroup, childrenIds)
      })
  end generateUsagesMap

  // valid CSV with at least 1 row and all the proper headers
  private def validateCsvFile(rawRows: List[Map[String, String]], report: TaskReport) =
    rawRows.headOption match
      case Some(row) =>
        row.keys.foreach(header => if !headers.contains(header) then report.addError(missingHeaderMsg(header)))
      case None      => report.addError(emptyCsvMsg())

  private def validateRowData(rows: Seq[CompetencyCsvImportRow], report: TaskReport) =
    val groupByIndex: Map[Long, Seq[CompetencyCsvImportRow]] = rows.groupBy(_.index)

    // duplicate indices
    groupByIndex
      .foreach(row => if row._2.length > 1 then report.addError(duplicateIndicesMsg(row._1)))

    val assets: Map[Long, CompetencyCsvImportRow] = groupByIndex.view
      .mapValues(_.headOption)
      .filter(_._2.isDefined)
      .mapValues(_.get)
      .toMap

    // make sure parents are present
    // parents have a usage group
    // parent -> child are allowed types
    assets
      .filter(_._2.parent.isDefined)
      .foreach(row =>
        val parentId: Long = row._2.parent.get

        if assets.contains(parentId) then
          val parentAssetType: AssetTypeId                           = assets(parentId).typeId
          val childAssetType: AssetTypeId                            = row._2.typeId
          val constraints: CompetencyCsvImportRelationshipConstraint = assetTypeRelationshipConstraints(parentAssetType)
          if constraints.usageGroup.isEmpty then report.addError(missingDefaultUsageGroup(parentAssetType))

          constraints.allowedChildType
            .orElse({
              report.addError(invalidRelationship(parentAssetType, childAssetType, row._1))
              None
            })
            .foreach(allowedChildType =>
              if !allowedChildType.equals(childAssetType) then
                report.addError(invalidRelationship(parentAssetType, childAssetType, row._1))
            )
        else report.addError(missingParentMsg(parentId))
        end if
      )
  end validateRowData

  private def validateRow(
    rawRow: Map[String, String],
    rowNum: Long,
    report: TaskReport
  ): Option[CompetencyCsvImportRow] =
    val index: Option[Long]            = validateIndex(rawRow.get("Index"), rowNum, report)
    val parent: Option[Long]           = validateParentFormat(rawRow.get("Parent_Index"), rowNum, report)
    val typeIdOpt: Option[AssetTypeId] = validateTypeId(rawRow.get("Type"), rowNum, report)
    val title: Option[String]          = validateTitle(rawRow.get("Title"), rowNum, report)
    val description: Option[String]    = rawRow.get("Description")
    val keywords: Option[String]       = rawRow.get("Keywords")

    report.markProgress(1)

    typeIdOpt.foreach(typeId =>
      if competencyTypes.contains(typeId) && parent.isEmpty then report.addError(requiredParent(typeId))
    )

    // index, assetType, and title are the only required fields
    if Seq(index, typeIdOpt, title).forall(_.isDefined) then
      Some(CompetencyCsvImportRow(index.get, parent, typeIdOpt.get, title.get, description, keywords))
    else None
  end validateRow

  private def validateIndex(rawIndex: Option[String], rowNum: Long, report: TaskReport): Option[Long] =
    rawIndex
      .orElse({
        report.addError(missingFieldMsg(rowNum, "Index"))
        None
      })
      .flatMap(indexStr =>
        indexStr.toLong_! match
          case Success(index) if index > 0 => Option(index)
          case _                           =>
            report.addError(invalidNumberMsg(rowNum, "Index"))
            None
      )

  private def validateParentFormat(rawParent: Option[String], rowNum: Long, report: TaskReport): Option[Long] =
    rawParent
      .orElse({
        None
      })
      .flatMap(parentStr =>
        parentStr.toLong_! match
          case Success(parent) if parent > 0 => Option(parent)
          case _                             =>
            report.addError(invalidNumberMsg(rowNum, "Parent_Index"))
            None
      )

  private def validateTypeId(rawAssetType: Option[String], rowNum: Long, report: TaskReport): Option[AssetTypeId] =
    rawAssetType
      .orElse({
        report.addError(missingFieldMsg(rowNum, "Type"))
        None
      })
      .flatMap(assetTypeStr =>
        // convert from spreadsheet value to real asset type if available or else use
        // whatever was in the spreadsheet
        val typeId: AssetTypeId = humanReadableAssetTypes
          .getOrElse(assetTypeStr, AssetTypeId.withName(assetTypeStr))

        // has to be a supported type
        if assetTypeRelationshipConstraints.keys.toSeq.contains(typeId)
          && AssetType.types.contains(typeId)
        then

          Some(typeId)
        else
          report.addError(invalidAssetTypeMsg(rowNum, assetTypeStr))
          None
      )

  private def validateTitle(title: Option[String], rowNum: Long, report: TaskReport): Option[String] =
    title.orElse({
      report.addError(missingFieldMsg(rowNum, "Title"))
      None
    })

  private def emptyCsvMsg(): BundleMessage =
    assetMessage("import.csv.empty")

  private def missingHeaderMsg(header: String): BundleMessage =
    assetMessage("import.csv.missingHeader", header)

  private def missingFieldMsg(rowNum: Long, fieldName: String): BundleMessage =
    assetMessage("import.csv.missingField", fieldName, rowNum.toString)

  private def invalidNumberMsg(rowNum: Long, fieldName: String): BundleMessage =
    assetMessage("import.csv.invalidNumberField", fieldName, rowNum.toString)

  private def invalidAssetTypeMsg(rowNum: Long, assetTypeStr: String): BundleMessage =
    assetMessage("import.csv.invalidAssetType", assetTypeStr, rowNum.toString)

  private def duplicateIndicesMsg(index: Long): BundleMessage =
    assetMessage("import.csv.duplicateIndices", index.toString)

  private def missingParentMsg(parent: Long): BundleMessage =
    assetMessage("import.csv.missingParent", parent.toString)

  private def missingDefaultUsageGroup(typeId: AssetTypeId): BundleMessage =
    assetMessage("import.csv.missingDefaultUsageGroup", typeId.entryName)

  private def invalidRelationship(parentType: AssetTypeId, childType: AssetTypeId, childId: Long): BundleMessage =
    assetMessage("import.csv.invalidRelationship", parentType.entryName, childType.entryName, childId.toString)

  private def requiredParent(typeId: AssetTypeId): BundleMessage =
    assetMessage("import.csv.requiredParent", typeId.entryName)

  private def assetMessage(key: String, args: String*): BundleMessage =
    AuthoringBundle.message(key, args)
end CompetencyCsvImportServiceImpl

object CompetencyCsvImportServiceImpl:

  // allowable CSV headers
  private val headers = Set(
    "Index",
    "Parent_Index",
    "Type",
    "Title",
    "Description",
    "Keywords",
  )

  private val assetTypeRelationshipConstraints = Map[AssetTypeId, CompetencyCsvImportRelationshipConstraint](
    AssetTypeId.CompetencySet    -> CompetencyCsvImportRelationshipConstraint(
      Some(Group.Level1Competencies),
      Some(AssetTypeId.Level1Competency)
    ),
    AssetTypeId.Level1Competency -> CompetencyCsvImportRelationshipConstraint(
      Some(Group.Level2Competencies),
      Some(AssetTypeId.Level2Competency)
    ),
    AssetTypeId.Level2Competency -> CompetencyCsvImportRelationshipConstraint(
      Some(Group.Level3Competencies),
      Some(AssetTypeId.Level3Competency)
    ),
    AssetTypeId.Level3Competency -> CompetencyCsvImportRelationshipConstraint(None, None)
  )

  // human readable asset types used in the CSV
  private val humanReadableAssetTypes = Map(
    "Set"                -> AssetTypeId.CompetencySet,
    "Competency"         -> AssetTypeId.Level1Competency,
    "Sub-Competency"     -> AssetTypeId.Level2Competency,
    "Sub-Sub-Competency" -> AssetTypeId.Level3Competency
  )

  private val competencyTypes = Seq(
    AssetTypeId.Level1Competency,
    AssetTypeId.Level2Competency,
    AssetTypeId.Level3Competency
  )
end CompetencyCsvImportServiceImpl

// container for usage data
case class CompetencyCsvImportUsages(
  group: Group,
  children: Seq[Long]
)

case class CompetencyCsvImportRelationshipConstraint(
  usageGroup: Option[Group],
  allowedChildType: Option[AssetTypeId]
)
