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

import { map } from 'lodash';

/*
Util to create html that, when compiled by angular, renders to an angular component
example:
    renderComponent(
        'some-question',
        {
            'question': '$ctrl.question',
            'some-flag': true
        }
    )

@param componentName: string
    html name of the component, in-dash-case
@param componentArgs: { [string] : [string] }
    component args,
    the keys are names also in-dash-case,
    the values are strings the would appear on the html attr value
@return string
*/
const renderComponent = function (componentName, componentArgs) {
  return `
        <${componentName}
            ${map(componentArgs, (value, key) => `${key}="${value}"`).join(' ')}
        ></${componentName}>
    `;
};

export default renderComponent;
