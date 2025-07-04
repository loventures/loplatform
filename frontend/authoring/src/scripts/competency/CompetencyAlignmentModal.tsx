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
import React, { useMemo, useState } from 'react';
import { GrCheckmark } from 'react-icons/gr';
import { IoCloseOutline, IoSearchOutline } from 'react-icons/io5';
import { RxTriangleRight } from 'react-icons/rx';
import { useDispatch } from 'react-redux';
import {
  Button,
  FormGroup,
  Input,
  InputGroup,
  InputGroupText,
  Label,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from 'reactstrap';
import { useDebounce } from 'use-debounce';

import edgeRules from '../editor/EdgeRuleConstants';
import { setToggle } from '../gradebook/set';
import {
  addProjectGraphEdge,
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  computeEditedOutEdges,
  computeEditedTargets,
  deleteProjectGraphEdge,
  getFilteredContentList,
  TreeAsset,
  useContentTree,
  useEditedAsset,
  useGraphEdits,
  useGraphEditSelector,
} from '../graphEdit';
import { useDcmSelector, useModal, usePolyglot } from '../hooks';
import { getIcon } from '../story/AddAsset';
import { useProjectAccess } from '../story/hooks';
import { isQuestion } from '../story/questionUtil';
import { useIsStoryEditMode } from '../story/storyHooks';
import { useProjectGraph } from '../structurePanel/projectGraphActions';
import { openToast, TOAST_TYPES } from '../toast/actions';
import { NewAsset, NodeName, TypeId } from '../types/asset';
import { EdgeGroup, NewEdge } from '../types/edge';
import { CompetencyContentEdgeGroups } from './useFlatCompetencies';

const teachableTypes = Object.entries(edgeRules)
  .filter(entry => {
    const typeRules = entry[1];
    const edgeGroups = Object.keys(typeRules);
    return edgeGroups.includes('teaches');
  })
  .map(entry => entry[0] as TypeId);

const assessableTypes = Object.entries(edgeRules)
  .filter(entry => {
    const typeRules = entry[1];
    const edgeGroups = Object.keys(typeRules);
    return edgeGroups.includes('assesses');
  })
  .map(entry => entry[0] as TypeId);

const allowableTypes: TypeId[] = teachableTypes.concat(assessableTypes);

const getEdgeGroup = (newAsset: NewAsset<TypeId>): Extract<EdgeGroup, 'teaches' | 'assesses'> =>
  teachableTypes.includes(newAsset.typeId) ? 'teaches' : 'assesses';

type CompetencyAlignmentModalData = {
  competencyToAlign: NodeName;
  resetAlignments: () => void;
};

const CompetencyAlignmentModal: React.FC = () => {
  const {
    modalOpen,
    toggleModal,
    data: { competencyToAlign, resetAlignments },
  } = useModal<CompetencyAlignmentModalData>();
  const polyglot = usePolyglot();
  const [searchTerm, setSearchTerm] = useState('');
  const [aligned, setAligned] = useState(false);
  const projectGraph = useProjectGraph();
  const graphEdits = useGraphEdits();
  const { homeNodeName } = projectGraph;
  const dispatch = useDispatch();
  const [expanded, setExpanded] = useState(new Set<NodeName>());

  const homeNode = useEditedAsset(homeNodeName);
  const competency = useEditedAsset(competencyToAlign);
  const projectAccess = useProjectAccess();
  const editMode = useIsStoryEditMode() && projectAccess.EditObjectives;

  const contentTree = useContentTree(homeNode, [], CompetencyContentEdgeGroups);

  const alignedContentNames = useMemo(() => {
    const alignments = new Set<NodeName>();
    for (const content of getFilteredContentList(contentTree)) {
      for (const group of ['teaches', 'assesses'] as const) {
        const edges = computeEditedOutEdges(content.name, group, projectGraph, graphEdits);
        for (const { targetName } of edges) {
          if (targetName === competency.name) alignments.add(content.name);
        }
      }
    }
    return alignments;
  }, [projectGraph, graphEdits, contentTree, competency]);

  const role = useDcmSelector(state => state.layout.role);
  const accessRights = useGraphEditSelector(state => state.contentTree.accessRights);
  const [lc] = useDebounce(searchTerm.toLowerCase(), 300);
  const predicate = useMemo(() => {
    return (node: NewAsset<any>) =>
      role && !accessRights[node.name].ViewContent
        ? '.'
        : (!lc || node.data.title.toLowerCase().includes(lc)) &&
          (!aligned || alignedContentNames.has(node.name));
  }, [lc, aligned, alignedContentNames, role, accessRights]);
  const contentList = useMemo(
    () => getFilteredContentList(contentTree, predicate),
    [contentTree, predicate]
  );

  const alignedModules = useMemo(() => {
    const isAligned = (asset: TreeAsset): boolean =>
      alignedContentNames.has(asset.name) || asset.children.some(isAligned);
    return new Set(contentTree.children.filter(isAligned).map(module => module.name));
  }, [contentTree, alignedContentNames]);

  const alignContent = (content: NewAsset<any>, checked: boolean) => {
    const group = getEdgeGroup(content);
    dispatch(beginProjectGraphEdit(checked ? 'Align content' : 'Unalign content'));
    if (checked) {
      const edge: NewEdge = {
        name: crypto.randomUUID(),
        sourceName: content.name,
        targetName: competency.name,
        group: group,
        traverse: false,
        data: {},
        newPosition: 'end',
      };
      dispatch(addProjectGraphEdge(edge));
    } else {
      const siblings = computeEditedTargets(
        content.name,
        group,
        undefined,
        projectGraph,
        graphEdits
      );
      const deleted = siblings.find(e => e.edge.targetName === competency.name);
      if (!deleted) {
        dispatch(openToast('There was an error during unalignment.', TOAST_TYPES.DANGER));
        return;
      }
      dispatch(deleteProjectGraphEdge(deleted.edge));
    }
    dispatch(autoSaveProjectGraphEdits());
    resetAlignments();
  };

  return (
    <Modal
      id="competency-alignment-modal"
      isOpen={modalOpen}
      toggle={toggleModal}
      size="xl"
      className="not-100 competencies-editor"
    >
      <ModalHeader>{competency.data.title}</ModalHeader>
      <ModalBody
        style={{
          maxHeight: 'calc(100vh - 16rem)',
          overflowY: 'scroll',
        }}
      >
        <div className="my-3 d-flex align-items-center">
          <div className="flex-grow-1 w-100"></div>
          <InputGroup className="search-bar flex-grow-0 flex-shrink-0">
            <Input
              type="search"
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
              placeholder="Filter by title..."
              bsSize="sm"
              size={48}
            />
            <InputGroupText className="search-icon form-control form-control-sm flex-grow-0 d-flex align-items-center justify-content-center p-0 pe-1">
              <IoSearchOutline aria-hidden />
            </InputGroupText>
          </InputGroup>
          <FormGroup
            check
            className="flex-grow-1 w-100 ms-5"
          >
            <Label
              check
              className="small"
            >
              <Input
                type="checkbox"
                checked={aligned}
                onChange={e => setAligned(e.target.checked)}
              />
              Aligned content
            </Label>
          </FormGroup>
        </div>
        <div className="p-2 content-list">
          {!contentList.length && (
            <div className="text-muted depth-1 text-center">No content matches your search.</div>
          )}
          {contentList
            .filter(c => c.context.length < 2 || lc || aligned || expanded.has(c.context[1].name))
            .map(c => (
              <CompetencyAlignmentRow
                key={c.edge.name}
                content={c}
                aligned={alignedContentNames.has(c.name)}
                subaligned={alignedModules.has(c.name)}
                onChange={e => alignContent(c, e)}
                lc={lc}
                collapsible={
                  !aligned &&
                  !lc &&
                  c.typeId === 'module.1' &&
                  (!role || accessRights[c.name].ViewContent)
                }
                collapsed={!aligned && !lc && !expanded.has(c.name)}
                disabled={!editMode}
                toggle={all =>
                  setExpanded(
                    !all
                      ? setToggle(expanded, c.name)
                      : expanded.has(c.name)
                        ? new Set()
                        : new Set(contentTree.children.map(a => a.name))
                  )
                }
              />
            ))}
        </div>
      </ModalBody>
      <ModalFooter>
        <Button
          color="primary"
          onClick={toggleModal}
        >
          {polyglot.t('DONE')}
        </Button>
      </ModalFooter>
    </Modal>
  );
};

export default CompetencyAlignmentModal;

interface CompetencyAlignmentRowProps {
  content: TreeAsset;
  aligned: boolean;
  subaligned: boolean;
  lc: string;
  onChange: (checked: boolean) => void;
  collapsible: boolean;
  collapsed: boolean;
  disabled: boolean;
  toggle: (all: boolean) => void;
}

const CompetencyAlignmentRow: React.FC<CompetencyAlignmentRowProps> = ({
  content,
  aligned,
  subaligned,
  onChange,
  lc,
  collapsible,
  collapsed,
  disabled,
  toggle,
}) => {
  // so sad we don't use animated collapser but we're a flat content list so shrug
  const question = isQuestion(content.typeId);
  const Icon =
    content.typeId === 'module.1' || content.typeId === 'lesson.1' || question
      ? undefined
      : getIcon(content.typeId);
  const clickable = allowableTypes.includes(content.typeId);
  const edgeGroup = getEdgeGroup(content);
  const title = `${question ? `Question ${content.index + 1} – ` : ''}${content.data.title}`;
  return (
    <label
      htmlFor={content.edge.name}
      className={classNames(
        'd-flex',
        'gap-1',
        'align-items-center',
        `story-nav-${content.typeId.replace(/\..*/, '')}`,
        `depth-${content.depth}`,
        !!lc && content.data.title.toLowerCase().includes(lc) && 'hit',
        (clickable || content.typeId === 'module.1') && 'pointer'
      )}
      title={title}
    >
      {collapsible && (
        <Button
          id={content.edge.name}
          size="small"
          color="transparent"
          className={classNames(
            'mini-button p-0 d-inline-flex align-items-center justify-content-center module-toggle',
            !collapsed && 'expanded'
          )}
          style={{ lineHeight: 1 }}
          onClick={e => toggle(e.shiftKey || e.metaKey)}
        >
          <RxTriangleRight size="1.5rem" />
        </Button>
      )}
      {Icon && <Icon className="text-muted flex-shrink-0" />}
      <span className="text-truncate flex-grow-1">{title}</span>
      <div className="d-flex align-items-center flex-shrink-0">
        {clickable ? (
          <span
            className="text-muted"
            style={{
              fontSize: '.875rem',
              marginRight: '.375rem',
              paddingBottom: '.2rem',
              fontVariant: 'small-caps',
            }}
          >
            {edgeGroup}
          </span>
        ) : null}
        {clickable ? (
          !disabled ? (
            <input
              type="checkbox"
              id={content.edge.name}
              checked={aligned}
              onChange={e => onChange(e.target.checked)}
            />
          ) : aligned ? (
            <GrCheckmark size="1rem" />
          ) : (
            <IoCloseOutline
              className="text-light"
              size="1rem"
            />
          )
        ) : collapsed && subaligned ? (
          <input
            type="checkbox"
            checked
            disabled
          />
        ) : null}
      </div>
    </label>
  );
};
