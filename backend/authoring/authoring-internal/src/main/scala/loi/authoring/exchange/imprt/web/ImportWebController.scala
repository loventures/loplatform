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

package loi.authoring.exchange.imprt.web

import cats.syntax.either.*
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.github.tototoshi.csv.CSVReader
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.exception.HttpApiException.*
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.{GuidUtil, HtmlUtils, TempFileMap}
import com.learningobjects.de.authorization.Secured
import com.learningobjects.de.task.{FiniteTaskReport, TaskReport, UnboundedTaskReport}
import com.learningobjects.de.web.UncheckedMessageException
import loi.asset.competency.service.CompetencyService
import loi.asset.contentpart.HtmlPart
import loi.asset.question.*
import loi.authoring.blob.exception.IllegalBlobName
import loi.authoring.blob.{BlobRef, BlobService}
import loi.authoring.branch.Branch
import loi.authoring.edge.Group
import loi.authoring.exchange.docx.DocxErrors.{DocxValidationError, DocxValidationWarning}
import loi.authoring.exchange.docx.LoqiImportService
import loi.authoring.exchange.imprt.*
import loi.authoring.exchange.imprt.competency.*
import loi.authoring.exchange.imprt.exception.FatalQtiImportException
import loi.authoring.exchange.imprt.imscc.CommonCartridgeImporter
import loi.authoring.exchange.imprt.openstax.OpenStaxZipImportService
import loi.authoring.exchange.imprt.qti.QtiImporter
import loi.authoring.exchange.model.ExchangeManifest
import loi.authoring.project.{AccessRestriction, BaseProjectService, CreateProjectDto, ProjectType}
import loi.authoring.security.right.{AccessAuthoringAppRight, EditContentAnyProjectRight}
import loi.authoring.web.AuthoringWebUtils
import loi.authoring.write.*
import loi.authoring.write.web.WriteRequest
import loi.cp.asset.exchange.impl.AssetExchangeRequestStatus.Success
import loi.cp.i18n.AuthoringBundle
import loi.cp.i18n.syntax.bundleMessage.*
import loi.cp.user.UserService
import org.apache.commons.text.StringEscapeUtils
import org.log4s.Logger
import scalaz.\/
import scalaz.syntax.std.boolean.*
import scaloi.syntax.boolean.*
import scaloi.syntax.ʈry.*

import java.nio.file.Files
import java.util.UUID
import scala.collection.mutable
import scala.io.Source
import scala.util.{Try, Using}

