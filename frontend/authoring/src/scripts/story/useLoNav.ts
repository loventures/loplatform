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

import { push } from 'connected-react-router';
import { useEffect } from 'react';
import { useDispatch } from 'react-redux';

import {
  computeEditedOutEdges,
  getEditedAsset,
  computeEditedTargets,
  selectCurrentAssetName,
} from '../graphEdit';
import { NewAsset, NodeName } from '../types/asset';
import { Thunk } from '../types/dcmState';
import { editorUrl } from './story';

const loNavAction =
  (edgeId: string, target: string | undefined): Thunk =>
  (dispatch, getState) => {
    const state = getState();
    const { projectGraph, graphEdits } = state;
    const { branchId, homeNodeName } = projectGraph;
    const name = selectCurrentAssetName(state);

    // We cannot know what asset they clicked on, so we search all
    // descendants of the "current" asset for a matching edge id
    // in the case they have inline expanded a lesson. In most cases
    // this is fine. javascript: links don't give us the event target
    // to let us search the DOM.
    const targetNames = new Set<NodeName>();
    const findHyperlinkTargets = (name: NodeName) => {
      for (const edge of computeEditedOutEdges(name, 'hyperlinks', projectGraph, graphEdits)) {
        if (edge.edgeId === edgeId) targetNames.add(edge.targetName);
      }
      for (const edge of computeEditedOutEdges(name, 'elements', projectGraph, graphEdits)) {
        findHyperlinkTargets(edge.targetName);
      }
    };
    findHyperlinkTargets(name);
    // If there are multiple matches then don't navigate.
    const targetName = targetNames.size === 1 ? targetNames.values().next().value : undefined;
    // Find the first instance of the target node in the graph.
    const urls = new Array<string>();
    const findTargetUrl = (node: NewAsset<any>, path: NewAsset<any>[]) => {
      if (node.name === targetName) urls.push(editorUrl('story', branchId, node, path));
      const targets = computeEditedTargets(
        node.name,
        'elements',
        undefined,
        projectGraph,
        graphEdits
      );
      const subpath = [...path, node];
      for (const element of targets) {
        findTargetUrl(element, subpath);
      }
    };
    const homeNode = getEditedAsset(homeNodeName, projectGraph, graphEdits);
    findTargetUrl(homeNode, []);
    const to = urls[0];
    if (to) {
      const url = window.location.href.replace(/#.*/, '');
      if (target) window.open(`${url}#${to}`, target);
      else dispatch(push(to));
    }
  };

/**
 * This hook adds a `lonav(uuid)` function to the window object that HTML
 * content can invoke using javascript hyperlinks to navigate to content
 * by asset name.
 */
const useLoNav = () => {
  const dispatch = useDispatch();
  useEffect(() => {
    const winy = window as any;
    winy.lonav = (edgeOrEvent: string | MouseEvent) => {
      let edgeId: string, target: string | undefined;
      if (typeof edgeOrEvent === 'string') {
        // legacy javascript: href
        edgeId = edgeOrEvent;
      } else {
        // modern onclick()
        const event = edgeOrEvent;
        event.preventDefault();
        const newTab = event.metaKey || event.ctrlKey;
        const element = event.target as Element;
        if (element.closest('.note-editor')) return;
        // Matches EDGEID from javascript:lonav('EDGEID')
        edgeId = element.getAttribute('href')!.replace(/.*['"]([^'"]*)['"].*/, '$1');
        target = element.getAttribute('target') ?? (newTab ? '_blank' : undefined);
      }
      dispatch(loNavAction(edgeId, target));
    };
    return () => {
      delete winy.lonav;
    };
  }, []);
};

export default useLoNav;
