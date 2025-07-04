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

import { useEffect } from 'react';
import { useDispatch } from 'react-redux';

import { setNarrativeState } from './storyActions';

const useEditableIFrame = () => {
  const dispatch = useDispatch();

  // This allows us to capture click into iframes...
  useEffect(() => {
    let active: Element | null = null;
    let activeIFrame: string | undefined = undefined;
    const interval = setInterval(() => {
      const curActive = document.activeElement;
      if (active !== curActive) {
        active = curActive;
        const iFrameId = active?.tagName === 'IFRAME' ? active.id : undefined;
        if (activeIFrame !== iFrameId) {
          activeIFrame = iFrameId;
          dispatch(setNarrativeState({ activeIFrame }));
        }
      }
    }, 100);
    return () => clearInterval(interval);
  }, []);
};

export default useEditableIFrame;
