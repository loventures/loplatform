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

import { ConfirmationTypes } from '../modals/ConfirmModal';
import { trackAuthoringEvent } from './index';

// Project Navigation
export const trackFeedbackIndexDownload = () => trackAuthoringEvent('Feedback Index - Download');
export const trackFeedbackIndexRefresh = () => trackAuthoringEvent('Feedback Index - Refresh');
export const trackFeedbackIndexPage = dir => trackAuthoringEvent('Feedback Index - Page', dir);
export const trackFeedbackIndexDetails = () => trackAuthoringEvent('Feedback Index - Details');
export const trackFeedbackPageNav = kind => trackAuthoringEvent('Feedback Page - Nav', kind);
export const trackFeedbackPageReply = () => trackAuthoringEvent('Feedback Page - Reply');
export const trackFeedbackPageView = item => trackAuthoringEvent('Feedback Page - View', item);

export const trackFeedbackPanelStatus = state =>
  trackAuthoringEvent('Feedback Panel - Status', state);
export const trackFeedbackPanelReassign = assignee =>
  trackAuthoringEvent('Feedback Panel - Reassign', assignee);
export const trackFeedbackPanelReply = () => trackAuthoringEvent('Feedback Panel - Reply');
export const trackFeedbackPanelAdd = () => trackAuthoringEvent('Feedback Panel - Add');

export const trackNarrativeNav = kind => trackAuthoringEvent('Narrative Editor - Nav', kind);
const narrativeNavHandlerMemo: Record<string, () => void> = {};
export const trackNarrativeNavHandler = kind =>
  (narrativeNavHandlerMemo[kind] ??= () => trackNarrativeNav(kind));
export const trackNarrativeExpand = () => trackAuthoringEvent('Narrative Editor - Expand');
export const trackNarrativeCollapse = () => trackAuthoringEvent('Narrative Editor - Collapse');
export const trackNarrativeEditMode = on => trackAuthoringEvent('Narrative Editor - Edit Mode', on);
export const trackNarrativeAdd = typeId => trackAuthoringEvent('Narrative Editor - Add', typeId);
export const trackNarrativePaste = typeId =>
  trackAuthoringEvent('Narrative Editor - Paste', typeId);

// Playlist
export const trackOpenImportModalButton = assetType =>
  trackAuthoringEvent('Playlist - Open Import Modal (button)', assetType || '');
export const trackImportFile = fileName => {
  let ext = '';
  try {
    const idx = fileName.lastIndexOf('.');
    ext = fileName.slice(idx);
  } finally {
    trackAuthoringEvent('Playlist - Import File', ext);
  }
};

// Confirm Modal
export const trackConfirm = (type: ConfirmationTypes) =>
  trackAuthoringEvent('Confirm Modal - Click Confirm', type || '');

// Session Timeout Modal
export const trackSessionTimeoutModal = () =>
  trackAuthoringEvent('Session Timeout Modal - Displayed');
export const trackSessionExtension = () =>
  trackAuthoringEvent('Session Timeout Modal - Click Continue Session');
