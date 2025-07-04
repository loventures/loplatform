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

import { maxBy, sumBy } from 'lodash';
import React, { useCallback, useMemo } from 'react';
import { IoAdd, IoAddOutline, IoSearchOutline, IoTrashOutline } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import { Button } from 'reactstrap';

import { useFlatCompetencies } from '../../../competency/useFlatCompetencies';
import edgeRuleConstants from '../../../editor/EdgeRuleConstants';
import {
  addProjectGraphEdge,
  addProjectGraphNode,
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  computeEditedTargets,
  deleteProjectGraphEdge,
  getCopiedAsset,
  getEditedAsset,
  useAllEditedOutEdges,
  useEditedAsset,
  useEditedAssetDatum,
  useGraphEdits,
} from '../../../graphEdit';
import { noGraphEdits } from '../../../graphEdit/graphEditReducer';
import { FindContentModalData } from '../../../modals/FindContentModal';
import { openModal } from '../../../modals/modalActions';
import { ModalIds } from '../../../modals/modalIds';
import { loadRemoteProjectGraph } from '../../../modals/narrative/ProjectContentSelector';
import { ProjectGraph } from '../../../structurePanel/projectGraphReducer';
import { NewAsset, NodeName, TypeId } from '../../../types/asset';
import { DcmState, Thunk } from '../../../types/dcmState';
import { NewEdge } from '../../../types/edge';
import { cloneAsset } from '../../AddAsset';
import { isQuestion, newAssetData } from '../../questionUtil';
import { storyTypeName } from '../../story';
import { useIsEditable, useIsStoryEditMode } from '../../storyHooks';
import { CopiedAsset } from '../../storyReducer';
import { CriterionRow } from './CriterionRow';
import { maxPoints } from './util';

const selectNewRubricTitle = (name: NodeName, typeId: TypeId) => (state: DcmState) => {
  const polyglot = state.configuration.translations;
  const assetTitle = getEditedAsset(name, state.projectGraph, state.graphEdits).data.title;
  // This is bad and sad and stale but without an assigned title, rubrics
  // show up in search results as junk.
  return `Rubric for ${
    isQuestion(typeId) ? storyTypeName(polyglot, typeId, true) : assetTitle
  }`.substring(0, 255);
};

const addCriterionAction =
  (parentName: NodeName): Thunk =>
  dispatch => {
    const addCriterion: NewAsset<'rubricCriterion.1'> = {
      name: crypto.randomUUID(),
      typeId: 'rubricCriterion.1',
      data: {
        ...newAssetData('rubricCriterion.1'),
      },
    };
    dispatch(addProjectGraphNode(addCriterion));
    const criterionEdge: NewEdge = {
      name: crypto.randomUUID(),
      sourceName: parentName,
      targetName: addCriterion.name,
      group: 'criteria',
      traverse: true,
      data: {},
      newPosition: 'end',
    };
    dispatch(addProjectGraphEdge(criterionEdge));
    dispatch(autoSaveProjectGraphEdits());
  };

const addRubricAction =
  (name: NodeName, typeId: TypeId): Thunk =>
  (dispatch, getState) => {
    const state = getState();
    const newRubricTitle = selectNewRubricTitle(name, typeId)(state);

    dispatch(beginProjectGraphEdit('Add rubric'));
    const addRubric: NewAsset<'rubric.1'> = {
      name: crypto.randomUUID(),
      typeId: 'rubric.1',
      data: {
        ...newAssetData('rubric.1'),
        title: newRubricTitle,
      },
    };
    dispatch(addProjectGraphNode(addRubric));
    const rubricEdge: NewEdge = {
      name: crypto.randomUUID(),
      sourceName: name,
      targetName: addRubric.name,
      group: 'cblRubric',
      traverse: true,
      data: {},
      newPosition: 'end',
    };
    dispatch(addProjectGraphEdge(rubricEdge));
    dispatch(addCriterionAction(addRubric.name)); // saves
  };