@Component
@Secured(Array(classOf[AccessAuthoringAppRight]))
@Controller(value = "authoringImport", root = true)
class ImportWebController(
  ci: ComponentInstance,
  importService: ImportService,
  user: => UserDTO,
  userService: UserService,
  blobService: BlobService,
  webUtils: AuthoringWebUtils,
  commonCartridgeImportService: CommonCartridgeImporter,
  openStaxZipImportService: OpenStaxZipImportService,
  qtiImporter: QtiImporter,
  file2BlobService: File2BlobService,
  competencyCsvImportService: CompetencyCsvImportService,
  loqiImportService: LoqiImportService,
  layeredWriteService: LayeredWriteService,
  projectService: BaseProjectService,
  competencyService: CompetencyService,
  domainDto: => DomainDTO,
) extends BaseComponent(ci)
    with ApiRootComponent:
  import ImportWebController.*

  @RequestMapping(path = "authoring/{branchId}/import/loqi/validate", method = Method.POST)
  @Secured(Array(classOf[AccessAuthoringAppRight]))
  def validateDocxAssessmentQuestions(
    @RequestBody file: UploadInfo,
    @PathVariable("branchId") branchId: Long,
    @QueryParam(required = true) assessmentName: UUID,
  ): DocxValidationResponse =
    val ws = webUtils.writeWorkspaceOrThrow404(branchId) // TODO: Why write?

    loqiImportService
      .importLoqiAssessment(ws, assessmentName, file.getFile)
      .fold(
        es => DocxValidationResponse(Nil, Nil, es),
        { case (warnings, ops) =>
          ws match
            case lws: LayeredWriteWorkspace => layeredWriteService.validate(lws, ops).valueOr(_.bundleMsg.throw400)

          DocxValidationResponse(ops.map(WriteRequest.from), warnings, Nil)
        }
      )
  end validateDocxAssessmentQuestions

  @RequestMapping(path = "authoring/{branchId}/import/miqi/validate", method = Method.POST)
  @Secured(Array(classOf[AccessAuthoringAppRight]))
  def validateMiqiAssessmentQuestions(
    @RequestBody file: UploadInfo,
    @PathVariable("branchId") branchId: Long,
    @QueryParam(required = true) assessmentName: UUID,
  ): DocxValidationResponse =
    val ws = webUtils.writeWorkspaceOrThrow404(branchId) // TODO: Why write?

    val ops = Using.resource(Source.fromFile(file.getFile, "UTF-8")) { source =>
      val ops     = mutable.Buffer.empty[WriteOp]
      val lines   = source.getLines()
      val first   = lines.next()
      if !first.contains("<item-collection") then throw new Exception("Missing item collection")
      var leading = lines.next()
      while !leading.contains("</item-collection") do
        if !leading.startsWith("Item:") then throw new Exception("Missing leading item")
        val question    = lines.takeWhile(s => !s.contains("</question>")) // drops the trailing </question>
        val qtype       = question.next()
        if !qtype.startsWith("<question") then throw new Exception(s"Not a question: $qtype")
        val start       = lines.next()
        if !start.matches("^\\d+\\..*") then throw new Exception(s"Not a number: $start")
        val bits        = question.toIndexedSeq.filterNot(_.isBlank)
        val description = mutable.Buffer.empty[String]
        description.append(start.replaceFirst("^\\d+\\.\\s*", ""))
        var index       = 0
        while bits(index).startsWith(" ") || bits(index).isBlank do
          val x = bits(index).trim()
          if x.nonEmpty then description.append(x)
          index = index + 1
        val content     = description.map(s => s"<p>${StringEscapeUtils.escapeHtml4(s)}</p>").mkString("\n")
        if qtype.contains("""type="MC"""") then
          /*
          90. Which of the following refers to encoded page data that describes the appearance of the printed page in a way in which the printer can understand?
              More stuff.
          a. Integrated Print Server
          b. Printer Maintenance Kit
          c. Page Description Language
          d. Elevated Command Prompt
          ANS: c, d
          rejoinder1: Incorrect.
          rejoinder2: Incorrect.
          rejoinder3: Correct.
          rejoinder4: Incorrect.
           */
          var choices = mutable.Buffer.empty[ChoiceContent]
          while bits(index).matches("^[a-z]\\. .*") do
            val distractor = bits(index).replaceFirst("^[a-z]\\.\\s*", "")
            choices.append(
              ChoiceContent(
                choiceContent = Some(HtmlPart(StringEscapeUtils.escapeHtml4(distractor))),
                index = choices.size,
                correct = false,
              )
            )
            index = index + 1
          end while
          val ans     = bits(index)
          if !ans.startsWith("ANS: ") then throw new Exception(s"Not an answer: $ans")
          val rights  = ans.substring(5).trim.split(",\\s*")
          rights foreach { letter =>
            val i = letter.charAt(0) - 'a'
            choices = choices.map(c => if c.index == i then c.copy(correct = true, points = 1.0 / rights.length) else c)
          }
          choices = choices map { c =>
            val rejoinder = bits(index + 1 + c.index.intValue)
            if !rejoinder.startsWith(s"rejoinder${c.index + 1}: ") then
              throw new Exception(s"Not a rejoinder: $rejoinder")
            val text      = HtmlPart(StringEscapeUtils.escapeHtml4(rejoinder.replaceAll("^rejoinder\\d+:\\s*", "")))
            c.copy(
              correctChoiceFeedback = c.correct.option(text),
              incorrectChoiceFeedback = c.correct.noption(text),
            )
          }
          val addNode =
            if rights.length == 1 then
              AddNode(
                MultipleChoiceQuestion(
                  questionContent = ChoiceQuestionContent(
                    questionComplexText = HtmlPart(content),
                    choices = choices.toList
                  ),
                  title = HtmlUtils.toPlaintext(content).take(255)
                )
              )
            else
              AddNode(
                MultipleSelectQuestion(
                  questionContent = ChoiceQuestionContent(
                    questionComplexText = HtmlPart(content),
                    choices = choices.toList
                  ),
                  title = HtmlUtils.toPlaintext(content).take(255)
                )
              )
          val addEdge = AddEdge(
            sourceName = assessmentName,
            targetName = addNode.name,
            group = Group.Questions,
            position = Some(Position.End)
          )
          ops.append(addNode)
          ops.append(addEdge)
        else if qtype.contains("""type="true-false"""") then
          /*
          24. A system's performance is automatically doubled when you use two microprocessors.
          T
          Incorrect.
          F
          Correct.
           */
          if bits.size != index + 4 then throw new Exception("Not four TF rows")
          if bits(index) != "T" then throw new Exception(s"Not T: ${bits(index)}")
          if bits(index + 2) != "F" then throw new Exception(s"Not F: ${bits(index)}")
          val truth   = bits(index + 1) == "Correct."
          if truth && bits(index + 3) != "Incorrect." then throw new Exception(s"Not Incorrect: ${bits(index + 3)}")
          if !truth && bits(index + 1) != "Incorrect." then throw new Exception(s"Not Incorrect: ${bits(index + 1)}")
          if !truth && bits(index + 3) != "Correct." then throw new Exception(s"Not Correct: ${bits(index + 3)}")
          val addNode = AddNode(
            TrueFalseQuestion(
              questionContent = ChoiceQuestionContent(
                questionComplexText = HtmlPart(content),
                choices = List(
                  ChoiceContent(
                    choiceContent = Some(HtmlPart("true")),
                    index = 0,
                    correct = truth,
                    points = if truth then 1 else 0,
                    correctChoiceFeedback = truth.option(HtmlPart("Correct.")),
                    incorrectChoiceFeedback = truth.noption(HtmlPart("Incorrect.")),
                  ),
                  ChoiceContent(
                    choiceContent = Some(HtmlPart("false")),
                    index = 1,
                    correct = !truth,
                    points = if truth then 0 else 1,
                    correctChoiceFeedback = truth.noption(HtmlPart("Correct.")),
                    incorrectChoiceFeedback = truth.option(HtmlPart("Incorrect.")),
                  )
                )
              ),
              title = HtmlUtils.toPlaintext(content).take(255)
            )
          )
          val addEdge = AddEdge(
            sourceName = assessmentName,
            targetName = addNode.name,
            group = Group.Questions,
            position = Some(Position.End)
          )
          ops.append(addNode)
          ops.append(addEdge)
        else throw new Exception(s"Unknown type: $qtype")
        end if

        val blank = lines.next()
        if !blank.isBlank then throw new Exception("Missing trailing blank")
        leading = lines.next()
      end while
      ops.toList.map(WriteRequest.from)
    }

    DocxValidationResponse(ops, Nil, Nil)
  end validateMiqiAssessmentQuestions

  /** For importing LOAF zips
    */
  @RequestMapping(path = "authoring/{branchId}/imports", method = Method.POST)
  def importAssetLoaf(
    @RequestBody webDto: LoafImportRequest,
    @PathVariable("branchId") branchId: Long
  ): ImportReceiptsResponse =
    val target =
      webUtils.branchOrFakeBranchOrThrow404(branchId, AccessRestriction.projectOwnerOr[EditContentAnyProjectRight])
    bakeLoaf(target, webDto, defer = true)

  private def bakeLoaf(
    target: Branch,
    webDto: LoafImportRequest,
    defer: Boolean,
  ): ImportReceiptsResponse =
    blobService.blobExists(webDto.source).elseFailure(throw unprocessableEntity(ImportError.ImportBlobRequired))
    blobService
      .validateBlobData(
        webDto.source,
        // LOAF imports may have old blob names
        Some(webDto.source.name)
      )
      .mapExceptions({ case f: UncheckedMessageException => unprocessableEntity(f.getErrorMessage) })
      .get
    val dataJson = mapper.valueToTree[JsonNode](ImportReceiptData(webDto.description, target.id))
    val dto      = ConvertedImportDto(target, webDto.description, dataJson, webDto.source)
    val receipt  =
      if defer then importService.deferImport(dto, None)
      else importService.doImport(dto)
    ImportReceiptsResponse(receipt, user)
  end bakeLoaf

  @RequestMapping(path = "authoring/importProject", method = Method.POST, async = true)
  def importProjectLoaf(
    @RequestBody webDto: ProjectLoafImportRequest
  ): ImportReceiptsResponse =

    val rootlessProject = projectService
      .insertProject(
        CreateProjectDto(
          webDto.projectName,
          ProjectType.Course,
          user.id,
          webDto.code,
          webDto.productType,
          webDto.category,
          webDto.subCategory,
          webDto.revision,
          webDto.launchDate,
          webDto.liveVersion,
          webDto.s3,
          layered = true,
          webDto.projectStatus,
          webDto.courseStatus,
        ),
        domainDto.id,
        None,
        None
      )
      .valueOr(errors => ImportError.OtherError(errors.list.toList.map(_.value).mkString(", ")).throw422)

    val response = bakeLoaf(rootlessProject, LoafImportRequest(webDto.projectName, webDto.source), defer = false)

    if response.receipts.exists(_.status != Success) then projectService.deleteProject(rootlessProject.requireProject)

    response
  end importProjectLoaf

  /** Get all the receipts by limit/offset
    */
  @RequestMapping(path = "authoring/imports", method = Method.GET)
  def getImportReceipts(
    @QueryParam limit: Int,
    @QueryParam offset: Int
  ): ImportReceiptsResponse =
    val receipts = importService.loadImportReceipts(limit, offset)
    val users    = userService.getUsers(receipts.flatMap(_.createdBy))
    ImportReceiptsResponse(receipts, users)

  /** Download link for successful imports
    */
  @RequestMapping(path = "authoring/imports/{id}/package", method = Method.GET)
  def serveImportFile(@PathVariable("id") id: Long): WebResponse =

    val receipt = importReceiptOrThrow404(id)

    receipt.source match
      case Some(blobRef) => FileResponse(blobService.ref2Info(blobRef))
      case None          => throw unprocessableEntity(ImportError.PackageNotAvailable)

  /** Fetches an importreceipt by id
    */
  @RequestMapping(path = "authoring/imports/{id}", method = Method.GET)
  def getImportReceipt(@PathVariable("id") id: Long): ImportReceiptsResponse =
    val receipt = importReceiptOrThrow404(id)
    val users   = userService.getUsers(receipt.createdBy)
    ImportReceiptsResponse(Seq(receipt), users)

  @RequestMapping(path = "authoring/imports/{id}", method = Method.DELETE)
  def deleteImportReceipt(@PathVariable("id") id: Long): Unit =
    val receipt = importReceiptOrThrow404(id)
    importService.deleteImportReceipt(receipt)

  /** Step 1 of 2 QTI import process: Validate the original source and send back a preview of the LOAF. QTI import
    * process is unique in that we preserve the LOAF file being written during validation, so we can already get a
    * blobRef out of that and save it to the new import receipt.
    */
  @RequestMapping(path = "authoring/{branchId}/import/qti/validate", method = Method.POST)
  def validateQti(
    @RequestBody req: QtiValidateRequest,
    @PathVariable("branchId") branchId: Long
  ): ImportPreviewResponse =

    val unconvertedSource  = req.unconvertedSource
    val parentReport       = createParentReport(ThirdPartyImportType.Qti, unconvertedSource.filename)
    blobService.blobExists(unconvertedSource).elseFailure(throw unprocessableEntity(ImportError.ImportBlobRequired))
    blobService
      .validateBlobData(unconvertedSource, None)
      .mapExceptions({ case f: IllegalBlobName => unprocessableEntity(f) })
      .get
    val ws                 = webUtils.workspaceOrThrow404(branchId)
    val competenciesByName = competencyService.getCompetenciesByName(ws)

    // destination file for LOAF that validation will create
    // once created, the file is sent to the blobstore
    val convertedPath   = ImporterUtils.createTempFilePath(unconvertedSource.filename)
    val unconvertedFile = file2BlobService.ref2TempFile(unconvertedSource, ".zip")
    val taskReport      = ImporterUtils.createValidatingReport
    try
      val result   = qtiImporter.validateAndWriteExchangeZip(
        unconvertedFile,
        convertedPath,
        req.`type`,
        taskReport,
        competenciesByName
      )
      val response = if !result.taskReport.hasErrors then
        val convertedFile   = convertedPath.toFile
        val convertedSource = file2BlobService.putBlob(convertedFile).valueOr(e => throw e)
        recordValidation(
          unconvertedSource,
          Some(convertedSource),
          ThirdPartyImportType.Qti,
          branchId,
          result,
          parentReport.copy
        )
      else ImportPreviewResponse(None, None, taskReport)
      Files.deleteIfExists(unconvertedFile.toPath)
      Files.deleteIfExists(convertedPath)
      response
      // still too much goto exception handling, so have to do this until we can refactor it all out.
    catch
      case _: Exception =>
        Files.deleteIfExists(unconvertedFile.toPath)
        Files.deleteIfExists(convertedPath)
        ImportPreviewResponse(None, None, taskReport)
    end try
  end validateQti

  /** Step 2 of 2 QTI import process: Attempts to retrieve the LOAF file made in step 1 from the blobstore and apply any
    * changes.
    */
  @RequestMapping(path = "authoring/{branchId}/import/qti", method = Method.POST)
  def importQti(
    @RequestBody importDto: QtiImportRequest,
    @PathVariable("branchId") branchId: Long
  ): ImportReceiptsResponse =
    val receipt         = importReceiptOrThrow404(importDto.receiptId)
    // The LOAF source from validation step
    val convertedSource = receipt.source.getOrElse(throw unprocessableEntity(ImportError.ImportBlobRequired))
    // Make the source a file, which will be mutated in `qtiImporter.overwriteExchangeZip`
    val convertedFile   = file2BlobService.ref2TempFile(convertedSource, ".zip")

    // that random pile of JSON we put on the receipt entity
    val data = importDto match
      case dto: AssessmentQuestions =>
        QtiImportReceiptData(dto.description, branchId, dto.assessmentType, dto.assessmentTitle)
      case dto: PlainQuestions      => ImportReceiptData(dto.description, branchId)

    val overwrittenManifest = importDto match
      case dto: AssessmentQuestions =>
        Try(qtiImporter.overwriteExchangeZip(convertedFile.toPath, dto.assessmentType, dto.assessmentTitle))
          .mapExceptions({ case e: FatalQtiImportException =>
            logger.error(e.getMessage)
            unprocessableEntity(e.getErrorMessage)
          })
          .tapFailure(_ => Files.deleteIfExists(convertedFile.toPath))
          .get
      case _                        => None

    val loafSource = overwrittenManifest
      .map(_ => file2BlobService.putBlob(convertedFile).getOrElse(convertedSource))
      .getOrElse(convertedSource)
    Files.deleteIfExists(convertedFile.toPath)

    deferNonLoafImport(data, branchId, loafSource, receipt)
  end importQti

  /** Step 1 of 2 Common Cartridge import process: Validate the original source and send back a preview of the LOAF.
    */
  @RequestMapping(path = "authoring/{branchId}/import/imscc/validate", method = Method.POST)
  def validateCommonCartridge(
    @RequestBody unconvertedSource: BlobRef,
    @PathVariable("branchId") branchId: Long
  ): ImportPreviewResponse =
    blobService.blobExists(unconvertedSource).elseFailure(throw unprocessableEntity(ImportError.ImportBlobRequired))
    blobService
      .validateBlobData(unconvertedSource, None)
      .mapExceptions({ case e: UncheckedMessageException => unprocessableEntity(e) })
      .get
    webUtils.branchOrFakeBranchOrThrow404(branchId)
    Using.resources(new TempFileMap("imscc", ".tmp"), blobService.ref2Stream(unconvertedSource)) {
      (temp, unconvertedStream) =>
        val parentReport = createParentReport(ThirdPartyImportType.CommonCartridge, unconvertedSource.filename)
        val taskReport   = ImporterUtils.createValidatingReport
        try
          val result = commonCartridgeImportService
            .validateZip(temp, unconvertedStream, taskReport = taskReport)
          if result.taskReport.hasErrors then ImportPreviewResponse(None, None, result.taskReport)
          else
            recordValidation(
              unconvertedSource,
              convertedSource = None,
              ThirdPartyImportType.CommonCartridge,
              branchId,
              result,
              parentReport
            )
          end if
          // still too much goto exception handling, so have to do this until we can refactor it all out.
        catch case _: Exception => ImportPreviewResponse(None, None, taskReport)
        end try
    }
  end validateCommonCartridge

  /** Step 2 of 2 Common Cartridge import process: Bake the original dough into a LOAF.
    */
  @RequestMapping(path = "authoring/{branchId}/import/imscc", method = Method.POST)
  def importCommonCartridge(
    @PathVariable("branchId") branchId: Long,
    @RequestBody importDto: CcOsImportRequest
  ): Throwable \/ ImportReceiptsResponse =
    val receipt           = importReceiptOrThrow404(importDto.receiptId)
    val unconvertedSource =
      receipt.unconvertedSource.getOrElse(throw unprocessableEntity(ImportError.ImportBlobRequired))
    val unconvertedFile   = file2BlobService.ref2TempFile(unconvertedSource, ".zip")
    // Make a temp path for the future LOAF file
    val convertedPath     = ImporterUtils.createTempFilePath(unconvertedSource.filename)
    commonCartridgeImportService.importZip(unconvertedFile, convertedPath)
    val data              = ImportReceiptData(importDto.description, branchId)
    Files.deleteIfExists(unconvertedFile.toPath)
    val convertedSource   = file2BlobService.putBlob(convertedPath.toFile)
    Files.deleteIfExists(convertedPath)
    convertedSource.map(b => deferNonLoafImport(data, branchId, b, receipt)).toDisjunction
  end importCommonCartridge

  /** Step 1 of 2 of OpenStax import process: validate the original source and send back a preview of the LOAF.
    */
  @RequestMapping(path = "authoring/{branchId}/import/openstax/validate", method = Method.POST)
  def validateOpenStax(
    @RequestBody unconvertedSource: BlobRef,
    @PathVariable("branchId") branchId: Long
  ): ImportPreviewResponse =
    blobService
      .validateBlobData(unconvertedSource, None)
      .mapExceptions({ case ex: IllegalBlobName => unprocessableEntity(ex) })
      .get
    webUtils.branchOrFakeBranchOrThrow404(branchId)
    Using.resources(
      new TempFileMap(s"openstax_${unconvertedSource.filename}_${GuidUtil.shortGuid()}", ".tmp"),
      blobService.ref2Stream(unconvertedSource)
    ) { (tempMap, unconvertedStream) =>
      val parentReport     = createParentReport(ThirdPartyImportType.OpenStax, unconvertedSource.filename)
      val validatingReport = ImporterUtils.createValidatingReport
      try
        val result = openStaxZipImportService.validateZip(tempMap, unconvertedStream, validatingReport)
        if result.taskReport.hasErrors then ImportPreviewResponse(None, None, result.taskReport)
        else recordValidation(unconvertedSource, None, ThirdPartyImportType.OpenStax, branchId, result, parentReport)
        // still too much goto exception handling, so have to do this until we can refactor it all out.
      catch case _: Exception => ImportPreviewResponse(None, None, validatingReport)
    }
  end validateOpenStax

  /** Step 2 of 2 for OpenStax import process: Bake the original dough into a LOAF.
    */
  @RequestMapping(path = "authoring/{branchId}/import/openstax", method = Method.POST)
  def importOpenStax(
    @PathVariable("branchId") branchId: Long,
    @RequestBody importDto: CcOsImportRequest
  ): Throwable \/ ImportReceiptsResponse =
    val receipt           = importReceiptOrThrow404(importDto.receiptId)
    val unconvertedSource =
      receipt.unconvertedSource.getOrElse(throw unprocessableEntity(ImportError.ImportBlobRequired))
    val unconvertedFile   = file2BlobService.ref2TempFile(unconvertedSource, ".zip")
    // Make a temp path for the future LOAF file
    val convertedPath     = ImporterUtils.createTempFilePath(unconvertedSource.filename)
    // Convert the original file and place it in the loafPath
    openStaxZipImportService.importZip(unconvertedFile, convertedPath)
    val data              = ImportReceiptData(importDto.description, branchId)
    Files.deleteIfExists(unconvertedFile.toPath)
    val convertedSource   = file2BlobService.putBlob(convertedPath.toFile)
    Files.deleteIfExists(convertedPath)
    convertedSource.map(b => deferNonLoafImport(data, branchId, b, receipt)).toDisjunction
  end importOpenStax

  @RequestMapping(path = "authoring/import/csv/template", method = Method.GET)
  def downloadTemplate(): WebResponse =
    FileResponse.resource(this, "template.csv")

  // It would be nice to use InputStream instead of temp file
  @RequestMapping(path = "authoring/import/csv/competency", method = Method.POST)
  def generateCompetencyImport(
    @RequestBody source: BlobRef
  ): CompetencyCsvImportResponse =
    blobService
      .validateBlobData(source, exemptBlobName = None)
      .mapExceptions({ case f: UncheckedMessageException => unprocessableEntity(f) })
      .get
    val file                               = file2BlobService.ref2TempFile(source, ".zip")
    val csvRows: List[Map[String, String]] = CSVReader.open(file).allWithHeaders()

    val report: TaskReport                           = new FiniteTaskReport("Competency CSV Import", csvRows.length)
    val rowsOpt: Option[Seq[CompetencyCsvImportRow]] = competencyCsvImportService.parseCsv(csvRows, report)

    rowsOpt match
      case Some(rows) =>
        val manifest: ExchangeManifest        = competencyCsvImportService.generateManifest(rows)
        val assetTypeCounts: Map[String, Int] = rows
          .groupBy(_.typeId.entryName)
          .view
          .mapValues(_.length)
          .toMap
        CompetencyCsvImportResponse(report, Some(manifest), Some(assetTypeCounts))
      case None       =>
        CompetencyCsvImportResponse(report, None, None)
    end match
  end generateCompetencyImport

  private def importReceiptOrThrow404(id: Long): ImportReceipt =
    importService
      .loadImportReceipt(id)
      .getOrElse(
        throw notFound(AuthoringBundle.message("import.noSuchReceipt", long2Long(id)))
      )

  private def recordValidation(
    unconvertedSource: BlobRef,
    convertedSource: Option[BlobRef],
    importType: ThirdPartyImportType,
    branchId: Long,
    validationResult: ExchangeReportDto,
    parentReport: TaskReport
  ): ImportPreviewResponse =
    parentReport.addChild(validationResult.taskReport)
    val dataDto       = ImportReceiptData(s"Validating ${unconvertedSource.filename}", branchId)
    val validationDto = ValidatedImportDto(
      unconvertedSource.filename, // the default on the frontend in DCM
      mapper.valueToTree[JsonNode](dataDto),
      parentReport,
      convertedSource,
      unconvertedSource,
      importType
    )
    // DB hit!!!
    val receipt       = importService.recordValidation(validationDto)
    ImportPreviewResponse(validationResult.manifest, Some(receipt.id), validationResult.taskReport)
  end recordValidation

  private def deferImport(
    dto: ConvertedImportDto,
    existingReceipt: Option[ImportReceipt]
  ): ImportReceiptsResponse =
    // DB hit!!!
    val receipt = importService.deferImport(dto, existingReceipt)
    val users   = userService.getUsers(receipt.createdBy)
    ImportReceiptsResponse(Seq(receipt), users)

  private def deferNonLoafImport(
    data: ReceiptData,
    branchId: Long,
    convertedSource: BlobRef,
    existingReceipt: ImportReceipt
  ): ImportReceiptsResponse =
    val target = webUtils.branchOrFakeBranchOrThrow404(branchId, AccessRestriction.none)
    val dto    = ConvertedImportDto(
      target,
      data.description,
      mapper.valueToTree[JsonNode](data),
      convertedSource
    )
    deferImport(dto, Some(existingReceipt))
  end deferNonLoafImport

  private def createParentReport(
    imprtType: ThirdPartyImportType,
    filename: String
  ): TaskReport =
    val parent = new UnboundedTaskReport(s"$imprtType import: $filename")
    parent.markStart()
    parent
end ImportWebController

object ImportWebController:
  private val mapper: ObjectMapper = JacksonUtils.getFinatraMapper
  private val logger: Logger       = org.log4s.getLogger

  private type Errs = Seq[DocxValidationError]

  case class DocxValidationResponse(writeOps: Seq[WriteRequest], warnings: Seq[DocxValidationWarning], errors: Errs)
