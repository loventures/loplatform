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
import React, { useState } from 'react';
import { AiOutlineMenu } from 'react-icons/ai';
import { GiSettingsKnobs } from 'react-icons/gi';
import { IoBookmarkOutline, IoSchoolOutline } from 'react-icons/io5';
import { VscHistory } from 'react-icons/vsc';
import { Link } from 'react-router-dom';
import {
  Badge,
  DropdownItem,
  DropdownMenu,
  DropdownToggle,
  Spinner,
  UncontrolledDropdown,
} from 'reactstrap';

import { trackAuthoringEvent } from '../../analytics';
import { useDcmSelector } from '../../hooks';
import { ProjectResponse } from '../../story/NarrativeMultiverse';
import { editorUrl } from '../../story/story';
import { useUserProfile } from '../../user/userActions';
import gretchen from '../../grfetchen/';
import { useDispatch } from 'react-redux';
import { openToast, TOAST_TYPES } from '../../toast/actions.ts';
import { getAllEditedOutEdges } from '../../graphEdit';
import { openModal } from '../../modals/modalActions.ts';
import { ModalIds } from '../../modals/modalIds.ts';
import { deleteProjectAction } from '../../story/pages/ProjectSettings/ProjectActionsMenu.tsx';
import { Thunk } from '../../types/dcmState';

export const contrastColor = {
  light: 'text-dark',
  warning: 'text-dark',
};

const toLocalDate = (value: string) => {
  const date = new Date(value);
  return date.getTime() + date.getTimezoneOffset() * 60 * 1000;
};

const formatDate = (date?: string): string => {
  if (!date) return '';
  return new Intl.DateTimeFormat('en', {
    month: 'long',
    day: 'numeric',
    year: 'numeric',
  }).format(new Date(date.includes('T') ? date : toLocalDate(date)));
};

const doDownload = url => {
  const a = document.createElement('a');
  a.target = '_blank';
  a.innerHTML = 'dl';
  a.href = url;
  a.onclick = event => document.body.removeChild(event.target as any);
  a.style.display = 'none';
  document.body.appendChild(a);
  a.click();
};

const exportProjectAction =
  (hit: ProjectResponse, andThen?: Thunk): Thunk =>
  dispatch => {
    gretchen
      .post('/api/v2/authoring/exports/async')
      .data({
        name: hit.project.name,
        branchId: hit.branchId,
        rootNodeNames: [hit.project.rootNodeName],
      })
      .exec()
      .then(data => {
        if (data.status === 'async') {
          dispatch(openToast('Export started', TOAST_TYPES.SUCCESS));
          return new Promise((resolve, reject) => {
            const channel = data.channel;
            const msgs = new EventSource(`/event${channel}`);
            msgs.addEventListener(channel, event => {
              const data = JSON.parse(event.data);
              if (data.status === 'ok') {
                msgs.close();
                resolve(data.body);
              } else {
                msgs.close();
                reject(data.body);
              }
            });
          });
        } else {
          throw 'Unexpected success.';
        }
      })
      .then(result => {
        if (result.receipts[0]?.status !== 'SUCCESS') throw 'Export failed.';
        dispatch(openToast('Export complete', TOAST_TYPES.SUCCESS));
        const id = result.receipts[0].id;
        doDownload(`/api/v2/authoring/exports/${id}/package`);
      })
      .catch(e => {
        console.log(e);
        dispatch(openToast('Export failed', TOAST_TYPES.DANGER));
      })
      .finally(() => andThen && dispatch(andThen));
  };

