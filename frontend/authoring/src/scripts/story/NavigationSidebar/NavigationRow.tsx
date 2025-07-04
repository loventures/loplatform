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

import classNames from 'classnames';
import { sumBy } from 'lodash';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useCollapse } from 'react-collapsed';
import { GiSandsOfTime } from 'react-icons/gi';
import { SlNotebook } from 'react-icons/sl';
import { useDispatch } from 'react-redux';
import { Link } from 'react-router-dom';
import { Button, Input } from 'reactstrap';

import edgeRules from '../../editor/EdgeRuleConstants';
import {
  getAllEditedOutEdges,
  useAllEditedOutEdges,
  useEditedAssetTitle,
  useEditedAssetTypeId,
} from '../../graphEdit';
import { ContextPath } from '../../graphEdit/graphEditReducer';
import { useDcmSelector, useHomeNodeName, usePolyglot, useRouterPathVariable } from '../../hooks';
import { Bullseye } from '../../projectNav/NavIcons';
import { NodeName } from '../../types/asset';
import { DcmState } from '../../types/dcmState';
import { getIcon } from '../AddAsset';
import { useProjectAccess } from '../hooks';
import { useContentAccess } from '../hooks/useContentAccess';
import { ChevronCircle } from '../icons/ChevronCircle';
import { ChevronPlain } from '../icons/ChevronPlain';
import { usePresentUsers } from '../NarrativeAsset/NarrativePresence';
import { expandDescendantAction } from '../PreviewMenu/actions';
import { isQuestion } from '../questionUtil';
import {
  editorUrl,
  scrollToTopOfScreen,
  storyTypeName,
  trackNarrativeEventHandler,
} from '../story';
import { useIsEditable, useIsInlineNarrativeView } from '../storyHooks';
import { hideStructurePanel } from '../../structurePanel/projectStructureActions.ts';

const descendantsSelectedSelector =
  (name: NodeName, selected: Set<NodeName>) =>
  ({ projectGraph, graphEdits }: DcmState): number => {
    const loop = (name: NodeName): number =>
      sumBy(getAllEditedOutEdges(name, projectGraph, graphEdits), ({ group, targetName }) =>
        group === 'elements' || group === 'questions' ? loop(targetName) : 0
      ) + (selected.has(name) ? 1 : 0);
    return loop(name);
  };

