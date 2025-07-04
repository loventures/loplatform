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

import classnames from 'classnames';
import classNames from 'classnames';
import React, { useCallback, useEffect, useState } from 'react';
import { AiOutlineMenu } from 'react-icons/ai';
import { BsTornado } from 'react-icons/bs';
import { GiSettingsKnobs } from 'react-icons/gi';
import {
  IoCheckmarkOutline,
  IoClose,
  IoDocumentTextOutline,
  IoGitMergeOutline,
  IoSearchOutline,
} from 'react-icons/io5';
import { MdHistory, MdOutlineToc } from 'react-icons/md';
import { PiUser } from 'react-icons/pi';
import { VscHistory, VscListTree } from 'react-icons/vsc';
import { useDispatch } from 'react-redux';
import { Link } from 'react-router-dom';
import {
  Button,
  ButtonGroup,
  DropdownItem,
  DropdownMenu,
  DropdownToggle,
  UncontrolledDropdown,
} from 'reactstrap';
import { useDebounce } from 'use-debounce';

import { profileColor } from '../../feedback/FeedbackProfile';
import {
  confirmSaveProjectGraphEditsLink,
  useCurrentAssetName,
  useGraphEditSelector,
} from '../../graphEdit';
import { useBranchId, useDcmSelector, useHomeNodeName, useRouterPathVariable } from '../../hooks';
import { logoutAction, preferencesAction } from '../../nav/Navigation';
import { fetchStructure } from '../../structurePanel/projectGraphActions';
import { endFetchingStructure } from '../../structurePanel/projectStructureActions';
import { useUserProfile } from '../../user/userActions';
import { EditModeSwitch } from '../EditModeSwitch';
import { RolesRightsModal } from '../EditModeSwitch/RolesRightsModal';
import { SessionModal } from '../EditModeSwitch/SessionModal';
import { useProjectAccess } from '../hooks';
import { editorUrl, trackNarrativeEvent, trackNarrativeEventHandler } from '../story';
import { editModeAction, editRoleAction } from '../storyActions';
import { useIsStoryEditMode, useRevisionCommit, useStorySelector } from '../storyHooks';

const NonEditorActions = new Set(['index', 'search', 'history', 'multiverse', 'settings']);

