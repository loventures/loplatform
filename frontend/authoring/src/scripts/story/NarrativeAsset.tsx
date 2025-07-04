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
import { kebabCase } from 'lodash';
import React, { useEffect, useRef, useState } from 'react';
import { useCollapse } from 'react-collapsed';
import { RiKeyLine } from 'react-icons/ri';
import { useDispatch } from 'react-redux';
import VisibilitySensor from 'react-visibility-sensor';
import { Spinner } from 'reactstrap';
import { useDebouncedCallback } from 'use-debounce';

import QUESTION_TYPES from '../asset/constants/questionTypes.constants';
import { accessRightI18n, accessRightsMap } from '../components/AccessRightEditor';
import edgeRuleConstants, { CONTAINER_AND_ELEMENT_TYPES } from '../editor/EdgeRuleConstants';
import FeedbackContentHeader from '../feedback/FeedbackContentHeader';
import { useAddingFeedback, useFeedbackOpen } from '../feedback/feedbackHooks'; // defaults to GenericEditor that lets you edit instructions
import { FeedbackSection } from '../feedback/FeedbackSection';
import {
  useAllEditedOutEdges,
  useEditedAssetDatum,
  useEditedAssetTitle,
  useEditedAssetTypeId,
} from '../graphEdit';
import { useDcmSelector, usePolyglot } from '../hooks';
import { useProjectGraph } from '../structurePanel/projectGraphActions';
import { NodeName } from '../types/asset';
import { ActionMenu } from './ActionMenu';
import { AddAsset } from './AddAsset';
import { AlignmentEditor } from './AlignmentEditor';
import { ContentStatusPill } from './badges/ContentStatusPill';
import { Stornado } from './badges/Stornado';
import { RubricEditor } from './editors';
import { KeywordsEditor } from './editors/KeywordsEditor';
import { GateEditor } from './GateEditor';
import { GateInfo } from './GateInfo';
import { useContentAccess } from './hooks/useContentAccess';
import { InlineTitle } from './NarrativeAsset/InlineTitle';
import { Jaggies } from './NarrativeAsset/Jaggies';
import { NarrativeEditorProxy } from './NarrativeAsset/NarrativeEditorProxy';
import NarrativePresence from './NarrativeAsset/NarrativePresence';
import { NarrativeSettingsProxy } from './NarrativeAsset/NarrativeSettingsProxy';
import { RevisionTitle } from './NarrativeAsset/RevisionTitle';
import { NarrativeIndex } from './NarrativeIndex';
import { PageNub } from './PageNub';
import { PageNumber } from './PageNumber';
import { PreviewMenu } from './PreviewMenu';
import { QuestionTitle } from './QuestionTitle';
import { isQuestion } from './questionUtil';
import {
  childEdgeGroup,
  HideyHeaderHeight,
  NarrativeMode,
  primitiveTypes,
  scrollBottomIntoView,
  scrollTopIntoView,
  StickyHeaderHeight,
  storyTypeName,
} from './story';
import { setNarrativeActive, setNarrativeAssetState, setNarrativeState } from './storyActions';
import {
  useEditedServeUrl,
  useNarrativeAssetState,
  useRevisionCommit,
  useStorySelector,
} from './storyHooks';
import { TitleEditor } from './TitleEditor';

const learningPathTypes = new Set([...CONTAINER_AND_ELEMENT_TYPES, ...QUESTION_TYPES]);

