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

import { withTranslation } from '../../../i18n/translationContext';
import { withPromise } from '../../../utilities/WithPromise';
import { Histories, ItemSyncStatus } from './LtiItemSyncStatus';

import Course from '../../../bootstrap/course';
import { lojector } from '../../../loject';

export const columnIsSynced = history => {
  const latest = latestColumnSyncStatus(history);
  return latest && latest.type === ItemSyncStatus.Synced;
};

export const columnSyncStatuses = history => {
  return history?.ags ?? [];
};

export const latestColumnSyncStatus = history => {
  const statuses = columnSyncStatuses(history);
  return statuses[statuses.length - 1];
};

const LtiColumnSyncHistory = compose(
  withTranslation,
  withPromise({
    get: props =>
      lojector.get('GradebookAPI').getSingleColumnSyncHistory(Course.id, props.columnId),
  }),
  withState('syncing', 'setSyncing', false),
  withState('syncingAll', 'setSyncingAll', false),
  withHandlers({
    syncColumn: props => () => {
      props.setSyncing(true);
      // sync column
      lojector
        .get('GradebookAPI')
        .syncExternalColumn(Course.id, props.columnId)
        .then(() => {
          // reload the grade sync status
          setTimeout(() => {
            props.reload().then(() => {
              props.setSyncing(false);
            });
          }, 2000);
        });
    },
    syncAllGrades: props => () => {
      props.setSyncingAll(true);
      lojector
        .get('GradebookAPI')
        .syncExternalGradesForColumn(Course.id, props.columnId)
        .then(() => {
          // TODO: maybe "go to gradebook page" would be better than reloading?
          setTimeout(() => {
            props.reload().then(() => {
              props.setSyncingAll(false);
            });
          }, 2000);
        });
    },
  })
)(({ loading, data, syncing, syncColumn, translate, syncAllGrades, syncingAll }) =>
  loading ? (
    <span>'loading...'</span>
  ) : (
    <div>
      <h5>{translate('SYNC_STATUS_TITLE_COLUMN', { column: data.column.title })}</h5>
      <Histories
        histories={data.history.ags}
        renderItem={renderAgs(translate)}
      />
      <Button
        className={'btn-sync'}
        disabled={syncing || syncingAll}
        onClick={syncColumn}
      >
        <i className={'icon icon-reload' + (syncing ? ' de-spin' : '')}></i>{' '}
        {translate('SYNC_STATUS_SYNC_NOW')}
      </Button>
      <Button
        className={'btn-sync d-block mt-2'}
        disabled={!columnIsSynced(data.history) || syncing || syncingAll}
        onClick={syncAllGrades}
      >
        <i className={'icon icon-reload' + (syncingAll ? ' de-spin' : '')}></i>{' '}
        {translate('SYNC_STATUS_SYNC_ALL_GRADES')}
      </Button>
    </div>
  )
);

const renderAgs =
  translate =>
  ({ syncedValue }) => (
    <span>
      {syncedValue.label} -{' '}
      <b>
        {translate(
          syncedValue.scoreMaximum === 1
            ? 'SYNC_STATUS_TOTAL_POINTS'
            : 'SYNC_STATUS_TOTAL_POINTS_PLURAL',
          { total: syncedValue.scoreMaximum }
        )}
      </b>
    </span>
  );

export default LtiColumnSyncHistory;
