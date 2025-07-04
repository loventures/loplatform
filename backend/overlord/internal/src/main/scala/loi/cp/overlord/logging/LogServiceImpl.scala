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

package loi.cp.overlord
package logging

import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.zip.{ZipEntry, ZipOutputStream}

import org.apache.pekko.actor.{ActorSystem, Props}
import org.apache.pekko.cluster.pubsub.DistributedPubSub
import org.apache.pekko.util.Timeout
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.actor.ActorRefOps.*
import com.learningobjects.cpxp.scala.util.Misc.*
import com.learningobjects.cpxp.service.attachment.AttachmentService
import com.learningobjects.cpxp.service.upgrade.UpgradeService
import scala.util.Using
import com.learningobjects.cpxp.util.*
import org.apache.commons.io.IOUtils

import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}

object LogServiceImpl:

  /** The format for date-based log file names */
  val dateFmt = new SimpleDateFormat("yyyyMMddHHmmss")

  /** How long to wait for logs */
  implicit val timeout: Timeout = Timeout(45.seconds)

@Service
private class LogServiceImpl(
  as: AttachmentService,
  implicit val ec: () => ExecutionContext,
  system: ActorSystem,
  us: UpgradeService
) extends LogService:
  import LogServiceImpl.*

  override def getLogBlobByGuid(guid: String): Future[FileInfo] =
    val (node, time) = (GuidUtil.getErrorHost(guid), GuidUtil.getErrorTime(guid))

    DistributedPubSub
      .get(system)
      .mediator
      .askFor[RemoteLogsActor.LogPath](RemoteLogsActor.GetLogs(new Date(time), Some(node))) map {
      case RemoteLogsActor.LogPath(_, path) =>
        val outfile = as.getTemporaryBlob(path)
        outfile.setContentType(MimeUtils.MIME_TYPE_APPLICATION_X_GZIP)
        outfile.setDisposition(HttpUtils.getDisposition(HttpUtils.DISPOSITION_ATTACHMENT, s"$guid.log.gz"))
        outfile

    }
  end getLogBlobByGuid

  override def getLogBlobByAge(age: Long): Future[FileInfo] =
    val whence = DateUtils.delta(-age.days.toMillis)
    val prefix = s"${dateFmt `format` whence}_logs"

    val expectedNodes = us.findRecentHosts.size

    val logGetter = system.actorOf(Props(new LocalLogsActor(expectedNodes)))

    logGetter.askFor[LocalLogsActor.Logs](LocalLogsActor.AskForLogs(whence, timeout.duration - 1.second)) map {
      case LocalLogsActor.Logs(logs) =>
        val outfile = LocalFileInfo.tempFileInfo()
        Using.resource(new ZipOutputStream(new FileOutputStream(outfile.getFile))) { zos =>
          logs foreach { case (host, path) =>
            zos.putNextEntry(new ZipEntry(s"$prefix/$host.log.gz"))
            ManagedUtils.perform(() =>
              Using.resource(as.getTemporaryBlob(path).openInputStream()) { in =>
                IOUtils.copy(in, zos)
              }
            )
            zos.closeEntry()
          }
        }

        outfile.setContentType(MimeUtils.MIME_TYPE_APPLICATION_ZIP)
        outfile.setDisposition(HttpUtils.getDisposition(HttpUtils.DISPOSITION_ATTACHMENT, s"$prefix.zip"))

        outfile
    }
  end getLogBlobByAge
end LogServiceImpl
