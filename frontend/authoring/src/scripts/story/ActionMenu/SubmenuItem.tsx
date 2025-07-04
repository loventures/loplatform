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

import React, { useEffect, useRef, useState } from 'react';
import { IoCheckmarkOutline } from 'react-icons/io5';

export const SubmenuItem: React.FC<
  {
    label: string;
    className?: string;
    disabled?: boolean;
    checked?: boolean;
  } & React.PropsWithChildren
> = ({ label, className, disabled, checked, children }) => {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLLIElement>();
  useEffect(() => {
    if (open) {
      const listener = (e: MouseEvent) => {
        if (!ref.current?.contains(e.target as any)) setOpen(false);
      };
      document.addEventListener('mousedown', listener);
      (ref.current?.querySelector('.dropdown-item') as any)?.focus();
      return () => document.removeEventListener('mousedown', listener);
    }
  }, [open]);

  return (
    <li
      ref={ref}
      className={className}
    >
      <button
        className="dropdown-item dropdown-toggle"
        tabIndex={disabled ? -1 : 0}
        role={disabled ? undefined : 'menuitem'}
        disabled={disabled}
        onClick={e => {
          e.preventDefault();
          e.stopPropagation();
          setOpen(!open);
        }}
      >
        {checked != null && <div className="check-spacer">{checked && <IoCheckmarkOutline />}</div>}
        {label}
      </button>
      {open && <ul className="dropdown-menu dropdown-submenu">{children}</ul>}
    </li>
  );
};
