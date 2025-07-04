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

import { CustomisableContent, fetchCustomizations } from '../../api/customizationApi';
import { createLoadableReducer } from '../../loRedux/createLoadableReducer';
import { combineReducers } from 'redux';

import { courseCustomizerReducer } from './courseCustomizerReducer';
import { Tree } from './Tree';

export const { fetchAction: fetchCustomizationsAction, reducer: customizationsDataReducer } =
  createLoadableReducer<typeof fetchCustomizations, Tree<CustomisableContent>>(
    'CourseCustomisations',
    fetchCustomizations
  );

export const customizationsReducer = combineReducers({
  customisations: customizationsDataReducer,
  customizerState: courseCustomizerReducer,
});
