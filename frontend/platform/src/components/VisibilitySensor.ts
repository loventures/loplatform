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

import React, { useEffect, useRef } from 'react';

/**
 * A drop-in replacement for the (unmaintained) `react-visibility-sensor`, which
 * relied on `ReactDOM.findDOMNode` — removed in React 19. It observes the single
 * child element directly via a cloned ref (no wrapper element, so layout is
 * unchanged) using an IntersectionObserver. Supports the props our call sites
 * use: `onChange`, `active`, `partialVisibility`, `minTopValue`.
 */
export interface VisibilitySensorProps {
  onChange: (visible: boolean) => void;
  /** When false, observation is paused and no change is reported. Defaults to true. */
  active?: boolean;
  /** Report visible as soon as any part intersects (vs. requiring full visibility). */
  partialVisibility?: boolean;
  /** Minimum number of pixels that must be visible from the top to count as visible. */
  minTopValue?: number;
  /** Exactly one element child; its ref is used to observe visibility. */
  children: React.ReactElement<any>;
}

const VisibilitySensor: React.FC<VisibilitySensorProps> = ({
  onChange,
  active = true,
  partialVisibility = false,
  minTopValue,
  children,
}) => {
  const nodeRef = useRef<Element | null>(null);
  // Keep the latest onChange without re-subscribing the observer every render.
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;

  useEffect(() => {
    const el = nodeRef.current;
    if (!active || !el || typeof IntersectionObserver !== 'function') return;
    // partialVisibility -> any intersection (threshold 0); otherwise require the
    // element to be fully visible (threshold 1).
    const threshold = partialVisibility ? 0 : 1;
    const observer = new IntersectionObserver(
      ([entry]) => {
        const visible =
          entry.isIntersecting &&
          (minTopValue == null ||
            entry.intersectionRect.height >=
              Math.min(minTopValue, entry.boundingClientRect.height));
        onChangeRef.current(visible);
      },
      { threshold }
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [active, partialVisibility, minTopValue]);

  const child = React.Children.only(children);
  // Attach our ref to the child element directly (merging with any existing ref)
  // so we observe the real node without introducing a wrapper.
  const existingRef = (child as any).ref;
  return React.cloneElement(child as React.ReactElement<{ ref?: React.Ref<Element> }>, {
    ref: (node: Element | null) => {
      nodeRef.current = node;
      if (typeof existingRef === 'function') existingRef(node);
      else if (existingRef && typeof existingRef === 'object') existingRef.current = node;
    },
  });
};

export default VisibilitySensor;