export const ActionButtons: React.FC<{ stuck: boolean }> = () => {
  const dispatch = useDispatch();
  const current = useRouterPathVariable('name');
  const name = useCurrentAssetName();
  const { userCanEdit, userCanEditSettings, probableAdmin } = useDcmSelector(state => state.layout);
  const editMode = useIsStoryEditMode();
  const sudoed = useDcmSelector(s => s.user?.session?.sudoed || false);
  const dirty = useGraphEditSelector(s => s.dirty);

  const branchId = useBranchId();
  const homeNodeName = useHomeNodeName();
  const projectAccess = useProjectAccess();
  const projectSettings = userCanEditSettings && projectAccess.ProjectSettings;

  const commit = useRevisionCommit();

  const editorLink = editorUrl('story', branchId, homeNodeName, undefined, { commit });
  const tocLink = editorUrl('story', branchId, 'index', homeNodeName, { commit });
  const searchLink = editorUrl('story', branchId, 'search', homeNodeName, {});
  const historyLink = editorUrl('story', branchId, 'history', homeNodeName, { commit });
  const multiverseLink = editorUrl('story', branchId, 'multiverse', homeNodeName, { commit });
  const settingsLink = editorUrl('story', branchId, 'settings', homeNodeName, {});

  const isStale =
    useDcmSelector(
      state =>
        state.graphEdits.remoteHead != null &&
        state.graphEdits.remoteHead != state.projectGraph.commit.id
    ) && !commit;
  const io = useDcmSelector(state => state.graphEdits.saving || state.projectStructure.isFetching);
  const success = useDcmSelector(state => state.projectStructure.success);
  const synchronous = useStorySelector(state => state.synchronous);

  const doFetch = useCallback(() => {
    trackNarrativeEvent('Merge Updates');
    dispatch(fetchStructure());
  }, []);

  const [sessionModal, setSessionModal] = useState(false);
  const onSessionModal = useCallback(() => {
    trackNarrativeEvent('Session');
    setSessionModal(true);
  }, []);
  const offSessionModal = useCallback(() => setSessionModal(false), []);

  // TODO: I really need to look at commit create dates rather than commit ids because
  // if I start a fetch and then receive notification of a new commit while fetching,
  // I will overwrite that new remote commit id with the fetch result.
  const doSync = (synchronous || !editMode) && isStale && !io && success == null;
  useEffect(() => {
    if (doSync) dispatch(fetchStructure());
  }, [doSync]);

  // crappy way of debouncing fetches depending on the last response. this is counted after
  // the reducer is done with its junk which is why it is based on redux state
  useEffect(() => {
    if (success != null) {
      const timeout = setTimeout(
        () => dispatch(endFetchingStructure(undefined, null)),
        success === 'Delta' ? 1000 : success === 'Full' ? 5000 : 15000
      );
      return () => clearTimeout(timeout);
    }
  }, [success]);

  //const dispatch = useDispatch();
  //const editMode = useIsStoryEditMode();
  const rights = useDcmSelector(state => state.user.rights);
  const admin = rights?.includes('loi.authoring.security.right$EditContentAnyProjectRight');
  const role = useDcmSelector(state => state.layout.role);
  const [rolesHelp, setRolesHelp] = useState(false);

  const onEditMode = useCallback(
    (event: React.MouseEvent) => {
      const editMode = event.currentTarget.getAttribute('data-edit-mode') !== 'view';
      dispatch(editModeAction(editMode));
      if (admin && role) dispatch(editRoleAction(null));
    },
    [admin, role]
  );

  const onRolesHelp = useCallback(() => setRolesHelp(s => !s), []);

  // we have to debounce this because presence often tells us of a new remote head before
  // our own save has completed.
  const [debouncedStale] = useDebounce(isStale, 300);

  return (
    <div className="d-flex align-items-center gap-2 ms-2">
      {debouncedStale && !synchronous && editMode && (
        <Button
          id="graph-edit-fetch"
          size="sm"
          className="d-flex align-items-center text-truncate"
          color="warning"
          disabled={io}
          onClick={doFetch}
        >
          <IoGitMergeOutline className="me-2" />
          Merge Updates
        </Button>
      )}
      <div className="d-flex gap-1 action-buttons">
        <Link
          id="project-editor-button"
          to={editorLink}
          title="Editor"
          className={classNames(
            'd-flex p-1 btn btn-transparent cramped-hide br-50',
            !NonEditorActions.has(current) && 'disabled'
          )}
          style={{ opacity: 1 }}
          onClick={trackNarrativeEventHandler('Editor')}
        >
          <IoDocumentTextOutline size="1.2rem" />
        </Link>
        <Link
          id="project-toc-button"
          to={tocLink}
          title="Table of Contents"
          className={classNames(
            'd-flex p-1 btn btn-transparent cramped-hide br-50',
            current === 'index' && 'disabled'
          )}
          style={{ opacity: 1 }}
          onClick={trackNarrativeEventHandler('Table of Contents')}
        >
          <VscListTree size="1.2rem" />
        </Link>
        <Link
          id="search-button"
          to={searchLink}
          title="Search"
          className={classnames(
            'd-flex p-1 btn btn-transparent cramped-hide br-50',
            current === 'search' && 'disabled'
          )}
          style={{ opacity: 1 }}
          onClick={trackNarrativeEventHandler('Search')}
        >
          <IoSearchOutline size="1.2rem" />
        </Link>
        {projectAccess.ViewProjectHistory && (
          <Link
            id="project-history-button"
            to={historyLink}
            title="Project History"
            className={classnames(
              'd-flex p-1 btn btn-transparent cramped-hide br-50',
              current === 'history' && 'disabled'
            )}
            style={{ opacity: 1 }}
            onClick={trackNarrativeEventHandler('Project History')}
          >
            <VscHistory size="1.2rem" />
          </Link>
        )}
        {projectAccess.ViewMultiverse && (
          <Link
            id="project-multiverse-button"
            to={multiverseLink}
            title="Multiverse"
            className={classNames(
              'd-flex btn btn-transparent cramped-hide br-50 d-flex',
              current === 'multiverse' && 'disabled'
            )}
            onClick={trackNarrativeEventHandler('Multiverse')}
            style={{ opacity: 1, padding: '.35rem' }}
          >
            <BsTornado size="1rem" />
          </Link>
        )}
        {projectSettings && (
          <Link
            id="project-settings-button"
            to={settingsLink}
            title="Project Settings"
            className={classNames(
              'd-flex p-1 btn btn-transparent cramped-hide br-50',
              current === 'settings' && 'disabled'
            )}
            style={{ opacity: 1 }}
            onClick={trackNarrativeEventHandler('Project Settings')}
          >
            <GiSettingsKnobs size="1.2rem" />
          </Link>
        )}
      </div>
      <UncontrolledDropdown
        id="overflow-menu"
        className="cramped-show"
      >
        <DropdownToggle
          id="action-bar-toggle"
          title="More Options"
          color="transparent"
          className="d-flex"
          style={{ opacity: 1, padding: '.35rem' }}
        >
          <AiOutlineMenu size="1rem" />
        </DropdownToggle>
        <DropdownMenu
          end
          id="action-bar-menu"
        >
          <DropdownItem
            tag={Link}
            to={editorLink}
            className={classNames(!NonEditorActions.has(current) && 'disabled')}
            onClick={trackNarrativeEventHandler('Table of Contents')}
          >
            <IoDocumentTextOutline size=".85rem" />
            Editor
          </DropdownItem>
          <DropdownItem
            tag={Link}
            to={tocLink}
            className={classNames(current === 'index' && 'disabled')}
            onClick={trackNarrativeEventHandler('Table of Contents')}
          >
            <VscListTree size=".85rem" />
            Table of Contents
          </DropdownItem>
          <DropdownItem
            tag={Link}
            to={searchLink}
            className={classNames(current === 'search' && 'disabled')}
            onClick={trackNarrativeEventHandler('Search')}
          >
            <IoSearchOutline size=".85rem" />
            Search
          </DropdownItem>
          {projectAccess.ViewProjectHistory && (
            <DropdownItem
              tag={Link}
              to={historyLink}
              className={classNames(current === 'history' && 'disabled')}
              onClick={trackNarrativeEventHandler('Project History')}
            >
              <VscHistory size=".85rem" />
              Project History
            </DropdownItem>
          )}
          {projectAccess.ViewMultiverse && (
            <DropdownItem
              tag={Link}
              to={multiverseLink}
              className={classNames(current === 'multiverse' && 'disabled')}
              onClick={trackNarrativeEventHandler('Multiverse')}
            >
              <BsTornado
                size=".85rem"
                className="styled-multiverse"
              />
              Multiverse
            </DropdownItem>
          )}
          {projectSettings && (
            <DropdownItem
              tag={Link}
              to={settingsLink}
              className={classNames(current === 'settings' && 'disabled')}
              onClick={trackNarrativeEventHandler('Project Settings')}
            >
              <GiSettingsKnobs size=".85rem" />
              Project Settings
            </DropdownItem>
          )}
        </DropdownMenu>
      </UncontrolledDropdown>
      {commit ? (
        <ButtonGroup className="ms-2 ps-1">
          <Button
            size="sm"
            color="warning"
            className="d-flex align-items-center project-history"
            tag={Link}
            to={historyLink}
            onClick={trackNarrativeEventHandler('Project History')}
          >
            <MdHistory
              size="1rem"
              className="me-1"
            />
            Project History
          </Button>
          <Button
            tag={Link}
            to={`/branch/${branchId}/launch/${name}`}
            color="warning"
            size="sm"
            className="d-flex align-items-center commit-close"
            onClick={trackNarrativeEventHandler('Exit History')}
          >
            <IoClose />
          </Button>
        </ButtonGroup>
      ) : (
        <EditModeSwitch />
      )}
      <UncontrolledDropdown
        id="narrative-you-menu"
        style={{ paddingLeft: '.25rem', paddingRight: '1rem', marginRight: '-.75rem' }}
      >
        <DropdownToggle
          id="narrative-you-toggle"
          color="transparent"
          className="position-relative present-user d-flex border-0 p-0 br-50 active-you"
        >
          <ActionYou />
        </DropdownToggle>
        <DropdownMenu end>
          <DropdownItem
            onClick={onEditMode}
            data-edit-mode="view"
          >
            <div className="check-spacer">
              {!editMode && (!admin || !role) && <IoCheckmarkOutline />}
            </div>
            Viewing
          </DropdownItem>
          <DropdownItem
            disabled={!userCanEdit}
            onClick={onEditMode}
            data-edit-mode="edit"
          >
            <div className="check-spacer">
              {editMode && (!admin || !role) && <IoCheckmarkOutline />}
            </div>
            Editing
          </DropdownItem>
          {(admin || role) && (
            <DropdownItem onClick={onRolesHelp}>
              <div className="check-spacer">{admin && role && <IoCheckmarkOutline />}</div>
              {`${role ?? 'Rôle'}...`}
            </DropdownItem>
          )}
          <DropdownItem divider />
          <DropdownItem
            className="zero-show"
            tag="a"
            href="/Profile"
            onClick={e => {
              trackNarrativeEvent('Profile');
              return confirmSaveProjectGraphEditsLink(e, dirty, dispatch);
            }}
          >
            Profile
          </DropdownItem>
          <DropdownItem
            className="zero-show"
            onClick={() => {
              trackNarrativeEvent('Preferences');
              dispatch(preferencesAction(true));
            }}
          >
            Preferences...
          </DropdownItem>
          <DropdownItem onClick={onSessionModal}>Session...</DropdownItem>
          <DropdownItem
            className="zero-show"
            divider
          />
          <DropdownItem
            className="zero-show"
            tag="a"
            href="/"
            onClick={e => {
              trackNarrativeEvent('Courses');
              return confirmSaveProjectGraphEditsLink(e, dirty, dispatch);
            }}
          >
            Courses
          </DropdownItem>
          <DropdownItem
            className="zero-show"
            tag={Link}
            to="/"
            onClick={e => {
              trackNarrativeEvent('Projects');
              return confirmSaveProjectGraphEditsLink(e, dirty, dispatch);
            }}
          >
            Projects
          </DropdownItem>
          {probableAdmin && (
            <DropdownItem
              className="zero-show"
              tag="a"
              href="/Administration"
              onClick={e => {
                trackNarrativeEvent('Administration');
                return confirmSaveProjectGraphEditsLink(e, dirty, dispatch);
              }}
            >
              Administration
            </DropdownItem>
          )}
          <DropdownItem
            className="zero-show"
            divider
          />
          <DropdownItem
            className="zero-show"
            onClick={() => dispatch(logoutAction())}
          >
            {sudoed ? 'Exit' : 'Logout'}
          </DropdownItem>
        </DropdownMenu>
      </UncontrolledDropdown>

      <RolesRightsModal
        open={rolesHelp}
        toggle={onRolesHelp}
      />

      <SessionModal
        open={sessionModal}
        toggle={offSessionModal}
      />
    </div>
  );
};

const ActionYou: React.FC = () => {
  const profile = useUserProfile();
  const color = profileColor(profile);

  return (
    <div
      className="present-user-circle"
      style={{ width: '2rem', height: '2rem' }}
    >
      <div
        className="present-user-photo"
        style={{ backgroundColor: color }}
        role="presentation"
      >
        {profile.imageUrl ? (
          <img
            className="user-photo"
            alt=""
            src={profile.imageUrl}
          />
        ) : (
          <PiUser
            className="user-letter"
            size="85%"
          />
        )}
      </div>
    </div>
  );
};
