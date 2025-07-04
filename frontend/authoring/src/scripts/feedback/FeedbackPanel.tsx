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
import { uniq } from 'lodash';
import * as React from 'react';
import { useCallback, useEffect, useState } from 'react';
import { useCollapse } from 'react-collapsed';
import { BsPencil } from 'react-icons/bs';
import { CiViewList } from 'react-icons/ci';
import { IoAdd, IoSearchOutline, IoTrashOutline } from 'react-icons/io5';
import { RiChatNewLine } from 'react-icons/ri';
import { TfiClose } from 'react-icons/tfi';
import { useDispatch } from 'react-redux';
import { Link } from 'react-router-dom';
import { Button } from 'reactstrap';

import { SurveyFeedbackComponent } from '../assetSidePanel/SurveyFeedbackPanelBody';
import edgeRuleConstants from '../editor/EdgeRuleConstants';
import {
  useAllEditedOutEdges,
  useCurrentAssetName,
  useCurrentContextPath,
  useEditedAssetTitle,
  useEditedAssetTypeId,
} from '../graphEdit';
import { useBranchId } from '../hooks';
import { useProjectAccess } from '../story/hooks';
import { useContentAccess } from '../story/hooks/useContentAccess';
import { editorUrl } from '../story/story';
import { useIsInlineNarrativeView, useStorySelector } from '../story/storyHooks';
import AddFeedback from './AddFeedback';
import { useIsScrolled } from './feedback';
import { setAddFeedbackForAsset, toggleFeedbackOpen } from './feedbackActions';
import { FeedbackComponent } from './FeedbackComponent';
import FeedbackDetailNav from './FeedbackDetailNav';
import { FeedbackFilters } from './FeedbackFilters';
import {
  useAddFeedback,
  useCurrentFeedback,
  useFeedbackCount,
  useFeedbackMode,
  useFeedbackOpen,
} from './feedbackHooks';
import { addSurveyAction, findSurveyAction, removeSurveyAction } from './FeedbackPanel/actions';
import { FeedbackSideButton } from './FeedbackPanel/FeedbackSideButton';
import { SurveySideButton } from './FeedbackPanel/SurveySideButton';
import { FeedbackSection } from './FeedbackSection';

