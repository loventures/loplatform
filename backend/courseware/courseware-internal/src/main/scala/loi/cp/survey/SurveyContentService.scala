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

package loi.cp.survey

import com.learningobjects.cpxp.component.annotation.Service
import loi.asset.survey.Survey1
import loi.authoring.asset.Asset
import loi.authoring.edge.{EdgeService, Group, TraverseGraph}
import loi.authoring.render.RenderService
import loi.authoring.workspace.ReadWorkspace
import loi.cp.content.CustomisationTraversedGraphOps.*
import loi.cp.content.{Content, CourseContent}
import loi.cp.reference.EdgePath
import scaloi.syntax.collection.*

@Service
class SurveyContentService(
  renderService: RenderService,
)(implicit
  edgeService: EdgeService
):

  def loadSurveyTrees(ws: ReadWorkspace, contents: List[CourseContent]): List[SurveyTree] =
    val graph = edgeService.stravaigeOutGraphs(
      TraverseGraph
        .fromSources(contents.flatMap(_.survey).map(_._2)*)
        .traverse(Group.Questions)
        .noFurther :: Nil,
      ws
    )

    val rendees   = graph.edges.filter(_.group == Group.Questions).toList
    val rendereds = renderService.render(ws, Nil, rendees)._2.groupUniqBy(_.id)

    for
      content                  <- contents
      (surveyEdge, surveyName) <- content.survey.toList
      survey                   <- graph.node[Survey1](surveyName).toList
    yield
      val surveyEdgeNames = content.edgeNames ::: surveyEdge :: Nil
      // we are only using .customisedOutTrees because it builds my EdgePaths for us
      val questionTrees   = graph.customisedOutTrees(survey, surveyEdgeNames)
      val questions       = questionTrees map { node =>
        val edge = node.rootLabel.edge
        node.rootLabel.copy(edge = rendereds.getOrElse(edge.id, edge))
      }

      SurveyTree(EdgePath(surveyEdgeNames), survey, questions)
    end for
  end loadSurveyTrees
end SurveyContentService

case class SurveyTree(
  surveyEdgePath: EdgePath,
  surveyAsset: Asset[Survey1], // is `survey.asset`, but with the right type
  questions: List[Content]
)