export const NavigationRow: React.FC<{
  branchId: number;
  name: NodeName;
  depth: number;
  index: number;
  last?: boolean;
  lastX?: boolean;
  lastY?: boolean;
  lastZ?: boolean;
  contextPath?: string;
  active: NodeName;
  activePath: ContextPath;
  expanded: Set<NodeName>;
  visible: Set<NodeName> | undefined;
  toggle: (name: NodeName, open?: boolean) => void;
  editing: boolean;
  canDelete: boolean;
  ancestorSelected: boolean;
  selected: Set<NodeName>;
  select: (name: NodeName) => void;
  autoHide: boolean;
}> = ({
  branchId,
  name,
  index,
  last,
  lastX,
  lastY,
  lastZ,
  depth,
  contextPath,
  active,
  activePath,
  expanded,
  visible,
  toggle,
  editing,
  canDelete,
  ancestorSelected,
  selected,
  select,
  autoHide,
}) => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const homeName = useHomeNodeName();
  const title = useEditedAssetTitle(name);
  const typeId = useEditedAssetTypeId(name);
  const isCourse = typeId === 'course.1';
  const question = isQuestion(typeId);
  const Icon = getIcon(typeId);
  const contentAccess = useContentAccess(name);
  const inlineView = useIsInlineNarrativeView();
  const outEdges = useAllEditedOutEdges(name);
  const isExpanded =
    (name === homeName || expanded.has(name)) && (depth === 0 || contentAccess.ViewContent);
  const subcontext = contextPath ? `${contextPath}.${name}` : name;
  const questionable = edgeRules[typeId]?.questions;
  const parentable = edgeRules[typeId]?.elements || questionable;
  const [hold, setHold] = useState(isExpanded);
  useEffect(() => setHold(h => h || isExpanded), [isExpanded]);
  const isActive = active === name || (!isExpanded && activePath?.includes(name));
  const isVisible = !visible || visible.has(name);
  const current = useRouterPathVariable('name');
  const isSemiactive = inlineView && name === current;
  const { getCollapseProps } = useCollapse({
    defaultExpanded: isExpanded,
    isExpanded,
    onTransitionStateChange: state => {
      if (state === 'collapseEnd') setHold(false);
    },
  });
  const children = useMemo(
    () =>
      outEdges.filter(
        edge => (hold || isExpanded) && (edge.group === 'elements' || edge.group === 'questions')
      ),
    [outEdges, hold, isExpanded]
  );
  const Chevron = questionable ? ChevronCircle : ChevronPlain;
  const canDeleteChildren = useIsEditable(name, 'AddRemoveContent');

  const onClick = useCallback(
    (e: React.MouseEvent) => {
      if (name === current) {
        scrollToTopOfScreen();
        toggle(name, !isActive);
        e.preventDefault();
      } else if (inlineView && contextPath?.includes(current)) {
        dispatch(expandDescendantAction(name, contextPath));
        e.preventDefault();
      } else if (parentable) {
        toggle(name, !isActive);
      }
      if (autoHide) dispatch(hideStructurePanel(true));
    },
    [name, current, contextPath, isActive, inlineView, parentable, autoHide]
  );
  const onExpand = useCallback(() => toggle(name), [name]);

  const isSelected = selected.has(name);
  const onSelect = useCallback(() => select(name), [name]);

  const indeterminate = useDcmSelector(
    state =>
      !isSelected && !isExpanded && editing && descendantsSelectedSelector(name, selected)(state)
  );

  const checkboxRef = useRef<HTMLInputElement>();
  useEffect(() => {
    // sadly indeterminism must be done by dom manipulation
    if (checkboxRef.current) {
      (checkboxRef.current as any).indeterminate = !!indeterminate;
      checkboxRef.current.parentElement.title = indeterminate
        ? `${indeterminate} assets selected`
        : undefined;
    }
  }, [indeterminate]);

  const present = useDcmSelector(state =>
    isExpanded ? state.presence.usersAtAsset[name] : state.presence.usersWithinAsset[name]
  );

  return !typeId ? null : (
    <>
      {isVisible && (
        <div
          className={classNames(
            'd-flex align-items-center px-2 gap-1 navigation-row flex-shrink-0',
            isActive ? 'active' : isSemiactive ? 'semiactive' : null,
            isExpanded && 'expanded'
          )}
        >
          {present && <NavPresence handles={present} />}
          {depth > 4 && <div className={classNames('depther', lastZ && 'invisible')} />}
          {depth > 3 && <div className={classNames('depther', lastY && 'invisible')} />}
          {depth > 2 && <div className={classNames('depther', lastX && 'invisible')} />}
          {depth > 1 && <div className={last ? 'depther last' : 'depther mid'} />}
          {parentable && !isCourse ? (
            <Button
              className="p-0 d-flex mini-button br-50"
              color="transparent"
              onClick={onExpand}
              style={{ fontSize: '1em' }}
              title={storyTypeName(polyglot, typeId)}
            >
              <Chevron
                className={classNames(
                  'content-type-icon flex-shrink-0 text-truncate',
                  typeId.replace(/\..*/, '')
                )}
              />
            </Button>
          ) : !question ? (
            <div
              className={classNames('d-flex br-50', typeId.replace(/\..*/, ''))}
              title={storyTypeName(polyglot, typeId)}
            >
              <Icon className="flex-shrink-0 text-truncate" />
            </div>
          ) : null}
          <Link
            to={editorUrl('story', branchId, name, contextPath)}
            className={classNames(
              'flex-grow-1 flex-shrink-1 text-truncate py-1',
              contentAccess.ViewContent && 'hover-underline'
            )}
            title={title}
            onClick={onClick}
          >
            {question ? (
              <>
                {`Question ${index + 1} – `}
                <span className="unhover-muted">{title || 'Untitled'}</span>
              </>
            ) : (
              title
            )}
          </Link>
          {editing ? (
            <Input
              id={`${subcontext}-toggle`}
              type="checkbox"
              className="mt-0"
              checked={isSelected || ancestorSelected}
              disabled={ancestorSelected || !canDelete || isCourse}
              onChange={onSelect}
              innerRef={checkboxRef}
            />
          ) : isCourse ? (
            <CourseNav
              branchId={branchId}
              name={name}
              current={current}
            />
          ) : null}
        </div>
      )}
      {parentable && isVisible && (
        <div {...getCollapseProps()}>
          {!children.length && (
            <div className="text-muted d-flex align-items-center px-2 gap-1 navigation-row">
              {depth > 2 && <div className="depther" />}
              {depth > 1 && <div className={classNames('depther', last && 'invisible')} />}
              <div style={{ paddingLeft: '1.1rem' }}>
                {questionable ? 'No questions' : 'No contents'}
              </div>
            </div>
          )}
          {children.map((edge, index) => (
            <NavigationRow
              key={edge.name}
              branchId={branchId}
              name={edge.targetName}
              contextPath={subcontext}
              index={index}
              last={index === children.length - 1}
              lastX={last}
              lastY={lastX}
              lastZ={lastY}
              depth={1 + depth}
              active={active}
              activePath={activePath}
              expanded={expanded}
              visible={visible}
              toggle={toggle}
              editing={editing}
              canDelete={canDeleteChildren}
              ancestorSelected={ancestorSelected || isSelected}
              selected={selected}
              select={select}
              autoHide={autoHide}
            />
          ))}
        </div>
      )}
    </>
  );
};

