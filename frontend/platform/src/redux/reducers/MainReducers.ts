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

import Polyglot from 'node-polyglot';

import { LoPlatform } from '../../types/loPlatform';
import {
  SET_LO_PLATFORM,
  SET_PORTAL_ALERT_STATUS,
  SET_TRANSLATIONS,
} from '../actions/MainActions.js';

export type MainState = {
  lo_platform: LoPlatform;
  translations: Polyglot;
  adminPageError: boolean;
  adminPageSuccess: boolean;
  adminPageMessage: string;
  session: boolean;
};

const initialState: MainState = {
  lo_platform: window.lo_platform ?? { domain: {} },
  translations: new Polyglot(),
  adminPageError: false,
  adminPageSuccess: false,
  adminPageMessage: '',
  session: true,
};

export default function main(state = initialState, action: any): MainState {
  switch (action.type) {
    case SET_LO_PLATFORM:
      return { ...state, lo_platform: action.lo_platform };
    case SET_TRANSLATIONS:
      return { ...state, translations: action.translations };
    case SET_PORTAL_ALERT_STATUS:
      return {
        ...state,
        adminPageError: action.error,
        adminPageSuccess: action.success,
        adminPageMessage: action.message,
      };
    default:
      return state;
  }
}