const findRubricAction =
  (name: NodeName, typeId: TypeId): Thunk =>
  (dispatch, getState) => {
    const state = getState();
    const { projectGraph, graphEdits } = getState();
    const asset = getEditedAsset(name, projectGraph, graphEdits);
    const newRubricTitle = selectNewRubricTitle(name, typeId)(state);

    const modalData: FindContentModalData = {
      mode: 'rubric',
      parent: asset,
      link: () => {
        // This never happens because linking is questionable and so we don't show the button
      },
      clone: (project, names) => {
        dispatch(beginProjectGraphEdit('Clone rubric'));
        const rubricName = names[0];
        let rubricPromise: Promise<CopiedAsset>;
        if (project.branchId === projectGraph.branchId && projectGraph.nodes[rubricName]) {
          // If it's a known local asset clone it from the project graph
          rubricPromise = Promise.resolve(getCopiedAsset(rubricName, projectGraph, graphEdits));
        } else {
          // If it's a foreign asset, clone it from the remote project graph
          rubricPromise = loadRemoteProjectGraph(project).then(projectGraph =>
            getCopiedAsset(rubricName, projectGraph, noGraphEdits)
          );
        }
        rubricPromise.then(original => {
          const copied = cloneAsset(original, dispatch, newRubricTitle);
          const newEdge: NewEdge = {
            name: crypto.randomUUID(),
            sourceName: name,
            targetName: copied.name,
            group: 'cblRubric',
            data: {},
            traverse: true,
            newPosition: 'end',
          };
          dispatch(addProjectGraphEdge(newEdge));
        });
        dispatch(autoSaveProjectGraphEdits());
      },
    };
    dispatch(openModal(ModalIds.FindContentModal, modalData));
  };

export const RubricEditor: React.FC<{
  name: NodeName;
  typeId: TypeId;
  readOnly?: boolean;
  projectGraph: ProjectGraph;
}> = ({ name, typeId, readOnly, projectGraph }) => {
  const dispatch = useDispatch();
  const editable = useIsEditable(name) && !readOnly;
  const allEdges = useAllEditedOutEdges(name);
  const rubricEdge = useMemo(() => allEdges.find(edge => edge.group === 'cblRubric'), [allEdges]);

  // ungradable discussions get no rubric
  const addable = useEditedAssetDatum(name, data =>
    typeId === 'discussion.1' ? data.gradable : true
  );

  const onAddRubric = useCallback(() => {
    dispatch(addRubricAction(name, typeId));
  }, [name, typeId]);

  const onFindRubric = useCallback(() => {
    dispatch(findRubricAction(name, typeId));
  }, [name, typeId]);

  // student does not see the rubric title so ignore it
  return rubricEdge ? (
    <RubricTableProxy
      edge={rubricEdge}
      projectGraph={projectGraph}
    />
  ) : editable && addable ? (
    <div
      key="add-rubric"
      className="d-flex justify-content-center mt-4"
    >
      <div className="d-flex align-items-center ps-3 pe-1 py-1 no-rubric">
        <span className="text-muted">No Rubric</span>
        <Button
          outline
          color="primary"
          className="border-0 ms-2 d-flex find-rubric-btn"
          style={{ lineHeight: 1, padding: '.4rem' }}
          onClick={onFindRubric}
          title="Find Rubric"
        >
          <IoSearchOutline
            aria-hidden={true}
            size="1rem"
          />
        </Button>
        <Button
          outline
          color="primary"
          className="border-0 ms-2 d-flex add-rubric-btn"
          style={{ lineHeight: 1, padding: '.4rem' }}
          onClick={onAddRubric}
          title="Add Rubric"
        >
          <IoAdd
            aria-hidden={true}
            size="1rem"
          />
        </Button>
      </div>
    </div>
  ) : null;
};

