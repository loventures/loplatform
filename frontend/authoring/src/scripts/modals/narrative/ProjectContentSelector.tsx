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
import { useEffect, useMemo, useState } from 'react';
import { IoSearchOutline } from 'react-icons/io5';
import { Input, InputGroup, InputGroupText, Spinner } from 'reactstrap';
import { useDebounce } from 'use-debounce';

import { questionTypeSet } from '../../asset/constants/questionTypes.constants';
import edgeRuleConstants from '../../editor/EdgeRuleConstants';
import { setToggle } from '../../gradebook/set';
import {
  ElementsOnly,
  getContentTree,
  getEditedAsset,
  getFilteredContentList,
  QuestionsAndElements,
  RubricAndQuestionsAndElements,
  SurveyAndElements,
  TreeAsset,
  TreeAssetWithParent,
  useGraphEditSelector,
} from '../../graphEdit';
import { noGraphEdits } from '../../graphEdit/graphEditReducer';
import { useDcmSelector } from '../../hooks';
import { ProjectResponse } from '../../story/NarrativeMultiverse';
import { toMultiWordRegex } from '../../story/questionUtil';
import { childEdgeGroup } from '../../story/story';
import {
  computeInOutEdgesByNode,
  loadStructure,
  useProjectGraph,
} from '../../structurePanel/projectGraphActions';
import { ProjectGraph } from '../../structurePanel/projectGraphReducer';
import { NewAsset, NodeName, TypeId } from '../../types/asset';
import AddContentRow from './AddContentRow';
import NarrativePreview from './NarrativePreview';
import { PreviewNode, StateSetter } from './types';

export type RemoteProjectGraph = ProjectGraph & { project: ProjectResponse };

export const loadRemoteProjectGraph = (
  project: ProjectResponse,
  commit?: number
): Promise<RemoteProjectGraph> =>
  loadStructure(project.branchId, project.project.homeNodeName, commit).then(structure => {
    const edgesByNode = computeInOutEdgesByNode(structure.edges, project.project.homeNodeName);
    return {
      ...structure,
      ...edgesByNode,
      loading: {},
      branchProjects: {}, // we don't care about this here
      customizedAssets: new Set(), // this neither
      project,
    };
  });

