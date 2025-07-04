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

package loi.cp.message

import java.lang.Long as JLong
import java.util.Date

import com.learningobjects.cpxp.component.{ComponentEnvironment, ComponentService}
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.email.EmailService
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.EnrollmentType
import com.learningobjects.cpxp.service.enrollment.{EnrollmentConstants, EnrollmentWebService}
import com.learningobjects.cpxp.service.exception.ValidationException
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query.{BaseCondition, QueryService}
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.attachment.AttachmentComponent
import loi.cp.email.Email
import loi.cp.message.notification.MessageNotification
import loi.cp.notification.NotificationService
import loi.cp.reply.ReplyService
import loi.cp.user.UserComponent
import loi.cp.web.HandleService
import scaloi.syntax.any.*
import scaloi.syntax.set.*

import scala.jdk.CollectionConverters.*

@Service
class MessageServiceImpl(implicit
  fs: FacadeService,
  qs: QueryService,
  es: EmailService,
  ews: EnrollmentWebService,
  ns: NotificationService,
  rs: ReplyService,
  hs: HandleService,
  env: ComponentEnvironment,
  cs: ComponentService,
  user: () => UserDTO,
  domain: () => DomainDTO,
  date: () => Date
) extends MessageService:
  import MessageServiceImpl.*

  // TODO: messages should be delivered to recipients asynchronously...
  // to do that i think i would have to form the mime message from attachments rather than files.

  // This is all messy because we're trying to validate that the user is not trying
  // to add any new recipients they don't have an existing relationship with. If
  // we switched unilaterally to handles this would largely go away.

  // send a message or a reply
  override def sendMessage(message: NewMessage, parentId: Option[Long], emailCopy: Boolean): Message =
    val parent                   = parentId.map(_.facade[MessageStorageFacade])
    // find recipients of parent message
    val (xids, _, xcontexts)     = parent.fold(zeroRecipients) { parentStorage =>
      partitionRecipients(parentStorage.getRecipients, Set(parentStorage.getSender))
    }
    // partition recipients of new message
    val (ids, handles, contexts) = partitionRecipients(message.recipients)
    // validate recipients, ignoring recipients of the parent message
    val hids                     = expandHandles(handles)
    val selfId                   = user.id.longValue
    validateRecipients(ids -- xids -- hids - selfId, contexts -- xcontexts)

    // validate uploads
    val uploads = message.attachments
    validateUploads(uploads)

    // store message, expanding handles to PKs.
    val storage = storeMessage(message, (ids ++ hids).toSeq.map(UserRecipient.apply) ++ contexts, parent)
    uploads foreach attachUpload(storage)

    // deliver copy to recipients
    val userIds  = ids ++ hids ++ expandContextRecipients(contexts)
    val messages = userIds.mapTo(u => deliverMessage(storage, MessageLabel.Inbox)(u).getId.longValue)

    // deliver copy to myself
    val sent = deliverMessage(storage, MessageLabel.Sent)(selfId)

    // schedule emails
    if emailCopy then
      rs.scheduleEmail(
        user.component[UserComponent],
        classOf[MessageEmail],
        Email.Init(Some(sent.getId.longValue), None)
      )
    userIds.component[UserComponent] foreach { user =>
      // construct email info
      val init = Email.Init(messages.get(user.getId), None)
      // schedule an email to be sent
      rs.scheduleEmail(user, classOf[MessageEmail], init)
    }

    if messageRootConfig.sendMessageNotifications then
      ns.nοtify[MessageNotification](storage, MessageNotification.Init(storage, userIds.toSeq))

    // return sent mail
    sent.component[Message]
  end sendMessage

  import loi.cp.message.MessageRootApiImpl.Config

  // TODO: I am bad and I will be replaced by the generalized configuration framework
  private def messageRootConfig: Config =
    env.getJsonConfiguration(classOf[MessageRootApiImpl].getName, classOf[Config]).orElse(Config())

  // store the message
  def storeMessage(message: NewMessage, recipients: Seq[Recipient], inReplyTo: Option[MessageStorageFacade])(implicit
    fs: FacadeService
  ): MessageStorageFacade =
    storageParent.addMessageStorage() <| { storage =>
      // this requires the storage PK in order to set the thread id so does not operate
      // as a pre-insert facade add closure. we could use another thread id...
      storage.setSubject(message.subject)
      storage.setBody(message.body)
      storage.setContext(inReplyTo.fold(message.context)(_.getContext))
      storage.setRecipients(recipients) // this is a crappy data model.
      storage.setTimestamp(date())
      storage.setThread(inReplyTo.fold(storage.getId.longValue)(_.getThread))
      storage.setInReplyTo(inReplyTo.map(_.getId))
      storage.setSender(user.id)
    }

  def expandHandles(handles: Iterable[String]): Iterable[Long] =
    handles map { handle =>
      hs.unmask(handle) getOrElse {
        throw new ValidationException("UserRecipient#user", handle, "Not a valid user")
      }
    }

  // validate recipients
  def validateRecipients(userIds: Set[Long], contextRecipients: Set[ContextRecipient])(implicit
    ews: EnrollmentWebService
  ): Unit =
    // find my groups
    val myGroups = ews.getActiveUserGroupsQuery(user.getId).getValues[Long]
    // validate individuals
    validateUserRecipients(userIds, myGroups)
    // validate contexts
    validateContextRecipients(contextRecipients, myGroups.toSet)

  // validate that all individual users are members of one of your course
  def validateUserRecipients(userIds: Set[Long], myGroups: Seq[Long])(implicit ews: EnrollmentWebService): Unit =
    val matchedIds = usersInMyCourses(userIds, myGroups)
    // throw if the valid users and selected users are disjoint
    if userIds.size != matchedIds.size then
      throw new ValidationException("UserRecipient#user", (userIds diff matchedIds).mkString, "Not a valid user")

  def usersInMyCourses(userIds: Iterable[Long], myGroups: Iterable[Long])(implicit
    ews: EnrollmentWebService
  ): Set[Long] =
    // query for all enrollments for any of the selected users in any of my courses
    ews
      .getEnrollmentsQuery(EnrollmentType.ACTIVE_ONLY)
      .addCondition(BaseCondition.inIterable(DataTypes.META_DATA_TYPE_PARENT_ID, userIds.asJava))
      .addCondition(BaseCondition.inIterable(EnrollmentConstants.DATA_TYPE_ENROLLMENT_GROUP, myGroups.asJava))
      .setDataProjection(DataTypes.META_DATA_TYPE_PARENT_ID)
      .getValues[Long]
      .toSet

  // validate and expand all context recipients
  def validateContextRecipients(recipients: Iterable[ContextRecipient], myGroups: Set[Long]): Unit =
    recipients foreach { case ContextRecipient(contextId, roleId) =>
      if !myGroups.contains(contextId) then
        throw new ValidationException("ContextRecipient#context", contextId.toString, "Not a valid context")
    }

  // validate that no uploads exceed maximum attachment size
  def validateUploads(uploads: Seq[UploadInfo]): Unit =
    uploads filter {
      _.getSize > MaxAttachmentSize
    } foreach { upload =>
      throw new ValidationException("attachment", upload.getFileName, "Attachment too large")
    }

  // expand a list of recipients into a set of user pks
  def expandContextRecipients(recipients: Iterable[ContextRecipient])(implicit ews: EnrollmentWebService): Seq[Long] =
    recipients.foldLeft(List.empty[Long]) { case (userIds, ContextRecipient(context, roleOpt)) =>
      userIds ++ contextUsers(context, roleOpt)
    }

  // get the members of a context, optionally with a given role
  def contextUsers(context: Long, role: Option[Long])(implicit ews: EnrollmentWebService): Seq[Long] =
    ews
      .getGroupUsersQuery(context, role.map(JLong.valueOf).orNull, EnrollmentType.ACTIVE_ONLY)
      .setDataProjection(DataTypes.META_DATA_TYPE_ID)
      .getValues[Long]

  // partition recipients into a user ids, user handles and a list of context recipients
  def partitionRecipients(recipients: Iterable[Recipient], initialIds: Set[Long] = Set.empty): PartitionedRecipients =
    recipients
      .foldLeft((initialIds, Set.empty[String], Set.empty[ContextRecipient])) { // this is equivalent to a foldMap
        case ((userIds, handles, contexts), UserRecipient(UserId(u)))     =>
          (userIds + u, handles, contexts)
        case ((userIds, handles, contexts), UserRecipient(UserHandle(h))) =>
          (userIds, handles + h, contexts)
        case ((userIds, handles, contextRecipients), c: ContextRecipient) =>
          (userIds, handles, contextRecipients + c)
      }

  // deliver a message to a specific user
  def deliverMessage(storage: MessageStorageFacade, label: MessageLabel)(
    user: Long
  )(implicit fs: FacadeService): MessageFacade =
    user.facade[MessageParentFacade] addMessage { m =>
      m.setLabel(label)
      m.setRead(false)
      m.setStorage(storage)
    }

  // attach an upload to a message
  def attachUpload(storage: MessageStorageFacade)(upload: UploadInfo)(implicit fs: FacadeService): AttachmentComponent =
    storage.addAttachment(upload).component[AttachmentComponent]

  private def storageParent(implicit fs: FacadeService): MessageStorageParentFacade =
    domain.getFolderByType(MessageStorageFolderType).facade[MessageStorageParentFacade]
end MessageServiceImpl

object MessageServiceImpl:
  // maximum attachment size
  val MaxAttachmentSize = 4L * 1024 * 1024

  val MessageStorageFolderType = "messageStorage"

  type PartitionedRecipients = (Set[Long], Set[String], Set[ContextRecipient])

  val zeroRecipients: PartitionedRecipients = (Set.empty, Set.empty, Set.empty)
