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

package loi.cp.qna

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.ComponentInstance
import com.learningobjects.cpxp.component.annotation.{Component, Schema}
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.cpxp.Item.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.email.EmailService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.group.{GroupConstants, GroupFolderFacade}
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.qna.QnaQuestionFinder
import com.learningobjects.cpxp.service.query.{BaseCondition, Comparison, Direction, Function, Projection, QueryService}
import com.learningobjects.cpxp.util.{EntityContext, HtmlUtils, StringUtils}
import com.learningobjects.cpxp.web.ExportFile
import loi.asset.lesson.model.Lesson
import loi.asset.module.model.Module
import loi.cp.content.{CourseContentService, CourseContents}
import loi.cp.course.CourseSectionService
import loi.cp.group.SectionType
import loi.cp.job.*
import loi.cp.reference.EdgePath
import org.apache.commons.io.FileUtils
import scalaz.std.list.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import spoiwo.model.*
import spoiwo.natures.xlsx.Model2XlsxConversions.*

import java.lang
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}
import java.util.{Calendar, Date}
import scala.collection.mutable
import scala.util.Using

@Schema("qnaReportJob")
trait QnaReportJob extends EmailJob[QnaReportJob]:
  @JsonProperty
  def getStartTime: Date
  def setStartTime(time: Date): Unit

  @JsonProperty
  def getEndTime: Date
  def setEndTime(time: Date): Unit

  @JsonProperty
  def getSectionIdPrefix: String
  def setSectionIdPrefix(sectionIdsPrefix: String): Unit
end QnaReportJob

