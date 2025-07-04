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
import React from 'react';
import { Button } from 'reactstrap';

interface IconButtonProps {
  iconName: string; // https://material.io/tools/icons/
  size?: 'sm' | 'lg' | '';
  color?:
    | 'primary'
    | 'outline-primary'
    | 'secondary'
    | 'outline-secondary'
    | 'success'
    | 'outline-success'
    | 'danger'
    | 'outline-danger'
    | 'warning'
    | 'outline-warning'
    | 'info'
    | 'outline-info'
    | 'light'
    | 'outline-light'
    | 'dark'
    | 'outline-dark'
    | 'link';
  iconSize?: 18 | 24 | 36 | 48;
  classes?: string[];
  className?: string;
  disabled?: boolean;
  onClick?: () => void;
  tag?: React.ComponentClass;
  children?: React.ReactNode;
}

const IconButton: React.FC<IconButtonProps> = ({
  size = 'sm',
  color = 'primary',
  iconName,
  iconSize,
  classes = [],
  disabled = false,
  onClick = () => false,
  className = undefined,
  tag = undefined,
  children,
  ...props
}) => {
  const Component = tag ?? Button;
  return (
    <Component
      size={size}
      color={color}
      className={classnames([
        { 'ps-1 pe-2': children },
        'd-inline-flex',
        'align-items-center',
        'justify-content-center',
        ...classes,
        className,
      ])}
      disabled={disabled}
      onClick={onClick}
      {...props}
    >
      <i
        className={classnames([
          'material-icons',
          iconSize && `md-${iconSize}`,
          children && 'mx-1',
          !iconSize && children && `md-${18}`,
        ])}
      >
        {iconName}
      </i>
      {children}
    </Component>
  );
};

export default IconButton;
