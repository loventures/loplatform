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
import loi.cp.quiz.question.essay.Essay

object QuizQuestionUtils:

  /** Returns all competencies for questions and rubrics in the given set of questions.
    *
    * @param questionSet
    *   the container of questions to mine competencies from
    * @return
    *   all competencies from the questions
    */
  def competencies(questionSet: QuizQuestions): Seq[Competency] =
    questionSet match
      case LinearQuestionSet(questions)        => competencies(questions)
      case QuestionPool(_, candidateQuestions) => competencies(candidateQuestions)

  /** Returns all competencies for questions and rubrics in the given set of questions.
    *
    * @param questions
    *   the questions to mine competencies from
    * @return
    *   all competencies from the questions
    */
  def competencies(questions: Seq[Question]): Seq[Competency] =
    questions.flatMap(q => competencies(q)).distinct

  /** Returns all competencies for questions and rubrics in the given question.
    *
    * @param question
    *   the question to mine competencies from
    * @return
    *   all competencies from the question
    */
  def competencies(question: Question): Seq[Competency] =
    question match
      case Essay(_, _, _, _, possibleRubric, _, questionCompetencies) =>
        val rubricCompetencies: Seq[Competency] = possibleRubric.toSeq.flatMap(_.competencies)

        (rubricCompetencies ++ questionCompetencies).distinct
      case q: Question                                                => q.competencies
end QuizQuestionUtils
