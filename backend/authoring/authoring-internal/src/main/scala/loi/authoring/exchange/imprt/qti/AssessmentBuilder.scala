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

package loi.authoring.exchange.imprt.qti

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.component.annotation.Service
import loi.asset.assessment.model.*
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.Group
import loi.authoring.exchange.imprt.ImporterUtils.{buildEmptyEdgesFromAssets, guid}
import loi.authoring.exchange.imprt.NodeExchangeBuilder
import loi.authoring.exchange.model.NodeExchangeData

@Service
class AssessmentBuilder(mapper: ObjectMapper):

  def buildAssessmentOfType(
    assessmentType: String,
    title: String,
    questions: Seq[NodeExchangeData]
  ): NodeExchangeData =
    assessmentType match
      case "poolAssessment.1" => buildPoolAssessment(title, questions)
      case "diagnostic.1"     => buildDiagnostic(title, questions)
      case _                  => buildAssessment(title, questions)

  private def buildAssessment(title: String, questions: Seq[NodeExchangeData]): NodeExchangeData =
    val edges = buildEmptyEdgesFromAssets(questions, Group.Questions)
    val data  = Assessment(
      title = title,
      maxAttempts = Some(0),
      name = None,
      author = None,
      attribution = None
    )
    NodeExchangeBuilder
      .builder(guid, AssetTypeId.Assessment.entryName, mapper.valueToTree(data))
      .edges(edges)
      .build()
  end buildAssessment

  private def buildCheckpoint(title: String, questions: Seq[NodeExchangeData]): NodeExchangeData =
    val edges = buildEmptyEdgesFromAssets(questions, Group.Questions)
    val data  = Checkpoint(
      title = title,
      name = None,
      author = None,
      attribution = None
    )
    NodeExchangeBuilder
      .builder(guid, AssetTypeId.Checkpoint.entryName, mapper.valueToTree(data))
      .edges(edges)
      .build()
  end buildCheckpoint

  private def buildPoolAssessment(title: String, questions: Seq[NodeExchangeData]): NodeExchangeData =
    val edges = buildEmptyEdgesFromAssets(questions, Group.Questions)
    val data  = PoolAssessment(
      title = title,
      maxAttempts = Some(0),
      name = None,
      scoringOption = Some(ScoringOption.MostRecentAttemptScore),
      author = None,
      attribution = None,
      numberOfQuestionsForAssessment = questions.length,
      useAllQuestions = Some(true)
    )
    NodeExchangeBuilder
      .builder(guid, AssetTypeId.PoolAssessment.entryName, mapper.valueToTree(data))
      .edges(edges)
      .build()
  end buildPoolAssessment

  private def buildDiagnostic(title: String, questions: Seq[NodeExchangeData]): NodeExchangeData =
    val edges = buildEmptyEdgesFromAssets(questions, Group.Questions)
    val data  = Diagnostic(
      title = title,
      name = None,
      author = None,
      attribution = None
    )
    NodeExchangeBuilder
      .builder(guid, AssetTypeId.Diagnostic.entryName, mapper.valueToTree(data))
      .edges(edges)
      .build()
  end buildDiagnostic
end AssessmentBuilder
