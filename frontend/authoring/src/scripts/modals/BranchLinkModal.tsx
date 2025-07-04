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

import { uniq } from 'lodash';
import * as React from 'react';
import { useEffect, useMemo, useState } from 'react';
import { useSelector } from 'react-redux';
import Select from 'react-select';
import { Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import { useModal, usePolyglot } from '../hooks';
import { Project } from '../layout/dcmLayoutReducer';
import { useProjects } from '../story/dataActions';
import { ProjectResponse } from '../story/NarrativeMultiverse';
import { toMultiWordRegex } from '../story/questionUtil';
import { useProjectGraph } from '../structurePanel/projectGraphActions';
import { DcmState } from '../types/dcmState';

export interface SelectOption {
  value: string;
  label: string;
}

export type BranchLinkModalData = {
  callback: (branch: ProjectResponse) => void | Promise<any>;
};

const isString = (t: any): t is string => typeof t === 'string';

export const projectFieldOptions = (
  projects: ProjectResponse[],
  field: keyof Project
): SelectOption[] =>
  uniq(projects.map(p => p.project[field]).filter(isString)).map(t => ({
    value: t,
    label: t,
  }));

export const filterProjects = (
  projects: ProjectResponse[],
  field: keyof Project,
  options: SelectOption[]
): ProjectResponse[] =>
  !options.length
    ? projects
    : projects.filter(p => options.some(o => o.value === p.project[field]));

const BranchLinkModal = () => {
  const polyglot = usePolyglot();
  const { callback } = useSelector((state: DcmState) => state.modal.data as BranchLinkModalData);
  const { modalOpen, toggleModal } = useModal();
  const { branchId, branchCommits } = useProjectGraph();
  const [submitting, setSubmitting] = useState(false);

  const [selectedProject, setSelectedProject] = useState<SelectOption | null>(null);

  const allProjects = useProjects();
  const projects = useMemo(
    () => allProjects.filter(p => p.branchId !== branchId && !branchCommits[p.branchId]),
    [allProjects, branchId, branchCommits]
  );

  const productTypeOptions = useMemo(
    () => projectFieldOptions(projects, 'productType'),
    [projects]
  );
  const [productTypes, setProductTypes] = useState(new Array<SelectOption>());
  const projects1 = useMemo(
    () => filterProjects(projects, 'productType', productTypes),
    [projects, productTypes]
  );

  const categoryOptions = useMemo(() => projectFieldOptions(projects1, 'category'), [projects1]);
  const [categories, setCategories] = useState(new Array<SelectOption>());
  const projects2 = useMemo(
    () => filterProjects(projects1, 'category', categories),
    [projects1, categories]
  );

  const subCategoryOptions = useMemo(
    () => projectFieldOptions(projects2, 'subCategory'),
    [projects2]
  );
  const [subCategories, setSubCategories] = useState(new Array<SelectOption>());
  const projects3 = useMemo(
    () => filterProjects(projects2, 'subCategory', subCategories),
    [projects2, subCategories]
  );

  const projectOptions = useMemo(
    () => [
      ...projects3
        .map(p => ({
          value: `${p.branchId}`,
          label: p.project.name,
        }))
        .sort((a, b) => a.label.localeCompare(b.label)),
    ],
    [projects3]
  );

  useEffect(() => {
    if (selectedProject && !projects3.some(p => selectedProject.value === `${p.branchId}`))
      setSelectedProject(null);
  }, [projects3, selectedProject]);

  const submit = () => {
    const project = projects.find(p => selectedProject?.value === `${p.branchId}`);
    if (!project) return;
    setSubmitting(true);
    Promise.resolve(callback(project))
      .then(() => toggleModal())
      .finally(() => setSubmitting(false));
  };

  return (
    <Modal
      id="branch-link-modal"
      isOpen={modalOpen}
      toggle={toggleModal}
      size="lg"
      className="narrative-editor"
    >
      <ModalHeader>{polyglot.t('BRANCH_LINK_MODAL_TITLE')}</ModalHeader>

      <ModalBody className="d-flex flex-column gap-2">
        <Select
          isClearable={true}
          isMulti
          placeholder="Product Type"
          isLoading={!projects.length}
          options={productTypeOptions}
          onChange={a => setProductTypes([...a])}
          value={productTypes}
        />
        <Select
          isClearable={true}
          isMulti
          placeholder="Category"
          isLoading={!projects.length}
          options={categoryOptions}
          onChange={a => setCategories([...a])}
          value={categories}
        />
        <Select
          isClearable={true}
          isMulti
          placeholder="Sub-Category"
          isLoading={!projects.length}
          options={subCategoryOptions}
          onChange={a => setSubCategories([...a])}
          value={subCategories}
        />
        <Select
          isClearable={false}
          placeholder="Select project..."
          isLoading={!projects.length}
          options={projectOptions}
          onChange={setSelectedProject}
          value={selectedProject}
          filterOption={(option, filter) => toMultiWordRegex(filter).test(option.label)}
        />
      </ModalBody>
      <ModalFooter>
        <button
          id="modal-cancel"
          className="btn btn-secondary"
          onClick={toggleModal}
          disabled={submitting}
        >
          {polyglot.t('CANCEL')}
        </button>
        <button
          id="modal-submit"
          className="btn btn-primary"
          onClick={() => submit()}
          disabled={submitting || !selectedProject}
        >
          {polyglot.t('BRANCH_LINK_MODAL_SUBMIT')}
        </button>
      </ModalFooter>
    </Modal>
  );
};

export default BranchLinkModal;
