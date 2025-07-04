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

/*
 * Usage:
 *   export USER=$(whoami)
 *   read -s PASS
 *   <enter password when prompted>
 *   export PASS
 *   sbt "core/utl:runMain de.changelog.App -b master -o file.html"
 */
package de.changelog

import cats.effect.*
import cats.instances.list.*
import cats.instances.set.*
import cats.syntax.all.*
import cats.{Applicative, Foldable}
import de.common.*
import org.apache.commons.io.FileUtils
import org.http4s.Uri
import org.http4s.implicits.*
import scalaz.std.list.*
import scalaz.std.string.*
import scaloi.syntax.collection.*
import scaloi.syntax.hypermonad.*
import scaloi.syntax.map.*
import scaloi.syntax.option.*
import scopt.OptionParser

import java.io.File
import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext
import scala.xml.{Group as _, *}

/** Changelog app. Scrapes Stash and Jira for information.
  *
  * For a pull request branch just considers this PR. For master generates a changelog against the release branch. For
  * release generates a diff between the most recent two release tags.
  *
  * Usage:
  *
  * % export USER=fbar % read -s PASS % export PASS
  *
  * ditto for SLACK_S3_BUCKET, SLACK_S3_KEY, SLACK_S3_SECRET
  *
  * % sbt
  *
  * core/utl:runMain de.changelog.App --branch master --out changelog.html
  */
