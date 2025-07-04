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
import React from 'react';
import { GiSettingsKnobs } from 'react-icons/gi';
import { useDispatch } from 'react-redux';
import { useHistory } from 'react-router';
import { DropdownItem, DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

import { trackAuthoringEvent } from '../../../analytics';
import { UPDATE_BRANCH } from '../../../dcmStoreConstants';
import { useDcmSelector } from '../../../hooks';
import { ConfirmationTypes } from '../../../modals/ConfirmModal';
import { openModal } from '../../../modals/modalActions';
import { ModalIds } from '../../../modals/modalIds';
import { openToast, TOAST_TYPES } from '../../../toast/actions';
import { NoProjects, reloadProjects, setProjects } from '../../dataActions';
import { useIsStoryEditMode } from '../../storyHooks';
import { archiveProject, deleteProject, unarchiveProject } from '../ProjectHistory/projectApi';
import { Project } from '../../../layout/dcmLayoutReducer.ts';
import { Thunk } from '../../../types/dcmState';

export const deleteProjectAction =
  (project: Project, andThen?: Thunk): Thunk =>
  dispatch => {
    dispatch(
      openModal(ModalIds.Confirm, {
        confirmationType: ConfirmationTypes.DeleteProject,
        color: 'danger',
        words: {
          header: 'Delete Project?',
          body: 'Are you sure you want to permanently delete this project? The project can only be restored by filing a technical support ticket.',
          confirm: 'Delete',
        },
        confirmCallback: () => {
          trackAuthoringEvent('Narrative Editor - Delete Project');
          return deleteProject(project.id)
            .then(() => {
              dispatch(setProjects(NoProjects));
              dispatch(reloadProjects());
              if (andThen) dispatch(andThen);
            })
            .catch(e => {
              console.warn(e);
              dispatch(openToast('The delete operation failed.', 'danger'));
            });
        },
      })
    );
  };

export const ProjectActionsMenu: React.FC<{
  dirty: boolean;
}> = ({ dirty }) => {
  const dispatch = useDispatch();
  const history = useHistory();
  const editMode = useIsStoryEditMode();
  const project = useDcmSelector(s => s.layout.project);
  const user = useDcmSelector(s => s.configuration.user);
  const canDelete =
    project.ownedBy === user.id ||
    user.rights.includes('loi.authoring.security.right$DeleteAnyProjectRight');

  const verb = project.archived ? 'Reinstate' : 'Retire';
  return (
    <UncontrolledDropdown>
      <DropdownToggle
        id="project-settings-actions-toggle"
        color="primary"
        outline
        caret
        className={classNames(
          'border-0 asset-settings',
          dirty ? 'dirty' : 'unhover-muted hover-white'
        )}
      >
        <GiSettingsKnobs size="1.75rem" />
      </DropdownToggle>
      <DropdownMenu>
        <DropdownItem
          id="project-settings-retire-button"
          onClick={() => {
            trackAuthoringEvent(`Narrative Editor - ${verb} Project`);
            (project.archived ? unarchiveProject : archiveProject)(project.id)
              .then(({ projects: [{ project }] }) => {
                dispatch(openToast(`Project ${verb.toLowerCase()}d.`, TOAST_TYPES.SUCCESS));
                dispatch({ type: UPDATE_BRANCH, layout: { project } });
                dispatch(reloadProjects());
              })
              .catch(e => {
                console.log(e);
                dispatch(openToast('Update failed.', TOAST_TYPES.DANGER));
              });
          }}
          disabled={!editMode || !canDelete}
        >
          {`${verb} Project`}
        </DropdownItem>
        <DropdownItem
          id="project-settings-delete-button"
          onClick={() => {
            dispatch(deleteProjectAction(project, () => history.push('/')));
          }}
          disabled={!editMode || !canDelete}
          className="text-danger"
        >
          Delete Project
        </DropdownItem>
      </DropdownMenu>
    </UncontrolledDropdown>
  );
};