export const NarrativeAsset: React.FC<{
  name: NodeName;
  contextPath: string | undefined;
  mode: NarrativeMode;
  bottom?: boolean;
  top?: boolean;
}> = ({ name, contextPath, mode, bottom = true, top = true }) => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const typeId = useEditedAssetTypeId(name);
  const archived = useEditedAssetDatum(name, data => data.archived);
  const prefix = kebabCase(typeId.replace(/\..*/, '')); // lesson, essay-question
  const question = isQuestion(typeId);
  const edgeGroup = childEdgeGroup(typeId);
  const edgeRules = edgeRuleConstants[typeId];
  const childBearing = !!edgeRules?.[edgeGroup];
  const edges = useAllEditedOutEdges(name);
  const firstChild = edges.find(edge => edge.group === edgeGroup);
  const {
    created,
    deleted,
    collapsed,
    previewing,
    expanded: expanded0,
    invalid,
  } = useNarrativeAssetState(name);
  const inlineView = useStorySelector(state => state.inlineView);
  const scrollTo = useStorySelector(state => state.scrollTo);
  const expanded = expanded0 && mode !== 'feedback' && inlineView;
  const feedbackOpen = useFeedbackOpen();
  const feedbackAdding = useAddingFeedback();
  const inlineFeedback =
    feedbackOpen && inlineView && (mode === 'apex' ? childBearing : mode === 'inline');
  const commit = useRevisionCommit();
  const primitive = primitiveTypes.has(typeId);
  const io = useDcmSelector(state =>
    state.graphEdits.saving ? 'success' : state.projectStructure.isFetching ? 'warning' : undefined
  );

  const projectGraph = useProjectGraph();

  const contextNames = contextPath?.split('.') ?? [];
  const parentName = contextNames[contextNames.length - 1];
  const fullPath = contextPath ? `${contextPath}.${name}` : name;

  const innerRef = useRef<HTMLDivElement>();

  // scroll all onscreen if just created inline
  useEffect(() => {
    if (created && mode === 'inline') {
      setTimeout(() => scrollBottomIntoView(innerRef.current), 0);
    } else if (scrollTo === fullPath && top && mode === 'inline') {
      // This is used when we use the "Expand Parent" feature which expands parent
      // and scrolls to current. When there are a lot of pages there's a lot of
      // jank so we have to go to great efforts to scroll to the current page.
      // This is all horseshit. If we used non-anchor routing then I could set
      // the URL anchor to this element and the browser would scroll anchor to it.
      let offsetTop = innerRef.current?.getBoundingClientRect().top;
      let stable = 0;
      const interval = setInterval(() => {
        const newOffset = innerRef.current?.getBoundingClientRect().top;
        if (!innerRef.current) {
          clearInterval(interval);
        } else if (newOffset === offsetTop) {
          if (!stable) scrollBottomIntoView(innerRef.current, 'auto');
          ++stable;
          if (stable >= 2) clearInterval(interval);
        } else {
          offsetTop = newOffset;
          stable = 0;
        }
      }, 100);
      return () => clearInterval(interval);
    }
  }, [mode, created, scrollTo, fullPath, innerRef, top]);

  // stop trying to scroll to this asset after .5s
  useEffect(() => {
    if (scrollTo === fullPath) {
      const timeout = setTimeout(() => dispatch(setNarrativeState({ scrollTo: undefined })), 500);
      return () => clearTimeout(timeout);
    }
  }, [scrollTo, fullPath]);

  // drop collapsed style after .5s
  useEffect(() => {
    if (collapsed)
      setTimeout(() => dispatch(setNarrativeAssetState(name, { collapsed: undefined })), 500);
  }, [collapsed]);

  // scroll the top back on screen when children are collapsed
  const [wasExpanded, setWasExpanded] = useState(expanded);
  useEffect(() => {
    if (expanded !== wasExpanded) {
      setWasExpanded(expanded);
      if (top && !expanded) {
        scrollTopIntoView(innerRef.current);
      }
    }
  }, [top, expanded, wasExpanded, setWasExpanded, innerRef]);

  const { getCollapseProps } = useCollapse({
    defaultExpanded: false,
    isExpanded: feedbackOpen || !!feedbackAdding,
  });

  const assetTitle = useEditedAssetTitle(top ? undefined : name);
  const accessRight = useEditedAssetDatum(name, data => data.accessRight);
  const contentAccess = useContentAccess(name);
  const isSurvey = typeId === 'survey.1';
  const NoAccess = !contentAccess.ViewContent && typeId !== 'course.1';

  const [visible, setVisible] = useState(mode !== 'inline'); // visible

  const setDelta = useDebouncedCallback(
    (name: string, contextPath: string, delta: number) =>
      dispatch(setNarrativeActive(name, contextPath, delta)),
    100,
    { maxWait: 500 }
  );

  const trackOffset = visible && inlineView && top;
  useEffect(() => {
    // if you're in a rubric or resource... but not yet a survey question
    let activeName = name,
      activeContextPath = contextPath;
    if (contextPath && !learningPathTypes.has(typeId)) {
      const index = contextPath.lastIndexOf('.');
      activeName = contextPath.substring(1 + index);
      activeContextPath = index < 0 ? '' : contextPath.substring(0, index);
    }
    if (!inlineView) {
      dispatch(setNarrativeActive(activeName, activeContextPath, 0));
      return () => {
        dispatch(setNarrativeActive(activeName, activeContextPath, undefined));
      };
    } else if (trackOffset) {
      const listener = () => {
        const rect = innerRef.current?.getBoundingClientRect();
        if (mode === 'apex' && window.scrollY <= HideyHeaderHeight) {
          setDelta(name, contextPath, 0);
        } else if (rect) {
          const windowHeight = window.innerHeight;
          const top = Math.max(StickyHeaderHeight, rect.top);
          const bottom = Math.min(windowHeight, rect.bottom);
          const delta = Math.abs((window.innerHeight + StickyHeaderHeight - top - bottom) / 2);
          setDelta(name, contextPath, delta);
        }
      };
      listener();
      document.addEventListener('scroll', listener);
      return () => {
        setDelta.cancel();
        dispatch(setNarrativeActive(activeName, activeContextPath, undefined));
        document.removeEventListener('scroll', listener);
      };
    }
  }, [trackOffset, name, contextPath, typeId, mode, inlineView]);

  useEffect(() => {
    if (!top) return;
    const messageListener = (e: MessageEvent) => {
      if (e.data?.fn === 'highlightElement' && e.data?.name === name && innerRef.current) {
        const els = innerRef.current.querySelectorAll(`[data-id="${e.data.id}"]`);
        els.forEach(el => {
          // Summernote has two copies of the content, only one is visible
          if (!el.closest('.naked-html')) {
            el.scrollIntoView({ behavior: 'auto', block: 'center', inline: 'center' });
            el.classList.add('highlit');
            setTimeout(() => el.classList.remove('highlit'), 1000);
          }
        });
      }
    };
    window.addEventListener('message', messageListener);
    return () => {
      window.removeEventListener('message', messageListener);
    };
  }, [name, top]);

  const gateType = edgeRules?.gates ? 'gates' : edgeRules?.testsOut ? 'testsOut' : undefined;

  const imageEdge = edges.find(edge => edge.group === 'image');
  const bannerUrl = useEditedServeUrl(imageEdge?.targetName);

  return (
    <div className="d-flex">
      <VisibilitySensor
        active={(mode === 'inline' || !bottom) && typeId !== 'course.1' && top}
        onChange={visible => setVisible(visible)}
        partialVisibility
        minTopValue={200}
      >
        <div
          id={top ? `asset_${fullPath}` : undefined}
          className={classNames('container narrative-container', previewing && 'wide-container')}
          data-path={fullPath}
        >
          {!top && (
            <div className="jag-top">
              <Jaggies
                name={name}
                typeId={typeId}
                mode={mode}
              />
            </div>
          )}
          <div
            ref={innerRef}
            className={classNames(
              question && 'question-element',
              `${prefix}-element`,
              'story-element',
              'position-relative',
              !top && 'topless',
              !bottom && 'bottomless',
              mode,
              {
                expanded: false,
                created,
                deleted,
                collapsed,
                invalid,
                previewing,
              }
            )}
          >
            {top ? (
              <>
                <div className="title-grid">
                  <div className="position-relative">
                    <ActionMenu
                      name={name}
                      typeId={typeId}
                      contextPath={contextPath}
                      mode={mode}
                    />
                    {mode === 'apex' && io && (
                      <Spinner
                        id="graph-edit-fetching"
                        size="sm"
                        color={io}
                        type="grow"
                      />
                    )}
                  </div>

                  {mode === 'feedback' ? (
                    <FeedbackContentHeader />
                  ) : mode === 'revision' || (mode === 'apex' && commit) ? (
                    <RevisionTitle
                      name={name}
                      typeId={typeId}
                      mode={mode}
                      commit={commit}
                    />
                  ) : question ? (
                    <QuestionTitle
                      name={name}
                      typeId={typeId}
                      contextPath={contextPath}
                      mode={mode}
                      commit={commit}
                    />
                  ) : mode === 'inline' ? (
                    <InlineTitle
                      name={name}
                      typeId={typeId}
                      contextPath={contextPath}
                      commit={commit}
                    />
                  ) : (
                    <div className="d-flex align-items-center justify-content-center minw-0 text-muted asset-type overflow-hidden">
                      {storyTypeName(polyglot, typeId)}
                      <div
                        style={{ width: 0 }}
                        className="text-nowrap"
                      >
                        <Stornado name={name} />
                        {accessRight && (
                          <RiKeyLine
                            className={classNames(
                              'access-restricted ms-2',
                              'access-' + accessRightsMap[accessRight].toLowerCase()
                            )}
                            style={{ verticalAlign: '-.125rem' }}
                            title={polyglot.t(accessRightI18n(accessRight))}
                          />
                        )}
                        <ContentStatusPill
                          name={name}
                          effective
                        />
                      </div>
                    </div>
                  )}
                  <NarrativePresence name={name}>
                    <PreviewMenu
                      name={name}
                      typeId={typeId}
                      contextPath={contextPath}
                      mode={mode}
                    />
                  </NarrativePresence>
                </div>

                {!question && mode !== 'feedback' ? (
                  <TitleEditor
                    name={name}
                    typeId={typeId}
                    tag={mode === 'apex' ? 'h1' : 'h2'}
                    className={classNames("mt-3", mode === 'apex' ? 'h2' : 'h3')}
                    readOnly={archived}
                    bannerUrl={bannerUrl}
                  />
                ) : mode === 'feedback' ? (
                  <QuestionTitle
                    name={name}
                    typeId={typeId}
                    contextPath={contextPath}
                    mode={mode}
                    commit={commit}
                    className="my-3"
                  />
                ) : question ? (
                  <>
                    <KeywordsEditor name={name} />
                    <div style={{ height: '1rem' }} />
                  </>
                ) : (
                  <div style={{ height: '1rem' }} />
                )}

                {archived ? (
                  <div className="text-danger text-center my-5">This asset has been removed.</div>
                ) : NoAccess ? (
                  <div className="text-danger text-center my-5">
                    Your rôle does not currently have access to this content.
                  </div>
                ) : (
                  <>
                    <GateInfo name={name} />

                    {!question && (
                      <>
                        <NarrativeSettingsProxy
                          name={name}
                          typeId={typeId}
                        />
                        <AlignmentEditor
                          name={name}
                          typeId={typeId}
                        />
                      </>
                    )}

                    <NarrativeEditorProxy
                      name={name}
                      typeId={typeId}
                      contextPath={contextPath}
                      mode={mode}
                    />

                    {(edgeRules?.rubric || edgeRules?.cblRubric) && (
                      <RubricEditor
                        name={name}
                        typeId={typeId}
                        projectGraph={projectGraph}
                      />
                    )}

                    {question && (
                      <AlignmentEditor
                        name={name}
                        typeId={typeId}
                      />
                    )}

                    {gateType && (
                      <GateEditor
                        name={name}
                        typeId={typeId}
                        group={gateType}
                      />
                    )}

                    {childBearing && (mode === 'feedback' || mode === 'revision' || !expanded) ? (
                      <NarrativeIndex
                        name={name}
                        typeId={typeId}
                        contextPath={contextPath}
                        mode={mode}
                      />
                    ) : null}
                  </>
                )}
              </>
            ) : (
              <div className="page-number my-4 text-danger">End of {assetTitle}.</div>
            )}

            {bottom && !isSurvey && (
              <PageNumber
                name={name}
                typeId={typeId}
                parentName={parentName}
                contextPath={contextPath}
                mode={mode}
              />
            )}
            {mode === 'apex' && !isSurvey && top && (
              <PageNub
                name={name}
                contextPath={contextPath}
                parentName={parentName}
              />
            )}
          </div>
          {!bottom && (
            <div className="jag-bottom">
              <Jaggies
                name={name}
                typeId={typeId}
                mode={mode}
              />
            </div>
          )}

          {mode === 'apex' && bottom && parentName && !isSurvey && !primitive ? (
            <AddAsset
              tooltip={question ? undefined : 'sibling'}
              parent={parentName}
              contextPath={contextPath}
              after={name}
              redirect
              className="mb-3"
            />
          ) : firstChild && !bottom ? (
            <AddAsset
              parent={name}
              contextPath={contextPath}
              before={firstChild?.targetName}
              className="mb-3"
            />
          ) : mode === 'inline' && bottom ? (
            <AddAsset
              parent={parentName}
              contextPath={contextPath}
              after={name}
              className="mb-3"
            />
          ) : null}
        </div>
      </VisibilitySensor>
      {inlineFeedback && (
        <div
          className="flex-shrink-0 panel-sections feedback-width"
          {...getCollapseProps()}
        >
          <div className="feedback-width px-3 pb-3">
            {feedbackOpen && top && (
              <FeedbackSection
                name={name}
                narrative
                expanded={expanded}
              />
            )}
          </div>
        </div>
      )}
    </div>
  );
};
