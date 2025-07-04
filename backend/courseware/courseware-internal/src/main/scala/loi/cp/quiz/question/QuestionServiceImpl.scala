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
import loi.asset.TitleProperty
import loi.asset.assessment.model.{Assessment, Checkpoint, Diagnostic, PoolAssessment}
import loi.asset.question.*
import loi.authoring.asset.Asset
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.{EdgeService, Group, TraversedGraph}
import loi.authoring.node.AssetNodeService
import loi.authoring.render.RenderService
import loi.authoring.workspace.{AttachedReadWorkspace, ReadWorkspace}
import loi.cp.assessment.rubric.{AssessmentRubric, RubricService}
import loi.cp.competency.{Competency, CompetentCompetencyService}
import loi.cp.content.CustomisationTraversedGraphOps.*
import loi.cp.course.{CourseSection, CourseWorkspaceService}
import loi.cp.customisation.Customisation
import loi.cp.quiz.Quiz
import loi.cp.quiz.question.bindrop.BinDrop
import loi.cp.quiz.question.choice.{MultipleChoice, MultipleSelect, TrueFalse}
import loi.cp.quiz.question.essay.Essay
import loi.cp.quiz.question.fillintheblank.FillInTheBlank
import loi.cp.quiz.question.hotspot.Hotspot
import loi.cp.quiz.question.matching.Matching
import loi.cp.quiz.question.ordering.Sequencing
import loi.cp.quiz.question.shortanswer.ShortAnswer
import loi.cp.reference.VersionedAssetReference

import java.util.UUID
import scala.util.{Random, Try}

