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

package de.webpack

import cats.effect.*
import cats.instances.list.*
import cats.syntax.all.*
import de.common.Stash.NewComment
import de.common.{OptUnspecified, Stash, *}
import io.circe.*
import org.http4s.circe.*
import org.http4s.client.dsl.io.*
import org.http4s.client.{Client, JavaNetClientBuilder}
import org.http4s.headers.Authorization
import org.http4s.{BasicCredentials, Method}
import scalaz.\&/.{Both, That, This}
import scalaz.std.map.*
import scalaz.syntax.semigroup.*
import scalaz.syntax.these.*
import scalaz.{Semigroup, \&/}
import scopt.OptionParser

import java.util.concurrent.Executors
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext
import scala.util.Try

object App extends IOApp:

  val blockingEC             = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(5))
  val httpClient: Client[IO] = JavaNetClientBuilder[IO].create

  def readWebpackStats(filename: String): IO[WebpackStats] =
    val file = IO {
      scala.io.Source.fromFile(filename)
    }
    println(s"Reading stats from $filename")
    Resource
      .fromAutoCloseable(file)
      .use(contents =>
        for
          json  <- IO.fromEither(jawn.parse(contents.getLines().mkString))
          stats <- IO.fromEither(WebpackStats.codec.decodeJson(json))
        yield stats
      )
      .attempt
      .map {
        case Left(_)  => WebpackStats.empty
        case Right(w) => w
      }
  end readWebpackStats

  def readMasterWepbackStats(workingDir: String, project: String): IO[WebpackStats] =
    readWebpackStats(s"${workingDir}/cpv/${project}/master-target/stats.json")

  def readPrWepbackStats(workingDir: String, project: String): IO[WebpackStats] =
    readWebpackStats(s"${workingDir}/cpv/${project}/target/stats.json")

  def postComment(
    config: WebpackStatsConfig,
    user: String,
    pass: String,
    prId: Long,
    projectName: String,
    masterStats: WebpackStats,
    prStats: WebpackStats
  ): IO[Option[Json]] =
    generateCommentText(config, masterStats, prStats, prId, projectName).fold[IO[Option[Json]]](IO.pure(None)) {
      message =>
        val req        = Method.POST.apply(
          NewComment(message),
          Stash.commentsUri(Stash.Bfr, prId),
          Authorization(BasicCredentials(user, pass))
        )
        implicit val e = jsonEncoder[IO]
        httpClient
          .expect[Json](req)
          .map(r =>
            println(s"$projectName: json from client came back in what we assume to be Some")
            r.some
          )
    }

  case class WebpackStatsConfig(workingDir: String, projectNames: List[String], buildNumber: Long, buildUrl: String)

  override def run(args: List[String]): IO[ExitCode] =
    for
      config <- parser.parse(args, WebpackStatsConfig("", List(), 0, "")).liftTo[IO](OptUnspecified("malformed"))
      prId    = prIdFromUrl(config.buildUrl)
      _      <- prId.fold(IO.pure(()))(prId0 =>
                  config.projectNames
                    .traverse(projectName => comment[IO](config, projectName, prId0))
                    .map(_ => ())
                )
    yield
      blockingEC.shutdown()
      ExitCode.Success

  def comment[F[_]: Async: LiftIO](config: WebpackStatsConfig, projectName: String, prId: Long): F[Unit] =
    for
      user        <- getenv[F]("USER")
      pass        <- getenv[F]("PASS")
      masterStats <- readMasterWepbackStats(config.workingDir, projectName).to[F]
      prStats     <- readPrWepbackStats(config.workingDir, projectName).to[F]
      result      <- postComment(config, user, pass, prId, projectName, masterStats, prStats).to[F]
    yield println(
      result.fold(s"$projectName: Skipped commenting on stash, no changes were found")(r =>
        s"$projectName: got response from stash:\n $r"
      )
    )

  def generateCommentText(
    config: WebpackStatsConfig,
    masterStats: WebpackStats,
    prStats: WebpackStats,
    prId: Long,
    projectName: String
  ): Option[String] =
    val masterAssets = masterStats.assets.filter(_.emitted)
    val prAssets     = prStats.assets.filter(_.emitted)

    implicit def theseSemigroup[A]: Semigroup[A \&/ A] = new Semigroup[A \&/ A]:
      override def append(f1: A \&/ A, f2: => A \&/ A): A \&/ A =
        preferFirst[A](f1, f2)

    val assets =
      masterAssets.map(a => a.name -> a.size.`this`[BigInt]).toMap |+|
        prAssets.map(a => a.name -> a.size.that[BigInt]).toMap

    if masterAssets.size === 0 then println(s"$projectName master assets were empty")

    if prAssets.size === 0 then println(s"$projectName PR assets were empty")

    println(s"$projectName: the assets comparison is ${assets.size} size and value: ${assets.toString.take(256)}")

    val comparisons =
      assets.toList
        .map({ case (name, asset) =>
          (name, asset, compare1(asset))
        })

    val filteredComparisons = comparisons
      .filterNot(_._3.m == Equal)
      .sortBy(_._3.difference)

    filteredComparisons match
      case Nil      =>
        println(s"$projectName: deep in message generation no comparisons were found")
        None
      case compares =>
        val columns = compares.map({ case (key, a, c) =>
          List(
            key,
            prin(a.a.map(format)),
            prin(a.b.map(format)),
            c.printComparison,
            c.marker,
          )
        })

        Some( // TODO: fix this.
          s"""
             ![Build #${config.buildNumber}](${config.buildUrl})
             ![Analysis](${config.buildUrl}artifact/cpv/$projectName/target/stats-$projectName.html)
             !
             !|File|Master|PR-$prId|||
             !|----|------|----|--------------|---|
             !${columns.map(cols => s"|${cols.mkString("|")}|").mkString("\n")}
             !""".stripMargin('!')
        )
    end match
  end generateCommentText

  implicit class BigIntOps(b: BigInt):
    def over(a: BigInt): Ratio = Ratio(b, a)

  val measurements = List("B", "KB", "MB", "GB", "TB")

  def format(bytes: BigInt): String =
    @tailrec
    def inner(meas: List[String], step: Long): String =
      meas match
        case m :: tail =>
          if bytes.doubleValue <= Math.pow(1000, step.doubleValue) then
            s"${convert(bytes, Math.pow(1000, step.doubleValue - 1))} $m"
          else inner(tail, step + 1)
        case _         => "Way too big..."

    inner(measurements, 1)
  end format

  def convert(b: BigInt, rate: Double): BigDecimal =
    round((BigDecimal(b) / BigDecimal(rate)), 2)

  def round(b: BigDecimal, scale: Int): BigDecimal =
    b.setScale(scale, BigDecimal.RoundingMode.HALF_UP).toDouble

  import scalaz.std.option.*
  import scalaz.syntax.applicative.*

  val warningThreshold = 0.15
  val exclaimThreshold = 0.5

  def compare1(oldAndNew: BigInt \&/ BigInt): Comparison =
    oldAndNew match
      case Both(a, b) if a `over` b `exceeds` exclaimThreshold => Comparison(Exclaim, b - a, a `over` b)     // "❗️"
      case Both(a, b) if a `over` b `exceeds` warningThreshold => Comparison(Warn, b - a, a `over` b)        // "⚠️"
      case Both(a, b) if a == b                                => Comparison(Equal, 0, BigInt(0) `over` BigInt(1))
      case Both(a, b)                                          => Comparison(Small, b - a, a `over` b)
      case That(b)                                             => Comparison(NewFile, b, b `over` 0)         // +
      case This(a)                                             => Comparison(DeletedFile, 0 - a, a `over` 0) // -

  def compare(old: Option[BigInt], nue: Option[BigInt]): Option[Comparison] =
    ^(
      old,
      nue
    )({
      case (a, b) if a `over` b `exceeds` exclaimThreshold =>
        Comparison(Exclaim, b - a, a `over` b) // "❗️"
      case (a, b) if a `over` b `exceeds` warningThreshold =>
        Comparison(Warn, b - a, a `over` b) // "⚠️"
      case (a, b) if a == b => Comparison(Equal, 0, BigInt(0) `over` BigInt(1))
      case (a, b)           => Comparison(Small, b - a, a `over` b)
    })

  case class Comparison(m: Marker, difference: BigInt, ratio: Ratio):
    def printComparison: String =
      m match
        case NewFile     => s"➕${format(difference)}"
        case DeletedFile => s"➖${format(difference)}"
        case _           =>
          if difference > 0 then s"⬆️${format(difference)} (+${round(ratio.percentage.abs, 2)}%)"
          else if difference < 0 then s"⬇${format(difference.abs)}  (-${round(ratio.percentage, 2)}%)"
          else ""

    def marker: String = m match
      case Exclaim => "❗️"
      case Warn    => "⚠️"
      case _       => ""
  end Comparison

  sealed trait Marker

  case object Exclaim extends Marker

  case object Warn extends Marker

  case object Small extends Marker

  case object Equal extends Marker

  case object NewFile extends Marker

  case object DeletedFile extends Marker

  case class Ratio(a: BigInt, b: BigInt):
    def percentage: Double =
      ((a - b).doubleValue / a.doubleValue) * 100

    def invert: Ratio = Ratio(b, a)

    def doubleValue: Double = a.doubleValue / b.doubleValue

    def exceeds(threshold: Double): Boolean = (1 - doubleValue) > threshold

  def prin(a: Option[String]) = a.fold("")(identity)

  def preferFirst[A](f1: A \&/ A, f2: A \&/ A): A \&/ A =
    (f1, f2) match
      case (This(a), This(_))    => This(a)
      case (That(a), That(_))    => That(a)
      case (That(a), This(b))    => Both(a, b)
      case (This(a), That(b))    => Both(a, b)
      case (That(b), Both(a, _)) => Both(a, b)
      case (This(a), Both(_, b)) => Both(a, b)
      case (Both(a, b), _)       => Both(a, b)

  private final val parser = new OptionParser[WebpackStatsConfig]("webpackStats"):
    head("webpackStats", "0.1-SNAPSHOT", "Process webpack stats")
    help("help") text "Prints this usage text."
    version("version")
    opt[String]("workingDir") text "Working directory of Jenkins" action { (workingDir, params) =>
      params.copy(workingDir = workingDir)
    }
    opt[Seq[String]]("projectNames")
      .valueName("<project1>,<project2>")
      .action { (projectNames, params) => params.copy(projectNames = projectNames.toList) }
      .text("comma separated cpv project names")
    opt[Long]("buildNumber") text "The build number" action { (buildNumber, params) =>
      params.copy(buildNumber = buildNumber)
    }
    opt[String]("buildUrl") text "The url of the build" action { (url, params) => params.copy(buildUrl = url) }

  final val BuildUrl = "^.*/bfr/job/PR-(.*)/.*/".r

  def prIdFromUrl(url: String): Option[Long] = url match
    case BuildUrl(num) => Try(num.toLong).toOption
    case _             => None
end App
