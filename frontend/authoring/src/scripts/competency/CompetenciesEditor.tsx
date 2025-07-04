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
import React, { useCallback, useEffect, useState } from 'react';
import { AiOutlinePlus } from 'react-icons/ai';
import { IoSearchOutline } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import { Alert, Button, Input, InputGroup, InputGroupText } from 'reactstrap';

import { formatFullDate } from '../dateUtil';
import {
  addProjectGraphEdge,
  addProjectGraphNode,
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  deleteProjectGraphEdge,
  discardProjectGraphEdit,
  editProjectGraphNodeData,
  useEditedAssetDatum,
} from '../graphEdit';
import { usePolyglot, useRootNodeName } from '../hooks';
import PresenceService from '../presence/services/PresenceService';
import { Stornado } from '../story/badges/Stornado';
import { useProjectAccess } from '../story/hooks';
import NarrativePresence from '../story/NarrativeAsset/NarrativePresence';
import { newAssetData } from '../story/questionUtil';
import { useEditSession, useEscapeOrEnterToStopEditing } from '../story/story';
import { useIsStoryEditMode, useRevisionCommit } from '../story/storyHooks';
import { useProjectGraph } from '../structurePanel/projectGraphActions';
import { NewAsset } from '../types/asset';
import { NewEdge } from '../types/edge';
import { CompetencySet, Level1Competency } from '../types/typeIds';
import {
  useCachedCompetencyAlignments,
  useCachedCompetencyTree,
  useFilteredCompetencies,
} from './competencyEditorHooks';
import CompetencyRow from './CompetencyRow';
import { CompetencySetActionsMenu } from './CompetencySetActionsMenu';
import CompetencySetMenu from './CompetencySetMenu';
import { useEditedCompetencySetEdges } from './useFlatCompetencies';

