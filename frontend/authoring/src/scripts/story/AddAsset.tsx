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
import { push } from 'connected-react-router';
import gretchen from '../grfetchen/';
import { mapValues } from 'lodash';
import qs from 'qs';
import React, { useCallback, useEffect, useRef } from 'react';
import { IconType } from 'react-icons';
import { AiOutlinePlus, AiOutlineQuestionCircle, AiOutlineSolution } from 'react-icons/ai';
import { BiWater } from 'react-icons/bi';
import { ImPaste } from 'react-icons/im';
import {
  IoBookOutline,
  IoBriefcaseOutline,
  IoChatbubblesOutline,
  IoCheckboxOutline,
  IoClipboardOutline,
  IoCubeOutline,
  IoDocumentTextOutline,
  IoGlobeOutline,
  IoNewspaperOutline,
  IoPulseOutline,
  IoSchoolOutline,
  IoSearchOutline,
  IoSettingsOutline,
} from 'react-icons/io5';
import { MdInsights } from 'react-icons/md';
import { PiFile, PiFileAudio, PiFileImage, PiFilePdf, PiFileVideo } from 'react-icons/pi';
import { TbListCheck, TbWorldWww } from 'react-icons/tb';
import { useDispatch } from 'react-redux';
import { Button, Popover, UncontrolledTooltip } from 'reactstrap';
import { ThunkDispatch } from 'redux-thunk';

import { trackAuthoringEvent } from '../analytics';
import { trackNarrativeAdd, trackNarrativePaste } from '../analytics/AnalyticsEvents';
import { reloadAssetEditor } from '../editor/assetEditorActions';
import edgeRules from '../editor/EdgeRuleConstants';
import {
  addProjectGraphEdge,
  addProjectGraphNode,
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  computeEditedTargets,
  confirmSaveProjectGraphEdits,
  getCopiedAsset,
  getEditedAsset,
  insertProjectGraphNode,
  setProjectGraphOrigins,
  useEditedAssetTitle,
  useEditedAssetTypeId,
} from '../graphEdit';
import { usePolyglot } from '../hooks';
import { FindContentModalData } from '../modals/FindContentModal';
import { openModal } from '../modals/modalActions';
import { ModalIds } from '../modals/modalIds';
import AuthoringOpsService from '../services/AuthoringOpsService';
import { ApiQueryResults } from '../srs/apiQuery';
import { openToast } from '../toast/actions';
import { AddEdgeWriteOp } from '../types/api';
import { NewAsset, NodeName, TypeId } from '../types/asset';
import { DcmState, Thunk } from '../types/dcmState';
import { EdgeGroup, NewEdge } from '../types/edge';
import { LtiToolApiResponse } from '../types/lti';
import { Polyglot } from '../types/polyglot';
import { useLtiTools } from './dataActions';
import { ObservationIcon } from './icons/ObservationIcon';
import { ProjectResponse } from './NarrativeMultiverse';
import { isQuestion, newAssetData } from './questionUtil';
import { childEdgeGroup, editorUrl, storyTypeName } from './story';
import { setNarrativeAssetState, setNarrativeState } from './storyActions';
import {
  narrativeSaveAndContinue,
  useIsEditable,
  useIsInlineNarrativeView,
  useStorySelector,
} from './storyHooks';
import { CopiedAsset, isCopiedAsset } from './storyReducer';

const EmptySet = new Set();

const supportedElementSets = Object.fromEntries(
  Object.entries(edgeRules).map(([typeId, rules]) => [
    typeId,
    new Set([...((rules as any).elements ?? []), ...((rules as any).questions ?? [])]),
  ])
);

const stopPropagation = (event: React.MouseEvent) => {
  event.stopPropagation();
};

const closePopoverAction = () => setNarrativeState({ addingTo: undefined });

