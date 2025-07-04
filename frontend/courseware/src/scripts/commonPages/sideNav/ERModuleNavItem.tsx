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

import classnames from 'classnames';
import ERActivityNavItem from '../../commonPages/sideNav/ERActivityNavItem';
import ERNavItemIcon from '../../commonPages/sideNav/ERNavItemIcon';
import { ERSidebarLandmarkContext } from '../../commonPages/sideNav/ERSidebarLandmarkProvider';
import useGatingTooltip from '../../commonPages/sideNav/useGatingTooltip';
import LoLink from '../../components/links/LoLink';
import { ModulePath } from '../../resources/LearningPathResource';
import { useNextUpInModule } from '../../resources/useNextUp';
import { ContentPlayerPageLink, CoursePageLink } from '../../utils/pageLinks';
import { sumBy } from 'lodash';
import React, { MouseEventHandler, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { BsKey } from 'react-icons/bs';
import { ListGroup, ListGroupItem } from 'reactstrap';

interface ERModuleNavItemProps {
  modulePath: ModulePath;
  expanded: boolean;
  collapsed: boolean;
  setCollapsed: (collapsed: boolean) => void;
  previousExpanded: boolean;
  activeAncestors?: string[];
  bookmarked: Set<string>;
}

/**
 * Side bar nav element that displays a module and it's flattened children if needed.
 */
const ERModuleNavItem: React.FC<ERModuleNavItemProps> = ({
  modulePath,
  expanded,
  collapsed,
  setCollapsed,
  previousExpanded,
  activeAncestors,
  bookmarked,
}) => {
  // This would all be a lot nicer if we had a hierarchical content tree: the handling of lessons
  // would be much more obvious.
  const { content: module, elements } = modulePath;
  const nextUp = useNextUpInModule(modulePath, expanded);
  const [collapsedLesson, setCollapsedLesson] = useState<string | undefined>(undefined);

  // If you nav to the next lesson or collapse the module then uncollapse the lesson
  const collapsedLessonActive = collapsedLesson && activeAncestors?.includes(collapsedLesson);
  useEffect(() => {
    if (!collapsedLessonActive || collapsed) setCollapsedLesson(undefined);
  }, [collapsedLessonActive, collapsed]);

  const {
    linkProps: { onClick, ...linkProps },
    iconProps,
    GatingTooltip,
    locked,
    showLockIcon,
    showKeyIcon,
    pointerEvents,
  } = useGatingTooltip(module.id);
  const linkRef = useRef<HTMLAnchorElement>(null);
  const { setSidemark } = useContext(ERSidebarLandmarkContext);
  const lessonCount = useMemo(
    // count the number of contents that are the first item in a lesson
    () => sumBy(elements, e => Number(e.index === 0 && !!e.lesson)),
    [elements]
  );

  useEffect(
    () => (expanded ? setSidemark('topPadding', linkRef.current) : void 0),
    [setSidemark, expanded, linkRef.current]
  );
  useEffect(
    () => (previousExpanded ? setSidemark('bottomPadding', linkRef.current) : void 0),
    [setSidemark, previousExpanded, linkRef.current]
  );
  const moduleClick: MouseEventHandler = e => {
    if (expanded || collapsed) {
      e.preventDefault();
      e.stopPropagation();
    }
    setCollapsed(expanded);
  };

  return (
    <ListGroupItem className={classnames(expanded && 'expanded', collapsed && 'collapsed')}>
      <LoLink
        className={classnames(
          'content-navigation-link module-link',
          locked && 'locked',
          collapsed && 'active'
        )}
        to={nextUp ? ContentPlayerPageLink.toLink({ content: nextUp }) : CoursePageLink.toLink()}
        onClick={onClick ?? moduleClick}
        linkRef={linkRef}
        {...linkProps}
      >
        <div
          className="d-flex justify-content-between align-items-center w-100"
          style={{ pointerEvents }}
        >
          <span className="clamp-title">
            {showKeyIcon && (
              <BsKey
                aria-hidden={true}
                fill="#2e4954"
                className="me-2"
              />
            )}
            {module.name}
          </span>
          <ERNavItemIcon
            content={module}
            bookmarked={bookmarked.has(module.id) && !expanded}
            {...iconProps}
          />
        </div>
        {showLockIcon || showKeyIcon ? <GatingTooltip /> : null}
      </LoLink>
      {expanded && (
        <ListGroup flush>
          {
            elements.reduce<[boolean, Array<JSX.Element>]>(
              ([inLesson, items], content, index) => {
                const lessonId = content.lesson?.id;
                // If either there are multiple lessons in this module or a lesson is not the first module
                // child then show the lesson name before the first piece of lesson content.
                // This is to avoid showing the lesson in an old common case where the first module
                // child is the sole lesson, usually having the same name as the module.
                if (
                  (lessonCount > 1 || index > 0) &&
                  lessonId &&
                  content.lesson &&
                  content.index === 0
                ) {
                  if (activeAncestors?.includes(lessonId)) {
                    const isCollapsed = lessonId === collapsedLesson;
                    items.push(
                      <ERActivityNavItem
                        key={lessonId}
                        content={content.lesson}
                        className={classnames('module-child', 'lesson', 'current-lesson')}
                        onClick={e => {
                          e.preventDefault();
                          setCollapsedLesson(isCollapsed ? undefined : lessonId);
                        }}
                        activeLesson={isCollapsed}
                        bookmarkedLesson={isCollapsed && bookmarked.has(lessonId)}
                      />
                    );
                  } else {
                    // Non-active collapsed lesson
                    items.push(
                      <ERActivityNavItem
                        key={lessonId}
                        content={content.lesson}
                        className={classnames('module-child', 'lesson')}
                        bookmarkedLesson={bookmarked.has(lessonId)}
                      />
                    );
                  }
                  inLesson = true;
                }
                // If this is the last item in a lesson and there is a next item not in a lesson and this
                // lesson wasn't the first and only lesson, per above, then highlight the end of the lesson
                const lastInLesson =
                  inLesson && index < elements.length - 1 && !elements[index + 1].lesson;
                // This lesson is expanded if a child is active and it is not collapsed, or it is the
                // only lesson and first child of the module
                const lessonExpanded =
                  (lessonId &&
                    lessonId !== collapsedLesson &&
                    activeAncestors?.includes(lessonId)) ||
                  (lessonCount === 1 && elements[0].lesson);
                if (!content.lesson || lessonExpanded)
                  items.push(
                    <ERActivityNavItem
                      key={content.id}
                      content={content}
                      className={classnames(
                        'module-child',
                        inLesson && 'in-lesson',
                        lastInLesson && 'last-in-lesson'
                      )}
                    />
                  );
                inLesson &&= !lastInLesson;
                return [inLesson, items];
              },
              [false, []]
            )[1]
          }
        </ListGroup>
      )}
    </ListGroupItem>
  );
};

export default ERModuleNavItem;
