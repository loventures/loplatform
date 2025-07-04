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

package de.pony.stables

import cats.effect.{Async, ExitCode, IO, IOApp}
import cats.syntax.all.*
import de.common.Stash.PrSettings
import de.common.{Http, Klient, OptUnspecified, Stash, getenv}
import org.http4s.Uri
import org.http4s.implicits.*
import scopt.OptionParser

import scala.concurrent.ExecutionContext

/** Pony Stabilizer. Creates and merges pull requests moving a release towards production. Note: This generally should
  * only be used
  *
  * Merge Order: master -into-> 11.0-release newPonyBranch -into-> master
  *
  * Usage:
  *
  * % export USER=fbar % read -s PASS % export PASS % sbt
  *
  * core/utl:runMain de.pony.stables.App --newPonyBranch test/friendship/derpy --trotMethod (blind|withApproval) --mode
  * test
  */
object App extends IOApp:

  override def run(args: List[String]): IO[ExitCode] =
    trot[IO](args).map(_ => ExitCode.Success)

  def trot[F[_]: Async](args: List[String]): F[Unit] =
    for
      user          <- getenv[F](UserEnv)
      pass          <- getenv[F](PassEnv)
      params        <- parser.parse(args, PonyStabilizerParams()).liftTo[F](OptUnspecified("malformed"))
      trotBlindly    = params.trotMethod.contains("blind")
      newPonyBranch <- params.newPonyBranch.liftTo[F](OptUnspecified("no pony branch"))
      http           = Http(user, pass, jiraUri, Http.config[F].resource, ExecutionContext.global)
      _             <- http.use(initiateTrotting(_, newPonyBranch, trotBlindly, params.mode))
    yield println("trotted")

  private def initiateTrotting[F[_]: Async](
    http: Http[F],
    newPonyBranch: String,
    trotBlindly: Boolean,
    mode: String
  ): F[Unit] =
    val trot = for
      _               <- Stash.changePrSettings[F](PrSettings(0, 0))
      masterToRelease <- if mode == "test" then Stash.createPr[F]("master to release", TestMasterRef, TestReleaseRef)
                         else Stash.createPr[F]("master to staging", MasterRef, ReleaseRef)
      _               <- mergePr[F](masterToRelease, trotBlindly)
      newPony         <- if mode == "test" then
                           Stash.createPr[F]("New pony to master", Stash.Ref(s"refs/heads/$newPonyBranch"), TestMasterRef)
                         else Stash.createPr[F]("New pony to master", Stash.Ref(s"refs/heads/$newPonyBranch"), MasterRef)
      _               <- mergePr[F](newPony, trotBlindly)
      _               <- Stash.changePrSettings[F](PrSettings(1, 1))
    yield ()
    trot(http)
  end initiateTrotting

  // Currently, only supports blind
  private def mergePr[F[_]: Async](pr: Stash.PullRequest, trotBlindly: Boolean): Klient[F, Unit] =
    def poll(): Klient[F, Stash.PullRequest] =
      Stash
        .getPR(pr.id)
        .flatMap(pr =>
          if pr.approvers.exists(_.nonEmpty) then Stash.mergePr(pr)
          else
            Thread.sleep(1000)
            poll()
        )

    if trotBlindly then Stash.mergePr(pr).void
    else poll().void
  end mergePr

  final case class PonyStabilizerParams(
    newPonyBranch: Option[String] = None,
    trotMethod: Option[String] = None,
    reviewers: Seq[String] = Seq.empty,
    mode: String = "run"
  )

  private final val parser = new OptionParser[PonyStabilizerParams]("Pony Stabilizer"):
    head("ponystabilizer", "0.1-SNAPSHOT", "Promotes builds")
    opt[String]('b', "newPonyBranch") text "The branch that contains the new pony" action { (branch, params) =>
      params.copy(newPonyBranch = branch.some)
    }
    opt[String]('m', "mode") text "The branch that contains the new pony" action { (mode, params) =>
      params.copy(mode = mode)
    }
    opt[String]('t', "trotMethod") text "Whether to wait for approvals" action { (method, params) =>
      params.copy(trotMethod = method.some)
    }
    opt[Seq[String]]('r', "reviewers") text "reviewers to add to the created PRs" action { (reviewers, params) =>
      params.copy(reviewers = reviewers)
    }

  private def jiraUri: Uri = uri"https://jira.example.org/"

  private val TestMasterRef  = Stash.Ref("refs/heads/test/master")
  private val TestReleaseRef = Stash.Ref("refs/heads/test/11.0-release")
  private val MasterRef      = Stash.Ref("refs/heads/master")
  private val ReleaseRef     = Stash.Ref("refs/heads/release/11.0-release")

  private final val UserEnv = "JFROG_CREDENTIALS_USR"
  private final val PassEnv = "JFROG_CREDENTIALS_PSW"
end App
