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

import * as React from 'react';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useCollapse } from 'react-collapsed';
import { IoArrowBack } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import {
  Button,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  Spinner,
  UncontrolledTooltip,
} from 'reactstrap';

import { reloadAssetEditor } from '../editor/assetEditorActions';
import { useAllEditedOutEdges } from '../graphEdit';
import { useModal, usePolyglot } from '../hooks';
import { NoProjects, useProjects } from '../story/dataActions';
import { useProjectAccess } from '../story/hooks';
import { addDependencies, ProjectResponse } from '../story/NarrativeMultiverse';
import { childEdgeGroup } from '../story/story';
import { useProjectGraph } from '../structurePanel/projectGraphActions';
import { openToast } from '../toast/actions';
import { NewAsset, NodeName } from '../types/asset';
import ContentSearchSection from './narrative/ContentSearchSection';
import ProjectSearchSection from './narrative/ProjectSearchSection';
import RelatedProjectsSection from './narrative/RelatedProjectsSection';
import { PreviewNode } from './narrative/types';

export type FindContentModalData = {
  mode?: 'survey' | 'rubric';
  parent: NewAsset<any>;
  clone: (branch: ProjectResponse, names: NodeName[]) => void | Promise<any>;
  link: (branch: ProjectResponse, names: NodeName[]) => void | Promise<any>;
};

const intersects = <A,>(a0: Set<A>, a1: Set<A>): boolean => {
  if (a0.size <= a1.size) {
    for (const a of a0) if (a1.has(a)) return true;
  } else {
    for (const a of a1) if (a0.has(a)) return true;
  }
  return false;
};