export const ProjectRow: React.FC<{
  hit: ProjectResponse;
  recent: boolean;
}> = ({ hit, recent }) => {
  const {
    branchId,
    project: {
      code,
      name,
      homeNodeName,
      archived,
      launchDate,
      productType,
      category,
      subCategory,
      liveVersion,
      ownedBy,
      maintenance,
    },
    branchCreated,
    headCreated,
    headCreatedByUser,
  } = hit;
  const dispatch = useDispatch();
  const [exporting, setExporting] = useState(false);

  const self = useUserProfile();
  const rights = useDcmSelector(state => state.user.rights);
  const userIsOwner = ownedBy === self?.id;

  const canEditSettings =
    userIsOwner || rights?.includes('loi.authoring.security.right$EditSettingsAnyProjectRight');

  const canDeleteProject =
    userIsOwner || rights?.includes('loi.authoring.security.right$DeleteAnyProjectRight');

  const canCreateProject = rights?.includes('loi.authoring.security.right$CreateProjectRight');

  const projectStatuses = useDcmSelector(state => state.configuration.projectStatuses ?? {});
  const projectStatusColor = useDcmSelector(state => state.configuration.projectStatusColor ?? {});

  const status = Object.entries(projectStatuses ?? {}).find(([, v]) => v === liveVersion)?.[0];
  const color = projectStatusColor?.[status] ?? 'light';

  return (
    <div
      className={classNames(
        'story-index-item project-row position-relative edit-mode d-flex',
        maintenance && 'maintenance'
      )}
    >
      <div className="d-flex align-items-stretch flex-grow-1 minw-0">
        <div className="flex-shrink-0 leader">
          <IoSchoolOutline
            className="icon"
            size="2rem"
          />
          {recent && (
            <IoBookmarkOutline
              className="most-recent"
              size=".85rem"
            />
          )}
        </div>
        <div className="flex-grow-1 minw-0 content">
          <div className="d-flex justify-content-between align-items-start">
            <Link
              to={maintenance ? '' : editorUrl('story', branchId, homeNodeName, undefined)}
              className="regular-block-link flex-shrink-1 minw-0 project-full-name"
              style={{ textIndent: '-1rem', marginLeft: '1rem' }}
            >
              {code && <span className="fw-bold me-2 project-code flex-shrink-0">{code}</span>}
              <span className="project-name">{name}</span>
            </Link>
            {exporting && (
              <Spinner
                size="sm"
                className="project-exporting"
              />
            )}
            {!maintenance && (
              <div className="d-flex flex-shrink-0 controls ms-2">
                {canEditSettings && (
                  <>
                    <Link
                      to={editorUrl('story', branchId, 'history', homeNodeName)}
                      title="History"
                      className="d-flex btn btn-outline-primary btn-sm p-2 border-0 history-btn"
                      onClick={() => trackAuthoringEvent('Narrative Projects - History')}
                    >
                      <VscHistory />
                    </Link>
                    <Link
                      to={editorUrl('story', branchId, 'settings', homeNodeName)}
                      title="Settings"
                      className="d-flex btn btn-outline-primary btn-sm p-2 border-0 settings-btn"
                      onClick={() => trackAuthoringEvent('Narrative Projects - Settings')}
                    >
                      <GiSettingsKnobs />
                    </Link>
                  </>
                )}
                {(canCreateProject || canEditSettings || canDeleteProject) && (
                  <UncontrolledDropdown>
                    <DropdownToggle
                      size="sm"
                      color="primary"
                      outline
                      className="p-2 border-0 d-flex"
                      style={{ zIndex: 99 }}
                    >
                      <AiOutlineMenu />
                    </DropdownToggle>
                    <DropdownMenu end>
                      <DropdownItem
                        onClick={() =>
                          window.postMessage({ type: 'cloneProject', project: hit })
                        }
                        disabled={!canCreateProject}
                      >
                        Clone Project
                      </DropdownItem>
                      <DropdownItem
                        onClick={() => {
                          setExporting(true);
                          dispatch(exportProjectAction(hit, () => setExporting(false)));
                        }}
                        disabled={exporting || !canEditSettings}
                      >
                        Export Project
                      </DropdownItem>
                      <DropdownItem
                        className={canDeleteProject ? "text-danger" : undefined}
                        onClick={() => dispatch(deleteProjectAction(hit.project))}
                        disabled={!canDeleteProject}
                      >
                        Delete Project
                      </DropdownItem>
                    </DropdownMenu>
                  </UncontrolledDropdown>
                )}
              </div>
            )}
          </div>
          <div className="d-flex gap-2 align-items-center">
            {liveVersion && (
              <Badge
                color={color}
                className={classNames(
                  'project-live-version metadata-badge fw-normal',
                  contrastColor[color]
                )}
              >
                {liveVersion}
              </Badge>
            )}
            {productType && (
              <Badge
                color="light"
                className="project-product-type metadata-badge fw-normal text-dark"
                title="Product Type"
              >
                {productType}
              </Badge>
            )}
            {category && (
              <Badge
                color="light"
                className="project-category metadata-badge fw-normal text-dark"
                title="Category"
              >
                {category}
              </Badge>
            )}
            {subCategory && (
              <Badge
                color="light"
                className="project-sub-category metadata-badge fw-normal text-dark"
                title="Subcategory"
              >
                {subCategory}
              </Badge>
            )}
            {launchDate && (
              <small
                className="d-flex align-items-center project-launch-date"
                style={{ lineHeight: 1 }}
              >
                {formatDate(launchDate)}
              </small>
            )}
            {archived && (
              <Badge
                color="warning"
                className="project-retired metadata-badge fw-normal text-dark"
              >
                Retired
              </Badge>
            )}
            {maintenance && (
              <Badge
                color="danger"
                className="project-maintenance metadata-badge fw-normal"
              >
                Maintenance
              </Badge>
            )}
          </div>
          <div className="text-muted small mt-1">
            Created on {formatDate(branchCreated)}. Lasted edited by{' '}
            {headCreatedByUser?.givenName ?? 'Unknown'} {headCreatedByUser?.familyName ?? 'Person'}{' '}
            on {formatDate(headCreated)}.
          </div>
        </div>
      </div>
    </div>
  );
};
