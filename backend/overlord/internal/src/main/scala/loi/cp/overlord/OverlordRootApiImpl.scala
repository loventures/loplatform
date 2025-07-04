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

import java.util.concurrent.TimeUnit
import java.util.logging.*

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import com.google.common.cache.{Cache, CacheBuilder}
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQueries, ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.FileResponse
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.exception.ValidationException
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query.QueryService
import com.learningobjects.cpxp.service.trash.{TrashConstants, TrashRecordFacade, TrashService}
import com.learningobjects.cpxp.util.cache.settings.SystemSettingsCache
import com.learningobjects.cpxp.util.{FileInfo, GuidUtil}
import loi.cp.limiter.IpBannedFilter
import loi.cp.overlord.logging.*
import loi.cp.user.UserComponent
import scaloi.syntax.AnyOps.*

import scala.jdk.CollectionConverters.*
import scala.concurrent.{ExecutionContext, Future}

object OverlordRootApiImpl:
  val logger                         = Logger.getLogger(classOf[OverlordRootApiImpl].getName)
  val files: Cache[String, FileInfo] = CacheBuilder
    .newBuilder()
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .removalListener[String, FileInfo] { removal =>
      if removal.wasEvicted() then removal.getValue.asInstanceOf[FileInfo].deref()
    }
    .build[String, FileInfo]()

@Component
class OverlordRootApiImpl(val componentInstance: ComponentInstance)(implicit
  ec: ExecutionContext,
  fs: FacadeService,
  ls: LogService,
  qs: QueryService,
  ts: TrashService,
  ssc: SystemSettingsCache,
  system: ActorSystem,
  cs: ComponentService
) extends OverlordRootApi
    with ComponentImplementation:
  import OverlordRootApiImpl.*

  override def logsByGuid(guid: String): Future[String] =
    ls.getLogBlobByGuid(guid) map { fi =>
      GuidUtil.shortGuid() <| (fileId => files.put(fileId, fi))
    }

  override def logsByAge(age: Long): Future[String] =
    ls.getLogBlobByAge(age) map { fi =>
      GuidUtil.shortGuid() <| (fileId => files.put(fileId, fi))
    }

  override def logLevelAlter(level: AlterLevel): Unit =
    if level.allNodes then
      DistributedPubSub.get(system).mediator !
        DistributedPubSubMediator.Publish(RemoteLogsActor.topic, level)
    else
      RemoteLogsActor.instance foreach { actor =>
        actor ! level
      }

  override def retrieveLogs(fileId: String): Option[FileResponse[?]] =
    Option(files.getIfPresent(fileId)).map { file =>
      files.invalidate(fileId)
      FileResponse(file)
    }

  override def getBannedIps(): ApiQueryResults[String] =
    val bannedIps = IpBannedFilter.getBannedIps
    new ApiQueryResults(bannedIps.asJava, bannedIps.size.toLong, bannedIps.size.toLong)

  override def banIp(ip: String): Unit =
    if !IpBannedFilter.addBannedIp(ip) then
      throw new ValidationException("ip", ip, s"Cannot parse IP or CIDR from '$ip'.")

  override def unbanIp(ip: String): Unit =
    IpBannedFilter.removeBannedIp(ip)

  override def getTrashRecords(query: ApiQuery): ApiQueryResults[TrashRecordComponent] =
    ApiQueries.query[TrashRecordComponent](qs.queryAllDomains(TrashConstants.ITEM_TYPE_TRASH_RECORD), query)

  override def restoreTrashRecord(id: Long): Unit =
    id.facade_?[TrashRecordFacade].foreach(t => ts.restore(t.getTrashId))
end OverlordRootApiImpl

@Component
class TrashRecord(
  val componentInstance: ComponentInstance,
  self: TrashRecordFacade
)(implicit cs: ComponentService)
    extends TrashRecordComponent
    with ComponentImplementation:
  override def getId = self.getId

  override def getTrashId = self.getTrashId

  override def getCreated = self.getCreated

  override def getCreatorId = self.getCreator.getId

  override def getCreator = self.getCreator.component[UserComponent]
end TrashRecord
