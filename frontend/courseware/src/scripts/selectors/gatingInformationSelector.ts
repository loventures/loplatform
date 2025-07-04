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

import { ContentGate, GatingInformation } from '../api/contentsApi.ts';
import { CourseState } from '../loRedux';
import { every, filter, get, isEmpty, map, mapValues, some } from 'lodash';
import { GatingInformationWithNebulousDetails } from '../courseContentModule/selectors/contentEntry';
import { LOCKED_STATUS, OPEN_STATUS, READ_ONLY_STATUS } from '../utilities/gatingPolicyConstants';
import { createInstanceSelector } from '../utilities/reduxify.ts';
import { selectCourse, selectCurrentUserId } from '../utilities/rootSelectors.ts';
import { createSelector } from 'reselect';

const gatingInformationByContentByUserSelector = (state: CourseState) =>
  state.api.gatingInformationByContentByUser;

const rawCurrentUserGatingInformationByContentSelector = createInstanceSelector<
  Record<string, GatingInformation>
>(gatingInformationByContentByUserSelector, selectCurrentUserId, {});

export const selectCurrentUserGatingInformation = createSelector(
  rawCurrentUserGatingInformationByContentSelector,
  selectCourse,
  (state: CourseState) => state.api.contentItems,
  (gatingInformationByContent, course, contentItems) => {
    const withGatingInfos = mapValues(
      // but not parent infos, yet
      gatingInformationByContent,
      gatingInformation => {
        // meh but everything assumes that gating handles this for some reason
        const isCourseEnded = course.hasEnded;

        const isOpen = gatingInformation.gateStatus === OPEN_STATUS && !isCourseEnded;
        const isLocked = gatingInformation.gateStatus === LOCKED_STATUS;
        const isReadOnly = gatingInformation.gateStatus === READ_ONLY_STATUS || isCourseEnded;

        const addAssignments = (gv: ContentGate) => {
          const { activityGatingPolicy: agp } = gv;
          const activityGates1 =
            !!agp &&
            !!agp.gates &&
            agp.gates.map(g => ({
              ...g,
              name: get(contentItems[g.assignmentId], 'name'),
            }));
          const agp1 = { ...agp, gates: activityGates1 || [] };
          return { ...gv, activityGatingPolicy: agp1 || {} };
        };
        const thisGate = gatingInformation.gate && addAssignments(gatingInformation.gate);

        return {
          gatingInformation: { ...gatingInformation, gate: thisGate },
          thisGate,
          isOpen,
          isLocked,
          isReadOnly,
        };
      }
    );

    const withParentInfos = mapValues(withGatingInfos, (gatingInformation, contentId) => {
      const parentString = contentItems[contentId] || { path: '/' };
      const parents = parentString.path.split('/').slice(0, -2);
      const parentGatingInfos = parents.map(pid => withGatingInfos[pid]);

      const allGateInfos = [gatingInformation, ...parentGatingInfos];

      const isOpen = every(allGateInfos, 'isOpen');
      const isLocked = some(allGateInfos, 'isLocked');
      const isReadOnly = !isLocked && some(allGateInfos, 'isReadOnly');

      const allGates = filter(map(allGateInfos, 'gatingInformation.gate'), g => !!g); // eslint-disable-line

      const isGated = !isEmpty(allGates);

      return {
        ...gatingInformation,
        allGates,
        isGated,
        isOpen,
        isLocked,
        isReadOnly,
        isClosed: false, // lw discussion boards do this themselves
      };
    });

    // TODO: fix the type of GatingInformationWithNebulouDetails. It is incomplete. But also
    //       why are we building this structure at all?
    return withParentInfos as unknown as Record<string, GatingInformationWithNebulousDetails>;
  }
);
