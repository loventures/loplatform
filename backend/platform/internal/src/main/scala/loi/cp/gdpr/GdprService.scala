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

package loi.cp.gdpr

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.io.ByteSource
import com.learningobjects.cpxp.async.async.AsyncOperationActor
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.attachment.AttachmentService
import com.learningobjects.cpxp.service.domain.{DomainDTO, DomainFacade}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.util.EntityContext
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.meta.Meta.Basic
import loi.cp.analytics.event.UserObfuscateEvent1
import scalaz.std.list.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.misc.TimeSource
import scaloi.syntax.map.*
import scaloi.syntax.option.*
import com.learningobjects.cpxp.postgresql.DoobieMetas.*

import java.text.SimpleDateFormat
import java.time.{Instant, Period}
import java.util.{Date, UUID}

@Service
class GdprService(implicit
  fs: FacadeService,
  as: AttachmentService,
  timeSource: TimeSource,
  xa: => Transactor[IO],
  mapper: ObjectMapper,
  domain: => DomainDTO
):

  import GdprService.*

  def lookup(emails: Emails, allDomains: Boolean): Emails =
    logger.info(s"GDPR lookup: ${emails.mkString(", ")}")
    val found = emails filter { email => findUsersByEmailAddress(email, allDomains).nonEmpty }
    found

  def obfuscate(emails: Emails): Emails =
    logger.info(s"GDPR process: ${emails.mkString(", ")}")
    for
      email <- emails
      users  = findUsersByEmailAddress(email, allDomains = true)
      if users.nonEmpty
    yield
      val domains = users.map(_._2).distinct.map(_.facade[DomainFacade].getName)
      logger.info(s"Email address $email found in domains ${domains.mkString(", ")}")
      obfuscateUsers(users, delKey)
      email
  end obfuscate

  private def findUsersByEmailAddress(email: String, allDomains: Boolean): List[(Long, Long)] =
    allDomains
      .fold(selectUsersByEmailAddress(email), selectUsersByEmailAddressAndDomain(email, domain.id))
      .to[List]
      .transact(xa)
      .unsafeRunSync()

  // A data integrity cleanup for ancient users, invoked from sys script
  def ensureUserCreateTimes(): Unit =
    logger.info(s"GDPR ensure user create times")
    val updates = for
      updated0 <- updateUserCreateTimeFromLoginTime.run
      updated1 <- updateUserCreateTimeFromNow.run
    yield updated0 -> updated1

    val (updated0, updated1) = updates.transact(xa).unsafeRunSync()
    logger.info(s"Updated $updated0 + $updated1 user create times")

  def countInactiveUsers(minors: List[String]): Int =
    findAllObfuscations(minors).size

  def purgeInactiveUsers(minors: List[String]): Unit =
    logger.info(s"GDPR purge inactive users")
    val obfuscations = findAllObfuscations(minors)
    logger.info(s"Purging ${obfuscations.size} inactive users")
    obfuscateUsers(obfuscations, delKey)
    val rls          = purgeInactiveRestrictedLearners()
    logger.info(s"Purged $rls restricted learners")

  private def purgeInactiveRestrictedLearners(): Long =
    val purge =
      for count <- deleteInactiveRestrictedLearners(OneYear).run
      yield count
    purge.transact(xa).unsafeRunSync()

  private def delKey: String = new SimpleDateFormat("'gdpr-'yyyy/MM/dd_HH-mm-ss").format(timeSource.date)

  // An obfuscation is a tuple of user PK and domain PK
  private def findAllObfuscations(minors: List[String]): List[(Long, Long)] =
    val fiveYearObfuscations = selectInactiveUsers(FiveYears)
      .to[List]
      .transact(xa)
      .unsafeRunSync()

    // A minor who hasn't logged in in five years will appear in both
    // selects. While there's no harm in a duplicate, it will inflate
    // counts so we will omit minors that are already selected.
    val fiveYearUsers = fiveYearObfuscations.map(_._1).toSet

    // The minors are passed as a single array parameter so the 64K
    // parameter limit doesn't apply; the query size limit is pretty
    // much unbounded. There's no way to avoid PostgreSQL running
    // a sequence scan on users, so chunking the minors just slows
    // everything down. Using a temp table with an index is better
    // than filtering each user row against a massive array parameter.
    val selectMinors = for
      _      <- sql"DROP TABLE IF EXISTS Minors".update.run
      _      <- sql"CREATE TEMP TABLE Minors(externalId TEXT PRIMARY KEY)".update.run
      _      <- sql"INSERT INTO Minors SELECT * FROM UNNEST($minors::TEXT[])".update.run
      _      <- sql"ANALYZE Minors".update.run
      minors <- selectInactiveMinors(OneYear).to[List]
      _      <- sql"DROP TABLE Minors".update.run
    yield minors

    val minorObfuscations = selectMinors
      .transact(xa)
      .unsafeRunSync()
      .filterNot(obfuscation => fiveYearUsers.contains(obfuscation._1))
    fiveYearObfuscations ++ minorObfuscations
  end findAllObfuscations

  private def obfuscateUsers(obfuscations: List[(Long, Long)], guid: String): Unit =
    val s3 = as.getDefaultProvider
    AsyncOperationActor.withTodo(obfuscations.length) { progress =>
      for chunk <- obfuscations.grouped(ObfuscateChunkSize)
      do
        val userIds     = chunk.map(_._1)
        val userDomains = chunk.toMap

        val archiveProgram = for
          users        <- selectUsersForArchive(userIds).to[List]
          integrations <- selectIntegrationsForArchive(userIds).to[List]
        yield
          val integrationMap = integrations.groupBy(_.userId)
          for user <- users
          do
            val archive = GdprArchive(guid, user, integrationMap.getOrZero(user.id))
            val json    = mapper.writeValueAsBytes(archive)
            // expectation is that calls come with their external id for a restore
            val key     = user.externalId.filter(_.nonEmpty) || user.emailAddress.filter(_.nonEmpty) | user.id.toString
            val domain  = userDomains(user.id)
            s3.putBlob(s"gdpr/$domain/${key.replace('/', '_')}", ByteSource.wrap(json))
        archiveProgram.transact(xa).unsafeRunSync()

        val analytics  = chunk.map((analyticsEvent).tupled)
        val purgeChunk = for
          _ <- purgeUsers(userIds).run
          _ <- purgeUserHistories(userIds).run
          _ <- purgeUserUrls(userIds).run
          _ <- delEnrolments(userIds, guid).run
          _ <- delIntegrations(userIds, guid).run
          _ <- Update[AnalyticsEvent](insertAnalyticsEvent.internals.sql).updateMany(analytics)
        yield ()
        purgeChunk.transact(xa).unsafeRunSync()

        logger.info(s"Purged ${chunk.size} inactive users")
        progress.done(chunk.size)
    }
  end obfuscateUsers

  private def analyticsEvent(user: Long, root: Long): AnalyticsEvent =
    analyticsEventTuple(obfuscateEvent1(user, root), root)

  private def obfuscateEvent1(user: Long, root: Long) =
    UserObfuscateEvent1(
      id = UUID.randomUUID(),
      time = new Date(), // want distinct times for the analytics bus that can't handle so many simultaneously
      source = root.toString,
      userId = user,
    )

  private def analyticsEventTuple(event: UserObfuscateEvent1, root: Long): AnalyticsEvent =
    (
      EntityContext.generateId().longValue,
      mapper.valueToTree[ObjectNode](event),
      event.id,
      event.time.toInstant,
      root
    )

  private val insertAnalyticsEvent =
    sql"""
      INSERT INTO
        AnalyticFinder
        (id, dataJson, guid, time, root_id)
      VALUES
        (?, ?, ?, ?, ?)
    """

  private val updateUserCreateTimeFromLoginTime: Update0 =
    sql"""
      UPDATE
        UserFinder u
      SET
        createTime = h.loginTime
      FROM
        UserHistoryFinder h
      WHERE
        u.createTime IS NULL AND
        h.parent_id = u.id AND
        u.uType = 'Standard'
    """.update

  private val updateUserCreateTimeFromNow: Update0 =
    sql"""
      UPDATE
        UserFinder u
      SET
        createTime = now()
      WHERE
        u.createTime IS NULL AND
        u.uType = 'Standard'
    """.update

  private def selectUsersByEmailAddress(email: String): Query0[(Long, Long)] =
    sql"""
      SELECT
        id,
        root_id
      FROM
        UserFinder
      WHERE
        LOWER(emailAddress) = LOWER($email)
    """.query[(Long, Long)]

  private def selectUsersByEmailAddressAndDomain(email: String, domain: Long): Query0[(Long, Long)] =
    sql"""
      SELECT
        u.id,
        u.root_id
      FROM
        UserFinder u
      WHERE
        LOWER(emailAddress) = LOWER($email) AND
        root_id = $domain
    """.query[(Long, Long)]

  // The coalesce with createTime is to account for users who are created
  // via integration but then never log in.
  private def selectInactiveUsers(interval: Period): Query0[(Long, Long)] =
    sql"""
      SELECT
        u.id,
        u.root_id
      FROM
        UserFinder u
      LEFT OUTER JOIN
        UserHistoryFinder h ON h.parent_id = u.id
      WHERE
        COALESCE(h.loginTime, u.createTime) < now() - $interval::INTERVAL AND
        u.state <> 'Gdpr' AND
        u.uType = 'Standard'
    """.query[(Long, Long)]

  private def selectInactiveMinors(interval: Period): Query0[(Long, Long)] =
    sql"""
    SELECT
      u.id,
      u.root_id
    FROM
      UserFinder u
    INNER JOIN
      Minors USING (externalId)
    LEFT OUTER JOIN
      UserHistoryFinder h ON h.parent_id = u.id
    WHERE
      COALESCE(h.loginTime, u.createTime) < now() - $interval::INTERVAL AND
      u.state <> 'Gdpr' AND
      u.uType = 'Standard'
  """.query[(Long, Long)]

  private def selectUsersForArchive(userIds: List[Long]): Query0[GdprUser] =
    sql"""
      SELECT
        u.id,
        u.emailAddress,
        u.givenName,
        u.middleName,
        u.familyName,
        u.userName,
        u.externalId
      FROM
        UserFinder u
      WHERE
        u.id = ANY ($userIds::BIGINT[]) AND
        u.del IS NULL
       """.query[GdprUser]

  private def selectIntegrationsForArchive(userIds: List[Long]): Query0[GdprIntegration] =
    sql"""
    SELECT
      i.parent_id,
      i.externalSystem_id,
      i.uniqueId
    FROM
      IntegrationFinder i
    WHERE
      i.parent_id = ANY ($userIds::BIGINT[]) AND
      i.del IS NULL
     """.query[GdprIntegration]

  // We don't delete users because we don't know that everything will play well
  // with null-dereferenced users (activity, creator, ...)
  private def purgeUsers(userIds: List[Long]): Update0 =
    sql"""
      UPDATE
        UserFinder
      SET
        disabled = TRUE,
        state = 'Gdpr',
        emailAddress = NULL,
        givenName = NULL,
        middleName = NULL,
        familyName = NULL,
        fullName = NULL,
        title = NULL,
        userName = NULL,
        externalId = NULL,
        password = NULL,
        rssUsername = NULL,
        rssPassword = NULL,
        url = NULL
      WHERE
        id = ANY ($userIds::BIGINT[])
    """.update

  private def purgeUserHistories(userIds: List[Long]): Update0 =
    sql"""
      UPDATE
        UserHistoryFinder
      SET
        json = NULL
      WHERE
        parent_id = ANY ($userIds::BIGINT[])
    """.update

  private def purgeUserUrls(userIds: List[Long]): Update0 =
    sql"""
      UPDATE
        Data
      SET
        string = NULL
      WHERE
        owner_id = ANY ($userIds::BIGINT[]) AND
        type_name = 'url'
    """.update

  private def delEnrolments(userIds: List[Long], guid: String): Update0 =
    sql"""
        UPDATE
          EnrollmentFinder
        SET
          del = $guid
        WHERE
          del IS NULL AND
          parent_id = ANY ($userIds::BIGINT[])
      """.update

  private def delIntegrations(userIds: List[Long], guid: String): Update0 =
    sql"""
      UPDATE
        IntegrationFinder
      SET
        uniqueId = NULL,
        del = $guid
      WHERE
        del IS NULL AND
        parent_id = ANY ($userIds::BIGINT[])
    """.update

  private def deleteInactiveRestrictedLearners(interval: Period): Update0 =
    sql"""
        WITH restricted_access AS (
          SELECT
            r.id,
            MAX(COALESCE(h.loginTime, r.created)) AS access
          FROM
            RestrictedLearnerFinder r
          LEFT OUTER JOIN
            UserFinder u ON LOWER(u.emailAddress) = LOWER(r.email)
          LEFT OUTER JOIN
            UserHistoryFinder h ON h.parent_id = u.id
          GROUP BY
            r.id
        )
        DELETE FROM
          RestrictedLearnerFinder r
        USING
          restricted_access a
        WHERE
          r.id = a.id AND
          a.access < now() - $interval::INTERVAL
      """.update
