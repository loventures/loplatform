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

import { capitalize, uniqueId } from 'lodash';
import React, { KeyboardEventHandler, useCallback, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';

import { trackAuthoringEvent } from '../analytics';
import edgeRuleConstants, { FILE_TYPES } from '../editor/EdgeRuleConstants';
import { confirmDiscardProjectGraphEdit } from '../graphEdit';
import { ProjectGraph } from '../structurePanel/projectGraphReducer';
import { NewAsset, TypeId } from '../types/asset';
import { EdgeGroup } from '../types/edge';
import { Polyglot } from '../types/polyglot';
import { isQuestion } from './questionUtil';

export type NarrativeEditor<T extends TypeId> = React.FC<{
  asset: NewAsset<T>;
  contextPath: string;
  mode: NarrativeMode;
  projectGraph: ProjectGraph;
  readOnly: boolean;
}>;

export type NarrativeMode = 'apex' | 'inline' | 'feedback' | 'revision';

export type NarrativeSettings<T extends TypeId> = React.FC<{
  asset: NewAsset<T>;
}>;

const defaultEdgeGroups: Partial<Record<TypeId, EdgeGroup>> = {
  'competencySet.1': 'level1Competencies',
  'level1Competency.1': 'level2Competencies',
  'level2Competency.1': 'level3Competencies',
  'level3Competency.1': 'never' as any,
};

export const childEdgeGroup = (typeId?: TypeId): EdgeGroup =>
  defaultEdgeGroups[typeId] ?? (edgeRuleConstants[typeId]?.questions ? 'questions' : 'elements');

export const primitiveTypes = new Set<TypeId>([
  ...FILE_TYPES,
  'rubric.1',
  'webDependency.1',
  'js.1',
  'css.1',
]);

export const HideyHeaderHeight = 56;
export const StickyHeaderHeight = 47;

export const scrollTopIntoView = (
  el: HTMLElement | undefined,
  behavior: ScrollBehavior = 'smooth'
) => {
  // Magic offset for the sticky headers.
  const main = document.scrollingElement;
  if (el && main) {
    if (el.offsetTop < main.scrollTop) {
      main.scrollTo({
        top: Math.max(HideyHeaderHeight, el.offsetTop - StickyHeaderHeight - 16),
        behavior,
      });
    }
  }
};

export const scrollBottomIntoView = (
  el: HTMLElement | undefined,
  behavior: ScrollBehavior = 'smooth'
) => {
  const main = document.scrollingElement;
  if (el && main) {
    const { top, bottom } = el.getBoundingClientRect();
    if (
      top < main.scrollTop - StickyHeaderHeight ||
      bottom - top >= main.clientHeight - StickyHeaderHeight
    ) {
      main.scrollTo({ top: main.scrollTop + top - StickyHeaderHeight, behavior });
    } else if (bottom > main.clientHeight) {
      main.scrollTo({ top: main.scrollTop + bottom - main.clientHeight, behavior });
    }
  }
};

export const scrollToTopOfScreen = (behavior: ScrollBehavior = 'smooth') => {
  const main = document.scrollingElement;
  if (main) {
    main.scrollTo({
      left: 0,
      top: Math.min(main.scrollTop, HideyHeaderHeight),
      behavior,
    });
  }
};

// turducken
export const plural = (n: number, str: string): string => (n === 1 ? `1 ${str}` : `${n} ${str}s`);

export const sentence = (...parts: string[]): string =>
  capitalize(
    parts
      .filter(s => !!s)
      .join(', ')
      .toLowerCase()
  ) + '.';

export const editorUrl = (
  type: 'story' | 'asset' | 'revision' | 'rubric-editor',
  branchId: number,
  asset: NewAsset<any> | string,
  context: NewAsset<any>[] | string | undefined,
  params: Record<string, any> = {}
) => {
  const contextPath = Array.isArray(context) ? context.map(asset => asset.name).join('.') : context;
  let queryString = contextPath ? `contextPath=${contextPath}` : '';
  for (const [k, v] of Object.entries(params)) {
    if (v != null) queryString = queryString ? `${queryString}&${k}=${v}` : `${k}=${v}`;
  }
  const name = typeof asset === 'string' ? asset : asset?.name;
  queryString = queryString ? `?${queryString}` : '';
  return `/branch/${branchId}/${type}/${name}${queryString}`;
};

export const useEditSession = (editing = true) => {
  const [session, setSession] = useState('');
  useEffect(() => {
    if (editing) setSession(uniqueId);
  }, [editing, setSession]);
  // i could rotate session after a period of time but not for now..
  return session;
};

const Escape = new Set(['Escape']);

export const useEscapeToDiscardEdits = (session: string, exitEditing: () => void) => {
  const dispatch = useDispatch();
  const discardEditing = useCallback(() => {
    dispatch(confirmDiscardProjectGraphEdit(session, exitEditing));
  }, [exitEditing, session]);
  return useKeysToStopEditing(Escape, discardEditing);
};

const EscapeOrEnter = new Set(['Escape', 'Enter']);

export const useEscapeOrEnterToStopEditing = (
  exitEditing: (enter: boolean, e: React.KeyboardEvent) => void
) => {
  const cb = useCallback(
    (e: React.KeyboardEvent) => exitEditing(e.key === 'Enter', e),
    [exitEditing]
  );
  return useKeysToStopEditing(EscapeOrEnter, cb);
};

export const useKeysToStopEditing = (
  keys: Set<string>,
  exitEditing: (e: React.KeyboardEvent) => void
) =>
  useCallback<KeyboardEventHandler>(
    e => {
      if (keys.has(e.key)) {
        e.preventDefault();
        e.stopPropagation();
        exitEditing(e);
      }
    },
    [exitEditing]
  );

export type StagedBlob = {
  type: 'text/html';
  value: string;
  get: () => string;
};

export const isStagedBlob = (a: any): a is StagedBlob => (a as any)?.type === 'text/html';

export const preventUndefinedClick = (asset: any | undefined) => (e: React.MouseEvent) =>
  !asset && e.preventDefault();

// 0mc4: Foo Bar (live: baz), 0mc4 - Foo Bar (live: baz) -> Foo Bar
// Lesson 1: Foo Bar Baz -> Lesson 1
export const truncateAssetName = (asset: NewAsset<any>) =>
  truncateAssetTitle(asset.data?.title, asset.typeId);

export const truncateAssetTitle = (title: string, typeId: TypeId) =>
  typeId === 'course.1'
    ? title?.trim().replace(/^(?:.*?(?:: | - ))?(.*?)(?:\s*\(.*\))?$/, '$1')
    : title?.trim().replace(/:.*/, '');

export const contextPathQuery = (nodes: NewAsset<any>[] | string, commit?: number) => {
  const commitParam = commit ? `commit=${commit}` : undefined;
  const contextPath = Array.isArray(nodes) ? nodes.map(n => n?.name).join('.') : nodes;
  return contextPath
    ? commitParam
      ? `?contextPath=${contextPath}&${commitParam}`
      : `?contextPath=${contextPath}`
    : commitParam
      ? `?${commitParam}`
      : '';
};

export const storyTypeName = (
  polyglot: Polyglot,
  typeId: TypeId | undefined,
  questionSuffix = true
) => {
  const qSuf = questionSuffix && isQuestion(typeId) ? ' Question' : '';
  // _ provides a fallback to use if the primary key is not defined
  return polyglot.t(`STORY_${typeId}`, { _: polyglot.t(typeId) + qSuf });
};

// lodash capitalize lowercases everything..
export const cap = (s: string) => (s ? `${s.slice(0, 1).toUpperCase()}${s.slice(1)}` : s);

export const Onceler = <A>() => {
  const set = new Set<A>();
  return (a: A) => {
    if (set.has(a)) {
      return false;
    } else {
      set.add(a);
      return true;
    }
  };
};

export type SubPage =
  | 'index'
  | 'search'
  | 'history'
  | 'objectives'
  | 'timeline'
  | 'multiverse'
  | 'defaults'
  | 'gradebook'
  | 'graph'
  | 'settings';

export const subPageNames: Record<SubPage, string> = {
  index: 'Table of Contents',
  search: 'Search',
  history: 'Project History',
  objectives: 'Learning Objectives',
  timeline: 'Course Timeline',
  multiverse: 'Multiverse',
  defaults: 'Course Defaults',
  gradebook: 'Gradebook',
  graph: 'Project Graph',
  settings: 'Project Settings',
};

export const trackNarrativeEvent = (kind: string): void =>
  trackAuthoringEvent(`Narrative Editor - ${kind}`);

const narrativeEventHandlerMemo: Record<string, () => void> = {};

export const trackNarrativeEventHandler = (kind: string): (() => void) =>
  (narrativeEventHandlerMemo[kind] ??= () => trackNarrativeEvent(kind));
