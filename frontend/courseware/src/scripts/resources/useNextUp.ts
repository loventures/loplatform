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

import { Content } from '../api/contentsApi';
import { useAllGatingInfoResource } from './GatingInformationResource';
import {
  ContentWithAncestors,
  ModulePath,
  useFlatLearningPathResource,
} from './LearningPathResource';
import { useCourseSelector } from '../loRedux';
import { selectPageContentId } from '../courseContentModule/selectors/contentEntrySelectors';
import { selectCurrentUser } from '../utilities/rootSelectors';

/**
 * When searching next up for clicking a Module in the left navigation
 * we have the following scenario:
 *
 * Anyone:
 *  zero elements in module (dead content) -> undefined, shouldn't break app
 *  module collapsed -> [0]
 *  module expanded -> current content item, no navigation
 *
 * This really isn't next up.
 */
export const useNextUpInModule = (module: ModulePath, expanded: boolean): Content | undefined => {
  const currentContentId = useCourseSelector(selectPageContentId);
  return expanded ? module.elements.find(c => c.id === currentContentId) : module.elements[0];
};

export const useNextUpInUnit = (module: ModulePath, expanded: boolean): Content | undefined => {
  const currentContentId = useCourseSelector(selectPageContentId);
  return expanded
    ? module.effectiveElements?.find(c => c.id === currentContentId)
    : module.effectiveElements?.[0];
};

type NextUpFullCourse = [ContentWithAncestors, boolean];

/**
 * When searching for next up item in the course from the dashboard.
 * We need both the next up item and the module it comes from to display the
 * widget properly.
 *
 * Instructor:
 *  undefined, should never be used, perhaps module[0][0]
 *
 * Student:
 *  search the flat path for first incomplete item not locked by policy.
 *
 * */
export const useNextUpFullCourse = (): NextUpFullCourse | [] => {
  const { id: currentUserId, isInstructor } = useCourseSelector(selectCurrentUser);
  const learningPath = useFlatLearningPathResource(currentUserId);
  const gatingInformation = useAllGatingInfoResource(currentUserId);

  if (isInstructor) {
    return [learningPath[0], true];
  }

  const isFirstUp = learningPath.every(content => !content.progress?.progressTypes.length);

  const nextUp = learningPath.find(content => {
    const hasNoProgress =
      (content.progress?.total ?? 0) > 0 && !content.progress?.progressTypes.length;
    const lockedByPolicy =
      gatingInformation[content.id]?.isLocked &&
      gatingInformation[content.id].allGates.some(g => g.rightsGatingPolicy?.policyType);
    return hasNoProgress && !lockedByPolicy;
  });

  return nextUp ? [nextUp, isFirstUp] : [];
};

/**
 * When searching for the next item from a given content item, i.e. the bottom nav.
 *
 * Current content not found or on last item or other edge cases:
 *  undefined (dashboard)
 *
 * Instructor:
 *  current content + 1
 *
 * Student:
 *  next item in flat path not locked by policy.
 */
export const useNextUpFromContent = (): ContentWithAncestors | undefined =>
  usePrevNextFromContent(false);

export const usePreviousFromContent = (): ContentWithAncestors | undefined =>
  usePrevNextFromContent(true);

const usePrevNextFromContent = (reverse: boolean): ContentWithAncestors | undefined => {
  const { id: currentUserId } = useCourseSelector(selectCurrentUser);
  const currentContentId = useCourseSelector(selectPageContentId);
  const flatLearningPath = useFlatLearningPathResource(currentUserId);
  const gatingInformation = useAllGatingInfoResource(currentUserId);
  if (reverse) flatLearningPath.reverse();

  let found = false;
  for (const content of flatLearningPath) {
    if (content.id === currentContentId) {
      found = true;
    } else if (found) {
      const lockedByPolicy =
        gatingInformation[content.id]?.isLocked &&
        gatingInformation[content.id].allGates.some(g => g.rightsGatingPolicy?.policyType);
      if (!lockedByPolicy) {
        return content;
      }
    }
  }
};
