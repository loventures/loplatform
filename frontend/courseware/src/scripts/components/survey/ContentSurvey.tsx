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

import { createJumperSelector } from '../../discussion/selectors';
import classNames from 'classnames';
import { SurveyQuestionResponse, postSurveyResponse } from '../../api/surveyApi';
import {
  Survey as SurveyObj,
  fetchKeyedSurveyAction,
  modifySurveyAction,
} from '../../components/survey/contentSurveyReducer';
import SurveyQuestion from '../../components/survey/SurveyQuestion';
import { ConnectedLoader } from '../../instructorPages/assignments/ConnectedLoader';
import { CourseState, useCourseSelector } from '../../loRedux';
import { loaded as loadeder, loading, sequenceObj } from '../../types/loadable';
import {
  trackSurveyInlineInteraction,
  trackSurveySidePanelInteraction,
  trackSurveySubmitInteraction,
} from '../../analytics/trackEvents';
import { ContentWithNebulousDetails } from '../../courseContentModule/selectors/contentEntry';
import { TranslationContext } from '../../i18n/translationContext';
import { timeoutEffect } from '../../utilities/effectUtils';
import { selectActualUser } from '../../utilities/rootSelectors';
import React, { useContext, useEffect, useMemo, useRef, useState } from 'react';
import { IoCloseOutline } from 'react-icons/io5';
import { Alert, Button, Form } from 'reactstrap';
import { Dispatch } from 'redux';

type QuestionId = string;
type Response = string;
type SubmitState = 'initial' | 'touched' | 'submitted' | 'error';

function isErrorState(s: SubmitState): s is 'error' {
  return s === 'error';
}

function isSurvey(loadedSurvey: boolean | SurveyObj): loadedSurvey is SurveyObj {
  return typeof loadedSurvey !== 'boolean';
}

function shouldRenderSurvey(loadedSurvey: boolean | SurveyObj): loadedSurvey is SurveyObj {
  if (isSurvey(loadedSurvey)) {
    return loadedSurvey.questions.length > 0 && !loadedSurvey.disabled;
  } else {
    return false;
  }
}

interface SurveyProps {
  survey: SurveyObj;
  contentId: string;
  sectionId: number;
  dispatch: Dispatch;
  content: ContentWithNebulousDetails;
  loaded?: boolean;
}

type SubmittedCallback = (submitted: boolean) => void;

