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

import gretchen from '../../grfetchen/';
import * as React from 'react';
import { useEffect, useMemo, useState } from 'react';
import { useCollapse } from 'react-collapsed';
import { Spinner } from 'reactstrap';

import edgeRules from '../../editor/EdgeRuleConstants';
import edgeRuleConstants from '../../editor/EdgeRuleConstants';
import { getEditedAsset } from '../../graphEdit';
import { noGraphEdits } from '../../graphEdit/graphEditReducer';
import { usePolyglot } from '../../hooks';
import { ProjectResponse } from '../../story/NarrativeMultiverse';
import { isQuestion } from '../../story/questionUtil';
import { childEdgeGroup, storyTypeName } from '../../story/story';
import { NewAsset, NodeName, TypeId } from '../../types/asset';
import AggregateRow from './AggregateRow';
import HitRow from './HitRow';
import { ModalSearchFilter } from './ModalSearchFilter';
import NarrativePreview from './NarrativePreview';
import { RemoteProjectGraph, loadRemoteProjectGraph } from './ProjectContentSelector';
import SearchBar from './SearchBar';
import { PreviewNode, ProjectWithHitCount, SearchWebHit, StateSetter } from './types';

const ContentSearchSection: React.FC<{
  parent: NewAsset<any>;
  projects: Array<ProjectResponse>;
  active: boolean;
  setActive: (active: boolean) => void;
  selectedProject: ProjectResponse | undefined;
  setSelectedProject: StateSetter<ProjectResponse | undefined>;
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
  const polyglot = usePolyglot();
  const [search, setSearch] = useState('');
  const [error, setError] = useState(false);
  const [searching, setSearching] = useState(false);
  const [aggregates, setAggregates] = useState<Array<ProjectWithHitCount> | undefined>();
  const [hits, setHits] = useState<Array<SearchWebHit>>();
  const group = childEdgeGroup(parent.typeId);
  const typeIds = edgeRules[parent.typeId]?.[group] ?? [];
  const [types, setTypes] = useState(new Set<TypeId>(typeIds));
  const [unused, setUnused] = useState(false);

  const [projectGraph, setProjectGraph] = useState<RemoteProjectGraph | undefined>();

  useEffect(() => {
    setProjectGraph(undefined);
    // This is racy in that if I click one and then click another, the first one may respond
    // after the second.
    if (selectedProject) loadRemoteProjectGraph(selectedProject).then(setProjectGraph);
  }, [selectedProject]);

  const previewAsset = useMemo(
    () =>
      preview && projectGraph
        ? getEditedAsset(preview.name, projectGraph, noGraphEdits)
        : undefined,
    [projectGraph, preview]
  );

  const allTypeIds = useMemo(() => {
    return mode === 'survey'
      ? ['survey.1']
      : mode === 'rubric'
        ? ['rubric.1']
        : edgeRuleConstants[parent.typeId][group];
  }, [parent, group]);

  // const contentTypePlaceholder = group === 'elements' ? 'Any content' : 'Any question';

  const { getCollapseProps } = useCollapse({
    defaultExpanded: false,
    isExpanded: aggregates != null,
  });

  const doSearch = () => {
    if (!search.trim()) return;
    const webQuery = JSON.stringify({
      query: search,
      typeIds: Array.from(types),
      unusedAssets: unused,
      aggregate: 'project',
    });
    setActive(true);
    setSelectedProject(undefined);
    setSelected([]);
    setSearching(true);
    setError(false);
    gretchen
      .get(`/api/v2/authoring/search/aggregates?query=${encodeURIComponent(webQuery)}`)
      .exec()
      .then((hits: Record<number, number>) => {
        const results = projects
          .filter(project => hits[project.project.id])
          .map(project => ({
            ...project,
            hits: hits[project.project.id],
          }))
          .sort((p1, p2) => p2.hits - p1.hits);
        setAggregates(results);
      })
      .catch(e => {
        console.log(e);
        setError(true);
      })
      .finally(() => setSearching(false));
  };

  // If the search is cleared, set myself inactive
  useEffect(() => {
    if (!search.trim()) setActive(false);
  }, [search, selectedProject]);

  useEffect(() => {
    if (!selectedProject) setHits(undefined);
    if (!active) setAggregates(undefined);
    setSelected([]);
    setError(false);
  }, [selectedProject, active]);

  useEffect(() => {
    if (!selectedProject || !search) return;
    const webQuery = JSON.stringify({
      query: search,
      typeIds: Array.from(types),
      unusedAssets: unused,
      offset: 0,
      limit: 100,
    });
    setSearching(true);
    setError(false);
    gretchen
      .get(
        `/api/v2/authoring/search/branch/${selectedProject.branchId}?query=${encodeURIComponent(
          webQuery
        )}`
      )
      .exec()
      .then(({ objects }: { totalCount: number; objects: SearchWebHit[] }) => {
        setHits(objects);
      })
      .catch(e => {
        console.log(e);
        setError(true);
      })
      .finally(() => setSearching(false));
  }, [selectedProject]);

  return preview ? (
    <div className="story-preview">
      {previewAsset ? (
        <NarrativePreview
          asset={previewAsset}
          projectGraph={projectGraph}
        />
      ) : (
        <Spinner className="text-muted" />
      )}
    </div>
  ) : (
    <div className="d-flex flex-column">
      <div className="p-4 d-flex flex-column align-items-stretch">
        {aggregates == null && <h5>Free Text Search for Content in All Projects</h5>}
        <div className="d-flex gap-3 pt-2 justify-content-center">
          <SearchBar
            style={{ width: '75%' }}
            value={search}
            setValue={setSearch}
            onSearch={doSearch}
            className="search-all-projects"
            placeholder="Search in all projects..."
            disabled={!search || !types.size}
            append={
              <ModalSearchFilter
                typeIds={allTypeIds}
                types={types}
                setTypes={setTypes}
                unused={unused}
                setUnused={setUnused}
              />
            }
          />
        </div>
      </div>
      <div {...getCollapseProps()}>
        {error ? (
          <div className="text-center text-danger py-5">A search error occurred.</div>
        ) : searching ? (
          <div className="d-flex justify-content-center py-5">
            <Spinner className="text-muted" />
          </div>
        ) : hits ? (
          <div className="mb-4">
            {hits.map(hit => (
              <HitRow
                key={hit.path[0].name}
                hit={hit}
                isSelected={selected.includes(hit.path[0].name)}
                onClick={() =>
                  setSelected(selected.includes(hit.path[0].name) ? [] : [hit.path[0].name])
                }
                onPreview={() => {
                  const name = hit.path[0].name;
                  const title = isQuestion(hit.path[0].typeId)
                    ? storyTypeName(polyglot, hit.path[0].typeId)
                    : hit.path[0].title;
                  setPreview({ name, title });
                }}
              />
            ))}
          </div>
        ) : aggregates == null ? (
          <div />
        ) : !aggregates.length ? (
          <div className="text-center text-muted pt-4 pb-5">No content matched your search.</div>
        ) : (
          <div className="mb-4">
            {aggregates.map(project => (
              <AggregateRow
                key={project.project.id}
                onClick={() => setSelectedProject(project)}
                project={project}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default ContentSearchSection;
