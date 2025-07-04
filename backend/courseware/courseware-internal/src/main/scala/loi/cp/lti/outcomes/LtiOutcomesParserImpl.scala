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

package loi.cp.lti.outcomes

import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.lti.outcomes.LtiOutcomesParser.*
import org.imsglobal.lti.*

import java.io.StringReader
import javax.xml.bind.JAXB

@Service
class LtiOutcomesParserImpl extends LtiOutcomesParser:

  override def parseOutcome(requestBody: String): ProcessResult[BasicOutcomesRequest] =
    parseLtiOutcome(requestBody)

  def parseLtiOutcome(requestBody: String): ProcessResult[BasicOutcomesRequest] =
    val envelope = JAXB.unmarshal(new StringReader(requestBody), classOf[ImsxPOXEnvelopeType])
    for
      requestId <- getRequestId(envelope)
      body      <- required(envelope.getImsxPOXBody, "imsx_POXBody")
      req       <- parseBody(requestId, body)
    yield req

  override def parseSourcedId(sourcedId: String): ProcessResult[GradeSourcedId] =
    GradeSourcedId.decode(sourcedId).toRight(s"The sourcedid: $sourcedId could not be resolved")

  private def parseBody(requestId: Option[String], body: ImsxPOXBodyType): ProcessResult[BasicOutcomesRequest] =
    getReadResult(requestId, body)
      .orElse(getDeleteResult(requestId, body))
      .orElse(getReplaceResult(requestId, body))
      .getOrElse(Left("readResult, deleteResult, and replaceResult are the only supported methods"))

  private def getReadResult(requestId: Option[String], body: ImsxPOXBodyType): Option[ProcessResult[ReadResult]] =
    Option(body.getReadResultRequest) map { req =>
      for
        resultRecord <- required(req.getResultRecord, "resultRecord")
        sourcedid    <- getSourcedId(resultRecord)
      yield ReadResult(requestId, sourcedid)
    }

  private def getDeleteResult(requestId: Option[String], body: ImsxPOXBodyType): Option[ProcessResult[DeleteResult]] =
    Option(body.getDeleteResultRequest) map { req =>
      for
        resultRecord <- required(req.getResultRecord, "resultRecord")
        sourcedid    <- getSourcedId(resultRecord)
      yield DeleteResult(requestId, sourcedid)
    }

  private def getReplaceResult(requestId: Option[String], body: ImsxPOXBodyType): Option[ProcessResult[ReplaceResult]] =
    Option(body.getReplaceResultRequest) map { req =>
      for
        resultRecord <- required(req.getResultRecord, "resultRecord")
        sourcedid    <- getSourcedId(resultRecord)
        score        <- getScore(resultRecord)
      yield ReplaceResult(requestId, sourcedid, score)
    }

  private def getRequestId(envelope: ImsxPOXEnvelopeType): ProcessResult[Option[String]] =
    for
      header <- required(envelope.getImsxPOXHeader, "imsx_POXHeader")
      info   <- required(header.getImsxPOXRequestHeaderInfo, "imsx_POXRequestHeaderInfo")
    yield Option(info.getImsxMessageIdentifier)

  private def getSourcedId(resultRecord: ResultRecordType): ProcessResult[String] =
    for
      sourcedGuid <- required(resultRecord.getSourcedGUID, "sourcedGUID")
      sourcedId   <- required(sourcedGuid.getSourcedId, "sourcedId")
    yield sourcedId

  private def getScore(resultRecord: ResultRecordType): ProcessResult[String] =
    for
      result      <- required(resultRecord.getResult, "result")
      resultScore <- required(result.getResultScore, "resultScore")
      score       <- required(resultScore.getTextString, "textString")
    yield score

  def required[A](a: => A, value: String): ProcessResult[A] =
    Option(a).toRight(s"$value is required")
end LtiOutcomesParserImpl
