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

import AttachmentsViewer from '../components/attachments/AttachmentsViewer.js';
import { HtmlWithMathJax } from '../components/HtmlWithMathjax.js';
import RubricGrid from '../components/rubric/RubricGrid.js';
import GradeBadge from './GradeBadge.jsx';
import { withTranslation } from '../i18n/translationContext.js';

const ViewRubricScore = ({
  translate,
  score,
  scoreAttachments = [],
  rubric,
  rubricFeedback,

  feedback = score && score.feedback,
  hasScoreAttachments = !!scoreAttachments.length,
  hasAnyFeedback = feedback || hasScoreAttachments,
}) => (
  <div className="view-composite-grade mt-4 mb-3 mx-3">
    {score && (
      <div
        className="h1 my-4 flex-center-center"
        title={translate('ASSIGNMENT_GRADE_DESCRIPTION')}
      >
        <GradeBadge
          grade={score}
          outline={true}
          display="percent"
          percent="half"
        />
      </div>
    )}

    {rubric && (
      <RubricGrid
        noTitle
        rubric={rubric}
        rubricScore={score}
        rubricFeedback={rubricFeedback}
      />
    )}

    {hasAnyFeedback && (
      <div className="mt-2 mb-4 instructor-feedback alert alert-info bg-transparent">
        <div className="font-weight-bold">{translate('ASSIGNMENT_INSTRUCTOR_FEEDBACK')}</div>
        {feedback && (
          <div className="mt-1">
            <HtmlWithMathJax html={feedback.replace(/\n/gi, '<br/>')} />
          </div>
        )}
        {hasScoreAttachments && (
          <AttachmentsViewer
            className="mt-2"
            attachments={scoreAttachments}
          />
        )}
      </div>
    )}
  </div>
);

export default withTranslation(ViewRubricScore);
