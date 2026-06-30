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

import { Omit } from '../../types/omit';
import navBlockerService from '../../services/navBlockerService';
import { FromApp, LoLocationDescriptor } from '../../utils/linkUtils';
import React, { AnchorHTMLAttributes, RefObject } from 'react';
import { Link, useNavigate } from 'react-router-dom';

export type LoLinkProps = {
  to: string | LoLocationDescriptor<FromApp>;
  disabled?: boolean;
  linkRef?: RefObject<HTMLAnchorElement>;
} & Omit<AnchorHTMLAttributes<Element>, 'href'>;

const LoLink: React.FC<LoLinkProps> = ({
  to,
  children,
  target,
  disabled = false,
  linkRef,
  style = {},
  onClick,
  ...props
}) => {
  const navigate = useNavigate();
  // react-router v6 keeps navigation `state` out of `to` (it's a separate prop), so split it off.
  const { state, ...path } = typeof to === 'string' ? { state: undefined } : to;
  const target_ = typeof to === 'string' ? to : path;

  // In-app unsaved-work guard. The v4 implementation used history.block, which is fragile under
  // react-router v6's HistoryRouter (it corrupted grader/print/preview POP navigation), so the
  // guard now lives at the point of user-initiated navigation: if a page has registered a hot
  // nav-blocker (quiz/grader/submission editor with unsaved work), confirm before leaving and only
  // navigate on confirm. Modified / new-tab / already-handled clicks fall through to the browser.
  const handleClick: React.MouseEventHandler<HTMLAnchorElement> = e => {
    onClick?.(e);
    if (
      e.defaultPrevented ||
      target ||
      e.button !== 0 ||
      e.metaKey ||
      e.altKey ||
      e.ctrlKey ||
      e.shiftKey
    )
      return;
    if (navBlockerService.getActiveBlockerMessages().length) {
      e.preventDefault();
      navBlockerService.confirmNavByModal().then(
        () => navigate(target_ as any, { state }),
        () => {}
      );
    }
  };

  return (
    <Link
      to={target_}
      state={state}
      target={target}
      aria-disabled={disabled}
      {...props}
      onClick={handleClick}
      style={{ textDecoration: 'underline', ...style }}
      ref={linkRef}
    >
      {children}
    </Link>
  );
};

export default LoLink;