// It would be nice if this was an action, but it has to return a future in order for the modal
// to do its reload dances, so it's just a function.
const addContent = (
  dispatch: ThunkDispatch<any, any, any>,
  state: DcmState,
  branch: ProjectResponse,
  names: NodeName[],
  parent: NewAsset<any>,
  contextPath: string,
  after: NewAsset<any>,
  before: NewAsset<any>,
  redirectFindAdd: boolean,
  link: boolean
): void | Promise<any> => {
  const {
    projectGraph,
    graphEdits,
    configuration: { translations: polyglot },
  } = state;
  const { branchId } = projectGraph;
  const group = childEdgeGroup(parent.typeId);
  const { name: savedName } = getEditedAsset(parent.name, projectGraph, graphEdits);
  const siblings = computeEditedTargets(parent.name, group, undefined, projectGraph, graphEdits);
  let index = -1;
  if (after) {
    const afterIndex = siblings.findIndex(node => node.name === after.name);
    if (afterIndex >= 0) index = 1 + afterIndex;
  } else if (before) {
    index = siblings.findIndex(node => node.name === before.name);
  }
  const beforeEdge = siblings[index]?.edge.name;
  if (link) {
    const addEdges = names.map<AddEdgeWriteOp>(name => ({
      op: 'addEdge',
      name: crypto.randomUUID(),
      sourceName: savedName,
      targetName: name,
      group,
      traverse: true,
      data: {},
      position: beforeEdge ? { before: beforeEdge } : 'end',
    }));
    return AuthoringOpsService.postWriteOps(addEdges)
      .then(() => {
        dispatch(reloadAssetEditor());
        const last = names[names.length - 1];
        if (last && redirectFindAdd) {
          dispatch(push(editorUrl('story', branchId, last, contextPath, { confirm: false })));
        }
      })
      .catch(e => {
        console.log(e);
        dispatch(openToast('Link failed to save.', 'danger'));
        throw e;
      });
  } else if (branch.branchId === branchId) {
    let last: NewAsset<any> | undefined = undefined;
    if (after) names.reverse();
    for (const name of names) {
      const copied = getCopiedAsset(name, projectGraph, graphEdits);
      last = addAsset(copied, parent, after, before, group, polyglot, dispatch);
    }
    if (last) {
      if (redirectFindAdd) {
        dispatch(push(editorUrl('story', branchId, last, contextPath, { confirm: false })));
      } else {
        dispatch(autoSaveProjectGraphEdits());
      }
    }
  } else {
    const remoteCopy = async () => {
      const results = (await gretchen
        .post('/api/v2/authoring/nodes/copyBulk')
        .data({
          source: { branch: branch.branchId, nodes: names },
          target: { branch: branchId, node: savedName },
          group: group,
          beforeEdge: beforeEdge,
        })
        .exec()) as ApiQueryResults<NodeName>;
      const last = results.objects[results.objects.length - 1];
      dispatch(reloadAssetEditor());
      if (last && redirectFindAdd) {
        dispatch(push(editorUrl('story', branchId, last, contextPath, { confirm: false })));
      }
    };
    return remoteCopy();
  }
};

const onFindAction =
  (
    parentName: NodeName,
    contextPath: string,
    afterName: NodeName | undefined,
    beforeName: NodeName | undefined,
    redirectFindAdd: boolean
  ): Thunk =>
  (dispatch, getState) => {
    const { projectGraph, graphEdits } = getState();
    const parent = getEditedAsset(parentName, projectGraph, graphEdits);
    const after = getEditedAsset(afterName, projectGraph, graphEdits);
    const before = getEditedAsset(beforeName, projectGraph, graphEdits);
    const doFind: Thunk = (dispatch, getState) => {
      const state = getState();
      const modalData: FindContentModalData = {
        parent,
        clone: (branch, names) =>
          addContent(
            dispatch,
            state,
            branch,
            names,
            parent,
            contextPath,
            after,
            before,
            redirectFindAdd,
            false
          ),
        link: (branch, names) =>
          addContent(
            dispatch,
            state,
            branch,
            names,
            parent,
            contextPath,
            after,
            before,
            redirectFindAdd,
            true
          ),
      };
      dispatch(openModal(ModalIds.FindContentModal, modalData));
    };
    dispatch(closePopoverAction());
    dispatch(confirmSaveProjectGraphEdits(doFind));
  };

