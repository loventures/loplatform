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

import ERLearnerAssignmentsPageHeader from '../../commonPages/assignmentsPage/components/ERLearnerAssignmentsPageHeader';
import LearnerAssignmentsList from '../../commonPages/assignmentsPage/components/LearnerAssignmentsList';
import ERContentContainer from '../../landmarks/ERContentContainer';
import { useTranslation } from '../../i18n/translationContext';
import GradebookColumnsLoader from '../../loaders/GradebookColumnsLoader';
import React from 'react';
import { ERLandmark } from '../../landmarks/ERLandmarkProvider.tsx';

const ERLearnerAssignmentsListPage: React.FC = () => {
  const translate = useTranslation();
  /** TODO: remove suspense from here. */
  return (
    <ERContentContainer title={translate('ER_SCORES_TITLE')}>
      <ERLandmark
        landmark="content"
        id="er-learner-scores"
      >
        <div className="container p-0">
          <div className="card er-content-wrapper mb-2 m-md-3 m-lg-4">
            <div className="card-body">
              <ERLearnerAssignmentsPageHeader showName={false} />
              <div className="mt-3">
                <GradebookColumnsLoader>
                  <LearnerAssignmentsList />
                </GradebookColumnsLoader>
              </div>
            </div>
          </div>
        </div>
      </ERLandmark>
    </ERContentContainer>
  );
};

export default ERLearnerAssignmentsListPage;