const CourseNav: React.FC<{ branchId: number; name: NodeName; current: string }> = ({
  branchId,
  name,
  current,
}) => {
  const projectAccess = useProjectAccess();
  return (
    <div className="d-flex align-items-stretch flex-shrink-0 pe-1">
      {projectAccess.ViewObjectives && (
        <Link
          id="learning-objectives-button"
          to={editorUrl('story', branchId, 'objectives', name)}
          title="Learning Objectives"
          className={classNames(
            'btn btn-sm btn-transparent d-flex p-1 br-50 border-0',
            current === 'objectives' && 'disabled navigation-active-icon'
          )}
          onClick={trackNarrativeEventHandler('Learning Objectives')}
        >
          <Bullseye />
        </Link>
      )}
      {projectAccess.ViewGradebook && (
        <Link
          id="course-gradebook-button"
          to={editorUrl('story', branchId, 'gradebook', name)}
          title="Course Gradebook"
          className={classNames(
            'btn btn-sm btn-transparent d-flex p-1 br-50 border-0',
            current === 'gradebook' && 'disabled navigation-active-icon'
          )}
          onClick={trackNarrativeEventHandler('Course Gradebook')}
        >
          <SlNotebook size=".85rem" />
        </Link>
      )}
      {projectAccess.ViewTimeline && (
        <Link
          id="course-timeline-button"
          to={editorUrl('story', branchId, 'timeline', name)}
          title="Course Timeline"
          className={classNames(
            'btn btn-sm btn-transparent d-flex p-1 br-50 border-0',
            current === 'timeline' && 'disabled navigation-active-icon'
          )}
          onClick={trackNarrativeEventHandler('Course Timeline')}
        >
          <GiSandsOfTime />
        </Link>
      )}
    </div>
  );
};

const NavPresence: React.FC<{ handles: string[] }> = ({ handles }) => {
  const presences = usePresentUsers(handles);

  return (
    <>
      {presences.map((profile, index) => (
        <div
          key={profile.handle}
          className="nav-presence"
          style={{
            backgroundColor: profile.color,
            transform: `translateX(${(presences.length - 1 - index) * -2}px)`,
          }}
        />
      ))}
    </>
  );
};
