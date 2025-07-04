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

import { withState, compose, withHandlers } from 'recompose';
import { Button } from 'reactstrap';
import { getUserFullName } from '../../../utilities/getUserFullName';

import { withTranslation } from '../../../i18n/translationContext';
import { withPromise } from '../../../utilities/WithPromise';
import { Histories } from './LtiItemSyncStatus';

import Course from '../../../bootstrap/course';
import { lojector } from '../../../loject';

/* eslint-disable angular/json-functions */

export const gradeSyncStatuses = grade => {
  if (grade && grade.gradeSyncHistory) {
    return grade.gradeSyncHistory.outcomes1 || grade.gradeSyncHistory.agsScore || [];
  } else {
    return [];
  }
};

export const latestGradeSyncStatus = grade => {
  const history = gradeSyncStatuses(grade);
  return history[history.length - 1];
};

const LtiGradeSyncHistory = compose(
  withTranslation,
  withPromise({
    get: props =>
      lojector
        .get('GradebookAPI')
        .getSingleGradeSyncHistory(Course.id, props.userId, props.columnId),
  }),
  withState('syncing', 'setSyncing', false),
  withHandlers({
    syncGrade: props => () => {
      props.setSyncing(true);
      // sync grade
      lojector
        .get('GradebookAPI')
        .syncExternalGrade(Course.id, props.userId, props.columnId)
        .then(() => {
          // reload the grade sync status
          setTimeout(() => {
            props.reload().then(() => {
              props.setSyncing(false);
            });
          }, 2000);
        });
    },
  })
)(({ loading, data, translate, syncing, syncGrade }) =>
  loading ? (
    <div>loading...</div> // loadingSpinner
  ) : (
    <div className="grade-sync-history">
      <h5>
        {data.column
          ? translate('SYNC_STATUS_TITLE_GRADE', {
              user: getUserFullName(data.user),
              column: data.column.title,
            })
          : translate('SYNC_STATUS_TITLE_GRADE_OVERALL', {
              user: getUserFullName(data.user),
            })}
      </h5>
      <Histories
        histories={data.history.outcomes1}
        renderItem={formatOutcomes1Grade}
      />
      <Histories
        histories={data.history.agsScore}
        renderItem={formatAgsGrade}
      />
      <Button
        className={'btn-sync'}
        disabled={syncing}
        onClick={syncGrade}
      >
        <i className={'icon icon-reload' + (syncing ? ' de-spin' : '')}></i>{' '}
        {translate('SYNC_STATUS_SYNC_NOW')}
      </Button>
    </div>
  )
);

const formatAgsGrade = ({ syncedValue }) =>
  `${Math.round(syncedValue.scoreGiven)} / ${syncedValue.scoreMaximum}`;

const formatOutcomes1Grade = grade => <pre>{JSON.stringify(grade, null, 2)}</pre>; // eslint-disable angular/json-functions

export default LtiGradeSyncHistory;
