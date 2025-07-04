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
import { GiNinjaVelociraptor, GiQuillInk } from 'react-icons/gi';
import { IoClose } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import { useRouteMatch } from 'react-router';
import { Link } from 'react-router-dom';
import {
  Button,
  DropdownItem,
  DropdownMenu,
  DropdownToggle,
  UncontrolledDropdown,
} from 'reactstrap';

import { FeedbackDto } from '../../feedback/FeedbackApi';
import { useCurrentFeedback } from '../../feedback/feedbackHooks';
import { useCurrentAssetName, useIsAdded } from '../../graphEdit';
import { useBranchId, useDcmSelector, useRouterQueryParam } from '../../hooks';
import { Feedback, Vault } from '../../projectNav/NavIcons';
import { dropboxPath, feedbackPath, storyPath } from '../../router/routes';
import { useProjectAccess } from '../hooks';
import { toggleOmegaEdit } from '../storyActions';
import { useStorySelector } from '../storyHooks';

export const feedbackAssetPath = (homeNodeName: string, feedback: FeedbackDto) => {
  const context = [
    homeNodeName,
    feedback?.moduleName,
    feedback?.lessonName,
    feedback?.contentName,
  ].filter(name => name && name !== feedback.assetName);
  return `${feedback.assetName}?contextPath=${context.join('.')}`;
};

export const QuillMenu: React.FC = () => {
  const dispatch = useDispatch();
  const branchId = useBranchId();
  const projectAccess = useProjectAccess();
  const homeNodeName = useDcmSelector(state => state.layout.project.homeNodeName);
  const name = useCurrentAssetName();
  const added = useIsAdded(name); // can't view in structural editor
  const feedback = useCurrentFeedback();
  const contextPath = useRouterQueryParam('contextPath');
  const omegaEdit = useStorySelector(s => s.omegaEdit);

  // bounce among editors of the current asset
  const assetPath = feedback
    ? feedbackAssetPath(homeNodeName, feedback)
    : name !== homeNodeName && !added
      ? `${name}?contextPath=${contextPath}`
      : homeNodeName;

  const storyUrl = `/branch/${branchId}/story/${assetPath}`;
  const storyMatch = useRouteMatch(storyPath);

  const feedbackUrl = `/branch/${branchId}/feedback`;
  const feedbackMatch = useRouteMatch(feedbackPath);

  const dropboxUrl = `/branch/${branchId}/dropbox`;
  const dropboxMatch = useRouteMatch(dropboxPath);

  const anyExtraAccess = projectAccess.FeedbackApp || projectAccess.VaultApp;

  return anyExtraAccess ? (
    omegaEdit ? (
      <Button
        id="ninja-raptor-mode"
        color="danger"
        className="d-flex align-items-center quillicon ninja-raptor position-relative"
        style={{
          marginLeft: '-1rem',
          paddingLeft: '.85rem',
          paddingRight: '.75rem',
          marginRight: '.75rem',
          borderRadius: 0,
          height: '3rem',
        }}
        onClick={() => dispatch(toggleOmegaEdit())}
        title="Exit Ninja Raptor Mode"
      >
        <GiNinjaVelociraptor size="1.5rem" />
        <IoClose
          size=".85rem"
          className="close-icon"
          style={{ position: 'absolute', right: '.15rem', bottom: '.15rem' }}
        />
      </Button>
    ) : (
      <UncontrolledDropdown id="quill-menu">
        <DropdownToggle
          caret
          className={classNames(
            'd-flex align-items-center quillicon',

            storyMatch
              ? 'is-story'
              : feedbackMatch
                ? 'is-feedback'
                : dropboxMatch
                  ? 'is-dropbox'
                  : null
          )}
          style={{
            marginLeft: '-1rem',
            paddingLeft: '.85rem',
            paddingRight: '.75rem',
            marginRight: '.75rem',
            borderRadius: 0,
            height: '3rem',
          }}
          color="dark"
          outline
        >
          {feedbackMatch ? (
            <Feedback size="1.5rem" />
          ) : dropboxMatch ? (
            <Vault size="1.5rem" />
          ) : (
            <GiQuillInk size="1.5rem" />
          )}
        </DropdownToggle>
        <DropdownMenu>
          <DropdownItem
            tag={Link}
            to={storyUrl}
            className={storyMatch ? 'disabled' : undefined}
          >
            Narrative Editor
          </DropdownItem>
          {projectAccess.FeedbackApp && (
            <DropdownItem
              tag={Link}
              to={feedbackUrl}
              className={feedbackMatch ? 'disabled' : undefined}
            >
              Content Feedback
            </DropdownItem>
          )}
          {projectAccess.VaultApp && (
            <DropdownItem
              tag={Link}
              to={dropboxUrl}
              className={dropboxMatch ? 'disabled' : undefined}
            >
              Storage Vault
            </DropdownItem>
          )}
        </DropdownMenu>
      </UncontrolledDropdown>
    )
  ) : (
    <div
      id="quill-icon"
      className=" bg-dark d-flex align-items-center quillicon"
      style={{
        marginLeft: '-1rem',
        paddingLeft: '.85rem',
        paddingRight: '.5rem',
        marginRight: '.75rem',
        borderRadius: 0,
        height: '3rem',
        color: '#f8f9fa',
      }}
    >
      <GiQuillInk size="1.5rem" />
    </div>
  );
};
