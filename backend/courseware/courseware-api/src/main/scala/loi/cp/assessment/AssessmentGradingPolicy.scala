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

package loi.cp.assessment

import enumeratum.{ArgonautEnum, Enum, EnumEntry}
import loi.cp.assessment.attempt.{AssessmentAttempt, AssessmentAttemptProperties, AttemptState}
import scaloi.syntax.OptionOps.*

sealed trait AssessmentGradingPolicy extends EnumEntry:

  /** Returns the grade (as a percentage, 1.0 being 100%) for the given attempts applying this policy.
    *
    * @param attempts
    *   the attempt to calculate the grade percentage from
    * @return
    *   the grade as a percentage, with 1.0 being 100%; otherwise [[None]] if no grade can be calculated
    */
  def getGrade(attempts: Seq[AssessmentAttempt]): Option[Double]

  /** Given a set of attempts (assuming from a single user), find the ones that need to be graded, based on the policy.
    *
    * NOTE: This means only one attempt will be returned if the policy is i.e. MostRecent, even if multiple students'
    * attempts are passed into this function.
    *
    * @param attempts
    *   The unfiltered attempts from a user.
    * @return
    *   The attempts that need to be graded.
    */
  def attemptsAwaitingGrade[T <: AssessmentAttemptProperties](attempts: Seq[T]): Seq[T]
end AssessmentGradingPolicy

object AssessmentGradingPolicy extends Enum[AssessmentGradingPolicy] with ArgonautEnum[AssessmentGradingPolicy]:
  val values = findValues

  /** Returns all attempts that valid and 'closed' to the student, i.e. they have been submitted for grading. Attempts
    * in this set may or may not have scores attached.
    *
    * @param attempts
    *   the attempts to filter
    * @return
    *   attempts for valid, closed attempts
    */
  private def getValidClosedAttempts[T <: AssessmentAttemptProperties](attempts: Seq[T]): Seq[T] =
    attempts
      .filter(_.valid)
      .filter(_.state != AttemptState.Open)

  /** Returns all scores (if available) for valid, finalized attempts, in order they were submitted. Submitted attempts
    * that are not yet in the Finalized state will not return a score, even if there is one drafted.
    *
    * i.e. passing in 5 attempts (assuming they are valid) with
    *
    * [ (Open, None), (Submitted, None), (Submitted, Some(Score(0.8, 1.0))), (Finalized, None), (Finalized,
    * Some(Score(0.7, 1.0))) ]
    *
    * will return
    *
    * [ None, None, None, Some(Score(0.7, 1.0)) ]
    *
    * As the final four are closed, but only the last one is Finalized AND has a Score
    *
    * @param attempts
    *   the attempts to sort and filter
    * @return
    *   scores for valid, finalized attempts, in order they were submitted
    */
  private def getFinalizedScores(attempts: Seq[AssessmentAttempt]): Seq[Option[Score]] =
    getValidClosedAttempts(attempts)
      .sortBy(a => (a.submitTime.get, a.id.value))
      .map(a => a.score.when(a.state == AttemptState.Finalized)) // scores with Submitted state are drafts - exclude

  /** A grading policy where the score of the first valid, completed attempt (judged by submission time) is used as the
    * grade.
    */
  case object FirstAttempt extends AssessmentGradingPolicy:
    override def getGrade(attempts: Seq[AssessmentAttempt]): Option[Double] =
      // head is the earliest attempt
      // we can handle the absence of any attempts or the first attempt not being submitted the same (flattening)
      getFinalizedScores(attempts).headOption.flatten
        .map(score => score.asPercentage)

    override def attemptsAwaitingGrade[T <: AssessmentAttemptProperties](attempts: Seq[T]): Seq[T] =
      getValidClosedAttempts(attempts)
        .sortBy(a => (a.submitTime, a.id.value))
        .headOption
        .filter(_.state == AttemptState.Submitted)
        .toSeq
  end FirstAttempt

  /** A grading policy where the score of the last valid, completed attempt (judged by submission time) is used as the
    * grade.
    */
  case object MostRecent extends AssessmentGradingPolicy:
    override def getGrade(attempts: Seq[AssessmentAttempt]): Option[Double] =
      // head was the earliest attempt, reversed head is the most recent
      // we can handle the absence of any attempts or the first attempt not being submitted the same (flattening)
      getFinalizedScores(attempts).reverse.headOption.flatten
        .map(score => score.asPercentage)

    override def attemptsAwaitingGrade[T <: AssessmentAttemptProperties](attempts: Seq[T]): Seq[T] =
      getValidClosedAttempts(attempts)
        .sortBy(a => (a.submitTime, a.id.value))
        .reverse
        .headOption
        .filter(_.state == AttemptState.Submitted)
        .toSeq
  end MostRecent

  /** A grading policy where the highest score (judged as percentages) of any valid, completed attempt is used as the
    * grade.
    */
  case object Highest extends AssessmentGradingPolicy:
    override def getGrade(attempts: Seq[AssessmentAttempt]): Option[Double] =
      val scores = getFinalizedScores(attempts)

      if scores.isEmpty || scores.exists(_.isEmpty) then None
      else
        Some(
          scores
            .flatMap(_.map(_.asPercentage))
            .max
        )
    end getGrade

    override def attemptsAwaitingGrade[T <: AssessmentAttemptProperties](attempts: Seq[T]): Seq[T] =
      getValidClosedAttempts(attempts)
        .filter(_.state == AttemptState.Submitted)
  end Highest

  /** A grading policy where the averaged score of valid, completed attempts (scores as a percentage summed then divided
    * by number of those attempts) is used as the grade.
    */
  case object Average extends AssessmentGradingPolicy:
    override def getGrade(attempts: Seq[AssessmentAttempt]): Option[Double] =
      val scores = getFinalizedScores(attempts)

      if scores.isEmpty || scores.exists(_.isEmpty) then None
      else
        val (totalAttemptCount, scoreTotal) =
          scores
            .flatMap(_.map(_.asPercentage))
            .foldLeft((0, 0.0))({ case ((numAttempts, aggScore), score) =>
              (numAttempts + 1, aggScore + score)
            })

        Some(scoreTotal / totalAttemptCount)
      end if
    end getGrade

    override def attemptsAwaitingGrade[T <: AssessmentAttemptProperties](attempts: Seq[T]): Seq[T] =
      getValidClosedAttempts(attempts)
        .filter(_.state == AttemptState.Submitted)
  end Average

  /** A grading policy where having at least one valid, completed attempt grants a full credit grade.
    */
  case object FullCreditOnCompletion extends AssessmentGradingPolicy:
    override def getGrade(attempts: Seq[AssessmentAttempt]): Option[Double] =
      val existingScores: Seq[Score] = getFinalizedScores(attempts).flatten
      if existingScores.nonEmpty then
        // You get full credit if you have a score for any attempt
        Some(1.0)
      else None

    override def attemptsAwaitingGrade[T <: AssessmentAttemptProperties](attempts: Seq[T]): Seq[T] =
      val closedValidatedAttempts = getValidClosedAttempts(attempts)
      if closedValidatedAttempts.exists(_.state == AttemptState.Finalized) then
        // We only need one finalized attempt
        Nil
      else closedValidatedAttempts.find(_.state == AttemptState.Submitted).toSeq
  end FullCreditOnCompletion
end AssessmentGradingPolicy
