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

package loi.cp.quiz.question

import loi.cp.competency.Competency

/** A container for all questions for a [[loi.cp.quiz.Quiz]]. Not all questions are necessarily used for all attempts.
  */
sealed trait QuizQuestions:

  /** Returns all competencies associated with these questions.
    *
    * @return
    *   all competencies associated with the questions
    */
  def competencies: Seq[Competency]

/** A set of [[Question]] s with a particular order. All questions in a [[LinearQuestionSet]] are taken with each
  * attempt.
  *
  * @param questions
  *   the questions in the [[loi.cp.quiz.Quiz]]
  */
case class LinearQuestionSet(questions: Seq[Question]) extends QuizQuestions:
  def competencies: Seq[Competency] = questions.flatMap(_.competencies)

/** A pool of [[Question]] s of which a random [[selectionSize]] number of questions are used with each attempt.
  *
  * @param selectionSize
  *   the number of elements to pick per attempt
  * @param candidateQuestions
  *   the candidate questions to choose from
  */
case class QuestionPool(selectionSize: Int, candidateQuestions: Seq[Question]) extends QuizQuestions:
  def competencies: Seq[Competency] = candidateQuestions.flatMap(_.competencies)
