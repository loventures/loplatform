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

import RubricGrid from '../../../../components/rubric/RubricGrid.tsx';
import { SubmissionActivity } from '../submissionActivity';
import React from 'react';
import { react2angular } from 'react2angular';

import ContentInstructions from '../../../parts/ContentInstructions';

const SubmissionInstructionsView: React.FC<{
  submissionActivity: SubmissionActivity;
  instructions?: any;
}> = ({ submissionActivity, instructions = submissionActivity.assessment.instructions }) => (
  <div>
    <ContentInstructions instructions={instructions} />

    {submissionActivity.assessment.rubric && (
      <div className="my-4 mx-3">
        <RubricGrid rubric={submissionActivity.assessment.rubric} />
      </div>
    )}
  </div>
);

export default SubmissionInstructionsView;

export const submissionInstructionsViewComponent = angular
  .module('lo.contentPlayer.submissionInstructionsView', [])
  .component(
    'submissionInstructionsView',
    react2angular(SubmissionInstructionsView, ['submissionActivity'], [])
  );
