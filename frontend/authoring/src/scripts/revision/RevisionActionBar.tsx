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
import { groupBy, isEqual } from 'lodash';
import React, { useEffect, useMemo, useState } from 'react';
import { AiOutlineArrowLeft, AiOutlineArrowRight } from 'react-icons/ai';
import { BsClock } from 'react-icons/bs';
import { useDispatch } from 'react-redux';
import { useHistory } from 'react-router';
import { Link } from 'react-router-dom';
import { Button } from 'reactstrap';

import { trackAuthoringEvent } from '../analytics';
import { trackNarrativeNav, trackNarrativeNavHandler } from '../analytics/AnalyticsEvents';
import edgeRuleConstants from '../editor/EdgeRuleConstants';
import {
  addProjectGraphEdge,
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  deleteProjectGraphEdge,
  editProjectGraphNodeData,
  restoreProjectGraphNode,
  setProjectGraphEdgeOrder,
  useCurrentContextPath,
  useEditedCurrentAsset,
} from '../graphEdit';
import { useBranchId, useDocumentTitle, usePolyglot } from '../hooks';
import { useContentAccess } from '../story/hooks/useContentAccess';
import { QuillMenu } from '../story/NarrativeActionBar/QuillMenu';
import { isQuestion } from '../story/questionUtil';
import { editorUrl, storyTypeName } from '../story/story';
import {
  useDiffCommit,
  useIsEditable,
  useRevisionCommit,
  useRevisionHistory,
} from '../story/storyHooks';
import { useProjectGraph } from '../structurePanel/projectGraphActions';
import { EdgeName, NodeName } from '../types/asset';
import { EdgeGroup, SlimEdge } from '../types/edge';
import { loadSlimAsset } from './revision';

const ProjectGraphEdgeGroups = new Set<EdgeGroup>([
  'elements',
  'questions',
  'survey',
  'gradebookCategories',
  'gradebookCategory',
  'assesses',
  'teaches',
  'cblRubric',
  'criteria',
]);

