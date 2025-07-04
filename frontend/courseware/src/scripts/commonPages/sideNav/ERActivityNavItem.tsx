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
import { Content } from '../../api/contentsApi';
import ERNavItemIcon from '../../commonPages/sideNav/ERNavItemIcon';
import { ERSidebarLandmarkContext } from '../../commonPages/sideNav/ERSidebarLandmarkProvider';
import useGatingTooltip from '../../commonPages/sideNav/useGatingTooltip';
import { selectBookmark } from '../../components/bookmarks/bookmarksReducer';
import LoLink from '../../components/links/LoLink';
import { useCourseSelector } from '../../loRedux';
import { ContentPlayerPageLink } from '../../utils/pageLinks';
import {
  selectNavToPageContent,
  selectPageContent,
} from '../../courseContentModule/selectors/contentEntrySelectors';
import { timeoutEffect } from '../../utilities/effectUtils';
import React, { SyntheticEvent, useContext, useEffect, useRef, useState } from 'react';
import { BsKey } from 'react-icons/bs';

type ERActivityNavItemProps = {
  content: Content;
  className?: string;
  onClick?: (e: SyntheticEvent) => void;
  activeLesson?: boolean;
  bookmarkedLesson?: boolean;
};

const ERActivityNavItem: React.FC<ERActivityNavItemProps> = ({
  content,
  className,
  onClick,
  activeLesson,
  bookmarkedLesson,
}) => {
  const linkRef = useRef<HTMLAnchorElement>(null);
  const activeContent = useCourseSelector(selectPageContent);
  const { linkProps, iconProps, GatingTooltip, locked, showLockIcon, showKeyIcon, pointerEvents } =
    useGatingTooltip(content.id);
  const { setSidemark } = useContext(ERSidebarLandmarkContext);
  const [active, setActive] = useState(false);
  const bookmarked = useCourseSelector(selectBookmark(content.id)) != null;
  const doNav = useCourseSelector(selectNavToPageContent);

  const current = activeLesson || content.id === activeContent.id;

  useEffect(
    () => (current && doNav ? setSidemark('active', linkRef.current) : void 0),
    [setSidemark, current, doNav, linkRef.current]
  );

  // ensure the arrow animation occurs after component is mounted
  useEffect(
    timeoutEffect(() => setActive(current), 0),
    [setActive, current]
  );

  // we cannot use <ListGroupItem> because it fails to expose an `innerRef` property
  return (
    <li className={classnames('list-group-item d-flex', className)}>
      <LoLink
        className={classnames('content-navigation-link', active && 'active', locked && 'locked')}
        to={ContentPlayerPageLink.toLink({ content })}
        linkRef={linkRef}
        aria-current={current}
        {...linkProps}
        onClick={linkProps.onClick ?? onClick}
      >
        <div
          className="d-flex justify-content-between align-items-center w-100"
          style={{ pointerEvents }}
        >
          <span
            className="clamp-title"
            title={content.name}
          >
            {showKeyIcon && (
              <BsKey
                aria-hidden={true}
                fill="#2e4954"
                className="me-2"
              />
            )}
            {content.name}
          </span>
          <ERNavItemIcon
            content={content}
            bookmarked={bookmarkedLesson ?? bookmarked}
            {...iconProps}
          />
        </div>
        {showLockIcon || showKeyIcon ? <GatingTooltip /> : null}
      </LoLink>
    </li>
  );
};

export default ERActivityNavItem;
