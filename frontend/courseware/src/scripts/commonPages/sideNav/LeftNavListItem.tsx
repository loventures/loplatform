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
import { ERSidebarLandmarkContext } from '../../commonPages/sideNav/ERSidebarLandmarkProvider';
import LoLink from '../../components/links/LoLink';
import { useCourseSelector } from '../../loRedux';
import { LoLinkBuilder, isLinkBuilder } from '../../utils/pageLinks';
import { timeoutEffect } from '../../utilities/effectUtils';
import { selectRouter } from '../../utilities/rootSelectors';
import React, { CSSProperties, useContext, useEffect, useRef, useState } from 'react';
import { ListGroupItem } from 'reactstrap';

type LeftNavListItemProps = {
  to: string | LoLinkBuilder;
  id?: string;
  prefix?: LoLinkBuilder;
  className?: string;
  style?: CSSProperties;
  target?: string;
  onClick?: (k: any) => void;
} & React.PropsWithChildren;

export const LeftNavListItem: React.FC<LeftNavListItemProps> = ({
  children,
  to,
  prefix,
  ...props
}) => {
  const { setSidemark } = useContext(ERSidebarLandmarkContext);
  const [active, setActive] = useState(false);
  const path = useCourseSelector(s => selectRouter(s).path);
  const linkRef = useRef<HTMLAnchorElement>(null);
  const current = prefix
    ? !!prefix.match(path)
    : isLinkBuilder(to)
      ? !!to.match(path)
      : path.startsWith(to);
  useEffect(
    () => (current ? setSidemark('active', linkRef.current) : void 0),
    [setSidemark, current, linkRef.current]
  );
  // ensure the arrow animation occurs after component is mounted
  useEffect(
    timeoutEffect(() => setActive(current), 0),
    [setActive, current]
  );
  return (
    <ListGroupItem {...props}>
      <LoLink
        className={classnames('course-navigation-link', active && 'active')}
        to={isLinkBuilder(to) ? to.toLink() : to}
        linkRef={linkRef}
        aria-current={current}
      >
        {children}
      </LoLink>
    </ListGroupItem>
  );
};
