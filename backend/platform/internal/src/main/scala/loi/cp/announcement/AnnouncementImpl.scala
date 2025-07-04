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

package loi.cp.announcement

import java.util.Date

import org.apache.pekko.actor.ActorSystem
import scala.annotation.nowarn
import com.learningobjects.cpxp.component.annotation.{Component, PostCreate}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.announcement.AnnouncementFinder
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.query.QueryService
import loi.cp.appevent.{AppEventService, OnEvent}
import loi.cp.presence.PresenceActor.DeliverMessage
import loi.cp.presence.SessionsActor.DomainMessage
import loi.cp.presence.*
import scaloi.misc.TimeSource

/** Implementation of the Announcement Trait.
  */
@Component
class AnnouncementImpl(val componentInstance: ComponentInstance, self: AnnouncementFinder)(implicit
  domain: () => DomainDTO,
  is: ItemService,
  qs: QueryService,
  appEvtService: AppEventService,
  actorSystem: ActorSystem,
  ts: TimeSource
) extends Announcement
    with ComponentImplementation:

  @PostCreate
  private def init(announcement: AnnouncementDTO): Unit =
    update(announcement)

  override def getId = componentInstance.getId

  override def getStartTime: Date = self.startTime

  override def getEndTime: Date = self.endTime

  override def getMessage: String = self.message

  override def getStyle: String = self.style

  override def isActive: Boolean = self.active

  override def update(announcement: AnnouncementDTO): Unit =
    self.startTime = announcement.startTime
    self.endTime = announcement.endTime
    self.message = announcement.message
    self.style = announcement.style
    self.active = announcement.active
    schedule()

  override def delete(): Unit = is.delete(self)

  /** Invoked by the app event framework. */
  @OnEvent
  private def onAnnouncement(@nowarn event: AnnouncementEvent): Option[Date] =
    if self.active && ts.date.before(self.endTime) then
      val annEvent = AnnouncementStart(self.id, self.startTime, self.endTime, self.message, self.style)
      ClusterBroadcaster.broadcast(DomainMessage(Option(domain.id), DeliverMessage(annEvent)))
      Some(self.endTime)
    else
      val annEvent = AnnouncementEnd(self.id())
      ClusterBroadcaster.broadcast(DomainMessage(Option(domain.id), DeliverMessage(annEvent)))
      None

  /** Schedule or un-schedule an app event for this announcement. */
  private def schedule(): Unit =
    appEvtService.deleteEvents(this, this, classOf[AnnouncementEvent])
    // We will not send a notification to active students for non-domain announcements,
    // we will instead rely on them logging in and polling for active announcements.
    if self.active && self.parent.getId == domain.id then
      appEvtService.scheduleEvent(self.startTime, this, this, new AnnouncementEvent)
end AnnouncementImpl