@Service
class QuestionServiceImpl(
  edgeService: EdgeService,
  nodeService: AssetNodeService,
  courseWorkspaceService: CourseWorkspaceService,
  rubricService: RubricService,
  renderService: RenderService,
  competentCompetencyService: CompetentCompetencyService,
) extends QuestionService:
  import QuestionServiceImpl.*

  def getQuestions(section: CourseSection, questionReferences: Seq[VersionedAssetReference]): Seq[Question] =
    for
      (commitId, refs) <- questionReferences.groupBy(_.commit).toSeq
      ws                = courseWorkspaceService.requireReadWorkspaceAtCommit(section, commitId)
      questionAssets0   = nodeService.load(ws).byName(refs.map(_.nodeName).distinct).getOrLoadingError
      questionAssets    = renderService.render(ws, questionAssets0.toList)
      question         <- buildQuestions(ws, questionAssets)
    yield question

  def buildQuestions(ws: AttachedReadWorkspace, contents: Seq[Asset[?]]): Seq[Question] =
    val allRubrics      = rubricService.getRubrics(ws, contents)
    val allRemediations = getAssetRemediations(contents, ws)

    val questionCompetencies = competentCompetencyService.getDirectlyAssessedCompetencies(ws, contents)

    contents.map(content =>
      val ref          = VersionedAssetReference(content, ws.commitId)
      val remediations = allRemediations.getOrElse(content, Nil)
      val competencies = questionCompetencies.getOrElse(content.info.name, Nil)
      val rubric       = allRubrics.get(content)
      buildQuestion(ref, content, remediations, competencies, rubric, ws)
    )
  end buildQuestions

  private def buildQuestion(
    ref: VersionedAssetReference,
    content: Asset[?],
    assetRemediations: Seq[AssetRemediation],
    competencies: Seq[Competency],
    rubric: Option[AssessmentRubric],
    ws: ReadWorkspace
  ): Question =
    content match
      case BinDropQuestion.Asset(bd)          => BinDrop(ref, bd, assetRemediations, competencies)
      case EssayQuestion.Asset(e)             => Essay(ref, e, rubric, assetRemediations, competencies)
      case FillInTheBlankQuestion.Asset(fitb) => FillInTheBlank(ref, fitb, assetRemediations, competencies)
      case MatchingQuestion.Asset(m)          => Matching(ref, m, assetRemediations, competencies)
      case MultipleChoiceQuestion.Asset(mc)   => MultipleChoice(ref, mc, assetRemediations, competencies)
      case MultipleSelectQuestion.Asset(ms)   => MultipleSelect(ref, ms, assetRemediations, competencies)
      case OrderingQuestion.Asset(o)          => Sequencing(ref, o, assetRemediations, competencies)
      case ShortAnswerQuestion.Asset(sa)      => ShortAnswer(ref, sa, assetRemediations, competencies)
      case TrueFalseQuestion.Asset(tf)        => TrueFalse(ref, tf, assetRemediations, competencies)
      case HotspotQuestion.Asset(hs)          =>
        val image = edgeService
          .loadOutEdges(ws, content, Group.Image)
          .headOption
          .map(edge => VersionedAssetReference(edge.target, ref.commit))
        Hotspot(ref, hs, assetRemediations, competencies, image)
      case _                                  => throw new QuestionBuildingException

  private def getAssetRemediations(
    questions: Seq[Asset[?]],
    ws: ReadWorkspace
  ): Map[Asset[?], Seq[AssetRemediation]] =
    edgeService
      .loadOutEdges(ws, questions, Set[Group](Group.RemediationResources), Set.empty[AssetTypeId])
      .groupMap(_.source)(edge =>
        AssetRemediation(
          TitleProperty.fromNode(edge.target).getOrElse(""),
          VersionedAssetReference(edge.target, ws.commitId),
          edge.target.info.typeId
        )
      )

  override def getQuestions(
    quiz: Quiz,
    unassessables: Set[UUID],
    customisation: Customisation,
    ws: AttachedReadWorkspace,
    competencies: Option[Set[UUID]],
  ): QuizQuestions =
    val quizAsset     = nodeService.load(ws).byName(quiz.assetReference.nodeName).getOrLoadingError
    val questionEdges = edgeService.loadOutEdges(ws, quizAsset, Group.Questions)
    val quizGraph     = TraversedGraph(Seq(quiz.courseContent.asset), questionEdges)

    val questionTrees =
      quizGraph.customisedOutTrees(quiz.courseContent.asset, quiz.courseContent.edgeNames, customisation)

    val questionAssets         = questionTrees.map(_.rootLabel.asset)
    val renderedQuestionAssets = renderService.render(ws, questionAssets)
    val questions              = buildQuestions(ws, renderedQuestionAssets)

    def isAssessable(q: Question) = q.competencies.forall(c => !unassessables.contains(c.nodeName)) &&
      competencies.forall(cs => q.competencies.exists(c => cs.contains(c.nodeName)))

    val assessableQuestions = questions.filter(isAssessable)

    quizAsset match
      case PoolAssessment.Asset(pa) =>
        val selectionSize =
          Some(pa.data.numberOfQuestionsForAssessment.toInt) // why is it a Long?
            .filter(_ > 0)
            .getOrElse(assessableQuestions.size)
        QuestionPool(selectionSize, assessableQuestions)

      case Assessment.Asset(_) | Diagnostic.Asset(_) =>
        LinearQuestionSet(assessableQuestions)
      case Checkpoint.Asset(_) | Diagnostic.Asset(_) =>
        LinearQuestionSet(assessableQuestions)

      case other =>
        throw new QuestionLoadingException(new IllegalStateException(s"Unknown assessment content $other"))
    end match
  end getQuestions

  override def pickQuestions(
    quiz: Quiz,
    unassessables: Set[UUID],
    customisation: Customisation,
    ws: AttachedReadWorkspace,
    competencies: Option[Set[UUID]],
  ): Seq[Question] =
    getQuestions(quiz, unassessables, customisation, ws, competencies) match
      case LinearQuestionSet(questions) => questions
      case pool: QuestionPool           => Random.shuffle(pool.candidateQuestions).take(pool.selectionSize)
end QuestionServiceImpl

object QuestionServiceImpl:
  implicit class ContentLoadingResult[A](result: Try[A]):
    def getOrLoadingError: A =
      result.fold(ex => throw new QuestionLoadingException(ex), a => a)