export const ProjectContentSelector: React.FC<{
  parent: NewAsset<any>;
  selectedProject: ProjectResponse;
  selected: Array<NodeName>;
  setSelected: (sel: Array<NodeName>) => void;
  preview: PreviewNode | undefined;
  setPreview: StateSetter<PreviewNode | undefined>;
  mode?: 'survey' | 'rubric';
}> = ({ parent, selectedProject, selected, setSelected, preview, setPreview, mode }) => {
  const { branchId, branchCommits } = useProjectGraph();
  const { role } = useDcmSelector(state => state.layout);
  const accessRights = useGraphEditSelector(state => state.contentTree.accessRights);
  const [search, setSearch] = useState('');
  const [regex] = useDebounce(toMultiWordRegex(search), 300);
  const surveys = mode === 'survey';
  const rubrics = mode === 'rubric';
  const group = surveys ? 'survey' : rubrics ? 'cblRubric' : childEdgeGroup(parent.typeId);
  const questions = !surveys && !!edgeRuleConstants[parent.typeId].questions;
  const targetTypes = useMemo(
    () => new Set(edgeRuleConstants[parent.typeId][group] as TypeId[]),
    [parent]
  );
  const [expanded, setExpanded] = useState(new Set<NodeName>());

  const [projectGraph, setProjectGraph] = useState<RemoteProjectGraph>();
  const prohibited = useMemo(
    () =>
      new Set(
        Object.entries(accessRights)
          .filter(([, permitted]) => role && !permitted.ViewContent)
          .map(([name]) => name)
      ),
    [accessRights]
  );

  const selectedSet = useMemo(() => new Set(selected), [selected]);

  useEffect(() => {
    setProjectGraph(undefined);
    setSearch('');
    // This is racy in that if I click one and then click another, the first one may respond
    // after the second.
    if (selectedProject)
      loadRemoteProjectGraph(selectedProject, branchCommits[selectedProject.branchId]).then(
        setProjectGraph
      );
  }, [selectedProject, branchCommits]);

  const previewAsset = useMemo(
    () =>
      preview && projectGraph
        ? getEditedAsset(preview.name, projectGraph, noGraphEdits)
        : undefined,
    [projectGraph, preview]
  );

  const groups = questions
    ? QuestionsAndElements
    : surveys
      ? SurveyAndElements
      : rubrics
        ? RubricAndQuestionsAndElements
        : ElementsOnly;

  const contentTree = useMemo(() => {
    if (!projectGraph) return undefined;
    const home = projectGraph.nodes[projectGraph.project.project.homeNodeName];
    return getContentTree(home, [], groups, projectGraph, noGraphEdits);
  }, [projectGraph, groups]);

  const contentList = useMemo(() => {
    if (!contentTree) return [];
    const predicate = (asset: TreeAsset) => {
      if (questions && !questionTypeSet.has(asset.typeId)) return false;
      if (!regex.test(asset.data.title)) return false;
      if (projectGraph.branchId === branchId && role && !accessRights[asset.name].ViewContent)
        return '.';
      // if (parent.typeId === 'course.1') return asset.typeId === 'module.1' ? '.' : false;
      return mode ? asset.typeId === 'survey.1' || asset.typeId === 'rubric.1' : true;
    };
    const contentList = getFilteredContentList(contentTree, predicate);
    return mode
      ? contentList
          .filter(
            c => c.typeId === 'survey.1' || c.typeId === 'rubric.1' || c.typeId === 'module.1'
          )
          .map(content => ({
            ...content,
            depth: Math.min(2, content.depth),
          }))
      : contentList;
  }, [contentTree, questions, regex, mode]);

  const setSelectedSet = (include: Set<NodeName>): void => {
    const selected = new Array<NodeName>();
    const loop = (node: TreeAsset) => {
      if (include.has(node.name)) {
        if (!selected.includes(node.name)) selected.push(node.name); // content reuse :disappointed:
      } else {
        for (const child of node.children) loop(child);
      }
    };
    for (const child of contentTree.children) loop(child);
    setSelected(selected);
  };

  const [last, setLast] = useState<TreeAssetWithParent | undefined>();

  const submit = (selection: Set<NodeName>) => void selection;

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
    <div className="content-list full-index non-a py-3 add-content-list">
      <div className="pt-2 pb-3 d-flex justify-content-center">
        <InputGroup className="search-bar">
          <Input
            type="search"
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Filter by title..."
            bsSize="sm"
            size={48}
          />
          <InputGroupText
            addonType="append"
            className="search-icon form-control form-control-sm flex-grow-0 d-flex align-items-center justify-content-center p-0 pe-1"
          >
            <IoSearchOutline aria-hidden />
          </InputGroupText>
        </InputGroup>
      </div>
      {!contentList.length ? (
        <div className="text-muted py-5 depth-1 text-center">
          {!projectGraph
            ? 'Loading...'
            : regex.ignoreCase
              ? 'No content matches your search.'
              : 'No contents.'}
        </div>
      ) : null}
      {contentList
        .filter(c => c.context.length < 2 || regex.ignoreCase || expanded.has(c.context[1].name))
        .map(content => (
          <AddContentRow
            key={content.edge?.name ?? '_root_'}
            content={content}
            targetTypes={targetTypes}
            prohibited={prohibited}
            regex={regex}
            selected={selectedSet}
            setSelected={setSelectedSet}
            onPreview={() => {
              const name = content.name;
              const title = content.data.title;
              setPreview({ name, title });
            }}
            last={last}
            setLast={setLast}
            submit={submit}
            collapsible={
              !regex.ignoreCase &&
              content.typeId === 'module.1' &&
              (branchId !== projectGraph.branchId ||
                !role ||
                accessRights[content.name].ViewContent)
            }
            collapsed={!regex.ignoreCase && !expanded.has(content.name)}
            multiple={!mode}
            showContext={!!mode}
            toggle={all =>
              setExpanded(
                !all
                  ? setToggle(expanded, content.name)
                  : expanded.has(content.name)
                    ? new Set()
                    : new Set(contentTree.children.map(a => a.name))
              )
            }
          />
        ))}
    </div>
  );
};

export default ProjectContentSelector;
