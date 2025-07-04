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

package loi.cp.quiz.question.api

import java.util.UUID

import loi.asset.TitleProperty
import loi.authoring.asset.Asset
import loi.cp.competency.Competency

final case class QuestionCompetencyDto(id: Long, name: UUID, title: String)

object QuestionCompetencyDto:
  def apply(competency: Competency): QuestionCompetencyDto =
    QuestionCompetencyDto(competency.id, competency.nodeName, competency.title)

  def apply(comeptencies: Seq[Competency]): Seq[QuestionCompetencyDto] =
    comeptencies.map(QuestionCompetencyDto(_))

  def apply(asset: Asset[?]): QuestionCompetencyDto =
    QuestionCompetencyDto(asset.info.id, asset.info.name, TitleProperty.fromNode(asset).getOrElse(""))
