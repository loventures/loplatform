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

package loi.cp.quiz.settings

import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.learningobjects.cpxp.scala.util.EnumDeserializer
import enumeratum.{Enum, EnumEntry}

/** A policy describing when results (scores and remediation) should be released to the user.
  *
  * @param resultReleaseTime
  *   at what time should the results be released to the user
  * @param remediationReleaseCondition
  *   what conditions must be meet for the question remediation to be released (scores will be released regardless)
  */
case class ResultsPolicy(resultReleaseTime: ResultReleaseTime, remediationReleaseCondition: ReleaseRemediationCondition)

@JsonSerialize(`using` = classOf[ToStringSerializer])
@JsonDeserialize(`using` = classOf[ResultReleaseTime.ResultReleaseTimeDeserializer])
sealed abstract class ResultReleaseTime extends EnumEntry

object ResultReleaseTime extends Enum[ResultReleaseTime]:
  val values = findValues

  /** The result for the response should be released when the response is scored.
    */
  case object OnResponseScore extends ResultReleaseTime

  /** The result for the response should be released when the entire attempt is score is submitted.
    */
  case object OnAttemptScore extends ResultReleaseTime

  private[settings] class ResultReleaseTimeDeserializer extends EnumDeserializer[ResultReleaseTime](ResultReleaseTime)
end ResultReleaseTime

/** A configuration describing whether the correct answer and question remediation (if any) should be made available to
  * the learner. Instructors should always have access to this data.
  */
@JsonSerialize(`using` = classOf[ToStringSerializer])
@JsonDeserialize(`using` = classOf[ReleaseRemediationCondition.ResultReleaseConditionDeserializer])
sealed abstract class ReleaseRemediationCondition extends EnumEntry

object ReleaseRemediationCondition extends Enum[ReleaseRemediationCondition]:
  val values = findValues

  /** The correct answer and remediation should always be released at [[ResultReleaseTime]].
    */
  case object AnyResponse extends ReleaseRemediationCondition

  /** The correct answer and remediation should only be released at [[ResultReleaseTime]] if it is correct (all points
    * possible are attained).
    */
  case object OnCorrectResponse extends ReleaseRemediationCondition

  private[settings] class ResultReleaseConditionDeserializer
      extends EnumDeserializer[ReleaseRemediationCondition](ReleaseRemediationCondition)
end ReleaseRemediationCondition