const RubricTableProxy: React.FC<{ edge: NewEdge; projectGraph: ProjectGraph }> = ({
  edge,
  projectGraph,
}) => {
  const dispatch = useDispatch();
  const rubric = useEditedAsset(edge.targetName);

  const onDeleteRubric = useCallback(() => {
    dispatch(beginProjectGraphEdit('Delete rubric'));
    dispatch(deleteProjectGraphEdge(edge));
    dispatch(autoSaveProjectGraphEdits());
  }, []);

  return (
    <RubricTable
      asset={rubric}
      projectGraph={projectGraph}
      onDeleteRubric={onDeleteRubric}
    />
  );
};

export const RubricTable: React.FC<{
  asset: NewAsset<'rubric.1'>;
  readOnly?: boolean;
  projectGraph: ProjectGraph;
  onDeleteRubric?: () => void;
}> = ({ asset: rubric, readOnly, projectGraph, onDeleteRubric }) => {
  const dispatch = useDispatch();
  const editMode = useIsStoryEditMode() && !readOnly;
  const flatCompetencies = useFlatCompetencies();
  const graphEdits = useGraphEdits();

  const criteria = useMemo(
    () =>
      computeEditedTargets(rubric?.name, 'criteria', 'rubricCriterion.1', projectGraph, graphEdits),
    [projectGraph, graphEdits]
  );

  const doAddCriterion = (parentName: NodeName) => {
    const addCriterion: NewAsset<'rubricCriterion.1'> = {
      name: crypto.randomUUID(),
      typeId: 'rubricCriterion.1',
      data: {
        ...newAssetData('rubricCriterion.1'),
      },
    };
    dispatch(addProjectGraphNode(addCriterion));
    const criterionEdge: NewEdge = {
      name: crypto.randomUUID(),
      sourceName: parentName,
      targetName: addCriterion.name,
      group: 'criteria',
      traverse: true,
      data: {},
      newPosition: 'end',
    };
    dispatch(addProjectGraphEdge(criterionEdge)); // autosaves
  };

  const onAddCriterion = () => {
    dispatch(beginProjectGraphEdit('Add criterion'));
    doAddCriterion(rubric.name); // autosaves
  };

  const maxLevels = Math.max(
    1,
    maxBy(criteria ?? [], c => c.data.levels.length)?.data.levels.length ?? 0
  );

  const totalPoints = sumBy(criteria ?? [], maxPoints);

  // student does not see the rubric title so ignore it
  return (
    <div
      key="rubric"
      className="rubric feedback-context"
    >
      <table className="w-100">
        <tr>
          <th style={{ width: '25%' }}>
            <div className="d-flex align-items-center justify-content-between">
              <div className="input-padding py-1">Rubric Criteria</div>
              {editMode && (
                <Button
                  size="sm"
                  outline
                  color="primary"
                  className="mini-button p-2 d-flex add-criterion"
                  title="Add Criterion"
                  onClick={() => onAddCriterion()}
                >
                  <IoAddOutline />
                </Button>
              )}
            </div>
          </th>
          <th colSpan={maxLevels}>
            <div className="input-padding py-1">Ratings</div>
          </th>
          {editMode && (
            <th style={{ width: 0 }}>
              <Button
                size="sm"
                outline
                color="danger"
                className="mini-button p-2 d-flex delete-rubric"
                title="Delete Rubric"
                onClick={onDeleteRubric}
              >
                <IoTrashOutline />
              </Button>
            </th>
          )}
        </tr>
        {criteria.map((criterion, index) => (
          <CriterionRow
            key={criterion.name}
            criterion={criterion}
            index={index}
            maxLevels={maxLevels}
            editable={editMode}
            competent={flatCompetencies.length > 0}
          />
        ))}
        <tr>
          <th colSpan={maxLevels + (editMode ? 2 : 1)}>
            <div className="input-padding py-1">{`Total: ${totalPoints} Points`}</div>
          </th>
        </tr>
      </table>
    </div>
  );
};
