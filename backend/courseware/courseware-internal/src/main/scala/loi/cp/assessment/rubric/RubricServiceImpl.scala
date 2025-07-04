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

package loi.cp.assessment.rubric

import com.learningobjects.cpxp.component.annotation.Service
import loi.asset.rubric.model.{Rubric, RubricCriterion}
import loi.authoring.asset.Asset
import loi.authoring.edge.{EdgeService, Group, TraverseGraph}
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.competency.CompetentCompetencyService

@Service
class RubricServiceImpl(
  competentCompetencyService: CompetentCompetencyService,
  edgeService: EdgeService,
) extends RubricService:
  override def getRubrics(
    ws: AttachedReadWorkspace,
    contents: Seq[Asset[?]]
  ): Map[Asset[?], AssessmentRubric] =
    val graph = edgeService.stravaigeOutGraphs(
      TraverseGraph
        .fromSources(contents.map(_.info.name)*)
        .traverse(Group.CblRubric)
        .traverse(Group.Criteria) :: Nil,
      ws
    )

    val criteria = graph.nodes.flatMap(_.filter[RubricCriterion])

    val criterionCompetencies = competentCompetencyService.getDirectlyAssessedCompetencies(ws, criteria)

    (for
      content <- contents.view
      rubric  <- graph.targetsInGroupOfType[Rubric](content, Group.CblRubric)
    yield
      val criteria = graph
        .targetsInGroupOfType[RubricCriterion](rubric, Group.Criteria)
        .map(criterion =>
          val competencies = criterionCompetencies.getOrElse(criterion.info.name, Nil)
          AssessmentRubricCriterion(criterion, competencies)
        )

      content -> AssessmentRubric(rubric.info.name, rubric.data.title, criteria)
    ).toMap
  end getRubrics
end RubricServiceImpl
