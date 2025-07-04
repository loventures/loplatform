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

export const ViewOverallGradeRight = 'loi.cp.lwgrade.right$ViewOverallGradeRight';
export const ViewLtiSyncStatusRight = 'loi.cp.lwgrade.right$ViewLtiSyncStatusRight';

export const hasLtiHistoryRight =
  window.lo_platform.user.rights.indexOf(ViewLtiSyncStatusRight) !== -1;

export const ItemSyncStatus = {
  Queued: 'Queued',
  Synced: 'Synced',
  Attempted: 'Attempted',
  Failed: 'Failed',
};

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