const FindContentModal = () => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const {
    modalOpen,
    toggleModal,
    data: { parent, clone, link, mode },
  } = useModal<FindContentModalData>();
  const [submitting, setSubmitting] = useState(false);
  const [selectedProject, setSelectedProject] = useState<ProjectResponse | undefined>();
  const [selected, setSelected] = useState(new Array<NodeName>());
  const [active, setActive] = useState<'search' | 'projects' | 'related' | undefined>();
  const [preview, setPreview] = useState<PreviewNode | undefined>();
  const [confirmLink, setConfirmLink] = useState(false);
  const body = useRef<HTMLDivElement>();
  const projectAccess = useProjectAccess();

  const { branchId, branchCommits } = useProjectGraph();
  const group = childEdgeGroup(parent.typeId);
  const allEdges = useAllEditedOutEdges(parent.name);
  const prohibited = useMemo(
    () =>
      new Set([
        ...allEdges.filter(edge => edge.group === group).map(edge => edge.targetName),
        parent.name,
      ]),
    [parent, allEdges, group]
  );

  const projects = useProjects();

  const { getCollapseProps: searchCollapse } = useCollapse({
    defaultExpanded: false,
    isExpanded: !active || active === 'search',
  });

  const { getCollapseProps: projectsCollapse } = useCollapse({
    defaultExpanded: false,
    isExpanded: !active || active === 'projects',
  });

  const { getCollapseProps: relatedCollapse } = useCollapse({
    defaultExpanded: false,
    isExpanded: !active || active === 'related',
  });

  useEffect(() => {
    if (!active) setSelectedProject(undefined);
  }, [active]);

  useEffect(() => {
    body.current?.scrollTo(0, 0);
  }, [active, selectedProject, preview]);

  const selection = useMemo(() => (preview ? [preview.name] : selected), [preview, selected]);

  const canClone = !!selection.length;
  const [canLink, linkReason] = useMemo<[boolean] | [boolean, string]>(() => {
    if (!selection.length || !selectedProject) return [false];
    if (intersects(prohibited, new Set(selection)))
      return [false, 'This content is already linked here.'];
    return [true];
  }, [selection, selectedProject, branchId, prohibited]);

  const doClone = () => {
    if (canClone) {
      setSubmitting(true);
      Promise.resolve(clone(selectedProject, selection))
        .then(() => {
          dispatch(openToast('Content cloned successfully.', 'success'));
          toggleModal();
        })
        .catch(e => {
          console.log(e);
          dispatch(openToast('A cloning error occurred.', 'danger'));
          throw e;
        })
        .finally(() => setSubmitting(false));
    }
  };

  const doLink = () => {
    if (canLink) {
      const needsLink =
        !branchCommits[selectedProject.branchId] && branchId !== selectedProject.branchId;
      if (!confirmLink && needsLink) {
        setConfirmLink(true);
        return;
      }
      setSubmitting(true);

      let maybeLink: Promise<any>;
      if (!needsLink) {
        maybeLink = Promise.resolve();
      } else {
        maybeLink = addDependencies(branchId, [selectedProject.branchId]);
      }

      maybeLink
        .then(() => link(selectedProject, selection))
        .then(() => {
          if (needsLink) dispatch(reloadAssetEditor());
          dispatch(openToast('Content linked successfully.', 'success'));
          toggleModal();
        })
        .catch(e => {
          console.log(e);
          dispatch(openToast('A linking error occurred.', 'danger'));
          throw e;
        })
        .finally(() => setSubmitting(false));
    }
  };

  return (
    <>
      <Modal
        id="find-content-modal"
        isOpen={modalOpen}
        toggle={toggleModal}
        size="xl"
        className="no-exit-edit narrative-editor not-100"
      >
        <div className="modal-header align-items-stretch minw-0 justify-content-start">
          {preview ? (
            <>
              <Button
                key="exit-preview"
                color="secondary"
                className="d-flex align-items-center me-3 px-2"
                onClick={() => {
                  setPreview(undefined);
                }}
              >
                <IoArrowBack />
              </Button>
              <h5 className="modal-title text-truncate">{preview.title}</h5>
            </>
          ) : selectedProject ? (
            <>
              <Button
                key="exit-project"
                color="secondary"
                className="d-flex align-items-center me-3 px-2"
                onClick={() => {
                  setSelectedProject(undefined);
                  setSelected([]);
                  if (active === 'related') setActive(undefined);
                }}
              >
                <IoArrowBack />
              </Button>
              <h5 className="text-truncate modal-title">{selectedProject.project.name}</h5>
            </>
          ) : active ? (
            <>
              <Button
                key="exit-section"
                color="secondary"
                className="d-flex align-items-center me-3 px-2"
                onClick={() => {
                  setSelected([]);
                  setActive(undefined);
                }}
              >
                <IoArrowBack />
              </Button>
              <h5 className="mb-0 text-truncate modal-title">
                {polyglot.t(`FIND_CONTENT_MODAL_TITLE_${active?.toUpperCase() ?? 'DEFAULT'}`)}
              </h5>
            </>
          ) : (
            <>
              <h5 className="modal-title">{polyglot.t(`FIND_CONTENT_MODAL_TITLE`)}</h5>
              {projects === NoProjects && (
                <div className="ms-auto me-2">
                  <Spinner
                    size="sm"
                    color="muted"
                  />
                </div>
              )}
            </>
          )}
        </div>

        <ModalBody className="p-0">
          <div
            ref={body}
            className="content-list full-index non-a add-content-list"
            style={{ overflowY: 'scroll', maxHeight: 'calc(100vh - 13rem)' }}
          >
            <div {...relatedCollapse()}>
              <RelatedProjectsSection
                parent={parent}
                projects={projects}
                selectedProject={active === 'related' ? selectedProject : undefined}
                setSelectedProject={p => {
                  setActive('related');
                  setSelectedProject(p);
                }}
                selected={selected}
                setSelected={setSelected}
                preview={preview}
                setPreview={setPreview}
                mode={mode}
              />
            </div>
            {projectAccess.AddExternal && (
              <div {...projectsCollapse()}>
                <ProjectSearchSection
                  parent={parent}
                  projects={projects}
                  active={active === 'projects'}
                  setActive={a => setActive(a ? 'projects' : undefined)}
                  selectedProject={active === 'projects' ? selectedProject : undefined}
                  setSelectedProject={setSelectedProject}
                  selected={selected}
                  setSelected={setSelected}
                  preview={preview}
                  setPreview={setPreview}
                  mode={mode}
                />
              </div>
            )}
            {projectAccess.AddExternal && (
              <div {...searchCollapse()}>
                <ContentSearchSection
                  parent={parent}
                  projects={projects}
                  active={active === 'search'}
                  setActive={a => setActive(a ? 'search' : undefined)}
                  selectedProject={active === 'search' ? selectedProject : undefined}
                  setSelectedProject={setSelectedProject}
                  selected={selected}
                  setSelected={setSelected}
                  preview={preview}
                  setPreview={setPreview}
                  mode={mode}
                />
              </div>
            )}
          </div>
        </ModalBody>
        <ModalFooter>
          <Button
            id="modal-cancel"
            color="secondary"
            onClick={toggleModal}
            disabled={submitting}
          >
            {polyglot.t('CANCEL')}
          </Button>
          {!mode && (
            <div id="modal-link-wrapper">
              <Button
                id="modal-link"
                color="primary"
                onClick={doLink}
                disabled={submitting || !canLink}
                style={linkReason ? { pointerEvents: 'none' } : undefined}
              >
                {polyglot.t(`FIND_CONTENT_MODAL_LINK`)}
              </Button>
              {linkReason && (
                <UncontrolledTooltip
                  target="modal-link-wrapper"
                  placement="top"
                >
                  {linkReason}
                </UncontrolledTooltip>
              )}
            </div>
          )}
          <Button
            id="modal-clone"
            color="primary"
            onClick={doClone}
            disabled={submitting || !canClone}
          >
            {polyglot.t(`FIND_CONTENT_MODAL_CLONE`)}
          </Button>
        </ModalFooter>
      </Modal>
      <Modal
        id="link-project-modal"
        isOpen={confirmLink}
        size="md"
      >
        <ModalHeader>Add Multiverse Link?</ModalHeader>
        <ModalBody>
          Are you sure you want to add a multiversal link to {selectedProject?.project.name}?
        </ModalBody>
        <ModalFooter>
          <Button
            id="modal-link-cancel"
            color="secondary"
            onClick={() => setConfirmLink(false)}
          >
            {polyglot.t('CANCEL')}
          </Button>
          <Button
            id="modal-link-confirm"
            color="primary"
            onClick={doLink}
            disabled={submitting}
          >
            {polyglot.t(`FIND_CONTENT_MODAL_LINK`)}
          </Button>
        </ModalFooter>
      </Modal>
    </>
  );
};

export default FindContentModal;