const FeedbackPanel: React.FC<{ narrative: boolean; detail: boolean }> = ({
  detail,
  narrative,
}) => {
  const name = useCurrentAssetName();
  const contextPath = useCurrentContextPath();
  const dispatch = useDispatch();
  const feedbackOpen = useFeedbackOpen();
  const mode = useFeedbackMode();
  const addFeedback = useAddFeedback();
  const feedback = useCurrentFeedback();
  const typeId = useEditedAssetTypeId(name);
  const branchId = useBranchId();
  const subcontextPath = contextPath ? `${contextPath}.${name}` : name;
  const contentAccess = useContentAccess(name);
  const projectAccess = useProjectAccess();

  const allEdges = useAllEditedOutEdges(name);
  const survey = allEdges.find(edge => edge.group === 'survey')?.targetName;
  const surveyTitle = useEditedAssetTitle(survey);
  const surveyable = !!edgeRuleConstants[typeId]?.survey;
  const rubric = allEdges.find(edge => edge.group === 'cblRubric')?.targetName;

  const sidebarOpen = feedbackOpen || !!addFeedback;
  // hold the adder around during the close animation.
  const add = !!addFeedback;
  const [hold, setHold] = useState(add);
  useEffect(() => setHold(h => (h || add) && sidebarOpen), [add, sidebarOpen]);
  const { getCollapseProps } = useCollapse({
    defaultExpanded: false,
    isExpanded: add,
    onTransitionStateChange: state => {
      if (state === 'collapseEnd') setHold(false);
    },
  });
  const inline = useIsInlineNarrativeView() && narrative;

  // Not a useRef because https://tkdodo.eu/blog/avoiding-use-effect-with-callback-refs
  // useRef across things doesn't trigger things so useIsScrolled just doesn't work
  // because it is called once with the null ref.
  const [divRef, setDivRef] = useState<HTMLDivElement | null>();
  const scrolled = useIsScrolled(inline ? document : divRef);

  const rubricCount = useFeedbackCount(rubric);
  const count = useFeedbackCount(name, inline) + (inline ? 0 : rubricCount);

  const onAddSurvey = useCallback(
    () => dispatch(addSurveyAction(name, subcontextPath)),
    [name, subcontextPath]
  );

  const onFindSurvey = useCallback(
    () => dispatch(findSurveyAction(name, contextPath)),
    [name, contextPath]
  );

  const onRemoveSurvey = useCallback(() => dispatch(removeSurveyAction(name)), [name]);

  const activeAssetPath =
    useStorySelector(({ activeNode, activeContextPath }) =>
      activeContextPath ? `${activeContextPath}.${activeNode}` : activeNode
    ) ?? subcontextPath;

  const onAddFeedback = useCallback(() => {
    const assetPath = feedback
      ? uniq(
          [
            feedback.unitName,
            feedback.moduleName,
            feedback.lessonName,
            feedback.contentName,
            feedback.assetName,
          ].filter(n => n)
        )
      : activeAssetPath.split('.');
    dispatch(setAddFeedbackForAsset(assetPath));
  }, [feedback, activeAssetPath]);

  // hide feedback if you're viewing survey data and navigate to unsurveyable content,
  // or you're viewing feedback and navigate to
  useEffect(() => {
    if (
      mode === 'survey'
        ? !narrative || !surveyable || !contentAccess.ViewSurvey
        : !contentAccess.ViewFeedback
    )
      dispatch(toggleFeedbackOpen(false));
  }, [mode, narrative, surveyable, contentAccess]);

  return (
    <div
      id="feedback-panel"
      className={classNames(
        'grid-feedback',
        narrative && 'narrative',
        inline && 'inline',
        detail && 'detail',
        !narrative && !detail && 'structural'
      )}
    >
      {!sidebarOpen ? (
        <>
          <FeedbackSideButton
            nice={narrative}
            count={count}
            disabled={!contentAccess.ViewFeedback}
          />
          {narrative && (
            <SurveySideButton
              surveyed={!!survey}
              disabled={!contentAccess.ViewSurvey || !surveyable}
            />
          )}
        </>
      ) : (
        <div
          className="inner"
          ref={setDivRef}
        >
          <div
            className={classNames(
              'feedback-header p-3 d-flex align-items-center justify-content-between',
              { scrolled }
            )}
          >
            {detail ? (
              <>
                <div>
                  <Button
                    color="light"
                    className={classNames(
                      'border-0 me-3 add-button d-flex align-content-center',
                      inline || !contentAccess.AddFeedback ? 'text-muted' : 'text-primary'
                    )}
                    style={{ padding: '.4rem' }}
                    onClick={onAddFeedback}
                    title="Add Feedback"
                    disabled={inline || !contentAccess.AddFeedback}
                  >
                    <RiChatNewLine
                      aria-hidden={true}
                      size="1rem"
                    />
                  </Button>
                </div>
                <FeedbackDetailNav feedback={feedback?.id} />
              </>
            ) : (
              <>
                <div className="d-flex me-3 gap-1">
                  <Button
                    color="light"
                    className="border-0 close-button d-flex align-content-center"
                    style={{ padding: '.4rem' }}
                    onClick={() => dispatch(toggleFeedbackOpen(false))}
                    title="Close"
                  >
                    <TfiClose
                      aria-hidden={true}
                      size="1rem"
                    />
                  </Button>
                  {feedbackOpen && !narrative ? (
                    <Button
                      color="primary"
                      size="sm"
                      className="px-2 py-1"
                      onClick={onAddFeedback}
                      disabled={!contentAccess.AddFeedback}
                    >
                      Add Feedback
                    </Button>
                  ) : feedbackOpen && !mode ? (
                    <>
                      <Button
                        color="light"
                        className={classNames(
                          'border-0 add-button d-flex align-content-center',
                          !contentAccess.AddFeedback ? 'text-muted' : 'text-primary'
                        )}
                        style={{ padding: '.4rem' }}
                        onClick={onAddFeedback}
                        title="Add Feedback"
                        disabled={!contentAccess.AddFeedback}
                      >
                        <RiChatNewLine
                          aria-hidden={true}
                          size="1rem"
                        />
                      </Button>
                      <Link
                        color="light"
                        className={classNames(
                          'btn btn-light border-0 me-3 add-button d-flex align-content-center',
                          !projectAccess.FeedbackApp ? 'text-muted disabled' : 'text-primary'
                        )}
                        style={{ padding: '.3rem' }}
                        title="Feedback App"
                        to={`/branch/${branchId}/feedback`}
                      >
                        <CiViewList
                          aria-hidden={true}
                          size="1.2rem"
                          style={{ strokeWidth: '.4px' }}
                        />
                      </Link>
                    </>
                  ) : null}
                </div>
                {mode === 'survey' ? (
                  survey ? (
                    <>
                      <div
                        className="flex-grow-1 text-truncate"
                        style={{ fontSize: '1.1rem' }}
                      >
                        {surveyTitle}
                      </div>
                      <Link
                        key="edit-survey"
                        className={classNames(
                          'btn btn-outline-primary border-0 ms-2 d-flex',
                          !contentAccess.EditSurvey && 'disabled'
                        )}
                        style={{ lineHeight: 1, padding: '.4rem' }}
                        to={
                          contentAccess.EditSurvey
                            ? editorUrl('story', branchId, survey, subcontextPath)
                            : ''
                        }
                        title="Edit Survey"
                      >
                        <BsPencil
                          aria-hidden={true}
                          size="1rem"
                        />
                      </Link>
                      <Button
                        key="remove-survey"
                        color="danger"
                        outline
                        className="border-0 ms-2 d-flex"
                        style={{ lineHeight: 1, padding: '.4rem' }}
                        onClick={onRemoveSurvey}
                        disabled={!contentAccess.EditSurvey}
                        title="Remove Survey"
                      >
                        <IoTrashOutline
                          aria-hidden={true}
                          size="1rem"
                        />
                      </Button>
                    </>
                  ) : (
                    <>
                      <div className="flex-grow-1"></div>
                      <Button
                        key="find-survey"
                        outline
                        color="primary"
                        className="border-0 ms-2 d-flex find-survey-btn"
                        style={{ lineHeight: 1, padding: '.4rem' }}
                        onClick={onFindSurvey}
                        title="Find Survey"
                        disabled={!contentAccess.EditSurvey}
                      >
                        <IoSearchOutline
                          aria-hidden={true}
                          size="1rem"
                        />
                      </Button>
                      <Button
                        key="add-survey"
                        outline
                        color="primary"
                        className="border-0 ms-2 d-flex add-survey-btn"
                        style={{ lineHeight: 1, padding: '.4rem' }}
                        onClick={onAddSurvey}
                        title="Add Survey"
                        disabled={!contentAccess.EditSurvey}
                      >
                        <IoAdd
                          aria-hidden={true}
                          size="1rem"
                        />
                      </Button>
                    </>
                  )
                ) : feedbackOpen ? (
                  <div
                    className="feedback-filters d-flex"
                    style={{ gap: '.5rem' }}
                  >
                    <FeedbackFilters right />
                  </div>
                ) : null}
              </>
            )}
          </div>
          <div className={classNames('add-feedback-container', { add, hold })}>
            <div {...getCollapseProps()}>{(add || hold) && <AddFeedback />}</div>
          </div>
          <div className="panel-body">
            {feedbackOpen && (
              <div className="panel-sections">
                {mode === 'survey' ? (
                  <SurveyFeedbackComponent
                    node={name}
                    surveyAssetName={survey}
                    narrative
                  />
                ) : detail ? (
                  feedback ? (
                    <FeedbackComponent
                      feedback={feedback}
                      feedbackPage
                    />
                  ) : null
                ) : name && !inline ? (
                  <FeedbackSection name={name} />
                ) : null}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default FeedbackPanel;
