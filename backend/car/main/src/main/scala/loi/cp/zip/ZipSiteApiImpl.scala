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

package loi.cp.zip

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.{ApiQueries, ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.attachment.AttachmentWebService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.name.NameService
import scalaz.*
import scalaz.std.list.*
import scalaz.std.option.*
import scalaz.std.string.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.syntax.BooleanOps.*

import java.util.zip.{ZipEntry, ZipFile}
import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*
import scala.util.Using

@Component
class ZipSiteApiImpl(val componentInstance: ComponentInstance)(implicit
  aws: AttachmentWebService,
  fs: FacadeService,
  ns: NameService,
  cs: ComponentService
) extends ZipSiteApi
    with ComponentImplementation:

  import ErrorResponse.*
  import ZipSiteApi.*
  import ZipSiteApiImpl.*

  def getZipSite(id: Long): Option[ZipSite] =
    id.component_?[ZipSite]

  def queryZipSites(aq: ApiQuery): ApiQueryResults[ZipSite] =
    ApiQueries.query[ZipSite](parent.querySites(), aq)

  def createZipSite(init: ZipSiteInit): ErrorResponse \/ ZipSite =
    val checker = new Checks[Id.Id]
    for
      name   <- checker.validateName(init.name)
      path   <- checker.validatePath(init.path, None)
      prefix <- checker.validateZip(init.site)
    yield
      val newSite    = parent.addSite()
      newSite.setName(name)
      newSite.setUrl(path)
      val attachment = aws.createAttachment(newSite.getId, init.site)
      attachment.facade[ZipAttachmentFacade].setPathPrefix(prefix)
      newSite.setActiveAttachment(attachment)
      newSite.setIdentifier(classOf[ZipSite].getName)
      newSite.setDisabled(Boolean box init.disabled.getOrElse(false))
      newSite.component[ZipSite]
    end for
  end createZipSite

  def updateZipSite(id: Long, update: ZipSiteUpdate): ErrorResponse \/ ZipSite =
    val checker = new Checks[Option]
    for
      name     <- checker.validateName(update.name)
      path     <- checker.validatePath(update.path, Some(id))
      prefix   <- checker.validateZip(update.site)
      revision <- checker.validateRevision(update.revision, id)
      _        <- (update.site.isDefined && revision.isDefined) `thenLeft` badRequest
      zs       <- id.facade_?[ZipSiteFacade] \/> notFound
    yield
      name foreach zs.setName
      path foreach zs.setUrl
      update.site.zip(prefix) foreach { case (ui, pfx) =>
        val attachment = aws.createAttachment(zs.getId, ui)
        attachment.facade[ZipAttachmentFacade].setPathPrefix(pfx)
        zs.setActiveAttachment(attachment)
      }
      revision foreach (revId => zs.setActiveAttachment(revId)) // η-expansion loses to boxing, sad
      update.disabled foreach (disabled => zs.setDisabled(disabled))
      zs.component[ZipSite]
    end for
  end updateZipSite

  def deleteZipSite(id: Long): ErrorResponse \/ NoContentResponse =
    for zs <- id.facade_?[ZipSiteFacade] \/> notFound
    yield
      zs.delete()
      NoContentResponse

  private def parent = ZipSiteFolder.facade[ZipSiteParentFacade]
end ZipSiteApiImpl

object ZipSiteApiImpl:
  final val logger        = org.log4s.getLogger
  final val ZipSiteFolder = "folder-zipSites"

  import ErrorResponse.*
  import ZipSite.*

  private final class Checks[F[_]: Traverse]:
    type Res[T] = ErrorResponse \/ F[T]

    def validateZip(fui: F[UploadInfo]): Res[String] =
      fui.traverseU(ui =>
        \/.attempt {
          Using.resource(new ZipFile(ui.getFile)) { zip =>
            // Compute the longest common directory prefix of the zip entries
            commonRoot(zip.entries.asScala.toList).orZero
          }
        } { e =>
          logger.warn(e)("Zip error")
          unacceptable
        }
      )

    def validateName(fname: F[String]): Res[String] =
      fname
        .map(_.trim)
        .traverseU(name =>
          name.isEmpty
            .thenLeft {
              validationError(NameProperty, name)("should not be empty")
            }
            .as(name)
        )

    def validatePath(fpath: F[String], existing: Option[Long])(implicit ns: NameService): Res[String] =
      fpath
        .map(_.trim)
        .traverseU(path =>
          for
            _       <- path.startsWith("/") elseLeft {
                         validationError(PathProperty, path)("should start with a /")
                       }
            _       <- path.endsWith("/") thenLeft {
                         validationError(PathProperty, path)("should not end with a /")
                       }
            pathItem = ns.getItemId(path)
            _       <- (pathItem eq null) || (existing contains pathItem) `elseLeft` conflict
          yield path
        )

    def validateRevision(frev: F[Long], owner: Long)(implicit fs: FacadeService): Res[Long] =
      frev.traverseU(revId =>
        for
          site  <- owner.facade_?[ZipSiteFacade].toRightDisjunction(notFound)
          child <- Option(site.getAttachment(revId)).toRightDisjunction(notFound)
        yield Long.unbox(child.getId)
      )
  end Checks

  /** Returns the common directory prefix for a list of zip entries. For `a/b/c` and `a/d/e` will return `a/`.
    */
  def commonRoot(entries: List[ZipEntry]): Option[String] =
    @tailrec def pfx(s: String, t: String, i: Int = 0): String =
      if (i >= s.length) || (i >= t.length) || (s.charAt(i) != t.charAt(i)) then s.take(i) else pfx(s, t, i + 1)
    entries.foldMapLeft1Opt(_.getDirectory) { (prefix, entry) => pfx(prefix, entry.getDirectory) }

  implicit class ZipEntryOps(val self: ZipEntry) extends AnyVal:
    def getDirectory: String = self.getName.take(1 + self.getName.lastIndexOf('/'))
end ZipSiteApiImpl
