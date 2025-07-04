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

import com.learningobjects.cpxp.component.annotation.Service
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.course.CourseSection
import loi.cp.customisation.Customisation
import loi.cp.quiz.Quiz
import loi.cp.reference.VersionedAssetReference

import java.util.UUID

/** A service for generating questions from authored content.
  */
@Service
trait QuestionService:

  /** A method to retrieve questions from references (likely from an attempt).
    *
    * @param questionReferences
    *   the references to retrieve questions for
    * @return
    *   the [[Question]] s from the given [[VersionedAssetReference]] s
    */
  def getQuestions(section: CourseSection, questionReferences: Seq[VersionedAssetReference]): Seq[Question]

  /** Returns the questions associated with the quiz. Note that not all quizzes use all questions for each attempt. If
    * you want to select questions for a new attempt, please use [[pickQuestions]].
    *
    * @param quiz
    *   the quiz to fetch the questions for
    * @param unassessables
    *   competencies that cannot be assessed because the instructor hide the content that teaches them. Is used to
    *   filter the questions that are returned
    * @return
    *   the questions for the quiz
    */
  def getQuestions(
    quiz: Quiz,
    unassessables: Set[UUID],
    customisation: Customisation,
    ws: AttachedReadWorkspace,
    competencies: Option[Set[UUID]] = None,
  ): QuizQuestions

  /** Selects a set of questions for a new attempt.
    *
    * @param quiz
    *   the quiz to pick questions for
    * @param unassessables
    *   competencies that cannot be assessed because the instructor hide the content that teaches them. Is used to
    *   filter the questions that are picked
    * @param competencies
    *   a set of competencies for which to select questions
    * @return
    *   the selected questions
    */
  def pickQuestions(
    quiz: Quiz,
    unassessables: Set[UUID],
    customisation: Customisation,
    ws: AttachedReadWorkspace,
    competencies: Option[Set[UUID]] = None,
  ): Seq[Question]
end QuestionService
