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

package loi.cp.scorm.impl

import com.google.common.cache.CacheBuilder
import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.cpxp.PK
import com.learningobjects.cpxp.scala.cpxp.PK.ops.*
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.group.GroupConstants.ID_FOLDER_COURSE_OFFERINGS
import com.learningobjects.cpxp.service.query.{Comparison, Direction}
import com.learningobjects.cpxp.util.{EntityContext, StringUtils}
import com.learningobjects.cpxp.web.ExportFile
import kantan.csv.*
import kantan.csv.ops.*
import loi.authoring.web.ExceptionResponses
import loi.cp.content.CourseContentService
import loi.cp.course.CourseFolderFacade
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.scorm.*
import loi.cp.scorm.impl.ScormExportServiceImpl.ZipOutputStreamOps
import scala.util.Using
import scalaz.\/
import scalaz.std.option.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.misc.TryInstances.tryInstance
import scaloi.syntax.set.*
import scaloi.syntax.ʈry.*

import java.io.{FileOutputStream, OutputStream}
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.zip.ZipOutputStream

@Component
class ScormPackageWebControllerImpl(val componentInstance: ComponentInstance)(implicit
  componentService: ComponentService,
  contentService: CourseContentService,
  facadeService: FacadeService,
  scormExportService: ScormExportService,
  scormPackageService: ScormPackageService,
) extends ScormPackageWebController
    with ComponentImplementation:
  import ScormPackageWebController.*
  import ScormPackageWebControllerImpl.*

  override def generateScormPackage(request: ScormPackageRequest): ErrorResponse \/ String =
    for
      oﬀering <- get(request.offeringId) \/> ErrorResponse.notFound
      system  <- request.systemId.component_?[ScormSystem] \/> ErrorResponse.badRequest("invalid system")
    yield scormPackageService.add(oﬀering, system, request.scormFormat).packageId

  override def downloadScormPackage(id: String): ErrorResponse \/ WebResponse =
    for
      рackage  <- scormPackageService.find(id) \/> ErrorResponse.notFound
      oﬀering  <- get(рackage.parent) \/> ErrorResponse.serverError("bad offering")
      contents <- contentService.getCourseContents(oﬀering) \/> ExceptionResponses.exceptionResponse
    yield FileResponse(
      scormExportService.exportScorm(
        oﬀering,
        contents,
        рackage.packageId,
        ScormFormat.withName(рackage.format)
      )
    )

  override def generateScormPackages(
    request: BatchScormPackageRequest,
  ): String =
    val system     = request.systemId.component_![ScormSystem].get // so, so much guilt and shame. guild, shame and disgust.
    val date       = Instant.now.toString.substring(0, 10)
    val exportFile = new ExportFile(s"Scorm Packages ${date}.zip", MediaType.ZIP)
    exportFiles.put(exportFile.guid, exportFile)
    Using.resource(new ZipOutputStream(new FileOutputStream(exportFile.file))) { zos =>
      exportScormPackages(request, system, zos)
    }
    exportFile.guid
  end generateScormPackages

  override def downloadScormPackages(guid: String): ErrorResponse \/ WebResponse =
    for exportFile <- Option(exportFiles.getIfPresent(guid)) \/> ErrorResponse.notFound
    yield FileResponse(exportFile.toFileInfo)

  private def exportScormPackages(
    request: BatchScormPackageRequest,
    system: ScormSystem,
    zos: ZipOutputStream
  ): Unit =
    val results = request.productCodes.toSet `mapTo` exportProductScormPackage(request.scormFormat, system, zos)
    zos.putEntry("Export Log.csv", exportLog(results, _))

  private def exportProductScormPackage(
    format: ScormFormat,
    system: ScormSystem,
    zos: ZipOutputStream
  )(productCode: String): ExportResult =
    val oﬀerings = findOﬀerings(productCode)
    val success  = oﬀerings.headOption traverse { oﬀering =>
      for contents <- contentService.getCourseContents(oﬀering)
      yield
        val packageId = scormPackageService.add(oﬀering, system, format).packageId
        val fileName  = s"${productCode}_${oﬀering.getName}.zip"
        zos.putEntry(
          fileName,
          scormExportService.exportScorm(oﬀering, contents, packageId, format, _)
        )
        ExportInfo(oﬀering.getName, oﬀering.getGroupId, packageId, fileName)
    }
    EntityContext.flushClearAndCommit()
    success.fold(
      _ => ExportResult("FAIL", "Error extracting course structure"),
      success =>
        success.cata(
          info =>
            if oﬀerings.size > 1 then ExportResult("PASS", "Multiple offerings found", info.some)
            else ExportResult("OK", "", info.some),
          ExportResult("FAIL", "No matching course offering found")
        )
    )
  end exportProductScormPackage

  private def exportLog(results: Map[String, ExportResult], os: OutputStream): Unit =
    val csv = os.asCsvWriter[List[String]](rfc)
    csv.write(List("Product Code", "Success", "Detail", "Offering Name", "Offering Id", "Package Id", "Filename"))
    results foreach {
      case (productCode, ExportResult(success, detail, None)) =>
        csv.write(List(productCode, success, detail))
      case (
            productCode,
            ExportResult(success, detail, Some(ExportInfo(offeringName, offeringId, packageId, fileName)))
          ) =>
        csv.write(List(productCode, success, detail, offeringName, offeringId, packageId, fileName))
    }
    csv.close()
  end exportLog

  // TODO: Project Metadata so not this...
  private def findOﬀerings(productCode: String) =
    offeringsFolder.queryGroups
      .addCondition(
        "name",
        Comparison.like,
        "%" + StringUtils.escapeSqlLike(productCode) + "%",
      )
      .setOrder("createTime", Direction.DESC_NULLS_LAST)
      .getComponents[LightweightCourse]

  private def offeringsFolder: CourseFolderFacade =
    ID_FOLDER_COURSE_OFFERINGS.facade[CourseFolderFacade]

  private def get[T: PK](id: T): Option[LightweightCourse] = id.pk.component_?[LightweightCourse]
end ScormPackageWebControllerImpl

object ScormPackageWebControllerImpl:
  // Super dodgy lookup for export files. Ordinarily we would bind them to the user's session but an asynchronous
  // function can't access the session. We have sticky appservers so the user should stay here.
  private val exportFiles = CacheBuilder.newBuilder.expireAfterWrite(15, TimeUnit.MINUTES).build[String, ExportFile]()

  private final case class ExportResult(
    success: String,
    detail: String,
    info: Option[ExportInfo] = None
  )

  private final case class ExportInfo(
    offeringName: String,
    offeringId: String,
    packageId: String,
    fileName: String
  )
end ScormPackageWebControllerImpl