const addAssetAction =
  (
    typeId: TypeId,
    parentName: NodeName,
    contextPath: string,
    afterName: NodeName | undefined,
    beforeName: NodeName | undefined,
    redirect: boolean
  ): Thunk =>
  (dispatch, getState) => {
    const {
      projectGraph,
      graphEdits,
      configuration: { translations: polyglot },
    } = getState();
    const parent = getEditedAsset(parentName, projectGraph, graphEdits);
    const after = getEditedAsset(afterName, projectGraph, graphEdits);
    const before = getEditedAsset(beforeName, projectGraph, graphEdits);
    const { branchId } = projectGraph;
    const group = childEdgeGroup(parent.typeId);
    const doAdd = () => {
      trackNarrativeAdd(typeId);
      const asset = addAsset(typeId, parent, after, before, group, polyglot, dispatch);
      if (redirect) {
        // suppress autosaving the new asset before navigating
        dispatch(push(editorUrl('story', branchId, asset, contextPath, { confirm: false })));
      }
      // no autosave because we'll autosave on blur
    };
    dispatch(closePopoverAction());
    if (redirect) {
      dispatch(narrativeSaveAndContinue(doAdd));
    } else {
      doAdd();
    }
  };

const addLtiToolAction =
  (
    tool: LtiToolApiResponse,
    parentName: NodeName,
    afterName: NodeName | undefined,
    beforeName: NodeName | undefined
  ): Thunk =>
  dispatch => {
    const launchAddTool: Thunk = (dispatch, getState) => {
      const { projectGraph, graphEdits } = getState();
      const { branchId } = projectGraph;
      const parent = getEditedAsset(parentName, projectGraph, graphEdits);
      const after = getEditedAsset(afterName, projectGraph, graphEdits);
      const before = getEditedAsset(beforeName, projectGraph, graphEdits);
      const query = qs.stringify({
        parent: parent.name,
        after: after?.name,
        before: before?.name,
      });
      document.location.href = `/api/v2/authoring/${branchId}/lti/${tool.id}/selectContent?${query}`;
    };
    dispatch(narrativeSaveAndContinue(launchAddTool));
  };

const onPasteAction =
  (
    parentName: NodeName,
    afterName: NodeName | undefined,
    beforeName: NodeName | undefined
  ): Thunk =>
  (dispatch, getState) => {
    const {
      projectGraph,
      graphEdits,
      story: { clipboard },
      configuration: { translations: polyglot },
    } = getState();
    const parent = getEditedAsset(parentName, projectGraph, graphEdits);
    const after = getEditedAsset(afterName, projectGraph, graphEdits);
    const before = getEditedAsset(beforeName, projectGraph, graphEdits);
    const group = childEdgeGroup(parent.typeId);
    trackNarrativePaste(clipboard?.typeId);
    addAsset(clipboard, parent, after, before, group, polyglot, dispatch);
    const copied = typeof clipboard === 'object' && isCopiedAsset(clipboard);
    dispatch(
      setNarrativeState(
        copied
          ? { addingTo: undefined }
          : {
              addingTo: undefined,
              clipboard: undefined,
              pasteboard: clipboard.name, // we don't clear this but there's no harm.
            }
      )
    );
    dispatch(autoSaveProjectGraphEdits());
  };

type AddAssetProps = {
  parent: NodeName;
  contextPath: string;
  after?: NodeName;
  before?: NodeName;
  className?: string;
  tooltip?: 'sibling' | 'parent';
  redirect?: boolean;
  redirectFindAdd?: boolean;
};

