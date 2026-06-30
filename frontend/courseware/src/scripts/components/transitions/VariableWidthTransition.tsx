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

import { Children, ReactElement, cloneElement, useRef } from 'react';
import { CSSTransition } from 'react-transition-group';

interface VariableWidthTransitionProps {
  children: ReactElement;
  [key: string]: any;
}

// React 19 removed findDOMNode, so react-transition-group needs an explicit
// nodeRef. We own the ref, forward it to the single child, and drive the
// max-width animation off nodeRef.current instead of the (now absent) callback
// element argument. The child must accept a ref (e.g. forwardRef CategoryTable).
const VariableWidthTransition = ({ children, ...props }: VariableWidthTransitionProps) => {
  const nodeRef = useRef<any>(null);
  const setStyle = (style: string) =>
    nodeRef.current && nodeRef.current.setAttribute('style', style);
  return (
    <CSSTransition
      nodeRef={nodeRef}
      onEnter={() => setStyle('max-width:0px;overflow:hidden')}
      onEntering={() => setStyle(`max-width:${nodeRef.current.scrollWidth}px;overflow:hidden`)}
      onEntered={() => setStyle('')}
      onExiting={() => setStyle('max-width:0px;overflow:hidden')}
      onExit={() => setStyle(`max-width:${nodeRef.current.scrollWidth}px;overflow:hidden`)}
      onExited={() => setStyle('')}
      {...(props as any)}
    >
      {cloneElement(Children.only(children), { ref: nodeRef } as any)}
    </CSSTransition>
  );
};

export default VariableWidthTransition;
