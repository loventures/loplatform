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

import { gradeAttemptActionCreator } from '../redux/submissionActivityActions.ts';
import { DisplaySubmissionAttempt } from '../submissionActivity';
import {
  ContentWithNebulousDetails,
  ViewingAs,
} from '../../../../courseContentModule/selectors/contentEntry';
import { Translate, withTranslation } from '../../../../i18n/translationContext.tsx';
import React from 'react';
import { ConnectedProps, connect } from 'react-redux';

const connector = connect(null, {
  jumpToGrader: gradeAttemptActionCreator,
});

interface GradeJumpButtonProps extends ConnectedProps<typeof connector> {
  translate: Translate;
  content: ContentWithNebulousDetails;
  attempt: DisplaySubmissionAttempt;
  viewingAs: ViewingAs;
}

const GraderJumpButton: React.FC<GradeJumpButtonProps> = ({
  translate,
  content,
  attempt,
  viewingAs,
  jumpToGrader,
}) => (
  <button
    className="goto-grader btn btn-secondary"
    onClick={() => jumpToGrader(content, attempt, viewingAs)}
  >
    <span className="goto-grade-label">{translate('GRADER_GO_TO')}</span>
    <i className="icon-circle-right"></i>
  </button>
);

export default connector(withTranslation(GraderJumpButton));