export const AddAsset: React.FC<AddAssetProps> = ({
  parent,
  contextPath,
  after,
  before,
  className,
  tooltip,
  redirect,
  redirectFindAdd,
}) => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const divRef = useRef<HTMLDivElement>();
  const buttonRef = useRef<HTMLButtonElement>();
  const id = `add-${parent}-${before}-${after}`;
  const parentTypeId = useEditedAssetTypeId(parent) ?? 'course.1';
  const group = childEdgeGroup(parentTypeId);
  const addingTo = useStorySelector(state => state.addingTo);
  const clipboard = useStorySelector(state => state.clipboard);
  const editMode = useIsEditable(parent, 'AddRemoveContent');
  const open = addingTo === id;
  const inline = useIsInlineNarrativeView();
  const supportedElements = supportedElementSets[parentTypeId] ?? EmptySet;
  const ltiTools = useLtiTools();

  useEffect(() => {
    if (open) {
      const close = () => dispatch(closePopoverAction());
      window.addEventListener('click', close, { once: true });
      return () => window.removeEventListener('click', close);
    }
  }, [open]);

  const onAdd = useCallback(
    (event: React.MouseEvent<HTMLButtonElement>) => {
      const typeId = event.currentTarget.getAttribute('data-type-id') as TypeId;
      dispatch(addAssetAction(typeId, parent, contextPath, after, before, redirect || !inline));
    },
    [parent, contextPath, after, before, redirect, inline]
  );

  const onAddLtiTool = useCallback(
    (event: React.MouseEvent<HTMLButtonElement>) => {
      const toolId = event.currentTarget.getAttribute('data-lti-tool');
      const tool = ltiTools.find(tool => toolId === tool.toolId);
      dispatch(addLtiToolAction(tool, parent, after, before));
    },
    [parent, after, before, redirect, inline]
  );

  const onPaste = useCallback(() => {
    dispatch(onPasteAction(parent, after, before));
  }, [parent, after, before, redirectFindAdd]);

  const onFind = useCallback(() => {
    dispatch(onFindAction(parent, contextPath, after, before, redirectFindAdd));
  }, [parent, contextPath, after, before, redirectFindAdd]);

  const toggleOpen = useCallback(
    (event: React.MouseEvent) => {
      event.stopPropagation();
      dispatch(setNarrativeState({ addingTo: open ? undefined : id }));
    },
    [open, id]
  );

  const addType =
    parentTypeId === 'course.1' ? 'lesson' : group === 'questions' ? 'question' : 'content';
  const addLoc = tooltip === 'parent' ? 'to' : before ? 'before' : 'after';
  const addName = useEditedAssetTitle(
    !tooltip ? undefined : tooltip === 'parent' ? parent : (before ?? after)
  );

  return (
    <div
      ref={divRef}
      className={classNames('add-content', className, { open }, editMode && 'edit-mode')}
    >
      <div className="rule" />
      <Button
        id={id}
        color="primary"
        className="plus"
        disabled={!editMode}
        onClick={toggleOpen}
        innerRef={buttonRef}
      >
        <AiOutlinePlus
          size="1.2rem"
          stroke="0px"
        />
      </Button>
      {buttonRef.current &&
        (open ? (
          <Popover
            isOpen
            target={buttonRef.current}
            placement="bottom"
            className="add-popover"
            innerClassName="add-palette"
            container={divRef.current}
            onClick={stopPropagation}
          >
            {addOptions
              .filter(({ typeId }) => supportedElements.has(typeId))
              .map(({ typeId, Icon }) => (
                <Button
                  key={typeId}
                  color="primary"
                  outline
                  data-type-id={typeId}
                  onClick={onAdd}
                >
                  <Icon
                    className={typeId.replace(/\..*/, '')}
                    size="1.75rem"
                  />
                  <div className="small">{storyTypeName(polyglot, typeId, false)}</div>
                </Button>
              ))}
            {ltiTools
              .filter(
                tool =>
                  supportedElements.has('lti.1') &&
                  tool.ltiConfiguration.defaultConfiguration.ltiVersion === 'LTI-1p3' &&
                  tool.ltiConfiguration.defaultConfiguration.deepLinkUrl
              )
              .map(tool => (
                <Button
                  key={tool.id}
                  color="primary"
                  outline
                  data-lti-tool={tool.toolId}
                  onClick={onAddLtiTool}
                >
                  <IoCubeOutline
                    className="ltiTool"
                    size="1.75rem"
                  />
                  <div className="small">{tool.name}</div>
                </Button>
              ))}
            <Button
              color="primary"
              outline
              onClick={onFind}
            >
              <IoSearchOutline size="1.75rem" />
              <div className="small">Find Content</div>
            </Button>
            <Button
              color="primary"
              outline
              onClick={onPaste}
              disabled={!clipboard || !supportedElements.has(clipboard.typeId)}
            >
              <ImPaste size="1.75rem" />
              <div className="small">Paste</div>
            </Button>
          </Popover>
        ) : tooltip ? (
          <UncontrolledTooltip
            delay={0}
            target={buttonRef.current}
            placement="bottom"
          >
            {`Add ${addType} ${addLoc} ${addName}`}
          </UncontrolledTooltip>
        ) : null)}
      <div className="rule" />
    </div>
  );
};

