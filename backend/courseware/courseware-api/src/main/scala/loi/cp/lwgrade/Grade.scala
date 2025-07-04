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

package loi.cp.lwgrade

import argonaut.*
import scalaz.*
import scalaz.std.option.*
import scalaz.syntax.applicative.*
import scalaz.syntax.order.*
import scaloi.misc.InstantInstances.*

import java.time.Instant
import scala.PartialFunction.condOpt

/** The state of grade on a gradeable item.
  */
sealed abstract class Grade extends Product with Serializable

object Grade:

  /** A grade has been submitted with a timestamp for submission time.
    * @param grade
    *   the grade in points (not percent)
    */
  final case class Graded(grade: Double, max: Double, date: Instant) extends Grade

  /** A grade has a max possible score, but no grade has submitted yet.
    */
  final case class Ungraded(max: Double) extends Grade

  /** A grade has a max possible score and submission time and is pending a grade from the instructor.
    */
  final case class Pending(max: Double, date: Instant) extends Grade

  /** Like Ungraded but for columns that are not for-credit. A grade has not been submitted.
    */
  case object Unassigned extends Grade

  /** This grade was computed as a from other grades.
    */
  final case class Rollup(grade: Double, max: Double, latestChange: Instant) extends Grade

  final case class NoCredit(grade: Double, max: Double, date: Instant) extends Grade

  /** This grade has no max possible score and is for extra credit.
    */
  final case class ExtraCredit(grade: Double, date: Instant) extends Grade

  implicit val gradeCodec: CodecJson[Grade] =
    import Argonaut.*
    // import scaloi.json.ArgoExtras._
    import scaloi.json.ArgoExtras.*

    val graded      = CodecJson.derive[Graded]
    val ungraded    = CodecJson.derive[Ungraded]
    val pending     = CodecJson.derive[Pending]
    val rollup      = CodecJson.derive[Rollup]
    val noCredit    = CodecJson.derive[NoCredit]
    val extraCredit = CodecJson.derive[ExtraCredit]
    val unassigned  = DecodeJson(_ => DecodeResult.ok(Unassigned))

    def encode(grade: Grade): Json = Json.jObjectFields(grade match
      case g: Graded      => "graded"      := graded encode g
      case g: Ungraded    => "ungraded"    := ungraded encode g
      case g: Pending     => "pending"     := pending encode g
      case g: Rollup      => "rollup"      := rollup encode g
      case g: NoCredit    => "nocredit"    := noCredit encode g
      case g: ExtraCredit => "extracredit" := extraCredit encode g
      case Unassigned     => "unassigned"  := "unassigned")

    def decode(hc: HCursor): DecodeResult[Grade] = List[DecodeResult[Grade]](
      (hc --\ "graded").as(using graded).widen,
      (hc --\ "ungraded").as(using ungraded).widen,
      (hc --\ "pending").as(using pending).widen,
      (hc --\ "rollup").as(using rollup).widen,
      (hc --\ "nocredit").as(using noCredit).widen,
      (hc --\ "extracredit").as(using extraCredit).widen,
      (hc --\ "unassigned").as(using unassigned).widen,
    ).reduce(_ ||| _).widen

    CodecJson(encode, decode)
  end gradeCodec

  def isGraded(grade: Grade): Boolean = grade.isInstanceOf[Graded]

  /** Get a grade when possible.
    */
  val grade: Grade =?> Double = Kleisli(condOpt(_) {
    case Graded(g, _, _)   => g
    case Rollup(g, _, _)   => g
    case ExtraCredit(g, _) => g
    case NoCredit(g, _, _) => g
  })

  /** Get a max grade when possible
    */
  val max: Grade => Double = {
    case Graded(_, m, _)   => m
    case Ungraded(m)       => m
    case Pending(m, _)     => m
    case Unassigned        => 0.0d
    case Rollup(_, m, _)   => m
    case ExtraCredit(_, _) => 0.0
    case NoCredit(_, m, _) => m
  }

  /** Get or set a submission date when possible.
    */
  val date: Grade =?> Instant = Kleisli(condOpt(_) {
    case Graded(_, _, d)   => d
    case Pending(_, d)     => d
    case Rollup(_, _, d)   => d
    case ExtraCredit(_, d) => d
    case NoCredit(_, _, d) => d
  })

  def fraction(g: Grade): Option[Double] = condOpt(g) {
    case Graded(grade, max, _)   => grade / max
    case Ungraded(_)             => 0d
    case Pending(_, _)           => 0d
    case Rollup(grade, max, _)   => grade / max
    case NoCredit(grade, max, _) => grade / max
  }

  implicit def eq: Equal[Grade] = Equal.equalA

  def rollup(extractor: GradeExtractor): (Grade, Grade) => Grade = (l, r) =>
    val ExtractedGrade(lGradeAndTime, lMax) = extractor(l)
    val ExtractedGrade(rGradeAndTime, rMax) = extractor(r)

    (lGradeAndTime, rGradeAndTime) match
      case (Some((lValue, lTime)), Some((rValue, rTime))) =>
        Rollup(lValue + rValue, lMax + rMax, lTime max rTime)

      case (Some((lValue, lTime)), None) =>
        Rollup(lValue, lMax + rMax, lTime)

      case (None, Some((rValue, rTime))) =>
        Rollup(rValue, lMax + rMax, rTime)

      case (None, None) =>
        Ungraded(lMax + rMax)
    end match

  /** Takes a rollup grade and returns it weighted by the category. */
  def weightedRollup(g: Grade, weight: Double): Grade = g match
    case Rollup(value, max, time) => Rollup(value * weight / max, weight, time)
    case _                        => Ungraded(weight)

  def getGradeValues(g: Grade): (Option[Double], Double, Option[Instant]) =
    val details = RawGradeExtractor(g)
    details.value match
      case Some((grade, date)) => (Some(grade), details.max, Some(date))
      case _                   => (None, details.max, None)

  final case class ExtractedGrade(value: Option[(Double, Instant)], max: Double):

    def percentage: Double =
      this match
        case ExtractedGrade(_, max) if max <= 0d   => 0d
        case ExtractedGrade(None, _)               => 0d
        case ExtractedGrade(Some((grade, _)), max) => grade / max

  object ExtractedGrade:
    def apply(g: Grade): ExtractedGrade =
      ExtractedGrade((Grade.grade |@| Grade.date).tupled.apply(g), Grade.max(g))

  type GradeExtractor = Grade => ExtractedGrade

  final val RawGradeExtractor: GradeExtractor = {
    case NoCredit(_, _, _) => ExtractedGrade(None, 0.0)
    case g                 => ExtractedGrade(g)
  }

  final val ProjectedGradeExtractor: GradeExtractor = {
    case NoCredit(_, _, _) => ExtractedGrade(None, 0.0)
    case Ungraded(_)       => ExtractedGrade(None, 0.0)
    case Pending(_, _)     => ExtractedGrade(None, 0.0)
    case g                 => ExtractedGrade(g)
  }
end Grade
