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

import React from 'react';
import { react2angular } from 'react2angular';
import { each } from 'lodash';

/**
 * This enhances react2Angluar by allowing you to specify props that you wish to have an
 * explicit call to $scope.$digest.  Use this when you need the UI to immediately refresh
 * upon any state changes in a react component.
 *
 * By design, react2angular will not trigger digest loops on changes.
 * See: https://github.com/coatue-oss/react2angular/issues/39
 */
export const react2angularWithDigest = (Component, bindings, injections, propsWithDigest) => {
  const ComponentWithDigest = props => {
    const newProps = {
      ...props,
    };

    each(propsWithDigest, prop => {
      newProps[prop] = (...args) => {
        props.$timeout(() => props[prop](...args), 0);
      };
    });

    return <Component {...newProps} />;
  };

  return react2angular(ComponentWithDigest, bindings, [...injections, '$scope', '$timeout']);
};
