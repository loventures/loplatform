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

import React, { Dispatch, SetStateAction, useCallback, useRef } from 'react';
import { Link } from 'react-router-dom';
import { DropdownItem } from 'reactstrap';

import { useBranchId, useRouterPathVariable } from '../../hooks';
import { NodeName } from '../../types/asset';
import { useContentAccess, useProjectAccess } from '../hooks';
import { editorUrl, trackNarrativeEvent, trackNarrativeEventHandler } from '../story';
import { useIsStoryEditMode, useRevisionCommit } from '../storyHooks';
import { SubmenuItem } from './SubmenuItem';
import { useDispatch } from 'react-redux';
import { setCourseBannerImage } from './actions.ts';
import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  deleteProjectGraphEdge,
  useAllEditedOutEdges,
} from '../../graphEdit';

export const CourseSubmenu: React.FC<{
  name: NodeName;
  setOpen: Dispatch<SetStateAction<boolean>>;
}> = ({ name, setOpen }) => {
  const branchId = useBranchId();
  const commit = useRevisionCommit();
  const current = useRouterPathVariable('name');
  const objectivesLink = editorUrl('story', branchId, 'objectives', name, { commit });
  const timelineLink = editorUrl('story', branchId, 'timeline', name, { commit });
  const gradebookLink = editorUrl('story', branchId, 'gradebook', name, { commit });
  const defaultsLink = editorUrl('story', branchId, 'defaults', name, { commit });
  const projectAccess = useProjectAccess();
  const editMode = useIsStoryEditMode();
  const contentAccess = useContentAccess(name);
  const bannerRef = useRef<HTMLInputElement>();
  const edges = useAllEditedOutEdges(name);
  const existingImage = edges.find(e => e.group === 'image');

  const dispatch = useDispatch();
  const onBannerImage = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      if (e.target.files.length) {
        dispatch(setCourseBannerImage(e.target.files[0], () => setOpen(false)));
      } else {
        setOpen(false);
      }
    },
    [dispatch, setOpen]
  );

  return (
    <SubmenuItem
      className="course-submenu"
      disabled={
        !projectAccess.ViewObjectives &&
        !projectAccess.ViewGradebook &&
        !projectAccess.ViewTimeline &&
        !contentAccess.EditSettings
      }
      label="Course"
    >
      <DropdownItem
        tag={Link}
        to={objectivesLink}
        className={
          current === 'objectives' || !projectAccess.ViewObjectives ? 'disabled' : undefined
        }
        onClick={trackNarrativeEventHandler('Learning Objectives')}
      >
        Learning Objectives
      </DropdownItem>
      <DropdownItem
        tag={Link}
        to={gradebookLink}
        className={current === 'gradebook' || !projectAccess.ViewGradebook ? 'disabled' : undefined}
        onClick={trackNarrativeEventHandler('Course Gradebook')}
      >
        Course Gradebook
      </DropdownItem>
      <DropdownItem
        tag={Link}
        to={timelineLink}
        className={current === 'timeline' || !projectAccess.ViewTimeline ? 'disabled' : undefined}
        onClick={trackNarrativeEventHandler('Course Timeline')}
      >
        Course Timeline
      </DropdownItem>
      <DropdownItem
        tag={Link}
        to={defaultsLink}
        className={current === 'defaults' || !contentAccess.EditSettings ? 'disabled' : undefined}
        onClick={trackNarrativeEventHandler('Course Defaults')}
      >
        Course Defaults
      </DropdownItem>
      {editMode && contentAccess.EditSettings && (
        <>
          <DropdownItem divider />
          <DropdownItem
            onClick={() => {
              trackNarrativeEvent('Upload Course Banner');
              bannerRef.current?.click();
            }}
          >
            Upload Banner Image
            <input
              ref={bannerRef}
              id="upload-banner-image"
              type="file"
              accept="image/*"
              className="d-none"
              onChange={onBannerImage}
            />
          </DropdownItem>
          <DropdownItem
            onClick={() => {
              trackNarrativeEvent('Remove Course Banner');
              dispatch(beginProjectGraphEdit('Remove banner image'));
              dispatch(deleteProjectGraphEdge(existingImage));
              dispatch(autoSaveProjectGraphEdits());
            }}
            disabled={!existingImage}
          >
            Remove Banner Image
          </DropdownItem>
        </>
      )}
    </SubmenuItem>
  );
};
