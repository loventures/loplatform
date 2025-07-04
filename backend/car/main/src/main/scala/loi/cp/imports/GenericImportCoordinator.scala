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

package loi.cp.imports

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.learningobjects.cpxp.component.{ComponentEnvironment, ComponentService, ComponentSupport, annotation}
import com.learningobjects.cpxp.operation.Operations
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.attachment.AttachmentWebService
import com.learningobjects.cpxp.service.component.misc.BatchConstants.*
import com.learningobjects.cpxp.service.domain.{DomainDTO, DomainWebService}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.task.Priority
import com.learningobjects.cpxp.util.{EntityContext, GuidUtil, InternationalizationUtils, ManagedUtils}
import fs2.*
import loi.cp.admin.FolderParentFacade
import loi.cp.imports.errors.{GenericError, PersistError}
import loi.cp.tx.DEIEIO
import scalaz.syntax.bind.*
import scalaz.syntax.std.either.*
import scalaz.{-\/, \/, \/-}

import java.lang.Long as JLong
import java.util.Date
import java.util.logging.{Level, Logger}
import scala.jdk.CollectionConverters.*

@annotation.Service
class GenericImportCoordinator(implicit
  aws: AttachmentWebService,
  env: ComponentEnvironment,
  cs: ComponentService,
  fs: FacadeService,
  domainWebService: DomainWebService,
  domain: () => DomainDTO,
) extends ImportCoordinator:
  import GenericImportCoordinator.*
  import Importer.*

  private val MaxErrorLimit = 5
  private val log           = Logger.getLogger(classOf[GenericImportCoordinator].getName)

  override def importStream(
    items: Stream[IO, GenericError \/ ImportItem],
    `type`: Option[String],
    invoker: UserDTO,
    identifier: Option[String],
    indexOffset: Option[Long] = None
  )(
    whenFinished: Throwable \/ ImportComponent => Unit
  ): ImportComponent =
    val total = countItems(items).compile.last.unsafeRunSync().get

    val importTask = createImport(identifier.getOrElse(GuidUtil.longGuid()))
    importTask.setStartTime(new Date())
    importTask.setTotal(total)
    importTask.setType(`type`)
    importTask.setStartedBy(Option(invoker))
    clearTx()

    val status = StreamStatusReport(ImportStatus.Progress, total)
    val task   = items
      .evalMap(i => IO.delay(i.flatMap(validate)))
      .evalMap(i => IO.delay(i.flatMap(execute(invoker))))
      .scan(status)((currentStatus: StreamStatusReport, result: GenericError \/ ImportSuccess) =>
        result.flatMap { success =>
          // make sure this success will actually pass a commit
          \/.attempt {
            ManagedUtils.commit()
            success
          }(toPersistError)
        } match
          case -\/(err)     =>
            rollback() // rollback if we encountered an error.
            importTask.addFailure(currentStatus.completed + indexOffset.getOrElse(0L))(err)
          case \/-(success) =>
            importTask.addSuccess(success)
            log.info(s"Success count: ${importTask.getSuccessCount} / ${importTask.getTotal}")
        end match
        clearTx() // clear transaction so we don't slow down
        whenFinished(\/-(importTask))
        currentStatus
          .copy(ImportStatus.Progress, total, currentStatus.completed + 1)
      )
    log.info(s"Kicking off import asynchronously, with process: $items")

    val io = DEIEIO.tx(task.compile.last.attempt)

    Operations.defer(
      () => handleImportResult(importTask)(io.unsafeRunSync().toDisjunction),
      Priority.High,
      s"GenericImportCoordinator(${importTask.getIdentifier}:${new Date().getTime}})"
    )
    importTask
  end importStream

  override def validateStream(
    items: Stream[IO, GenericError \/ ImportItem],
    onImport: Option[StreamStatusReport => Unit],
    indexOffset: Option[Long] = None
  ): Stream[IO, StreamStatusReport] =
    val setup = countItems(items).compile.lastOrError.flatMap(total =>
      IO.delay {
        val cd     = classOf[ImportErrorImpl].getComponentDescriptor
        val status = StreamStatusReport(ImportStatus.Progress, total)
        log.info(s"Kicking off validation, with process: $items")
        (total, cd, status)
      }
    )

    for
      (total, cd, status) <- Stream.eval(setup)
      report              <- items
                               .map(_.flatMap(validate))
                               .scan(status)((s, result: GenericError \/ Validated[ImportItem]) =>
                                 val errs: Seq[GenericErrorWrapper] =
                                   if s.errors.size >= MaxErrorLimit then Seq.empty
                                   else
                                     result.fold(
                                       err => Seq(GenericErrorWrapper(err, s.completed + indexOffset.getOrElse(0L))(using cd)),
                                       _ => Seq.empty
                                     )
                                 val errorCount                     = s.errorCount + result.fold(_ => 1, _ => 0)
                                 val status                         = s.copy(ImportStatus.Progress, total, s.completed + 1, errorCount, s.errors ++ errs)

                                 // noinspection ScalaUselessExpression
                                 onImport.foreach(_(status))

                                 status
                               )
    yield report
    end for
  end validateStream

  private def handleImportResult(importTask: ImportComponent)(result: Throwable \/ Option[StreamStatusReport]) =
    result match
      case -\/(t)      =>
        importTask.setStatus(ImportStatus.Failed)
        throw t
      case \/-(status) =>
        importTask.setEndTime(new Date())
        importTask.setStatus(ImportStatus.Finished)
        importTask

  def countItems[A](stream: Stream[IO, A]): Stream[IO, Long] =
    stream
      .map({ _ => 1L })
      .scan(0L)((count, total) => total + count)

  def validate[Item <: ImportItem]: Item => GenericError \/ Validated[Item] =
    item => \/.attempt(getImporterForItem(item).validate(item).widenl)(toPersistError).join

  def execute[Item <: ImportItem](
    invoker: UserDTO
  ): Validated[Item] => GenericError \/ ImportSuccess =
    validated => \/.attempt(getImporterForItem(validated.item).execute(invoker, validated).widenl)(toPersistError).join

  def getImporterForItem[Item <: ImportItem](item: Item): Importer[Item] =
    ComponentSupport.lookup(env, classOf[Importer[Item]], item.getClass)

  def createImport(id: String, cbUrl: Option[String] = Option.empty): ImportComponent =
    val importTask = importFolder.addImport()
    importTask.setCreateTime(new Date())
    importTask.setCallbackUrl(cbUrl)
    importTask.setIdentifier(id)
    importTask.setSuccessCount(JLong.valueOf(0))
    importTask.setFailureCount(JLong.valueOf(0))
    importTask.setStatus(ImportStatus.Progress)
    importTask

  override def getImportTypes: Map[ImportType, Importer[? <: ImportItem]] =
    (for
      importer   <- importers
      importType <- toImportType(importer)
    yield importType -> importer).toMap

  def importers = ComponentSupport.lookupAll(env, classOf[Importer[? <: ImportItem]]).asScala

  def toImportType(importer: Importer[?]): Option[ImportType] =
    val component = importer.getComponentInstance.getComponent
    val binding   = component.getAnnotation(classOf[ImportBinding])
    val message   = InternationalizationUtils.formatMessage(binding.label())
    if binding == null then Option.empty
    else Some(ImportType(binding.value(), message))

  def clearTx(): Unit =
    EntityContext.flushClearAndCommit()

  def rollback(): Unit = ManagedUtils.rollback()

  def toPersistError(t: Throwable): GenericError =
    log.log(Level.WARNING, "Import error", t)
    PersistError(s"An unknown error occurred: ${t.getMessage}")
end GenericImportCoordinator

object GenericImportCoordinator:

  def importFolder(implicit fs: FacadeService): ImportParentFacade =
    val folder = Current.getDomain
      .facade[FolderParentFacade]
      .findFolderByType(FOLDER_TYPE_BATCHES)
    folder.facade[ImportParentFacade]
