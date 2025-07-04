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

import gretchen from '../grfetchen/';

import { trackSessionTimeoutModal } from '../analytics/AnalyticsEvents';
import { useDcmSelector } from '../hooks';
import { openModal } from '../modals/modalActions';
import { ModalIds } from '../modals/modalIds';
import reactRouterService from '../router/ReactRouterService';
import { User } from '../types/user';
import { AuthoringPreferences, UserPreferences } from './reducers';

export const RECEIVE_SESSION_DURATION = 'RECEIVE_SESSION_DURATION';
export const RECEIVE_USER_SESSION = 'RECEIVE_USER_SESSION';
export const RECEIVE_AUTHORING_PREFERENCES = 'RECEIVE_AUTHORING_PREFERENCES';

export function fetchUserSession() {
  return dispatch => {
    gretchen
      .get('/api/v2/sessions/get;embed=user')
      .exec()
      .then(info => {
        dispatch({
          type: RECEIVE_USER_SESSION,
          info,
        });
        dispatch(getSessionDuration());
      });
  };
}

function getSessionDuration() {
  return dispatch => {
    gretchen
      .get('/api/v0/session')
      .exec()
      .then(resp => {
        const initialDuration = Math.ceil(resp.expires / 60000);
        dispatch({
          type: RECEIVE_SESSION_DURATION,
          duration: initialDuration,
        });
      });
  };
}

export function checkSessionTime() {
  return dispatch => {
    gretchen
      .get('/api/v0/session')
      .headers({ 'X-No-Session-Extension': true })
      .silent()
      .exec()
      .then(resp => {
        if (!resp.valid) {
          gretchen
            .post('/api/v2/sessions/logout')
            .params({})
            .exec()
            .then(() => {
              reactRouterService.logout();
            });
        } else {
          const isAboutToExpire = resp.expires < 1000 * 60 * 5; // Five minutes
          if (isAboutToExpire) {
            dispatch(openModal(ModalIds.SessionTimeout));
            trackSessionTimeoutModal();
          }
        }
      })
      .catch(err => {
        console.error(err);
      });
  };
}

export function receiveAuthoringPreferences(authoringPreferences: AuthoringPreferences) {
  return {
    type: RECEIVE_AUTHORING_PREFERENCES,
    authoringPreferences,
  };
}

export const useUserProfile = (): User | undefined => useDcmSelector(state => state.user.profile);

export const useUserPreferences = (): UserPreferences =>
  useDcmSelector(state => state.user.preferences);

export const useAuthoringPreferences = (): AuthoringPreferences =>
  useDcmSelector(state => state.user.preferences.authoringPreferences);
