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

import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.query.{ApiQueries, ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.announcement.AnnouncementFinder
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.overlord.OverlordWebService
import com.learningobjects.cpxp.service.query.{BaseCondition, Comparison, QueryService}
import com.learningobjects.cpxp.service.user.{UserDTO, UserFacade}
import scaloi.misc.TimeSource
import scaloi.syntax.option.*

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

/** Service that acts as a helper for the Announcement Crud Web Api and enables internal announcement crud operations.
  */
@Service
class AnnouncementServiceImpl(implicit
  domain: () => DomainDTO,
  qs: QueryService,
  fs: FacadeService,
  ows: OverlordWebService,
  ts: TimeSource,
  user: () => UserDTO
) extends AnnouncementService:
  override def get(parent: Id, apiQuery: ApiQuery): ApiQueryResults[Announcement] =
    ApiQueries.query[Announcement](parent.queryChildren[AnnouncementFinder], apiQuery)

  override def get(parent: Id, id: Long): Option[Announcement] = get(parent, ApiQuery.byId(id)).asOption

  override def create(parent: Id, announcement: AnnouncementDTO): Announcement =
    val annDTO = AnnouncementDTO(
      announcement.startTime,
      announcement.endTime,
      announcement.message,
      announcement.style,
      announcement.active
    )
    parent.addComponent[Announcement](classOf[AnnouncementImpl], annDTO)

  /** Query announcements that are active, have started, haven't ended, and have not been hidden by the current user.
    */
  override def getActive(apiQuery: ApiQuery, contexts: List[Long] = Nil): ApiQueryResults[Announcement] =
    // TODO: for query caching this could be a simple shared query that's then filter
    val now                 = ts.date
    val activeQuery         = qs.queryAllDomains(AnnouncementDataModel.itemType)
    val parents             = domain.id :: ows.findOverlordDomainId :: contexts
    activeQuery.addCondition(BaseCondition.inIterable(DataTypes.META_DATA_TYPE_PARENT_ID, parents))
    val history             = user.facade[UserFacade].getHistory.toScala
    val hiddenAnnouncements = history.mapNonNull(_.getHiddenAnnouncements).getOrElse(List())
    activeQuery.addCondition(DataTypes.META_DATA_TYPE_ID, Comparison.notIn, hiddenAnnouncements.asJavaCollection)
    activeQuery.addCondition(AnnouncementFinder.DATA_TYPE_ANNOUNCEMENT_ACTIVE, Comparison.eq, true)
    activeQuery.addCondition(AnnouncementFinder.DATA_TYPE_ANNOUNCEMENT_START_TIME, Comparison.le, now)
    activeQuery.addCondition(AnnouncementFinder.DATA_TYPE_ANNOUNCEMENT_END_TIME, Comparison.gt, now)
    ApiQueries.query[Announcement](activeQuery, apiQuery)
  end getActive

  override def hide(annId: Long): Unit =
    val history             = user.facade[UserFacade].getOrCreateHistory
    val hiddenAnnouncements = Option(history.getHiddenAnnouncements).getOrElse(List())
    history.setHiddenAnnouncements(Long.box(annId) :: hiddenAnnouncements.take(MaxHidden - 1))

  override def update(oldAnnouncement: Announcement, announcement: AnnouncementDTO): Announcement =
    oldAnnouncement.update(announcement)
    oldAnnouncement

  override def delete(announcement: Announcement): Unit =
    announcement.delete()

  private final val MaxHidden = 20
end AnnouncementServiceImpl
