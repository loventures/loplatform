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

package loi.cp.content

import java.time.Instant

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.exception.InvalidRequestException
import com.learningobjects.cpxp.service.exception.{AccessForbiddenException, ResourceNotFoundException}
import com.learningobjects.cpxp.service.user.UserId
import loi.authoring.AssetType
import loi.authoring.asset.Asset
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.reference.EdgePath

import scala.util.{Failure, Success, Try}

/** Service that validates a user's right to access a particular piece of content in a course.
  *
  * Checks include: . User enrollment in course . Course start date . Course end date . Course shutdown date . Content
  * exists in course . Gating rules permit access
  *
  * Failures are captured with appropriate Web exceptions. A future refactor will describe this as a disjunction with an
  * algebra of failure types.
  */
@Service
trait ContentAccessService:

  /** Load a lightweight course, validating permission to access as a learner.
    *
    * @param course
    *   the course PK
    * @param user
    *   the user desiring access
    * @return
    *   the lightweight course or an error
    */
  def getCourseAsLearner(course: Long, user: UserId): Try[LightweightCourse]

  /** Load a lightweight course, validating permission to access as an instructor.
    *
    * @param course
    *   the course PK
    * @param user
    *   the user desiring access
    * @return
    *   the lightweight course or an error
    */
  def getCourseAsInstructor(course: Long, user: UserId): Try[LightweightCourse]

  /** Load a lightweight course, validating permission to access as an administrator.
    *
    * @param course
    *   the course PK
    * @param user
    *   the user desiring access
    * @return
    *   the lightweight course or an error
    */
  def getCourseAsAdministrator(course: Long, user: UserId): Try[LightweightCourse]

  /** Load a lightweight course, validating permission to access.
    *
    * @param course
    *   the course PK
    * @param user
    *   the user desiring access
    * @param instructor
    *   whether instructor access is requested
    * @return
    *   the lightweight course or an error
    */
  def getCourse(course: Long, user: UserId, instructor: Boolean): Try[LightweightCourse]

  /** Load content for read-only access.
    *
    * @param course
    *   The course(!)
    * @param path
    *   The content path(!)
    * @param user
    *   The user requesting access(!)
    *
    * @return
    *   the content if the user is allowed to view it
    */
  def getContentReadOnly(course: LightweightCourse, path: EdgePath, user: UserId): Try[CourseContent]

  /** Load content for interact access.
    *
    * @param course
    *   The course(!)
    * @param path
    *   The content path(!)
    * @param user
    *   The user requesting access(!)
    *
    * @return
    *   the content if the user is allowed to interact with it
    */
  def getContentForInteraction(course: LightweightCourse, path: EdgePath, user: UserId): Try[CourseContent]

  /** Load a course and a piece of content in that course for read-only access.
    *
    * @param courseId
    *   The course id
    * @param path
    *   The content path
    * @param user
    *   The user requesting access
    *
    * @return
    *   the course and content, or an appropriate error if access is not confirmed
    */
  def readContent(courseId: Long, path: EdgePath, user: UserId): Try[(LightweightCourse, CourseContent)]

  /** Load a course and a piece of content of an expected type in that course for read-only access.
    *
    * @param courseId
    *   The course id
    * @param path
    *   The content path
    * @param user
    *   The user requesting access
    * @tparam A
    *   the expected asset type
    *
    * @return
    *   the course, content and asset, or an appropriate error if access is not confirmed
    */
  def readContentT[A: ContentType](
    courseId: Long,
    path: EdgePath,
    user: UserId
  ): Try[(LightweightCourse, CourseContent, Asset[A])]

  /** Load a course and all readable content matching a predicate.
    *
    * @param courseId
    *   The course id
    * @param user
    *   The user requesting access(!)
    * @param predicate
    *   A predicate to test content to return
    * @return
    *   the course, and all content that matches the predicate and which the user may view, or an appropriate error if
    *   course access is not confirmed
    */
  def readContents(
    courseId: Long,
    user: UserId,
    predicate: CourseContent => Boolean
  ): Try[(LightweightCourse, List[CourseContent])]

  /** Load a course and all contents of a particular type that the caller has read access to.
    *
    * @param courseId
    *   The course id
    * @param user
    *   The user requesting access
    * @tparam A
    *   the expected asset type
    *
    * @return
    *   the course, and all readable contents and assets in course order, or an appropriate error if course access is
    *   not confirmed
    */
  def readContentsT[A: ContentType](
    courseId: Long,
    user: UserId
  ): Try[(LightweightCourse, List[(CourseContent, Asset[A])])]

  /** Load a course and a piece of content in that course for interact access.
    *
    * @param courseId
    *   The course id
    * @param path
    *   The content path
    * @param user
    *   The user requesting access
    *
    * @return
    *   the course and content, or an appropriate access or type error
    */
  def useContent(courseId: Long, path: EdgePath, user: UserId): Try[(LightweightCourse, CourseContent)]

  /** Load a course and a piece of content of an expected type in that course for interact access.
    *
    * @param courseId
    *   The course id
    * @param path
    *   The content path
    * @param user
    *   The user requesting access
    * @tparam A
    *   the expected asset type
    *
    * @return
    *   the course, content and asset, or an appropriate access or type error
    */
  def useContentT[A: ContentType](
    courseId: Long,
    path: EdgePath,
    user: UserId
  ): Try[(LightweightCourse, CourseContent, Asset[A])]

  /** Load a course and a piece of content in that course for instructor access.
    *
    * @param courseId
    *   The course id
    * @param path
    *   The content path
    * @param user
    *   The user requesting access
    *
    * @return
    *   the course and content, or an appropriate access or type error
    */
  def teachContent(courseId: Long, path: EdgePath, user: UserId): Try[(LightweightCourse, CourseContent)]

  /** Load a course and a piece of content of an expected type in that course for instructor access.
    *
    * @param courseId
    *   The course id
    * @param path
    *   The content path
    * @param user
    *   The user requesting access
    * @tparam A
    *   the expected asset type
    *
    * @return
    *   the course, content and asset, or an appropriate access or type error
    */
  def teachContentT[A: ContentType](
    courseId: Long,
    path: EdgePath,
    user: UserId
  ): Try[(LightweightCourse, CourseContent, Asset[A])]
