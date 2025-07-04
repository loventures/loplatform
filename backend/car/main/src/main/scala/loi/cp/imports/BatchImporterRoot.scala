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

import argonaut.*
import argonaut.Argonaut.*
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults, ApiQuerySupport}
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.component.web.{ArgoBody, HttpResponse, NoContentResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentSupport}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.component.misc.BatchConstants.*
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query.{BaseOrder, Direction, QueryBuilder}
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.{HttpUtils, MimeUtils}
import fs2.*
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import loi.cp.admin.FolderParentFacade
import loi.cp.imports.errors.*
import loi.cp.web.HttpResponseEntity
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.util.EntityUtils
import scalaz.\/
import scalaz.syntax.either.*

import java.io.{File, FileOutputStream}
import java.lang as jl
import scala.util.Using

@Component
class BatchImporterRoot(val componentInstance: ComponentInstance)(implicit fs: FacadeService, user: UserDTO)
    extends BatchImporterRootComponent
    with ComponentImplementation:

  import loi.cp.imports.BatchImporterRoot.*

  private implicit val batchRestEncode: EncodeJson[BatchRest] =
    EncodeJson((b: BatchRest) =>
      ("identifier" := b.identifier) ->:
        ("callbackUrl" :=? b.callbackUrl) ->?:
        ("batch"    := Json.array(b.batch*)) ->: jEmptyObject
    )

  override def getBatch(batchId: jl.Long): Option[ImportComponent] =
    batchFolder.getImport(batchId)

  override def getBatches(q: ApiQuery): ApiQueryResults[? <: ImportComponent] =
    ApiQuerySupport.query(queryImports, q, classOf[ImportComponent])

  override def submitBatch(batch: BatchRest, req: HttpServletRequest): ImportComponent =

    val deserialized = batch.batch.map(
      _.as[ImportItem].fold(
        (err, _) => DeserializeError(err).widen.left[ImportItem],
        importItem => importItem.right[GenericError]
      )
    )

    /*
     * In practice, there could be multiple import types in the payload. Because we are forced to pick just one,
     * let's pick the last one since an enrollment import may be preceded by user and course section imports.
     */
    val importType = deserialized.lastOption.flatMap(_.toOption.map(_.getClass.getName))

    val items: Stream[Pure, GenericError \/ ImportItem] =
      Stream.emits(deserialized)
    val importCoordinator                               =
      ComponentSupport.lookupService(classOf[ImportCoordinator])

    val importIdentifier = Option(batch.identifier).filter(_.trim.nonEmpty)

    val result = importCoordinator
      .importStream(items, importType, user, importIdentifier) { importResult =>
        importResult foreach { imp => batch.callbackUrl foreach sendComplete(imp, 1) }
      }

    val tempFile = File.createTempFile("batchImport", ".json")
    Using.resource(new FileOutputStream(tempFile)) { fos => fos.write(batch.asJson.nospaces.getBytes("UTF-8")) }
    val upload   = new UploadInfo(
      "batchImport.json",
      MimeUtils.MIME_TYPE_APPLICATION_JSON + MimeUtils.CHARSET_SUFFIX_UTF_8,
      tempFile,
      true
    )
    result.setImportFileFromUpload(upload)

    result
  end submitBatch

  def importItem(invoker: UserDTO, item: ImportItem): GenericError \/ ImportSuccess =
    val importer = ComponentSupport
      .lookup(classOf[Importer[ImportItem]], item.getClass)
    for
      validated <- importer.validate(item).widenl
      persisted <- importer.execute(invoker, validated)
    yield persisted

  def sendComplete(batch: ImportComponent, attempt: Int)(url: String): Unit =
    if attempt > callbackRetryLimit then return
    \/.attempt { // TODO: I should be a bus message to get this for free
      logger.info(s"sending response callback to: $url, attempt #$attempt")
      val json = JacksonUtils.getMapper.writeValueAsString(batch)
      val post = new HttpPost(url)
      post `setEntity` new StringEntity(json, ContentType.create(MimeUtils.MIME_TYPE_APPLICATION_JSON, "UTF-8"))
      post `setConfig` RequestConfig
        .custom()
        .setSocketTimeout(5000)
        .setConnectTimeout(5000)
        .setConnectionRequestTimeout(5000)
        .build()

      val response     = HttpUtils.getHttpClient.execute(post)
      val status       = response.getStatusLine.getStatusCode
      val responseText = EntityUtils.toString(response.getEntity, "UTF-8")
      logger.info(s"got response $status: $responseText")
      if status != 200 then
        Thread.sleep(callbackRetryInterval)
        sendComplete(batch, attempt + 1)(url)
    } { t =>
      logger.info(s"failed to POST to: $url, caused by: ${ExceptionUtils.getStackTrace(t)}")
      Thread.sleep(callbackRetryInterval)
      sendComplete(batch, attempt + 1)(url)
    }
  end sendComplete

  override def submitSingle(batchItem: ArgoBody[ImportItem], req: HttpServletRequest): HttpResponse =
    importItem(user, batchItem.decode_!.get).fold(
      err => new HttpResponseEntity(HttpServletResponse.SC_BAD_REQUEST, err),
      u => NoContentResponse
    )

  override def queryImports: QueryBuilder =
    val query = batchFolder.queryImports
    query.addOrder(BaseOrder.byData(DATA_TYPE_BATCH_CREATE_TIME, Direction.DESC))
    query
end BatchImporterRoot

object BatchImporterRoot:
  private val logger = org.log4s.getLogger

  val callbackRetryLimit: Int     = 5
  val callbackRetryInterval: Long = 5000 // ms

  def batchFolder(implicit fs: FacadeService): ImportParentFacade =
    val folder = Current.getDomain
      .facade[FolderParentFacade]
      .findFolderByType(FOLDER_TYPE_BATCHES)
    folder.facade[ImportParentFacade]
end BatchImporterRoot
