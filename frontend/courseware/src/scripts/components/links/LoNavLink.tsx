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

import { ReactNode } from 'react';
import { NavLink } from 'react-router-dom';

interface LoNavLinkProps {
  to: any;
  activeClassName?: string;
  className?: string;
  children?: ReactNode;
  target?: string;
  disabled?: boolean;
  [key: string]: any;
}

const LoNavLink = ({
  to,
  activeClassName,
  className,
  children,
  target,
  disabled,
  ...props
}: LoNavLinkProps) => {
  // v6 dropped `activeClassName` (use a className callback) and keeps navigation `state` separate.
  const { state, ...path } = typeof to === 'string' ? { state: undefined } : to;
  return (
    <NavLink
      to={typeof to === 'string' ? to : path}
      state={state}
      className={({ isActive }) =>
        [className, isActive ? activeClassName : ''].filter(Boolean).join(' ')
      }
      target={target}
      aria-disabled={disabled}
      {...props}
    >
      {children}
    </NavLink>
  );
};

export default LoNavLink;
