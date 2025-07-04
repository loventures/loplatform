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

import ERActivityNavItem from '../../commonPages/sideNav/ERActivityNavItem';
import ERModuleNavItem from '../../commonPages/sideNav/ERModuleNavItem';
import ERUnitNavItem from '../../commonPages/sideNav/ERUnitNavItem';
import { ModulePath, useLearningPathResource } from '../../resources/LearningPathResource';
import { useCourseSelector } from '../../loRedux';
import {
  ContentWithRelationships,
  ModuleWithRelationships,
} from '../../courseContentModule/selectors/assembleContentView';
import { CourseWithRelationships } from '../../courseContentModule/selectors/assembleCourseRelations';
import {
  isCourseWithRelationships,
  selectNavToPageContent,
  selectPageContent,
} from '../../courseContentModule/selectors/contentEntrySelectors';
import { CONTENT_TYPE_MODULE, CONTENT_TYPE_UNIT } from '../../utilities/contentTypes';
import { selectCurrentUser } from '../../utilities/rootSelectors';
import React, { useEffect, useMemo, useState } from 'react';
import { selectBookmarks } from '../../components/bookmarks/bookmarksReducer';

const useActiveAncestors = (
  active: CourseWithRelationships | ModuleWithRelationships | ContentWithRelationships
): { unit?: string; ancestors: string[] } => {
  return useMemo(() => {
    if (isCourseWithRelationships(active)) {
      return { ancestors: [] };
    } else {
      return {
        unit: active.ancestors?.find(c => c.typeId === CONTENT_TYPE_UNIT)?.id,
        ancestors: (active.ancestors ?? []).map(c => c.id).concat(active.id),
      };
    }
  }, [active.id]);
};

const ERContentNavItems: React.FC = () => {
  const user = useCourseSelector(selectCurrentUser);
  const { modules } = useLearningPathResource(user.id);
  const activeContent = useCourseSelector(selectPageContent);
  const { unit: activeUnit, ancestors: activeAncestors } = useActiveAncestors(activeContent);
  const [collapsed, setCollapsed] = useState<string | undefined>(undefined);
  useEffect(() => {
    if (collapsed && !activeAncestors.includes(collapsed)) setCollapsed(undefined);
  }, [activeAncestors.join()]);
  const doNav = useCourseSelector(selectNavToPageContent);
  const isActive = (item: ModulePath) => activeAncestors.includes(item.content.id);
  // Force degenerate modules (module name == sole content name) to stay collapsed
  const isDegenerate = (item: ModulePath) =>
    item.elements.length === 1 &&
    item.elements[0].id === activeContent.id &&
    activeContent.name === item.content.name;
  const isCollapsed = (item: ModulePath) => collapsed === item.content.id || isDegenerate(item);
  const isExpanded = (item: ModulePath) => isActive(item) && !isCollapsed(item) && doNav;
  const effectiveActiveUnit = collapsed === activeUnit ? undefined : activeUnit;
  const filteredModules = modules.filter(module =>
    effectiveActiveUnit
      ? effectiveActiveUnit === (module.content.unit?.id ?? module.content.id)
      : !module.content.unit
  );

  const bookmarks = useCourseSelector(selectBookmarks);
  const bookmarked = useMemo(() => {
    const result = new Set<string>();
    for (const { elements } of modules) {
      for (const { id, lesson, unit, module } of elements) {
        if (bookmarks[id] != null) {
          if (unit) result.add(unit.id);
          if (module) result.add(module.id);
          if (lesson) result.add(lesson.id);
        }
      }
    }
    return result;
  }, [bookmarks]);

  return (
    <>
      {filteredModules.map((item, index) =>
        item.content.typeId === CONTENT_TYPE_MODULE ? (
          <ERModuleNavItem
            key={item.content.id}
            modulePath={item}
            expanded={isExpanded(item)}
            collapsed={isCollapsed(item)}
            setCollapsed={c => setCollapsed(c ? item.content.id : undefined)}
            previousExpanded={
              index > 0 &&
              filteredModules[index - 1].content.typeId !== CONTENT_TYPE_UNIT &&
              isExpanded(filteredModules[index - 1])
            }
            activeAncestors={activeAncestors}
            bookmarked={bookmarked}
          />
        ) : item.content.typeId === CONTENT_TYPE_UNIT ? (
          <ERUnitNavItem
            key={item.content.id}
            modulePath={item}
            expanded={isExpanded(item)}
            collapsed={isCollapsed(item)}
            setCollapsed={c => setCollapsed(c ? item.content.id : undefined)}
            previousExpanded={index > 0 && isExpanded(filteredModules[index - 1])}
            bookmarked={bookmarked}
          />
        ) : (
          // legacy courses have some top-level activities that we can't author anymore
          <ERActivityNavItem
            key={item.content.id}
            content={item.content}
          />
        )
      )}
    </>
  );
};

export default ERContentNavItems;
