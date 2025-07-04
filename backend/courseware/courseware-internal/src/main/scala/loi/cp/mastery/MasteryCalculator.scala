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

package loi.cp.mastery

import com.learningobjects.cpxp.util.KahanSummation
import loi.authoring.edge.Group
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.assessment.{BasicScore, RubricScore, Score}
import loi.cp.quiz.attempt.QuizAttempt
import loi.cp.submissionassessment.attempt.SubmissionAttempt
import scalaz.std.anyVal.*
import scaloi.syntax.mutableMap.*

import java.util.UUID
import scala.collection.mutable

private[mastery] object MasteryCalculator:
  def updateMasteryForQuizAttempt(
    ws: AttachedReadWorkspace,
    userCompetency: UserMasteryState,
    attempt: QuizAttempt
  ): UserMasteryState =
    val scores = for
      (response, question) <- attempt.responses.zip(attempt.questions)
      score                <- response.score
    yield question.questionPointer.nodeName -> score
    updateMastery(ws, userCompetency, scores)
  end updateMasteryForQuizAttempt

  def updateMasteryForSubmissionAttempt(
    ws: AttachedReadWorkspace,
    userCompetency: UserMasteryState,
    attempt: SubmissionAttempt
  ): UserMasteryState =
    val scores =
      for score <- attempt.score
      yield attempt.assessment.assetReference.nodeName -> score
    updateMastery(ws, userCompetency, scores)

  private def updateMastery(
    ws: AttachedReadWorkspace,
    userCompetency: UserMasteryState,
    scores: Iterable[(UUID, Score)],
  ): UserMasteryState =
    val contentGrade = mutable.Map.from(userCompetency.contentGrade)
    for (name, score) <- scores
    do
      score match
        case basicScore: BasicScore   =>
          contentGrade.update(name, score.asPercentage)
        case rubricScore: RubricScore =>
          contentGrade.update(name, score.asPercentage)
          rubricScore.criterionScores foreach { case (uuid, sectionScore) =>
            contentGrade.update(uuid, sectionScore.asPercentage)
          }
    end for
    computeUserMastery(ws, contentGrade.toMap, userCompetency.competencyMastery, userCompetency.recomputed)
  end updateMastery

  /** This never loads nodes so it will be blind to archived assets, assuming they don't happen. */
  private def computeUserMastery(
    ws: AttachedReadWorkspace,
    contentGrade: Map[UUID, Double],
    priorMastery: Set[UUID],
    recomputed: Boolean,
  ): UserMasteryState =
    // First, sum up the grades of all competencies that are assessed by elements, questions, criteria
    val competencyGradeTotal        = mutable.Map.empty[UUID, KahanSummation]
    val competencyGradeCount        = mutable.Map.empty[UUID, Int]
    def gradeLoop(name: UUID): Unit =
      val kids = ws.outEdgeAttrs(name, ContentGroups).map(_.tgtName)
      kids.foreach(gradeLoop)
      for
        competency <- ws.outEdgeAttrs(name, Group.Assesses).map(_.tgtName)
        grade      <- contentGrade.get(name)
      do
        competencyGradeTotal.get(competency) match
          case Some(kahan) => kahan.add(grade)
          case None        => competencyGradeTotal.put(competency, new KahanSummation(grade, 0))
        competencyGradeCount.append(competency, 1)
      end for
    end gradeLoop
    gradeLoop(ws.homeName)

    // Next, compute mastery and rollup mastery
    val competencyMastery = mutable.Set.from(priorMastery) // never loses competency
    for
      csetEdge <- ws.outEdgeAttrs(ws.rootName, Group.CompetencySets)
      cset      = csetEdge.tgtName
    do
      def masteryLoop(name: UUID): Unit =
        val subs = ws.outEdgeAttrs(name, CompetencyGroups).map(_.tgtName)
        subs.foreach(masteryLoop)

        val gradeMastered = competencyGradeCount.get(name).exists(_ >= MasteryGradeCount) &&
          competencyGradeTotal(name).value / competencyGradeCount(name) + Epsilon >= MasteryGradeAverage
        // I have mastered a competency if I have mastered all its subcompetencies or grade mastered it
        if if subs.isEmpty then gradeMastered else subs.forall(competencyMastery.contains) then
          competencyMastery.add(name)
      masteryLoop(cset)
    end for

    UserMasteryState(
      competencyMastery.toSet,
      competencyGradeTotal.view.mapValues(_.value).toMap,
      competencyGradeCount.toMap,
      contentGrade,
      recomputed,
    )
  end computeUserMastery

  final val MasteryGradeCount   = 1
  final val MasteryGradeAverage = 0.8
  final val Epsilon             = 0.0001

  final val ContentGroups    = Set[Group](Group.Elements, Group.Questions, Group.CblRubric, Group.Criteria)
  final val CompetencyGroups = Set[Group](Group.Level1Competencies, Group.Level2Competencies, Group.Level3Competencies)
end MasteryCalculator