export const addAsset = (
  assetOrTypeId: TypeId | NewAsset<any>,
  parent: NewAsset<any>,
  after: NewAsset<any> | undefined,
  before: NewAsset<any> | undefined,
  group: EdgeGroup,
  polyglot: Polyglot,
  dispatch: ThunkDispatch<any, any, any>
): NewAsset<any> => {
  let addAsset: NewAsset<any>;
  if (typeof assetOrTypeId === 'string') {
    trackAuthoringEvent('Narrative Editor - Create Asset', assetOrTypeId);
    dispatch(beginProjectGraphEdit(`Add ${storyTypeName(polyglot, assetOrTypeId)}`));
    addAsset = {
      name: crypto.randomUUID(),
      typeId: assetOrTypeId,
      data: {
        title: 'Untitled',
        ...newAssetData(assetOrTypeId),
      },
    };
    dispatch(addProjectGraphNode(addAsset));
  } else if (isCopiedAsset(assetOrTypeId)) {
    trackAuthoringEvent('Narrative Editor - Clone Asset', assetOrTypeId.typeId);
    dispatch(beginProjectGraphEdit(`Paste ${storyTypeName(polyglot, assetOrTypeId.typeId)}`));
    addAsset = cloneAsset(assetOrTypeId, dispatch);
  } else {
    trackAuthoringEvent('Narrative Editor - Paste Asset', assetOrTypeId.typeId);
    dispatch(beginProjectGraphEdit(`Paste ${storyTypeName(polyglot, assetOrTypeId.typeId)}`));
    addAsset = assetOrTypeId;
  }
  dispatch(insertProjectGraphNode(addAsset.name, parent.name, group, after?.name, before?.name));
  dispatch(setNarrativeAssetState(addAsset.name, { created: true }));
  // *NO AUTOSAVE* because we'll save on title blur

  // I could delay this for the new asset case until the title blurs, so there's
  // no "Untitled" entry in the commit log, but squash should take care of that
  // in most cases so why bother. Undo of set title would be much harder were I
  // to do that.
  setTimeout(() => dispatch(setNarrativeAssetState(addAsset.name, { created: undefined })), 500);
  return addAsset;
};

/** Clone a previously "copied" asset into the graph edit store.
 *
 * @param original an asset from the graph editor store along with all its edges and
 * targets, captured at the time of copy.
 * @param dispatch the dispatcher
 * @return the new asset from the edit store
 */
export const cloneAsset = (
  original: CopiedAsset,
  dispatch: ThunkDispatch<any, any, any>,
  overrideTitle?: string
): NewAsset<any> => {
  const copies: Record<NodeName, NewAsset<any>> = {};
  const origins: Record<NodeName, CopiedAsset> = {};
  // First we deep-copy the structural nodes
  const copyAsset = (asset: CopiedAsset) => {
    if (copies[asset.name]) return; // reused assets :|
    const editTitle = asset === original && !isQuestion(asset.typeId);
    const title =
      overrideTitle ??
      (editTitle ? `Copy of ${asset.data.title}`.substring(0, 255) : asset.data.title);
    const copiedAsset: NewAsset<any> = {
      name: crypto.randomUUID(),
      typeId: asset.typeId,
      data: editTitle ? { ...asset.data, title } : asset.data,
    };
    copies[asset.name] = copiedAsset;
    origins[copiedAsset.name] = asset;
    dispatch(addProjectGraphNode(copiedAsset));
    for (const targets of Object.values(asset.edgeTargets)) {
      for (const target of targets) {
        if (target.edgeTraverse) {
          copyAsset(target);
        }
      }
    }
  };
  copyAsset(original);
  // Next we add edges, which may be to global competencies or copied nodes (incl gates, hyperlinks)
  for (const copiedAsset of Object.values(copies)) {
    const asset = origins[copiedAsset.name];
    for (const group of Object.keys(asset.edgeTargets) as EdgeGroup[]) {
      for (const target of asset.edgeTargets[group]) {
        const copiedTarget = copies[target.name] ?? target;
        const edge: NewEdge = {
          name: crypto.randomUUID(),
          sourceName: copiedAsset.name,
          targetName: copiedTarget.name,
          group,
          traverse: !!target.edgeTraverse,
          data: target.edgeData,
          edgeId: target.edgeId,
          newPosition: 'end',
        };
        dispatch(addProjectGraphEdge(edge));
      }
    }
  }
  const originNames = mapValues(origins, a => a.name);
  dispatch(setProjectGraphOrigins(originNames));
  return copies[original.name];
};

