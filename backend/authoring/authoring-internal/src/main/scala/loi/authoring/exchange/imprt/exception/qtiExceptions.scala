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

package loi.authoring.exchange.imprt.exception

import com.learningobjects.de.web.UncheckedMessageException
import loi.authoring.asset.factory.AssetTypeId
import loi.cp.i18n.AuthoringBundle

case class FatalQtiImportException(errorGuid: String, message: String)
    extends UncheckedMessageException(AuthoringBundle.message("qti.import.fatalError", errorGuid, message))

case class UnsupportedAssetTypeException(assetTypeId: AssetTypeId)
    extends UncheckedMessageException(AuthoringBundle.message("qti.import.unsupportedAssetType", assetTypeId.entryName))

case class UnsupportedQuestionTypeException(questionId: String, questionType: String)
    extends UncheckedMessageException(
      AuthoringBundle.message("qti.import.unsupportedQuestionType", questionId, questionType)
    )

case class AssessmentFileNotDefinedException()
    extends UncheckedMessageException(AuthoringBundle.message("qti.import.assessmentFileNotDefined"))

case class AssessmentFileNotFoundException(filename: String)
    extends UncheckedMessageException(AuthoringBundle.message("qti.import.assessmentFileNotFound", filename))

case class QuestionFileNotFoundException(filename: String)
    extends UncheckedMessageException(AuthoringBundle.message("qti.import.questionFileNotFound", filename))

case class AssessmentItemRefNotFoundException(filename: String, id: String)
    extends UncheckedMessageException(AuthoringBundle.message("qti.import.assessmentItemRefNotFound", filename, id))

case class MissingCorrectChoiceException(filename: String)
    extends UncheckedMessageException(AuthoringBundle.message("qti.import.correctChoiceNotSpecified", filename))

case class ChoiceNotFoundException(filename: String, item: String)
    extends UncheckedMessageException(AuthoringBundle.message("qti.import.choice.choiceNotFound", filename, item))

case class MatchingChoiceNotFoundException(filename: String, item: String)
    extends UncheckedMessageException(AuthoringBundle.message("qti.import.matching.choiceNotFound", filename, item))

case class InvalidAssessmentItemException(filename: String)
    extends UncheckedMessageException(AuthoringBundle.message("qti.import.invalidAssessmentItem", filename))

case class InvalidAssessmentTestException(filename: String)
    extends UncheckedMessageException(AuthoringBundle.message("qti.import.invalidAssessmentTest", filename))

case class InvalidQti1Exception(filename: String)
    extends UncheckedMessageException(AuthoringBundle.message("qti.import.invalidQti1", filename))

case class HotspotImageNotSpecifiedException(questionId: String)
    extends UncheckedMessageException(AuthoringBundle.message("qti.import.hotspot.imageNotSpecified", questionId))

case class HotspotImageNotFoundException(questionId: String, src: String)
    extends UncheckedMessageException(AuthoringBundle.message("qti.import.hotspot.imageNotFound", questionId, src))

case class HotspotUnsupportedShapeException(questionId: String, shape: String)
    extends UncheckedMessageException(AuthoringBundle.message("qti.import.hotspot.unsupportedShape", questionId, shape))

case class FillInTheBlankInvalidBlankException(filename: String)
    extends UncheckedMessageException(AuthoringBundle.message("qti.import.fillInTheBlank.invalidBlank", filename))

case class FillInTheBlankMissingBlankException(filename: String)
    extends UncheckedMessageException(AuthoringBundle.message("qti.import.fillInTheBlank.missingBlank", filename))
