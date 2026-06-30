/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import { useState } from 'react';
import { Button } from 'reactstrap';

import { withTranslation } from '../../../i18n/translationContext';
import { withPromise } from '../../../utilities/WithPromise';
import { Histories, ItemSyncStatus } from './LtiItemSyncStatus';

import Course from '../../../bootstrap/course';
import { gradebookAPI } from '../../../services/gradebookAPI.ts';

export const columnIsSynced = (history: any) => {
  const latest = latestColumnSyncStatus(history);
  return latest && latest.type === ItemSyncStatus.Synced;
};

export const columnSyncStatuses = (history: any) => {
  return history?.ags ?? [];
};

export const latestColumnSyncStatus = (history: any) => {
  const statuses = columnSyncStatuses(history);
  return statuses[statuses.length - 1];
};

const LtiColumnSyncHistoryView = (props: any) => {
  const { loading, data, translate, reload, columnId } = props;
  const [syncing, setSyncing] = useState(false);
  const [syncingAll, setSyncingAll] = useState(false);

  const syncColumn = () => {
    setSyncing(true);
    // sync column
    gradebookAPI.syncExternalColumn(Course.id, columnId).then(() => {
      // reload the grade sync status
      setTimeout(() => {
        reload().then(() => {
          setSyncing(false);
        });
      }, 2000);
    });
  };

  const syncAllGrades = () => {
    setSyncingAll(true);
    gradebookAPI.syncExternalGradesForColumn(Course.id, columnId).then(() => {
      // TODO: maybe "go to gradebook page" would be better than reloading?
      setTimeout(() => {
        reload().then(() => {
          setSyncingAll(false);
        });
      }, 2000);
    });
  };

  return loading ? (
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
  );
};

const LtiColumnSyncHistory = withTranslation(
  withPromise({
    get: props => gradebookAPI.getSingleColumnSyncHistory(Course.id, props.columnId),
  })(LtiColumnSyncHistoryView)
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