const RevisionActionBar: React.FC = () => {
  const polyglot = usePolyglot();
  const history = useHistory();
  const dispatch = useDispatch();
  const contextPath = useCurrentContextPath();
  const asset = useEditedCurrentAsset();
  const branchId = useBranchId();
  const commit = useRevisionCommit();
  const diff = useDiffCommit();
  const contentAccess = useContentAccess(asset?.name);
  const editMode =
    useIsEditable(asset?.name, 'EditContent', true) &&
    contentAccess.EditSettings &&
    contentAccess.AlignContent;

  const revisions = useRevisionHistory(asset?.name);
  const [lag, lead, grandlead] = useMemo(() => {
    if (!commit) {
      return [undefined, revisions[1]?.first, revisions[2]?.first];
    } else {
      const index = revisions.findIndex(c => commit === c.first);
      const hidden = revisions[1 + index]?.hidden;
      const hidden2 = revisions[2 + index]?.hidden;
      return index < 0
        ? [undefined, undefined]
        : [
            revisions[index - 1]?.first,
            diff && hidden ? undefined : revisions[1 + index]?.first,
            diff && hidden2 ? undefined : revisions[2 + index]?.first,
          ];
    }
  }, [revisions, diff, commit]);

  const projectGraph = useProjectGraph();

  const [title, setTitle] = useState<string>();

  // Memoize this so it reflects the current name and doesn't change as you wade through history
  useEffect(() => {
    if (asset) loadSlimAsset(branchId, asset.name).then(({ asset }) => setTitle(asset.data.title));
  }, [asset?.name]);

  const crumbs = useMemo(
    () => [
      isQuestion(asset?.typeId) ? storyTypeName(polyglot, asset?.typeId) : title,
      'Page History',
    ],
    [asset?.typeId, title, polyglot]
  );
  useDocumentTitle(crumbs);

  const restoreRevision = () => {
    trackAuthoringEvent(`Narrative Editor - Restore Revision`, asset.typeId);
    const allGroups = Object.keys(edgeRuleConstants[asset.typeId]) as EdgeGroup[];
    const groups = allGroups.filter(g => ProjectGraphEdgeGroups.has(g));
    loadSlimAsset(branchId, asset.name, groups).then(({ asset: currentAsset, includes }) => {
      dispatch(beginProjectGraphEdit('Restore revision'));
      if (!isEqual(asset.data, currentAsset.data))
        dispatch(editProjectGraphNodeData(asset.name, asset.data));
      const commitOutEdgesByGroup = groupBy(
        projectGraph.outEdgesByNode[asset.name]?.map(name => projectGraph.edges[name]) ?? [],
        edge => edge.group
      );
      for (const group of groups) {
        const currentOutEdges = includes[group] ?? [];
        const commitOutEdges = commitOutEdgesByGroup[group] ?? [];
        const commitOutEdgesByTargetName: Record<NodeName, SlimEdge> = {};
        for (const commitEdge of commitOutEdges) {
          commitOutEdgesByTargetName[commitEdge.targetName] = commitEdge;
        }
        const currentEdgeNamesByTargetName: Record<NodeName, EdgeName> = {};
        for (const outEdge of currentOutEdges) {
          const commitEdge = commitOutEdgesByTargetName[outEdge.targetName];
          if (!commitEdge) {
            dispatch(deleteProjectGraphEdge(outEdge));
          } else {
            currentEdgeNamesByTargetName[outEdge.targetName] = outEdge.name;
            // TODO: setEdgeDataOp if necessary
          }
        }
        const edgeOrder = new Array<EdgeName>();
        for (const commitEdge of commitOutEdges) {
          const currentEdgeName = currentEdgeNamesByTargetName[commitEdge.targetName];
          if (currentEdgeName) {
            edgeOrder.push(currentEdgeName);
          } else {
            // TODO: What if I restore a multiverse edge and the multiverse branch is no longer in this
            // course because I already deleted the last edge using it. Ought to check. Will fail write-ops.
            const newEdge = {
              sourceName: asset.name,
              targetName: commitEdge.targetName,
              name: crypto.randomUUID(),
              group: group,
              data: commitEdge.data,
              traverse: commitEdge.traverse,
            };
            dispatch(addProjectGraphEdge(newEdge));
            // This node will probably not be in the head commit project graph so we put in a
            // placeholder so it can display.
            dispatch(restoreProjectGraphNode(projectGraph.nodes[commitEdge.targetName]));
            edgeOrder.push(newEdge.name);
          }
        }
        if (
          !isEqual(
            edgeOrder,
            currentOutEdges.map(edge => edge.name)
          )
        ) {
          dispatch(setProjectGraphEdgeOrder(asset.name, group, edgeOrder));
        }
      }
      dispatch(autoSaveProjectGraphEdits());
      history.push(editorUrl('story', branchId, asset, contextPath));
    });
  };

  return (
    <div className="narrative-action-bar d-flex align-items-center justify-content-between h-100 px-3">
      <h6 className="m-0 text-nowrap flex-shrink-1 me-3 d-flex align-items-center justify-content-end minw-0">
        <QuillMenu />
        <Link
          to={editorUrl('story', branchId, asset, contextPath)}
          onClick={trackNarrativeNavHandler('Revision Parent')}
          className="minw-0 text-truncate"
        >
          {crumbs[0] ?? ''}
        </Link>
        <span className="text-muted ms-2 me-2">/</span>
        <span className="text-truncate minw-0">{crumbs[1]}</span>
      </h6>
      <div className="d-flex align-items-center ms-2">
        <Link
          className={classNames(
            'p-1 btn btn-transparent mini-button d-flex',
            !(diff ? grandlead : lead) && 'disabled'
          )}
          id="older-revision"
          title={diff ? 'Older diff' : 'Older revision'}
          to={editorUrl('revision', branchId, asset, contextPath, {
            commit: lead,
            diff: diff ? grandlead : undefined,
          })}
          onClick={e => {
            if (!(diff ? grandlead : lead)) e.preventDefault();
            else trackNarrativeNav(diff ? 'Older Diff' : 'Older Revision');
          }}
        >
          <AiOutlineArrowLeft />
        </Link>
        <BsClock className="text-muted" />
        <Link
          className={classNames('p-1 btn btn-transparent mini-button d-flex', !lag && 'disabled')}
          id="newer-revision"
          title={diff ? 'Newer diff' : 'Newer revision'}
          to={editorUrl('revision', branchId, asset, contextPath, {
            commit: lag,
            diff: diff ? commit : undefined,
          })}
          onClick={e => {
            if (!lag) e.preventDefault();
            else trackNarrativeNav(diff ? 'Newer Diff' : 'Newer Revision');
          }}
        >
          <AiOutlineArrowRight />
        </Link>
        {editMode && (
          <Button
            color="warning"
            size="sm"
            className="ms-3"
            id="restore-revision"
            onClick={restoreRevision}
            disabled={!!diff || !commit || commit === revisions[0]?.first}
          >
            Restore
          </Button>
        )}
        <Link
          to={editorUrl('story', branchId, asset, contextPath)}
          onClick={trackNarrativeNavHandler('Revision Exit')}
          className="btn btn-sm btn-outline-primary ms-2"
          id="exit-revision"
        >
          Exit
        </Link>
      </div>
    </div>
  );
};

export default RevisionActionBar;