const CompetenciesEditor: React.FC = () => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const [search, setSearch] = useState('');
  const [lastAddedComp, setLastAddedComp] = useState<string | undefined>();
  const rootNodeName = useRootNodeName();
  const competencySetEdges = useEditedCompetencySetEdges();
  const competencySetEdge = competencySetEdges[0];
  const competencySetName = competencySetEdge?.targetName;
  const [editTitle, setEditTitle] = useState(false);
  const title = useEditedAssetDatum(competencySetName, data => data.title); // avoid Unknown
  const projectAccess = useProjectAccess();
  const editMode = useIsStoryEditMode() && projectAccess.EditObjectives;
  const projectGraph = useProjectGraph();
  const commit = useRevisionCommit();
  const canEdit = editMode;

  const [competencyTree, resetCompetencyTree] = useCachedCompetencyTree();
  const filteredCompetencies = useFilteredCompetencies(competencyTree, search);
  const [alignments, resetAlignments] = useCachedCompetencyAlignments();

  const session = useEditSession();
  const updateTitle = (value: string) => {
    const trimmed = value.trim();
    if (!competencySetName && trimmed) {
      addCompetencySet(trimmed);
    } else {
      const title = trimmed || 'Untitled';
      dispatch(beginProjectGraphEdit('Edit title', session));
      dispatch(editProjectGraphNodeData(competencySetName, { title }));
    }
  };

  const addCompetencySet = (title?: string) => {
    dispatch(beginProjectGraphEdit(`Add ${polyglot.t(CompetencySet)}`));
    const addSet: NewAsset<CompetencySet> = {
      name: crypto.randomUUID(),
      typeId: CompetencySet,
      data: {
        ...newAssetData(CompetencySet),
        title: title ?? polyglot.t('UNTITLED_COMPETENCY_SET'),
      },
    };
    dispatch(addProjectGraphNode(addSet));
    const setEdge: NewEdge = {
      name: crypto.randomUUID(),
      sourceName: rootNodeName,
      targetName: addSet.name,
      group: 'competencySets',
      traverse: true,
      data: {},
      newPosition: 'end',
    };
    dispatch(addProjectGraphEdge(setEdge));
    // no autosave because we'll autosave on blur
    resetCompetencyTree();
    return addSet.name;
  };

  const addNewCompetency = () => {
    const csName = competencySetName ?? addCompetencySet();
    dispatch(beginProjectGraphEdit(`Add ${polyglot.t(Level1Competency)}`));
    const addAsset: NewAsset<Level1Competency> = {
      name: crypto.randomUUID(),
      typeId: Level1Competency,
      data: {
        title: 'Untitled',
        ...newAssetData(Level1Competency),
      },
    };
    dispatch(addProjectGraphNode(addAsset));
    const edge: NewEdge = {
      name: crypto.randomUUID(),
      sourceName: csName,
      targetName: addAsset.name,
      group: 'level1Competencies',
      traverse: true,
      data: {},
      newPosition: 'end',
    };
    dispatch(addProjectGraphEdge(edge));
    // no autosave because we'll autosave on blur
    setLastAddedComp(addAsset.name); // for autofocus
    resetCompetencyTree();
  };

  useEffect(() => {
    // delay this because the narrative hooks version is  delayed
    if (competencySetName) {
      const timeout = setTimeout(() => PresenceService.onAsset(competencySetName), 150);
      return () => clearTimeout(timeout);
    }
  }, [competencySetName]);

  const endEditing = useCallback(() => {
    setEditTitle(false);
    dispatch(autoSaveProjectGraphEdits());
  }, []);

  const finishEditing = useCallback(
    (enter: boolean) => {
      if (enter) {
        endEditing();
      } else {
        dispatch(discardProjectGraphEdit(session));
        setEditTitle(false);
      }
    },
    [endEditing, session]
  );

  const keyHandler = useEscapeOrEnterToStopEditing(finishEditing);

  // TODO: Add Feedback option

  return (
    <div className="competencies-editor">
      <div style={{ display: 'grid', gridTemplateColumns: 'auto 1fr auto' }}>
        <CompetencySetActionsMenu
          competencyTree={competencyTree}
          getCompetencySet={() => competencySetName ?? addCompetencySet()}
          resetCompetencyTree={resetCompetencyTree}
          resetAlignments={resetAlignments}
        />
        <div
          className={classNames(
            'd-flex align-items-center justify-content-center minw-0',
            commit ? 'text-sienna' : 'text-muted'
          )}
        >
          {commit
            ? `Learning Objectives as of ${formatFullDate(projectGraph.commit.created)}`
            : 'Learning Objectives'}
          <div style={{ width: 0 }}>
            <Stornado name={competencySetName} />
          </div>
        </div>
        <NarrativePresence name={competencySetName ?? 'objectives'}>
          <CompetencySetMenu />
        </NarrativePresence>
      </div>
      <div className={classNames('asset-title d-flex flex-column mt-3', canEdit && 'edit-mode')}>
        {editTitle ? (
          <input
            className="h2"
            autoFocus
            defaultValue={title === 'Untitled' ? '' : title}
            maxLength={255}
            placeholder={polyglot.t('UNTITLED_COMPETENCY_SET')}
            onBlur={endEditing}
            onChange={e => updateTitle(e.target.value)}
            onKeyDown={keyHandler}
          />
        ) : (
          <h2
            className="title-editor"
            onClick={() => canEdit && setEditTitle(true)}
            tabIndex={canEdit ? 0 : undefined}
          >
            {title ?? polyglot.t('UNTITLED_COMPETENCY_SET')}
          </h2>
        )}
      </div>
      {competencySetEdges.length > 1 && (
        <Alert
          color="danger"
          className="mb-5 d-flex justify-content-between align-items-center"
        >
          <span>
            <strong>Warning:</strong> Multiple learning objective sets are present in this project.
          </span>
          <Button
            color="danger"
            size="sm"
            onClick={() => {
              dispatch(beginProjectGraphEdit('Purge learning objective sets'));
              for (let i = 1; i < competencySetEdges.length; ++i) {
                dispatch(deleteProjectGraphEdge(competencySetEdges[i]));
              }
              dispatch(autoSaveProjectGraphEdits());
            }}
          >
            Delete Others
          </Button>
        </Alert>
      )}
      {(!!search || !!filteredCompetencies?.length) && (
        <div className="d-flex justify-content-center align-items-center">
          <InputGroup className="search-bar">
            <Input
              type="search"
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="Filter learning objectives..."
              bsSize="sm"
              size={48}
            />
            <InputGroupText className="search-icon form-control form-control-sm flex-grow-0 d-flex align-items-center justify-content-center p-0 pe-1">
              <IoSearchOutline aria-hidden />
            </InputGroupText>
          </InputGroup>
        </div>
      )}

      <div className="mt-5 mb-4 px-3 d-flex flex-column align-items-stretch">
        {!filteredCompetencies?.length && (
          <div className="text-muted depth-1 text-center">
            {search ? 'No learning objectives match your search.' : 'No learning objectives.'}
          </div>
        )}
        {filteredCompetencies?.map(comp => (
          <CompetencyRow
            key={comp.name}
            competency={comp}
            autoFocus={comp.name === lastAddedComp}
            setAddedItem={setLastAddedComp}
            userCanEdit={canEdit}
            alignments={alignments[comp.name] ?? 0}
            resetCompetencyTree={resetCompetencyTree}
            resetAlignments={resetAlignments}
          />
        ))}
      </div>
      {canEdit && (
        <div className="my-5 add-comp">
          <div className="d-flex justify-content-center align-items-center">
            <div className="rule" />
            <Button
              color="primary"
              className="add-lvl-1-btn"
              onClick={() => addNewCompetency()}
            >
              <AiOutlinePlus
                size="1.5rem"
                stroke="0px"
              />
            </Button>
            <div className="rule" />
          </div>
        </div>
      )}
    </div>
  );
};

export default CompetenciesEditor;