end ContentAccessService

/** Typeclass describing a concrete content type that can be produced from course content.
  *
  * Implementation note: This is not the place to do database access.
  */
trait ContentType[A]:
  def option(content: CourseContent): Option[Asset[A]]

  /** Convert course content into the expected content type, or fail. */
  def accept(content: CourseContent): Try[Asset[A]]

  /** Predicate test whether the specified content matches this type. */
  def predicate(content: CourseContent): Boolean = option(content).isDefined

object ContentType:
  def apply[A](implicit ct: ContentType[A]): ContentType[A] = ct

  /** Support for summoning asset instances from course content. */
  implicit def assetContentType[A](implicit assetType: AssetType[A]): ContentType[A] = new ContentType[A]:
    override def option(content: CourseContent): Option[Asset[A]] =
      content.asset.filter[A]

    override def accept(content: CourseContent): Try[Asset[A]] =
      option(content).fold[Try[Asset[A]]](
        Failure(
          new InvalidRequestException(s"Content mismatch, expected ${assetType.id}, found ${content.asset.info.typeId}")
        )
      )(Success(_))
end ContentType

final case class CourseNotYetStartedException(startDate: Instant)
    extends AccessForbiddenException(s"Course starts on $startDate")

final case class CourseAlreadyEndedException(endDate: Instant)
    extends AccessForbiddenException(s"Course ended on $endDate")

final case class CourseAlreadyShutdownException(shutdownDate: Instant)
    extends AccessForbiddenException(s"Course shut down on $shutdownDate")

final case class ContentDeletedException(path: EdgePath) extends ResourceNotFoundException(s"Content $path deleted")
