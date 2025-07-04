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

/**
 * Converts a Render Prop to a function that returns an HoC
 * ex:
 * If you have a render prop `WithMouse`, that you use like:
 *
 *   <WithMouse every={500}>
 *     {mousePosition => (
 *       <div></div>
 *     )}
 *   </WithMouse>
 *
 * you can convert it to a HoC by calling:
 *
 *   const withMouse = toHoc(WithMouse)
 *
 * And then use that Hoc like:
 *
 *   withMouse({every: 500})(({mouse}) => (
 *     <div>Your mouse is at: {mouse.x}, {mouse.y}</div>
 *   ))
 *
 */
export default function toHoc(RenderProp) {
  return args => WrappedComponent => {
    return class Hoc extends React.Component {
      render() {
        return (
          <RenderProp
            {...args}
            {...this.props}
          >
            {data => (
              <WrappedComponent
                {...data}
                {...this.props}
              />
            )}
          </RenderProp>
        );
      }
    };
  };
}