end GdprService

private[gdpr] final case class GdprUser(
  id: Long,
  emailAddress: Option[String],
  givenName: Option[String],
  middleName: Option[String],
  familyName: Option[String],
  userName: Option[String],
  externalId: Option[String],
)

private[gdpr] final case class GdprIntegration(
  userId: Long,
  systemId: Long,
  uniqueId: String,
)
private[gdpr] final case class GdprArchive(
  guid: String,
  user: GdprUser,
  integrations: List[GdprIntegration],
)

object GdprService:
  final val logger = org.log4s.getLogger

  final val OneYear   = Period.ofYears(1)
  final val FiveYears = Period.ofYears(5)

  // transaction size concerns
  final val ObfuscateChunkSize = 100

  type AnalyticsEvent = (Long, ObjectNode, UUID, Instant, Long)

  import doobie.enumerated.JdbcType.*

  // per https://www.postgresql.org/docs/11/datatype-datetime.html#DATATYPE-INTERVAL-OUTPUT
  // parsing an interval response only works if `SET intervalstyle iso_8601` but we don't parse
  // so it doesn't actually matter
  implicit val PeriodMeta: Meta[java.time.Period] =
    Basic.one[java.time.Period](
      VarChar,
      List(VarChar, LongVarChar),
      (rs, i) => Period.parse(rs.getString(i)), // not so fast...
      (ps, i, p) => ps.setString(i, p.toString),
      (rs, i, p) => rs.updateString(i, p.toString)
    )
end GdprService
