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
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { BiCollapseVertical } from 'react-icons/bi';
import { IoTrashOutline } from 'react-icons/io5';
import { TfiClose, TfiPencil } from 'react-icons/tfi';
import { VscListTree } from 'react-icons/vsc';
import { useDispatch } from 'react-redux';
import { Button } from 'reactstrap';

import { useIsScrolled } from '../feedback/feedback';
import { setAdd, setToggle } from '../gradebook/set';
import { useBranchId, useDcmSelector, useHomeNodeName } from '../hooks';
import { useProjectGraphSelector } from '../structurePanel/projectGraphHooks';
import { NodeName } from '../types/asset';
import {
  deleteAssetsAction,
  navigationSearchAction,
  toggleSelectedAction,
} from './NavigationSidebar/actions';
import { NavigationRow } from './NavigationSidebar/NavigationRow';
import SearchBarAutohide from './NavigationSidebar/SearchBarAutohide';
import { useStorySelector } from './storyHooks';
import { hideStructurePanel } from '../structurePanel/projectStructureActions';
import { useMedia } from 'react-use';

// Chrome displays a target URL tooltip in the bottom left of the screen, which obscures the current
// location if it's at the very bottom, so factor some fudge in there.
const BottomFudgeFactor = 21;

export const NavigationSidebar: React.FC = () => {
  const { hidden: structureHidden } = useDcmSelector(s => s.projectStructure);
  const dispatch = useDispatch();
  const branchId = useBranchId();
  const activeNode = useStorySelector(state => state.activeNode);
  const contextPath = useStorySelector(state => state.activeContextPath);
  const editMode = useStorySelector(state => state.editMode);
  const homeName = useHomeNodeName();
  const loaded = useProjectGraphSelector(s => s.rootNodeName);
  const [expanded, setExpanded] = useState(new Set<NodeName>());
  const autoHide = useMedia('(max-width: 64em)');

  const [editing, setEditing] = useState(false);

  const [selected, setSelected] = useState(new Set<NodeName>());
  const select = useCallback(
    (name: NodeName) => dispatch(toggleSelectedAction(name, setSelected)),
    []
  );

  const [visible, setVisible] = useState<Set<NodeName> | undefined>();
  const toggle = useCallback(
    (name: NodeName, open?: boolean) =>
      setExpanded(e => (open ? setAdd(e, name) : setToggle(e, name))),
    []
  );

  useEffect(() => {
    if (contextPath) setExpanded(e => setAdd(e, contextPath.split('.')));
  }, [contextPath]);

  const divRef = useRef<HTMLDivElement | null>();
  useEffect(() => {
    if (!loaded || structureHidden) return;
    const timeout = setTimeout(() => {
      const active = divRef.current?.getElementsByClassName('active');
      if (active.length && divRef.current) {
        const item = active.item(0) as HTMLDivElement;
        const top = item.offsetTop;
        const bottom = top + item.offsetHeight;
        if (top < divRef.current.scrollTop) {
          divRef.current.scrollTo({
            top: bottom - divRef.current.clientHeight / 2, // was top - divRef.current.clientHeight
            behavior: 'smooth',
          });
        } else if (
          bottom >
          divRef.current.scrollTop + divRef.current.clientHeight - BottomFudgeFactor
        ) {
          divRef.current.scrollTo({
            top: top - divRef.current.clientHeight / 2, // was bottom - divRef.current.clientHeight + BottomFudgeFactor
            behavior: 'smooth',
          });
        }
      }
    }, 100);
    return () => clearTimeout(timeout);
  }, [activeNode, loaded, structureHidden]);

  const toggleSidebar = useCallback(
    () => dispatch(hideStructurePanel(!structureHidden)),
    [structureHidden]
  );

  const onDelete = useCallback(() => {
    dispatch(deleteAssetsAction(selected));
    setSelected(new Set());
  }, [selected]);

  const [hold, setHold] = useState(!structureHidden);
  useEffect(() => {
    if (structureHidden) {
      setSearch('');
      const timeout = setTimeout(() => setHold(false), 100);
      return () => clearTimeout(timeout);
    } else {
      setHold(true);
    }
  }, [structureHidden]);

  const scrolled = useIsScrolled(divRef.current);
  const [search, setSearch] = useState('');
  const doSearch = useCallback(() => {
    if (search)
      dispatch(
        navigationSearchAction(search, state => {
          setExpanded(state);
          setVisible(state);
        })
      );
  }, [search, homeName, contextPath]);

  useEffect(() => {
    if (!search) {
      setExpanded(new Set([homeName, ...(contextPath ? contextPath.split('.') : [])]));
      setVisible(undefined);
    }
  }, [search]);

  const toggleEditing = useCallback(() => {
    setEditing(e => !e);
    setSelected(new Set());
  }, []);

  const isExpanded = expanded.size > 0;

  return (
    <>
      {structureHidden && (
        <Button
          className="navigation-icon"
          onClick={toggleSidebar}
          title="Course Structure"
        >
          <VscListTree size="1.5rem" />
        </Button>
      )}
      <div className="grid-structure-panel narrative-nav narrative-editor">
        <div
          className={classNames(
            'navigation-header d-flex align-items-center px-2',

            scrolled && 'scrolled',
            search && 'searched',
            editing && 'editing'
          )}
        >
          <Button
            color="transparent"
            className="border-0 collapse-button d-flex align-content-center br-50"
            style={{ padding: '.4rem' }}
            title="Collapse All"
            onClick={() => setExpanded(new Set([homeName]))}
            disabled={!isExpanded}
          >
            <BiCollapseVertical
              aria-hidden={true}
              size="1rem"
              className={isExpanded ? 'text-muted' : undefined}
            />
          </Button>
          {editMode && (
            <Button
              color={editing ? 'primary' : 'transparent'}
              className="border-0 edit-button d-flex align-content-center br-50"
              style={{ padding: '.4rem' }}
              title="Edit"
              onClick={toggleEditing}
            >
              <TfiPencil
                aria-hidden={true}
                size="1rem"
                className={editing ? undefined : 'text-muted'}
              />
            </Button>
          )}
          <SearchBarAutohide
            size="sm"
            placeholder="Search by title..."
            value={search}
            setValue={setSearch}
            disabled={!search}
            onSearch={doSearch}
          />
          {editing ? (
            <Button
              key="delete-button"
              color="danger"
              outline
              className="border-0 delete-button d-flex align-content-center"
              style={{ padding: '.4rem' }}
              title="Remove"
              disabled={!selected.size}
              onClick={onDelete}
            >
              <IoTrashOutline
                aria-hidden={true}
                size="1rem"
              />
            </Button>
          ) : (
            <Button
              key="close-button"
              color="transparent"
              className="border-0 close-button d-flex align-content-center ms-auto"
              style={{ padding: '.4rem' }}
              onClick={toggleSidebar}
              title="Close"
            >
              <TfiClose
                aria-hidden={true}
                size="1rem"
              />
            </Button>
          )}
        </div>
        <div
          className="inner d-flex flex-column pb-3"
          ref={divRef}
        >
          {(hold || !structureHidden) && (
            <NavigationRow
              branchId={branchId}
              name={homeName}
              depth={0}
              index={0}
              active={activeNode}
              activePath={contextPath}
              expanded={expanded}
              visible={visible}
              toggle={toggle}
              editing={editing}
              canDelete={false}
              selected={selected}
              select={select}
              ancestorSelected={false}
              autoHide={autoHide}
            />
          )}
        </div>
      </div>
    </>
  );
};
