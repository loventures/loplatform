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

import { replace } from 'connected-react-router';
import qs from 'qs';
import React, { useEffect } from 'react';
import { useDispatch } from 'react-redux';

import { escapeProjectGraphEdit, pollForHeadCommit, useCurrentAssetName } from '../graphEdit';
import { useDcmSelector, useRouterPathVariable } from '../hooks';
import PresenceService from '../presence/services/PresenceService';
import { parseQueryParams } from '../router/ReactRouterService';
import { useProjectGraphSelector } from '../structurePanel/projectGraphHooks';
import { openToast } from '../toast/actions';
import LovLink from './LovLink';
import { scrollToTopOfScreen, subPageNames } from './story';
import { incrementFindCount, setNarrativeState, toggleOmegaEdit } from './storyActions';
import { useStorySelector } from './storyHooks';
import useEditableIFrame from './useEditableIFrame';
import useLoNav from './useLoNav';

/** Moves some state things down to a subcomponent of NarrativeMode so the
 * react render is smaller. */
const NarrativeHooks: React.FC = () => {
  const dispatch = useDispatch();
  const pathname = useDcmSelector(state => state.router.location?.pathname);
  const search = useDcmSelector(state => state.router.location?.search);
  const name = useCurrentAssetName();
  const page = useRouterPathVariable('name'); // includes the logical pages
  const active = useDcmSelector(
    state => state.presence.tabVisible && !state.presence.idling && state.configuration.realTime
  );
  const activeNode = useStorySelector(state => state.activeNode);
  const local = useDcmSelector(state => state.layout.platform.isLocal);

  useProjectGraphSelector(state => (((window as any).projectGraph = state), null));

  // If there is a success or danger message, pop it up and rewrite it out of the URL
  useEffect(() => {
    const { failure, success, ...rest } = parseQueryParams(search ?? '');
    if (success || failure) {
      dispatch(openToast((failure ?? success) as string, failure ? 'danger' : 'success'));
      dispatch(
        replace({
          pathname,
          search: qs.stringify(rest),
        })
      );
    }
  }, [pathname, search]);

  // On navigate, scroll top but don't scroll the navbar on-screen if it was off.
  useEffect(() => scrollToTopOfScreen('auto'), [page]);

  useEffect(() => {
    // I do not love this because when you navigate from a child to a parent,
    // the child starts open in the parent and then closes. This should really
    // happen before the nav, not after.
    dispatch(
      setNarrativeState({
        assetStates: name ? { [name]: { expanded: true } } : {},
        assetOffsets: {},
      })
    );
  }, [name]);

  // command-shift-H to engage omega edit mode, command-F increments find count
  useEffect(() => {
    const listener = (e: KeyboardEvent) => {
      if ((e.key === 'h' || e.key === 'H') && (e.ctrlKey || e.metaKey) && e.shiftKey) {
        e.preventDefault();
        dispatch(toggleOmegaEdit());
      } else if (e.key === 'f' && (e.ctrlKey || e.metaKey)) {
        dispatch(incrementFindCount());
      } else if (e.key === 'Escape') {
        dispatch(escapeProjectGraphEdit());
      }
    };
    window.addEventListener('keydown', listener);
    return () => window.removeEventListener('keydown', listener);
  }, []);

  useEffect(() => {
    if (active) {
      const poll = () => dispatch(pollForHeadCommit());
      // in case of presence failure, poll for head commit every minute while active
      const interval = setInterval(poll, 60000);
      poll();
      return () => clearInterval(interval);
    }
  }, [active]);

  useEditableIFrame();

  useLoNav();

  useEffect(() => {
    localStorage.setItem('DCM', 'story');
    return () => PresenceService.onAsset(null);
  }, []);

  useEffect(() => {
    // delay this because in expanded mode it bounces a bit
    const timeout = setTimeout(
      () => PresenceService.onAsset(subPageNames[page] ? page : activeNode),
      100
    );
    return () => clearTimeout(timeout);
  }, [activeNode, page]);

  useEffect(() => {
    const listener = (e: Event) => {
      dispatch(setNarrativeState({ offline: e.type === 'offline' && !local }));
    };
    window.addEventListener('online', listener);
    window.addEventListener('offline', listener);
    dispatch(setNarrativeState({ offline: !window.navigator.onLine && !local }));
    return () => {
      window.removeEventListener('online', listener);
      window.removeEventListener('offline', listener);
    };
  }, [local]);

  return <LovLink />;
};

export default NarrativeHooks;