type AddOption = {
  typeId: TypeId;
  Icon: IconType;
};

export const addOptions: Array<AddOption> = [
  { typeId: 'course.1', Icon: IoSchoolOutline },
  { typeId: 'unit.1', Icon: IoCubeOutline },
  { typeId: 'module.1', Icon: IoNewspaperOutline },
  { typeId: 'lesson.1', Icon: IoBookOutline },
  { typeId: 'html.1', Icon: IoDocumentTextOutline },
  { typeId: 'discussion.1', Icon: IoChatbubblesOutline },
  { typeId: 'assessment.1', Icon: TbListCheck },
  { typeId: 'assignment.1', Icon: IoClipboardOutline },
  { typeId: 'observationAssessment.1', Icon: ObservationIcon },
  { typeId: 'courseLink.1', Icon: IoSchoolOutline },
  { typeId: 'diagnostic.1', Icon: IoPulseOutline },
  { typeId: 'poolAssessment.1', Icon: BiWater },
  { typeId: 'checkpoint.1', Icon: IoCheckboxOutline },
  { typeId: 'lti.1', Icon: IoGlobeOutline },
  { typeId: 'scorm.1', Icon: IoBriefcaseOutline },
  { typeId: 'essayQuestion.1', Icon: AiOutlineQuestionCircle },
  { typeId: 'fillInTheBlankQuestion.1', Icon: AiOutlineQuestionCircle },
  { typeId: 'matchingQuestion.1', Icon: AiOutlineQuestionCircle },
  { typeId: 'multipleChoiceQuestion.1', Icon: AiOutlineQuestionCircle },
  { typeId: 'multipleSelectQuestion.1', Icon: AiOutlineQuestionCircle },
  { typeId: 'trueFalseQuestion.1', Icon: AiOutlineQuestionCircle },
  { typeId: 'surveyEssayQuestion.1', Icon: AiOutlineQuestionCircle },
  { typeId: 'likertScaleQuestion.1', Icon: AiOutlineQuestionCircle },
  { typeId: 'surveyChoiceQuestion.1', Icon: AiOutlineQuestionCircle },
  { typeId: 'ratingScaleQuestion.1', Icon: AiOutlineQuestionCircle },
  { typeId: 'rubric.1', Icon: AiOutlineSolution },
  { typeId: 'survey.1', Icon: MdInsights },
  { typeId: 'image.1', Icon: PiFileImage },
  { typeId: 'audio.1', Icon: PiFileAudio },
  { typeId: 'video.1', Icon: PiFileVideo },
  { typeId: 'pdf.1', Icon: PiFilePdf },
  { typeId: 'file.1', Icon: PiFile },
  { typeId: 'webDependency.1', Icon: TbWorldWww },
];

export const addIcons = addOptions.reduce<Record<string, IconType>>(
  (o, a) => ({ ...o, [a.typeId]: a.Icon }),
  {}
);

export const isContainer = (typeId?: TypeId) =>
  typeId === 'unit.1' || typeId === 'module.1' || typeId === 'lesson.1';

export const getIcon = (typeId?: TypeId) => addIcons[typeId] ?? IoSettingsOutline;
