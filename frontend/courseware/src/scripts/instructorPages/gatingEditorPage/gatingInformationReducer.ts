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

import { mapValues, map, get } from 'lodash';
import { createNamedReducer, setReduxState } from '../../utilities/reduxify';
import apiDataReducer from '../../utilities/apiDataReducer';

import { GATING_EDITOR_LOCKDATE_UPDATED, GATING_EDITOR_ACTIVITY_TOGGLED } from './actionTypes';

const dataReducer = createNamedReducer('gatingInformationByContentByUser', apiDataReducer);

const withNewLockDate = (gatingInformation, lockDate) => {
  const val = setReduxState(
    gatingInformation,
    ['gate', 'temporalGatingPolicy', 'lockDate'],
    lockDate
  );
  return val;
};

const withAssignmentRemoved = (gatingInformation, assignmentId) =>
  setReduxState(
    gatingInformation,
    ['gate', 'activityGatingPolicy', 'gates'],
    map(get(gatingInformation, ['gate', 'activityGatingPolicy', 'gates']), gate => {
      if (gate.assignmentId === assignmentId) {
        return {
          ...gate,
          disabled: true,
        };
      } else {
        return gate;
      }
    })
  );

const gatingInformationReducer = (
  gatingInformationByContentByUser: Record<string, any> = {},
  action
) => {
  switch (action.type) {
    case GATING_EDITOR_LOCKDATE_UPDATED:
      return mapValues(gatingInformationByContentByUser, gatingInformationByContent => {
        return {
          ...gatingInformationByContent,
          [action.contentId]: withNewLockDate(
            gatingInformationByContent[action.contentId],
            action.data.lockDate
          ),
        };
      });
    case GATING_EDITOR_ACTIVITY_TOGGLED:
      return mapValues(gatingInformationByContentByUser, gatingInformationByContent => {
        return {
          ...gatingInformationByContent,
          [action.contentId]: withAssignmentRemoved(
            gatingInformationByContent[action.contentId],
            action.data.removedAssignmentId
          ),
        };
      });
    default:
      return dataReducer(gatingInformationByContentByUser, action);
  }
};

export default gatingInformationReducer;
