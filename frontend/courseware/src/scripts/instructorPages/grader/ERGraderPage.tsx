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

import { ContentLite } from '../../api/contentsApi';
import ERContentTitle from '../../commonPages/contentPlayer/ERContentTitle';
import { GraderEntry } from '../../instructorPages/grader/parts/GraderEntry';
import ERContentContainer from '../../landmarks/ERContentContainer';
import { selectPageContent } from '../../courseContentModule/selectors/contentEntrySelectors';
import { useCourseSelector } from '../../loRedux';
import { QuizInfoBar } from '../../contentPlayerComponents/activityViews/quiz/views/QuizInfoBar';
import { SubmissionActivityLoader } from '../../contentPlayerComponents/activityViews/submission/loaders';
import { selectSubmissionActivityComponent } from '../../contentPlayerComponents/activityViews/submission/redux/submissionActivitySelectors';
import { useTranslation } from '../../i18n/translationContext';
import ForLearnerLoader from '../../loaders/ForLearnerLoader';
import { isSubmission } from '../../utilities/contentTypes';
import { selectCurrentUser } from '../../utilities/rootSelectors';
import React from 'react';

const SubAssInfo: React.FC = () => {
  const { submissionActivity } = useCourseSelector(selectSubmissionActivityComponent);
  return <QuizInfoBar instructions={submissionActivity.assessment.instructions} />;
};

const ERGraderPage: React.FC = () => {
  const translate = useTranslation();
  const content = useCourseSelector(selectPageContent) as ContentLite;
  const viewingAs = useCourseSelector(selectCurrentUser);

  const isSubAss = isSubmission(content);

  return (
    <ERContentContainer title={translate('GRADER_TITLE')}>
      <div className="container p-0">
        <div className="card er-content-wrapper mb-2 m-md-3 m-lg-4">
          <div className="card-body">
            <ERContentTitle content={content} />

            {isSubAss && (
              <SubmissionActivityLoader
                content={content}
                viewingAs={viewingAs}
              >
                <SubAssInfo />
              </SubmissionActivityLoader>
            )}

            <div className="pt-4">
              <ForLearnerLoader>
                <GraderEntry />
              </ForLearnerLoader>
            </div>
          </div>
        </div>
      </div>
    </ERContentContainer>
  );
};

export default ERGraderPage;
