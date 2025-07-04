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

package de.gdpr

import cats.effect.*
import cats.instances.list.*
import cats.instances.option.*
import cats.instances.string.*
import cats.syntax.all.*
import de.common.{Host, Http, Klient, prompt, promptPassword, given}
import org.http4s.Uri
import scalaz.std.list.*
import scalaz.{Foldable, MonadPlus}
import scaloi.syntax.any.*
import scopt.{OptionParser, Read}

import java.time.Instant
import scala.concurrent.ExecutionContext

/** GDPR thingumajig.
  *
  * Usage:
  *
  * % sbt -Dsbt.supershell=false --supershell=false
  *
  * core/utl:runMain de.gdpr.App --dry-run --email foo@bar.edu
  *
  * core/utl:runMain de.gdpr.App --email foo@bar.edu
  */
object App extends IOApp:

  override def run(args: List[String]): IO[ExitCode] = gdpr[IO](args).map(_ => ExitCode.Success)

  private def gdpr[F[_]](args: List[String])(implicit F: Async[F]): F[Unit] =
    for
      params <- F.point(parser.parse(args, GdprParams())) *#@% SyntaxError()
      http    = Http(Http.config[F].resource, ExecutionContext.global)
      _      <- http.use(client => execute[F](params).apply(client))
      _      <- F.delay(log info s"OK $Signature")
    yield ()

  private def execute[F[_]: Async](params: GdprParams): Klient[F, Unit] =
    for
      _      <- Sync[Klient[F, *]].point(params.email.nonEmpty) *#@% OptUnspecified(EmailOpt)
      emails <- params.email.toList.traverse(validateEmail[F])
      _      <- Platform.reals.toList.traverse(executePlatform[F](emails, params)).void
    yield ()

  private def validateEmail[F[_]: Sync](email: String): Klient[F, String] =
    for _ <- Sync[Klient[F, *]].point(EmailRe matches email) *#@% InvalidEmail(email)
    yield email

  private def executePlatform[F[_]: Async](emails: List[String], params: GdprParams)(
    platform: Platform
  ): Klient[F, Unit] =
    for
      found  <- platform.hosts.flatTraverse(gdprPlat(_, emails)(params))
      summary = emails.foldMap(summarise(found))
      _      <- Klient.delay(println(s"$platform\n$summary"))
    yield ()

  private def gdprPlat[F[_]: Async](
    hostname: String,
    emails: List[String]
  )(params: GdprParams): Klient[F, List[String]] =
    for
      host   <- accreditate(hostname, "/sys/gdpr")
      emails <- if params.dryRun then GdprApi.lookup[F](emails, host) else GdprApi.obfuscate[F](emails, host)
    yield emails

  private def envCredentials: Option[(String, String)] =
    sys.env.get(UsernameEnv) product sys.env.get(PasswordEnv)

  private def accreditate[F[_]: Sync](hostname: String, path: String): Klient[F, Host] =
    envCredentials.fold(accInteractive(hostname, path))(unPw => Klient.pure(Host(hostname, unPw._1, unPw._2)))

  private def accInteractive[F[_]: Sync](hostname: String, path: String): Klient[F, Host] =
    for
      un    <- prompt[Klient[F, *]](s"$hostname username: ")
      pw    <- promptPassword[Klient[F, *]]("password: ")
      host  <- validate[F](Host(hostname, un, pw.mkString), path)
      retry <- host.fold(accreditate[F](hostname, path))(Klient.pure[F, Host](_))
    yield retry

  private def validate[F[_]: Sync](host: Host, path: String): Klient[F, Option[Host]] =
    val uri = host.uri.withPath(Uri.Path.unsafeFromString(path))

    for
      http      <- Klient.ask[F]
      status    <- Klient.liftF(
                     GdprApi.logA(http.client.status(http.get(uri, host.cred)))(
                       s"validating $uri",
                       resp => s"${resp.code} ${resp.reason}"
                     )
                   )
      validHost <- status.code match
                     case 200  => Klient.pure[F, Option[Host]](host.some)
                     case 401  => Klient.pure[F, Option[Host]](None)
                     case code => Klient.liftF(Sync[F].raiseError[Option[Host]](UnexpectedStatus(code)))
    yield validHost
    end for
  end validate

  private def summarise[F[_]: Foldable](found: F[String])(email: String): String =
    s"$email: ${if found.containsIgnoreCase(email) then "Present" else "Absent"}\n"

  private def date: String = Instant.now.toString.take(10)

  private final val Version = "0.6-SNAPSHOT"

  private final val Signature = s"-- de.gdpr.App v$Version"

  private final val UsernameEnv = "GDPR_USR"
  private final val PasswordEnv = "GDPR_PSW"

  private final val parser = new OptionParser[GdprParams]("gdpr"):
    head("gdpr", Version, "Process gdpr tickets")
    help("help") text "Prints this usage text."
    version("version")
    opt[String]("email").unbounded() text "Email addresses to purge" action { (email, params) =>
      params.copy(email = params.email ++ email.split("\\s+").filter(_.nonEmpty))
    }
    opt[Unit]("dry-run") text "Dry-run only (no update)" action { (_, params) => params.copy(dryRun = true) }

  private final val EmailOpt = "email"

  private final case class GdprParams(
    email: Set[String] = Set.empty,
    dryRun: Boolean = false,
  )

  private final val log = org.log4s.getLogger

  sealed abstract class Platform(val real: Boolean, val name: String, val hosts: List[String])
      extends enumeratum.EnumEntry

  object Platform extends enumeratum.Enum[Platform]:
    override def values = findValues
    def reals           = values.filter(_.real)

    case object CampusPack extends Platform(true, "Campus Pack", CampusPacks)
    // case object LO_Platform extends Platform(true, "Learning Objects", "de-cbl-prod.difference-engine.com" :: Nil)
    case object local      extends Platform(false, "Localhost", "localhost:8181" :: Nil)

    private final val CampusPacks =
      "raven.campuspack.net" :: "hippi.campuspack.net" :: "maple.ca.campuspack.net" ::
        "sidhe.campuspack.eu" :: /*"sushi.campuspack.net" ::*/ "koala.campuspack.net" :: Nil
  end Platform

  implicit def readPlatform: Read[Platform] = Read reads Platform.withName // reads platform

  private final case class SyntaxError()                  extends Exception("syntax error, see --help")
  private final case class OptUnspecified(option: String) extends Exception(s"$option not specified")
  private final case class UnexpectedStatus(code: Int)    extends Exception(s"unexpected status $code")
  private final case class InvalidEmail(email: String)    extends Exception(s"$email is not valid")

  import language.implicitConversions

  final class ContainsNC[F[_]](val self: F[String]) extends AnyVal:

    /** Does [[self]] contain the given string, ignoring case. */
    def containsIgnoreCase(s: String)(implicit Foldable: Foldable[F]): Boolean =
      Foldable.any(self)(_ `equalsIgnoreCase` s)

  implicit def ContainsNC[F[_]: Foldable](fs: F[String]): ContainsNC[F] = new ContainsNC(fs)

  final class PlusNC[F[_]](val self: F[String]) extends AnyVal:

    /** Add a string to [[self]] if it does not already contain it, ignoring case. */
    def plusIgnoreCase(s: String)(implicit Foldable: Foldable[F], MonadPlus: MonadPlus[F]): F[String] =
      self.transformUnless(_ `containsIgnoreCase` s)(MonadPlus.plus(_, MonadPlus.point(s)))

  implicit def PlusNC[F[_]: Foldable: MonadPlus](fs: F[String]): PlusNC[F] = new PlusNC(fs)

  private final val EmailRe =
    ("""(?i)(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}""" +
      """~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|""" +
      """\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*""" +
      """[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:25[0-5]|""" +
      """2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]""" +
      """?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f""" +
      """\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])""").r
end App
