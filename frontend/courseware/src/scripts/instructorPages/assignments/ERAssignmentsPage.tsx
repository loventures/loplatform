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

import Course from '../../bootstrap/course';
import ERContentContainer from '../../landmarks/ERContentContainer';
import { CourseState } from '../../loRedux';
import { fetchColumnsAction } from '../../loRedux/columnsReducer';
import { fetchContentsAction } from '../../loRedux/contentsReducer';
import { fetchEnrollmentCountAction } from '../../loRedux/enrollmentCountReducer';
import { useTranslation } from '../../i18n/translationContext';
import { loading, sequenceObj } from '../../types/loadable';
import { selectCurrentUser } from '../../utilities/rootSelectors';
import * as React from 'react';

import { AttemptOverviews } from './AttemptOverviews';
import { ConnectedLoader } from './ConnectedLoader';
import ERNonContentTitle from '../../commonPages/contentPlayer/ERNonContentTitle';

const selector = (state: CourseState) => {
  const currentUserId: number = selectCurrentUser(state).id;
  const cols = state.api.columns;
  const conts = state.api.contents[currentUserId] || loading;
  const enrollmentCount = state.api.enrollmentCount;

  // we have:
  // Loading<Column[]>
  // Loading<Contents[]>
  // Loading<number>
  //
  // we need:
  //   Loading<{columns: Column[], contents: Contents[], enrollmentCount: number}>
  //
  // so we 'traverse' over the applicative with sequence using the power of functional programming
  return sequenceObj({
    columns: cols,
    contents: conts,
    enrollmentCount,
  });
};

const ERAssignmentsPage = () => {
  const translate = useTranslation();

  return (
    <ERContentContainer title={translate('INSTRUCTOR_ASSIGNMENTS')}>
      <div className="container p-0">
        <div className="card er-content-wrapper mb-2 m-md-3 m-lg-4">
          <div className="card-body">
            <ERNonContentTitle label={translate('INSTRUCTOR_ASSIGNMENTS')} />

            <ConnectedLoader
              onMount={(state, dispatch) => {
                const courseId = Course.id;
                const currentUserId: number = selectCurrentUser(state).id;
                dispatch(fetchColumnsAction(courseId));
                dispatch(fetchContentsAction(courseId, currentUserId));
                dispatch(fetchEnrollmentCountAction());
              }}
              selector={selector}
            >
              {([props]) => <AttemptOverviews {...props} />}
            </ConnectedLoader>
          </div>
        </div>
      </div>
    </ERContentContainer>
  );
};

export default ERAssignmentsPage;
