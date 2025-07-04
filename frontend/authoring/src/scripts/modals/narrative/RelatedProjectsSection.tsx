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
import { useMemo } from 'react';

import { useDcmSelector } from '../../hooks';
import { useProjectAccess } from '../../story/hooks';
import { ProjectResponse } from '../../story/NarrativeMultiverse';
import { useProjectGraph } from '../../structurePanel/projectGraphActions';
import { NewAsset, NodeName } from '../../types/asset';
import ProjectContentSelector from './ProjectContentSelector';
import ProjectRow from './ProjectRow';
import { PreviewNode, StateSetter } from './types';

const RelatedProjectsSection: React.FC<{
  parent: NewAsset<any>;
  projects: Array<ProjectResponse>;
  selectedProject: ProjectResponse | undefined;
  setSelectedProject: StateSetter<ProjectResponse>;
  selected: Array<NodeName>;
  setSelected: StateSetter<Array<NodeName>>;
  preview: PreviewNode | undefined;
  setPreview: StateSetter<PreviewNode | undefined>;
  mode?: 'survey' | 'rubric';
}> = ({
  parent,
  projects,
  selectedProject,
  setSelectedProject,
  selected,
  setSelected,
  preview,
  setPreview,
  mode,
}) => {
  const layout = useDcmSelector(state => state.layout);
  const thisProject = useMemo<ProjectResponse | undefined>(
    () =>
      layout.project
        ? {
            project: { ...layout.project, name: 'This Project' },
            branchId: parseInt(layout.branchId!),
            branchName: layout.branchName!,
            headId: 0,
          }
        : undefined,
    [layout]
  );
  const { branchCommits } = useProjectGraph();

  const linked = useMemo(
    () => projects.filter(p => branchCommits[p.branchId]),
    [projects, branchCommits]
  );
  const projectAccess = useProjectAccess();

  return (
    <div
      className="d-flex flex-column align-items-stretch"
      style={
        selectedProject != null || !projectAccess.AddExternal
          ? undefined
          : { borderBottom: '1px solid #dee2e6' }
      }
    >
      {selectedProject == null ? (
        <div className="pt-3">
          {thisProject && (
            <ProjectRow
              onClick={() => setSelectedProject(thisProject)}
              project={thisProject}
            />
          )}
          {linked.map(project => (
            <ProjectRow
              key={project.project.id}
              onClick={() => setSelectedProject(project)}
              project={project}
            />
          ))}
        </div>
      ) : (
        <ProjectContentSelector
          parent={parent}
          selectedProject={selectedProject}
          selected={selected}
          setSelected={setSelected}
          mode={mode}
          preview={preview}
          setPreview={setPreview}
        />
      )}
    </div>
  );
};

export default RelatedProjectsSection;
