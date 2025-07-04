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

import { getLastManualSync, syncEntireGradebook } from '../../../api/gradebookApi';
import Course from '../../../bootstrap/course';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import { Translate, withTranslation } from '../../../i18n/translationContext';
import React, { useEffect, useState } from 'react';
import { Tooltip } from 'reactstrap';
import { TfiInfoAlt, TfiReload } from 'react-icons/tfi';

dayjs.extend(relativeTime);

const GradebookSyncButton: React.FC<{ translate: Translate }> = ({ translate }) => {
  // SyncStatus of null means never manually synced
  type SyncStatus = 'syncing' | 'synced' | 'failed' | null;
  const [syncStatus, setSyncStatus] = useState<SyncStatus>(null);

  const [lastManualSync, setLastManualSync] = useState<string | null>(null);

  const [infoTitle, setInfoTitle] = useState<string>(
    translate('GRADEBOOK_SYNC.NEVER_SYNCED_TITLE')
  );
  const [infoBody, setInfoBody] = useState<string | undefined>(undefined);
  const [showInfo, setShowInfo] = useState(false);

  useEffect(() => {
    getLastManualSync(Course.id).then(lms => {
      setLastManualSync(lms);
      lms && setSyncStatus('synced');
    });
  }, []);

  useEffect(() => {
    lastManualSync &&
      setInfoTitle(
        translate('GRADEBOOK_SYNC.SYNCED_TITLE', {
          lastManualSync: dayjs(lastManualSync).fromNow(),
        })
      );
  }, [showInfo, lastManualSync]);

  const handleSync = () => {
    if (syncStatus !== 'syncing') {
      setSyncStatus('syncing');

      syncEntireGradebook(Course.id)
        .then(lms => {
          setSyncStatus('synced');
          setLastManualSync(lms);
          setInfoBody(translate('GRADEBOOK_SYNC.SYNCED_BODY'));
        })
        .catch(() => {
          setSyncStatus('failed');
          setLastManualSync(null);
          setInfoTitle(translate('GRADEBOOK_SYNC.FAILED_TITLE'));
          setInfoBody(translate('GRADEBOOK_SYNC.FAILED_BODY'));
        })
        .finally(() => {
          setShowInfo(true);
        });
    }
  };

  return (
    <>
      <button
        id="gradebook-sync-info"
        className="btn btn-link p-1 d-flex"
        onClick={() => setShowInfo(!showInfo)}
      >
        {syncStatus === 'syncing' ? (
          <TfiReload className="counter-clockwise-spin" />
        ) : (
          <TfiInfoAlt />
        )}
        <span className="sr-only">
          {infoTitle}
          {infoBody && ` - ${infoBody}`}
        </span>
        <Tooltip
          target="gradebook-sync-info"
          placement="left"
          isOpen={showInfo}
          innerClassName="bg-dark text-light"
        >
          <span className="d-flex flex-column align-items-center px-2">
            <small className="font-italic font-weight-bold">{infoTitle}</small>
            <small className="text-center">{infoBody}</small>
          </span>
        </Tooltip>
      </button>

      <button
        disabled={syncStatus === 'syncing'}
        className="btn btn-primary gradebook-sync"
        onClick={handleSync}
      >
        {translate('GRADEBOOK_SYNC')}
      </button>
    </>
  );
};

export default withTranslation(GradebookSyncButton);
