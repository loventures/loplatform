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
import React, { useEffect, useState } from 'react';
import { IoHomeOutline, IoSearchOutline } from 'react-icons/io5';
import { Link } from 'react-router-dom';
import { Button, Modal, ModalHeader } from 'reactstrap';

import { trackAuthoringEvent } from '../analytics';
import { useDcmSelector, useDocumentTitle } from '../hooks';
import { ProjectResponse } from '../story/NarrativeMultiverse';
import { ProjectSettingsForm } from '../story/pages/ProjectSettings/ProjectSettingsForm';
import { GiOrbital0, GiOrbital1, GiOrbital2, LightningArc, LightningRods } from './icons';

export const ProjectsActionBar: React.FC<{ label?: string }> = ({ label }) => {
  useDocumentTitle(label ?? 'Projects');
  const rights = useDcmSelector(state => state.user.rights);
  const canSearch = rights?.includes('loi.authoring.security.right$ViewAllProjectsRight');
  const canCreateProject = rights?.includes('loi.authoring.security.right$CreateProjectRight');
  const [creating, setCreating] = useState(false);
  const [copy, setCopy] = useState<ProjectResponse>();
  useEffect(() => {
    const listener = (ev: MessageEvent) => {
      if (ev.data.type === 'cloneProject') {
        setCopy(ev.data.project);
        setCreating(true);
      }
    };
    window.addEventListener('message', listener, false);
    return () => window.removeEventListener('message', listener, false);
  }, []);
  return (
    <div className="narrative-action-bar d-flex align-items-center justify-content-between h-100 px-3">
      <h6 className="m-0 text-nowrap flex-shrink-1 me-3 d-flex align-items-center justify-content-end minw-0">
        <div
          className="bg-dark d-flex align-items-center justify-content-center quillicon orbitals"
          style={{
            marginLeft: '-.5rem',
            width: '3rem',
            marginRight: '.5rem',
            borderRadius: 0,
            height: '3rem',
            color: '#f8f9fa',
          }}
        >
          <GiOrbital0 size="2rem" />
          <GiOrbital1 size="2rem" />
          <GiOrbital2 size="2rem" />
        </div>
        {label ? (
          <>
            <Link
              to="/"
              className="projects-crumb"
            >
              Projects
            </Link>
            <span className="text-muted ms-2 me-2">/</span>
            <span className="final-crumb">{label}</span>
          </>
        ) : (
          <span className="final-crumb">Projects</span>
        )}
      </h6>
      <div className="d-flex align-items-center gap-1">
        <Link
          to="/"
          title="Projects"
          id="projects-button"
          className={classnames('d-flex p-1 btn btn-transparent br-50', !label && 'disabled')}
          style={{ opacity: 1 }}
          onClick={() => trackAuthoringEvent('Narrative Projects - Projects')}
        >
          <IoHomeOutline size="1.2rem" />
        </Link>
        {canSearch && (
          <Link
            to="/search"
            title="Content Search"
            id="search-button"
            className={classnames('d-flex p-1 btn btn-transparent br-50', label && 'disabled')}
            style={{ opacity: 1 }}
            onClick={() => trackAuthoringEvent('Narrative Projects - Search')}
          >
            <IoSearchOutline size="1.2rem" />
          </Link>
        )}
        {canCreateProject && (
          <Button
            size="sm"
            color="primary"
            id="new-project"
            className="ms-2"
            onClick={() => {
              setCopy(undefined);
              setCreating(true);
            }}
            disabled={!!label}
          >
            New Project
          </Button>
        )}
      </div>
      <Modal
        id="new-project-modal"
        size="lg"
        isOpen={creating}
        toggle={() => setCreating(false)}
      >
        <ModalHeader>
          <div className="d-flex gap-2">
            <div className="d-flex align-items-center justify-content-center text-primary me-1">
              <LightningRods />
              <LightningArc className="flickering" />
            </div>
            {copy ? 'Clone Project' : 'New Project'}
          </div>
        </ModalHeader>
        <ProjectSettingsForm
          create
          copy={copy}
          dirty={false}
          setDirty={() => void 0}
          doCancel={() => setCreating(false)}
        />
      </Modal>
    </div>
  );
};
