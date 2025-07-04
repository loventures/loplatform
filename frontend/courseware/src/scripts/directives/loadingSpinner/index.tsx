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

import * as angular from 'angular';
import { useTranslation } from '../../i18n/translationContext.tsx';
import * as React from 'react';
import { react2angular } from 'react2angular';

type LoadingSpinnerProps = {
  message?: string;
  className?: string;
  style?: React.HTMLAttributes<HTMLSpanElement>['style'];
};

const LoadingSpinner = ({ message, className, style }: LoadingSpinnerProps) => {
  const translate = useTranslation();
  return (
    <span
      {...(className ? { className } : {})}
      {...(style ? { style } : {})}
    >
      <svg
        className="loading-spinner"
        version="1.1"
        xmlns="http://www.w3.org/2000/svg"
        xmlnsXlink="http://www.w3.org/1999/xlink"
        xmlSpace="preserve"
        x="0px"
        y="0px"
        viewBox="4 4 50 50"
      >
        <path
          className="loading-spinner-path"
          transform="rotate(180 25 25)"
          d="M43.935,25.145c0-10.318-8.364-18.683-18.683-18.683c-10.318,0-18.683,8.365-18.683,18.683h4.068c0-8.071,6.543-14.615,14.615-14.615c8.072,0,14.615,6.543,14.615,14.615H43.935z"
        >
          <animateTransform
            attributeType="xml"
            attributeName="transform"
            type="rotate"
            from="0 25 25"
            to="360 25 25"
            dur="0.8s"
            repeatCount="indefinite"
          />
        </path>
      </svg>
      <span> {translate(message ? message : 'Loading')}</span>
    </span>
  );
};

export default LoadingSpinner;

export const ng = angular
  .module('lo.directives.loadingSpinner', [])
  .component(
    'loadingSpinner',
    react2angular(LoadingSpinner, ['message', 'className', 'style'], [])
  );