const Survey: React.FC<SurveyProps> = ({
  survey,
  contentId,
  sectionId,
  dispatch,
  content: { id, typeId, progress },
  loaded,
}) => {
  const translate = useContext(TranslationContext);
  const visibilitySensor = useRef<HTMLDivElement | null>(null);
  const [sensorVisible, setSensorVisible] = useState(false);
  const actualUser = useCourseSelector(selectActualUser);
  const isPreviewing = actualUser.user_type === 'Preview';

  const [showPanel, setShowPanel] = useState<boolean>(false);
  const [responses, setResponses] = useState<Record<QuestionId, Response>>({});
  const [submitted, setSubmitted] = useState<SubmitState>('initial');
  const [error, setError] = useState<string | null>(null);
  const [callback, setCallback] = useState<SubmittedCallback | undefined>(undefined);

  // Discussions don't grant completion so for them we need to select jumpbar state
  // to find out your personal post count, so any post grants survey. This code is
  // weird because we can't conditionally run a selector.
  const complete = useCourseSelector(state =>
    typeId === 'discussion.1'
      ? !!(createJumperSelector(id, 'user-posts')(state) as any).totalCount
      : progress?.total > 0 && progress.completions >= progress.total
  );

  // track visibility of sensor
  useEffect(() => {
    if (visibilitySensor.current) {
      const observer = new IntersectionObserver(
        ([entry]) => setSensorVisible(entry.isIntersecting),
        { threshold: 1 }
      );
      observer.observe(visibilitySensor.current);
      return () => observer.disconnect();
    }
  }, [visibilitySensor.current]);

  // Pop the sidepanel once the content is loaded, the sensor is visible and the content is progressed.
  // Two-second delay lets this lag the progress checkbox animation and gives you a moment to breathe.
  const launchSurvey = loaded && sensorVisible && complete && !survey.programmatic;
  useEffect(
    () => (launchSurvey ? timeoutEffect(() => setShowPanel(true), 2000)() : void 0),
    [launchSurvey]
  );

  // If it is a programmatic survey then put `loContentSurvey` function on the window.
  useEffect(() => {
    if (survey.programmatic && submitted === 'initial') {
      (window as any).loContentSurvey = (callback?: (submitted: boolean) => void) => {
        setShowPanel(true);
        setCallback(() => callback);
      };
      return () => {
        delete (window as any).loContentSurvey;
      };
    }
  }, [survey.programmatic, submitted]);

  // If programmatically opened then call back when the panel is closed.
  useEffect(() => {
    if (callback && !showPanel) {
      callback(submitted === 'submitted');
      setCallback(undefined);
    }
  }, [callback, showPanel, submitted]);

  // escape closes the panel
  useEffect(() => {
    if (showPanel) {
      const listener = (e: KeyboardEvent) => {
        if (e.key === 'Escape') setShowPanel(false);
      };
      window.addEventListener('keydown', listener);
      return () => window.removeEventListener('keydown', listener);
    }
  }, [showPanel]);

  const trackEventOnce = useMemo(() => {
    const noop = () => {};
    let sidePanel = trackSurveySidePanelInteraction;
    let inline = trackSurveyInlineInteraction;
    let submit = trackSurveySubmitInteraction;
    return {
      sidePanel: () => {
        sidePanel();
        sidePanel = noop;
      },
      inline: () => {
        inline();
        inline = noop;
      },
      submit: () => {
        submit();
        submit = noop;
      },
    };
  }, [contentId]);

  const submitResponses = () => {
    const surveyResponses = Object.entries(responses).map(([key, value]) => {
      return {
        questionAssetId: key,
        response: value,
      } as SurveyQuestionResponse;
    });
    const onSubmitted = () => {
      setSubmitted('submitted');
      dispatch(modifySurveyAction({ submitted: true }, contentId));
      setTimeout(() => {
        setShowPanel(false);
      }, 1000);
    };
    if (isPreviewing) {
      onSubmitted();
    } else {
      postSurveyResponse(sectionId, contentId, { responses: surveyResponses })
        .then(onSubmitted)
        .catch(error => {
          setSubmitted('error');
          setError(error.message);
        });
    }
    trackEventOnce.submit();
  };

  const togglePanel = () => {
    setShowPanel(!showPanel);
    if (!showPanel) {
      setSubmitted('initial');
      setResponses({});
      setError(null);
    }
    trackEventOnce.sidePanel();
  };

  return (
    <>
      {survey.inline ? (
        <div
          style={{ height: '1px', marginBottom: '-1px' }}
          ref={visibilitySensor}
        />
      ) : (
        <button
          className={classNames('feedback-button d-print-none', {
            'panel-open': showPanel,
            hidden: survey.submitted,
          })}
          onClick={togglePanel}
        >
          <div>{translate('CONTENT_SURVEY_FEEDBACK_BUTTON')}</div>
        </button>
      )}
      <div
        className={classNames(
          'panel',
          'panel-right',
          'survey-panel',
          { open: showPanel },
          survey.inline && 'inline'
        )}
        aria-labelledby="survey-header"
      >
        <div className="panel-inner">
          <header className="panel-header border-bottom">
            <h5
              className="panel-title text-truncate"
              id="survey-header"
            >
              {survey.title}
            </h5>
            <Button
              className="btn-close close-btn me-2"
              onClick={togglePanel}
              title={translate('CONTENT_SURVEY_CLOSE')}
              aria-label={translate('CONTENT_SURVEY_CLOSE')}
            ></Button>
          </header>
          <div className="panel-block">
            <Form>
              {survey.questions.map((question, idx) => (
                <SurveyQuestion
                  key={idx}
                  index={idx}
                  question={question}
                  disabled={submitted === 'submitted'}
                  numQuestions={survey.questions.length}
                  setResponse={(value: string) => {
                    setResponses({
                      ...responses,
                      [question.id]: value,
                    });
                    setSubmitted('touched');
                  }}
                />
              ))}
            </Form>
          </div>
          <div className="panel-block px-4 py-0 d-flex justify-content-center panel-buttons">
            <Button
              color="link"
              onClick={togglePanel}
            >
              {translate('CONTENT_SURVEY_CANCEL')}
            </Button>
            <Button
              color={error ? 'danger' : 'primary'}
              onClick={submitResponses}
              disabled={['submitted', 'initial'].includes(submitted)}
            >
              {translate(
                submitted === 'submitted' ? 'CONTENT_SURVEY_SUBMITTED' : 'CONTENT_SURVEY_SUBMIT'
              )}
            </Button>
          </div>
          {isErrorState(submitted) && (
            <div className="panel-block">
              <Alert color="danger">{translate('SURVEY_SUBMIT_ERROR_MESSAGE') + ` ${error}`}</Alert>
            </div>
          )}
          <div className="panel-block d-flex justify-content-center text-center text-muted small px-md-5">
            {translate('SURVEY_DISCLAIMER')}
          </div>
        </div>
      </div>
    </>
  );
};

const ContentSurvey: React.FC<{
  content: ContentWithNebulousDetails;
  loaded?: boolean;
}> = ({ content, loaded }) => {
  const [prevContent, setPrevContent] = useState(content.id);
  useEffect(() => {
    setPrevContent(content.id);
  }, [content.id]);

  return prevContent !== content.id ? null : (
    <ConnectedLoader
      onMount={(state, dispatch) => {
        if (content.hasSurvey && !state.survey[content.id]) {
          const sectionId = state.course.id;
          dispatch(fetchKeyedSurveyAction(content.id)(sectionId, content));
        }
      }}
      selector={(state: CourseState) => {
        return sequenceObj({
          survey: content.hasSurvey ? state.survey[content.id] || loading : loadeder(false),
          contentId: loadeder(content.id),
          sectionId: loadeder(state.course.id),
        });
      }}
    >
      {([props, dispatch]) => {
        if (shouldRenderSurvey(props.survey)) {
          // NOTE: the order of the prop spread is simply to make typescript happy about the narrowed type.
          return (
            <Survey
              {...props}
              survey={props.survey}
              dispatch={dispatch}
              content={content}
              loaded={loaded}
            />
          );
        } else {
          return null;
        }
      }}
    </ConnectedLoader>
  );
};

export default ContentSurvey;