@Component(name = "Q&A Report")
final class QnaReportJobImpl(implicit
  val self: EmailJobFacade,
  val es: EmailService,
  val fs: FacadeService,
  val componentInstance: ComponentInstance,
  domain: DomainDTO,
  is: ItemService,
  qs: QueryService,
  csService: CourseSectionService,
  ccService: CourseContentService,
  qnaService: QnaService,
) extends AbstractEmailJob[QnaReportJob]
    with QnaReportJob:

  val logger = org.log4s.getLogger

  override def update(job: QnaReportJob): QnaReportJob =
    setStartTime(job.getStartTime)
    setEndTime(job.getEndTime)
    setSectionIdPrefix(job.getSectionIdPrefix)
    super.update(job)

  override def getStartTime: Date =
    self.getAttribute("startTime", classOf[Date])

  override def setStartTime(time: Date): Unit =
    self.setAttribute("startTime", time)

  override def getEndTime: Date =
    self.getAttribute("endTime", classOf[Date])

  override def setEndTime(time: Date): Unit =
    self.setAttribute("endTime", time)

  override def getSectionIdPrefix: String =
    self.getAttribute("sectionIdPrefix", classOf[String])

  override def setSectionIdPrefix(sectionIdPrefix: String): Unit =
    self.setAttribute("sectionIdPrefix", sectionIdPrefix)

  /** Generate the report to be emailed out. */
  override protected def generateReport(): GeneratedReport =
    val prefixOpt  = Option(getSectionIdPrefix)
    val startOpt   = Option(getStartTime)
    val endOpt     = Option(getEndTime)
    val sectionIds = prefixOpt map { prefix =>
      val sectionFolder = SectionType.Sections.folderName.facade[GroupFolderFacade]
      sectionFolder
        .queryGroups()
        .addCondition(
          GroupConstants.DATA_TYPE_GROUP_ID,
          Comparison.like,
          StringUtils.escapeSqlLike(prefix.toLowerCase) + "%",
          Function.LOWER
        )
        .setProjection(Projection.ID)
        .getValues[lang.Long]
    }

    val monthAgo = Calendar.getInstance()
    monthAgo.add(Calendar.MONTH, -1)

    // default to open (i.e. no instructor response) or created within the last month
    val defaultCondition = (startOpt.isEmpty && endOpt.isEmpty) ?? List(
      BaseCondition.getInstance(QnaQuestionFinder.Open, Comparison.eq, true),
      BaseCondition.getInstance(QnaQuestionFinder.Created, Comparison.ge, monthAgo.getTime)
    )

    val questionIds = domain
      .queryAll[QnaQuestionFinder]
      .addConjunction(sectionIds.map(ids => BaseCondition.inIterable(QnaQuestionFinder.Section, ids)))
      .addConjunction(startOpt.map(start => BaseCondition.getInstance(QnaQuestionFinder.Created, Comparison.ge, start)))
      .addConjunction(endOpt.map(end => BaseCondition.getInstance(QnaQuestionFinder.Created, Comparison.lt, end)))
      .addDisjunction(defaultCondition)
      .setOrder(QnaQuestionFinder.Created, Direction.DESC)
      .setLimit(5000)
      .setProjection(Projection.ID)
      .getValues[lang.Long]

    val reportDate = dateFormatter.format(Instant.now())

    val out = new ExportFile(s"QnA ${prefixOpt.cata(_ + " ", "")}$reportDate.xlsx", MediaType.OOXML_SHEET)

    val contentsCache = mutable.Map.empty[Long, Option[CourseContents]]

    val rows = mutable.ListBuffer.empty[Row]

    rows += Row(style = CellStyle(font = Font(bold = true))).withCellValues(
      "Question #" :: "Section Id" :: "Section Name" :: "Module" :: "Lesson" :: "Content" :: "State" ::
        "Category" :: "Subcategory" :: "Date" :: "Sender" :: "Learner" :: "Message" :: "Attachment" :: Nil
    )

    questionIds.zipWithIndex foreach { case (qid, index) =>
      val question  = qid.finder[QnaQuestionFinder]
      val contents  = contentsCache.getOrElseUpdate(
        question.section.id,
        csService
          .getCourseSection(question.section.id)
          .flatMap(section => ccService.getCourseContents(section.lwc).toOption)
      )
      val content   = contents.flatMap(_.get(EdgePath.parse(question.edgePath)))
      val ancestors = for
        cc      <- contents.toList
        c       <- content.toList
        path    <- c.edgeNames.reverse.tails
        content <- cc.get(EdgePath(path.reverse))
      yield content
      val module    = ancestors.find(_.asset.is[Module])
      val lesson    = ancestors.find(_.asset.is[Lesson])
      qnaService.getMessages(Seq(question)).zipWithIndex foreach {
        case (message, 0) =>
          rows += Row.Empty.withCellValues(
            question.id ::
              question.section.groupId ::
              question.section.name ::
              module.cata(_.title, "") ::
              lesson.cata(_.title, "") ::
              content.cata(_.title, "") ::
              (if question.open then "Open" else if question.closed then "Closed" else "Pending") ::
              (Option(question.category) | "") ::
              (Option(question.subcategory) | "") ::
              dateFormatter.format(message.created.toInstant) ::
              s"${message.creator.givenName} ${message.creator.familyName}" ::
              (message.creator == question.creator) ::
              HtmlUtils.toPlaintext(message.message).take(32767) ::
              message.attachments.nonEmpty ::
              Nil
          )
        case (message, _) =>
          rows += Row.Empty.withCellValues(
            "" ::
              "" ::
              "" ::
              "" ::
              "" ::
              "" ::
              "" ::
              "" ::
              "" ::
              dateFormatter.format(message.created.toInstant) ::
              s"${message.creator.givenName} ${message.creator.familyName}" ::
              (message.creator == question.creator) ::
              HtmlUtils.toPlaintext(message.message).take(32767) ::
              message.attachments.nonEmpty ::
              Nil
          )
      }
      if index % 100 == 99 then EntityContext.flushClearAndCommit()
    }

    def chars(width: Int): Width = new Width(width, WidthUnit.Character)

    val wrap = CellStyle(wrapText = true)
    val pk   = CellStyle(dataFormat = CellDataFormat("#,###"))

    val reportSheet = Sheet(name = "Q&A Report")
      .withFreezePane(FreezePane(0, 1, 1, 1))
      .withRows(rows)
      .withColumns(
        Column(width = chars(14), style = pk) ::     // id
          Column(width = chars(14)) ::               // section id
          Column(width = chars(28), style = wrap) :: // section name
          Column(width = chars(28), style = wrap) :: // module
          Column(width = chars(28), style = wrap) :: // lesson
          Column(width = chars(28), style = wrap) :: // content
          Column(width = chars(12)) ::               // state
          Column(width = chars(20), style = wrap) :: // category
          Column(width = chars(28), style = wrap) :: // subcategory
          Column(width = chars(14), style = wrap) :: // date
          Column(width = chars(18)) ::               // sender
          Column(width = chars(8)) ::                // learner
          Column(width = chars(96), style = wrap) :: // message
          Column(width = chars(10)) ::               // attachment
          Nil
      )

    Using.resource(FileUtils.openOutputStream(out.file)) { fos =>
      Workbook(reportSheet).writeToOutputStream(fos)
    }

    val fromStr = startOpt.map(_.toInstant).map(dateFormatter.format)
    val toStr   = endOpt.map(_.toInstant).map(dateFormatter.format)

    GeneratedReport(
      s"Q&A Report for ${prefixOpt.cata(_ + " on ", "")}$reportDate",
      s"Attached is the Q&A report${fromStr.cata(" from " + _, "")}${toStr.cata(" to " + _, "")}.",
      html = false,
      Seq(out.toUploadInfo)
    )
  end generateReport

  val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM-dd-yyyy h:mm:ss a z").withZone(ZoneId.of(domain.timeZone))
end QnaReportJobImpl
