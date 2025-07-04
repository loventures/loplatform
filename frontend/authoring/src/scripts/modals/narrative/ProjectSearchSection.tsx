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
import { useEffect, useState } from 'react';

import { ProjectFilter } from '../../projects/ProjectList/ProjectFilter';
import { ProjectResponse } from '../../story/NarrativeMultiverse';
import { toMultiWordRegex } from '../../story/questionUtil';
import { NewAsset, NodeName } from '../../types/asset';
import AggregateRow from './AggregateRow';
import ProjectContentSelector from './ProjectContentSelector';
import SearchBar from './SearchBar';
import { PreviewNode, StateSetter } from './types';

const ProjectSearchSection: React.FC<{
  parent: NewAsset<any>;
  projects: Array<ProjectResponse>;
  active: boolean;
  setActive: (active: boolean) => void;
  selectedProject: ProjectResponse | undefined;
  setSelectedProject: (project: ProjectResponse) => void;
  selected: Array<NodeName>;
  setSelected: StateSetter<Array<NodeName>>;
  preview: PreviewNode | undefined;
  setPreview: StateSetter<PreviewNode | undefined>;
  mode?: 'survey' | 'rubric';
}> = ({
  parent,
  projects,
  active,
  setActive,
  selectedProject,
  setSelectedProject,
  selected,
  setSelected,
  preview,
  setPreview,
  mode,
}) => {
  const [search, setSearch] = useState('');
  const [matches, setMatches] = useState<Array<ProjectResponse> | undefined>();

  const [projectStatuses, setProjectStatuses] = useState(() => new Set<string>());
  const [productTypes, setProductTypes] = useState(() => new Set<string>());
  const [categories, setCategories] = useState(() => new Set<string>());
  const [subCategories, setSubCategories] = useState(() => new Set<string>());

  const noChoice =
    !projectStatuses.size &&
    !productTypes.size &&
    !categories.size &&
    !subCategories.size &&
    !search;

  // If I become inactive, clear matches
  useEffect(() => {
    if (!active) setMatches(undefined);
  }, [active]);

  // If selections are cleared, make myself inactive
  useEffect(() => {
    if (noChoice) setActive(false);
  }, [noChoice]);

  const doSearch = () => {
    const regex = toMultiWordRegex(search);
    setMatches(
      noChoice
        ? undefined
        : [
            ...projects
              .filter(
                p =>
                  regex.test(
                    p.project.code ? `${p.project.code} ${p.project.name}` : p.project.name
                  ) &&
                  (!projectStatuses.size ||
                    projectStatuses.has(p.project.liveVersion?.toLowerCase())) &&
                  (!productTypes.size || productTypes.has(p.project.productType?.toLowerCase())) &&
                  (!categories.size || categories.has(p.project.category?.toLowerCase())) &&
                  (!subCategories.size || subCategories.has(p.project.subCategory?.toLowerCase()))
              )

              .sort((a, b) => a.project.name.localeCompare(b.project.name)),
          ]
    );
    setActive(!noChoice);
  };

  return (
    <div style={{ borderBottom: active ? undefined : '1px solid #dee2e6' }}>
      {selectedProject == null ? (
        <>
          <div className="p-4">
            {matches == null && <h5>Find a Project, then Browse for Content</h5>}
            <div className="d-flex mt-3 justify-content-center">
              <SearchBar
                className="search-for-projects"
                style={{ width: '75%' }}
                value={search}
                setValue={setSearch}
                onSearch={doSearch}
                placeholder="Search for projects..."
                disabled={noChoice}
                append={
                  <ProjectFilter
                    modalized
                    projects={projects}
                    projectStatuses={projectStatuses}
                    setProjectStatuses={setProjectStatuses}
                    productTypes={productTypes}
                    setProductTypes={setProductTypes}
                    categories={categories}
                    setCategories={setCategories}
                    subCategories={subCategories}
                    setSubCategories={setSubCategories}
                  />
                }
              />
            </div>
          </div>
          {matches == null ? null : !matches.length ? (
            <div className="text-center text-muted pt-4 pb-5">No projects matched your search.</div>
          ) : (
            <div className="mb-4">
              {matches.map(project => (
                <AggregateRow
                  key={project.project.id}
                  onClick={() => setSelectedProject(project)}
                  project={project}
                />
              ))}
            </div>
          )}
        </>
      ) : (
        <ProjectContentSelector
          parent={parent}
          selectedProject={selectedProject}
          selected={selected}
          setSelected={setSelected}
          preview={preview}
          setPreview={setPreview}
          mode={mode}
        />
      )}
    </div>
  );
};

export default ProjectSearchSection;