object App extends IOApp:
  import de.common.Jira.*
  import de.common.Stash.PullRequest

  override def run(args: List[String]): IO[ExitCode] =
    changelog(args).as(ExitCode.Success)

  private def changelog(args: List[String]): IO[Unit] =
    for
      user   <- getenv[IO](UserEnv)
      pass   <- getenv[IO](PassEnv)
      params <- parser.parse(args, ChangelogParams()).liftTo[IO](OptUnspecified("wut"))
      httpR   = Http[IO](user, pass, jiraUri, Http.config[IO].resource, ExecutionContext.global)
      _      <- httpR.use(http => execute[IO](params).apply(http))
    yield println("OK")

  private def execute[F[_]: Async](params: ChangelogParams): Klient[F, Unit] =
    for
      branch       <- params.branch.liftTo[Klient[F, *]](OptUnspecified(BranchOpt): Throwable)
      output       <- params.out.liftTo[Klient[F, *]](OptUnspecified(OutputOpt): Throwable)
      s3           <- Klient.liftF(S3.getClient)
      (name, prs)  <- loadPRs[F](branch)(params)
      getCommits    = prs.map(_.id).groupUniqTo(Stash.getCommits[F](_, count = 256))
      commits      <- sequenceMap(getCommits)
      getActivities = prs.map(_.id).groupUniqTo(Stash.getActivities[F](_, count = 256))
      activities   <- sequenceMap[Klient[F, *], Long, List[Stash.Activity]](getActivities)
      comments      = activities.mapValuesEagerly(_ collect Stash.extractComments)
      keys          = prs.groupMapUniq(_.id)(pr => Stash.scrapeTicketKeys(pr, commits(pr.id), comments(pr.id)))
      getTickets    = Foldable[List].fold(keys.values.toList).toList.groupUniqTo(getTicket[F])
      tickets      <- sequenceMap(getTickets)
      issues        = keys.mapValuesEagerly(_.flatMap(tickets.apply).toList)
      prsᛌ          = prs.filterNot(pr => ignorePR(pr, comments(pr.id))).map(pr => mutate(pr, comments(pr.id)))
      prsᛌᛌ         = rewriteScalaStewards(prsᛌ)
      groups        = prsᛌᛌ.groupBy(pr => inferGroup(pr, comments(pr.id), issues.getOrZero(pr.id)))
      html         <- React.render[F](groups, issues, name)
      _            <- Klient.liftF(save[F](html, output))
      images       <- Publish.publishImages[F](prsᛌᛌ, branch, name, s3)
      slack        <- Slack.render[F](groups, issues, images, name)
      _            <- Klient.liftF(save[F](slack, slackFile(output)))
      email         = Email.renderEmail(html, images)
      _            <- Klient.liftF(s3.publishIndex[F](email, branch, name))
      _            <- Klient.liftF(save[F](email, emailFile(output)))
      _            <- params.email.fold(Klient.unit[F])(Email.emailResults[F](_, params.subject, name, html, branch == Release))
    yield ()

  def getTicket[F[_]: Async](key: String): Klient[F, Option[Ticket]] =
    if `Aha!`.TicketRe matches key then `Aha!`.getTicket[F](key).map(_.map(aha2jira)) else Jira.getTicket[F](key)

  private def aha2jira(feature: `Aha!`.Ticket): Ticket =
    Ticket(
      0,
      feature.reference_num,
      Fields(
        issuetype = IssueType("Story"),
        status = JiraName("Done"),
        description = None,
        summary = feature.name,
        issuelinks = Nil
      )
    )

  private def ignorePR(pr: PullRequest, comments: List[String]): Boolean =
    pr.title.equals("New pony to master") ||
      comments.exists(Command.Fnord.matches) ||
      (pr.title.toUpperCase.contains(Fnörd) && !comments.exists(Command.Negafnord.matches))

  private def rewriteScalaStewards(prs: List[PullRequest]): List[PullRequest] =
    prs.partition(isScalaSteward) match
      case (Nil, pullRequests)             => pullRequests
      case (steward :: rest, pullRequests) =>
        // For reasons, when branch names are reused we are also pulling up historic PRs with same branch name
        val branches = (steward :: rest).map(_.fromRef.id)
        stewardSummary(steward, branches.distinct.size) :: pullRequests

  private def isScalaSteward(pr: PullRequest): Boolean =
    pr.description.exists(_ `contains` "Have a fantastic day writing Scala") || pr.description.exists(
      _ `contains` "This PR has been generated by [Renovate Bot]"
    )

  private def stewardSummary(stu: PullRequest, count: Int): PullRequest =
    stu.copy(
      title = "Update dependencies",
      description = s"Update $count internal application dependenc${if count == 1 then "y" else "ies"}.".some
    )

  private def slackFile(file: File): File =
    if file.getName == "-" then file else new File(file.getParentFile, file.getName.replace("html", "slack"))

  private def emailFile(file: File): File =
    if file.getName == "-" then file else new File(file.getParentFile, file.getName.replace("html", "email"))

  private def save[F[_]: Sync](html: Node, file: File): F[Unit] = Sync[F] delay {
    if file.getName == "-" then println(html)
    else XML.save(file.getPath, html, "UTF-8")
  }

  private def save[F[_]: Sync](text: String, file: File): F[Unit] = Sync[F] delay {
    if file.getName == "-" then println(text)
    else FileUtils.writeStringToFile(file, text, StandardCharsets.UTF_8)
  }

  private def inferGroup(pr: PullRequest, comments: List[String], issues: List[Jira.Ticket]): Group =
    comments
      .findMap(commentGroup)
      .orElse(issueGroup(issues))
      .getOrElse(Group.TechDebt)

  private def commentGroup(comment: String): Option[Group] = comment match
    case Command.Group(name) => Group.withNameInsensitiveOption(name)
    case _                   => None

  private def issueGroup(issues: List[Jira.Ticket]): Option[Group] =
    if issues.exists(i => isFeature(i.fields.issuetype.name) && !i.key.startsWith("TECH")) then Group.Feature.some
    else if issues.exists(_.fields.issuetype.name == "Bug") then Group.BugFix.some
    else None

  private def isFeature(name: String): Boolean =
    name == "Story" || name == "New Feature"

  private def mutate(pr: PullRequest, comments: List[String]): PullRequest =
    comments.foldRight(pr) {
      case (Command.Title(title), p)      => p.copy(title = title)
      case (Command.Description(desc), p) => p.copy(description = desc.some)
      case (_, p)                         => p
    }

  private def loadPRs[F[_]: Async](branch: String)(params: ChangelogParams): Klient[F, (String, List[PullRequest])] =
    branch match
      case PrBranchRe(id) => Stash.getPR(id.toLong).map(List(_)).tupleLeft(s"PR $id")
      case Master         => Klient.liftF(Git.getPony[F](Master)) product findPRs[F](Master, Release)(params)
      case Release        => Klient.liftF(Git.getPony[F](Release)) product findReleasePRs[F](params)
      case unknown        => Klient.liftF(Sync[F].raiseError(UnsupportedBranch(unknown)))

  private def findReleasePRs[F[_]: Async](params: ChangelogParams): Klient[F, List[PullRequest]] =
    for
      tags         <- Klient.liftF(Git.getTags(Release))
      _             = log info s"Tags ${tags.take(16).mkString(", ")}, ..."
      (curr, prev) <- latestTags(tags).liftTo[Klient[F, *]](TagsNotFound(Release): Throwable)
      _             = log info s"Latest: $curr -> $prev"
      prs          <- findPRs[F](curr, prev)(params)
    yield prs

  private def latestTags(tags: List[String]): Option[(String, String)] = PartialFunction.condOpt(tags) {
    case curr :: prev :: _ => (curr, prev)
  }

  private def findPRs[F[_]: Async](from: String, to: String)(params: ChangelogParams): Klient[F, List[PullRequest]] =
    for
      merges <- Klient.liftF(
                  if from == Master then Git.getMerges[F](from, to) else Git.getMergesFromCommitMessages[F](from, to)
                )
      _      <- Klient.delay(log info s"Merges: ${merges.mkString(", ")}")
      allPrs <- Stash.getPRs[F](params.prs) // expect to find relevant PRs in the most recent bunch.
      prMap   = allPrs.groupBy(_.fromRef.id.stripPrefix("refs/heads/"))
      _       =
        log info s"PRs by from: ${prMap.view.filterKeys(merges.contains).mapValues(_.mkString(", ")).toMap.mkString("; ")}"
      prs     = merges.flatterMap(branch => prMap.get(branch) -<| logMissing(branch))
    yield prs

  private def logMissing(branch: String): Unit = log.warn(s"Unknown branch: $branch")

  private final val parser = new OptionParser[ChangelogParams]("changelog"):
    head("changelog", "0.3-SNAPSHOT", "Generate changelogs")
    opt[String]('b', "branch") text "The branch to track changes from." action { (branch, params) =>
      params.copy(branch = branch.some)
    }
    opt[File]('o', "out") text "File to write." action { (out, params) => params.copy(out = out.some) }
    opt[String]('e', "email") text "The email address to which to send." action { (email, params) =>
      params.copy(email = OptionNZ(email)) // empty == none for easier orchestration
    }
    opt[String]('s', "subject") text "The email subject." action { (subject, params) =>
      params.copy(subject = OptionNZ(subject))
    }
    opt[Int]('p', "prs") text "Number of PRs to retrieve." action { (prs, params) => params.copy(prs = prs) }
    help("help") text "Prints this usage text."
    version("version")

  private final case class ChangelogParams(
    branch: Option[String] = None,
    out: Option[File] = None,
    prs: Int = 500,
    email: Option[String] = None,
    subject: Option[String] = None
  )

  private def jiraUri: Uri = uri"https://jira.example.org/"

  private[changelog] final val PrBranchRe = """^PR-(\d+)$""".r
  private final val Master                = "master"
  private[changelog] final val Release    = "release/11.0-release"

  private final val UserEnv = "USER"
  private final val PassEnv = "PASS"

  private final val BranchOpt = "branch"
  private final val OutputOpt = "output"

  private final val Fnörd = "FNORD"

  private final case class UnsupportedBranch(branch: String) extends Exception(s"Unsupported branch: $branch")
  private final case class TagsNotFound(branch: String)      extends Exception(s"Tags not found in branch: $branch")

  private final val log = org.log4s.getLogger

  def sequenceMap[F[_]: Applicative, A, B](mab: Map[A, F[B]]): F[Map[A, B]] =
    mab.toList.map({ case (a, fb) => fb.tupleLeft(a) }).sequence.map(_.toMap)
end App
