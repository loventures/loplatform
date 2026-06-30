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

import { useDispatch, useSelector } from 'react-redux';
import type { TypedUseSelectorHook } from 'react-redux';

import type { MainState } from './reducers/MainReducers';

/**
 * The union of every reducer slice mounted across the platform apps. Individual
 * apps mount a subset (every app has `main`; some add router/presence/
 * announcement), so the optional slices keep a single RootState usable
 * everywhere without per-app store types.
 */
export interface RootState {
  main: MainState;
  router?: any;
  presence?: any;
  announcement?: any;
}

export const useTypedSelector: TypedUseSelectorHook<RootState> = useSelector;

/** Convenience hooks for the two most commonly read pieces of `main`. */
export const useTranslations = () => useTypedSelector(state => state.main.translations);
export const useLoPlatform = () => useTypedSelector(state => state.main.lo_platform);

export const useThunkDispatch = () => useDispatch();
