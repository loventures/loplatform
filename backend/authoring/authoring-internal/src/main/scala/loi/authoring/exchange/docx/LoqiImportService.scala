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

package loi.authoring.exchange.docx

import com.learningobjects.cpxp.component.annotation.Service
import loi.asset.competency.service.CompetencyService
import loi.authoring.edge.Group.Questions
import loi.authoring.edge.{EdgeService, TraverseGraph}
import loi.authoring.exchange.docx.DocxErrors.DocxValidationError
import loi.authoring.node.AssetNodeService
import loi.authoring.workspace.WriteWorkspace
import loi.authoring.write.{AddEdge, AddNode, SetEdgeOrder, WriteOp}
import scalaz.\/

import java.io.{File, FileInputStream}
import java.util.UUID
import scala.util.Using

@Service
class LoqiImportService(
  nodeService: AssetNodeService,
  competencyService: CompetencyService,
)(implicit
  es: EdgeService
):

  def importLoqiAssessment(
    ws: WriteWorkspace,
    assessmentName: UUID,
    file: File,
  ): List[DocxValidationError] \/ Warned[List[WriteOp]] =
    val assessment = nodeService.load(ws).byName(assessmentName).get
    val graph      = es.stravaigeOutGraph(TraverseGraph.fromSource(assessmentName).traverse(Questions).noFurther, ws)

    val existingQuestionEdgeNames = graph.outEdges(assessment).map(_.name)

    val competencyMap = competencyService.getCompetenciesByName(ws)

    Using.resource(new FileInputStream(file)) { fis =>
      for
        (warnings, addOps) <-
          DocxValidator.toWriteOps(fis, Some(assessment), competencyMap).toDisjunction.leftMap(_.list.toList)
        edgeOps             = addOps collect { case a: AddNode[?] =>
                                AddEdge(assessmentName, a.name, Questions)
                              }
        edgeOrderOp         = SetEdgeOrder(assessmentName, Questions, existingQuestionEdgeNames ++ edgeOps.map(_.name))
        ops                 = addOps ++ edgeOps :+ edgeOrderOp
      yield warnings -> ops
    }
  end importLoqiAssessment
end LoqiImportService
