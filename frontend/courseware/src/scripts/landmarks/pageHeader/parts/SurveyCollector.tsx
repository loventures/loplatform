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

import {
  trackSurveyCollectorLaterEvent,
  trackSurveyCollectorNoEvent,
  trackSurveyCollectorYesEvent,
} from '../../../analytics/trackEvents';
import { TranslationContext } from '../../../i18n/translationContext';
import { useCourseSelector } from '../../../loRedux';
import { surveyCollectorLaterHours, surveyCollectorUrl } from '../../../utilities/preferences';
import { selectActualUser } from '../../../utilities/rootSelectors';
import React, { useContext, useEffect, useState } from 'react';
import { usePopper } from 'react-popper';
import { Button } from 'reactstrap';
/*
 * SurveyCollector sends users to an external site (i.e. Survey Monkey) to collect
 * feedback on DE?CBL?LO. For surveys about the authored content, see ContentSurveyCollector
 */

const saidNoKey = 'SurveyUrlUserSaidNo-' + window.lo_platform.user.id;
const saidLaterKey = 'SurveyUrlUserSaidLater-' + window.lo_platform.user.id;
const laterPeriodMinutes = surveyCollectorLaterHours * 60;

const PopoverRefresher = ({ isOpen, scheduleUpdate }: any) => {
  useEffect(() => {
    scheduleUpdate && scheduleUpdate();
  }, [isOpen]);
  return null;
};

const SurveyCollector: React.FC = () => {
  const translate = useContext(TranslationContext);
  const [isOpen, setIsOpen] = useState<boolean>(false);
  const actualUser = useCourseSelector(selectActualUser);
  const isPreviewing = actualUser.user_type === 'Preview';

  const [referenceElement, setReferenceElement] = useState<HTMLElement | null>(null);
  const [popperElement, setPopperElement] = useState<HTMLElement | null>(null);
  const { styles, attributes, update } = usePopper(referenceElement, popperElement, {
    placement: 'bottom-end',
    modifiers: [
      {
        name: 'offset',
        options: {
          offset: [0, -5],
        },
      },
    ],
  });

  useEffect(() => {
    const userSaidNoToUrl = lscache.get(saidNoKey);
    const userSaidLaterToUrl = lscache.get(saidLaterKey);
    const userSaidNo = userSaidNoToUrl === surveyCollectorUrl;
    const userSaidLater = userSaidLaterToUrl === surveyCollectorUrl;
    const timeout = window.setTimeout(() => {
      setIsOpen(!!surveyCollectorUrl && !userSaidNo && !userSaidLater && !isPreviewing);
    }, 500);
    return () => clearTimeout(timeout);
  }, []);

  const sayNo = () => {
    trackSurveyCollectorNoEvent();
    lscache.set(saidNoKey, surveyCollectorUrl);
    setIsOpen(false);
  };

  const sayLater = () => {
    trackSurveyCollectorLaterEvent();
    lscache.set(saidLaterKey, surveyCollectorUrl, laterPeriodMinutes);
    setIsOpen(false);
  };

  const sayYes = () => {
    trackSurveyCollectorYesEvent();
    //storing this under saidNoKey to not show it again
    lscache.set(saidNoKey, surveyCollectorUrl);
    setIsOpen(false);
  };

  const toggle = () => setIsOpen(!isOpen);

  const userSurveyCollectorUrl = `${surveyCollectorUrl}?role=${
    actualUser.isInstructor ? 'instructor' : 'learner'
  }`;

  return (
    <div className="survey-collector d-none d-md-block">
      <button
        className={
          isOpen
            ? 'btn btn-primary survey-button'
            : 'btn btn-outline-primary border-white survey-button'
        }
        ref={setReferenceElement}
        onClick={toggle}
        aria-controls="survey-collector-prompt"
        aria-expanded={isOpen}
      >
        <span className="sr-only">{translate('SURVEY_COLLECTOR_TOGGLE')}</span>
      </button>
      <div
        className="popover show survey-collector-popover border-white"
        id="survey-collector-prompt"
        hidden={!isOpen}
        aria-hidden={!isOpen}
        ref={setPopperElement}
        style={{
          ...styles.popper,
          transition: 'transform 0.2s linear 0',
        }}
        {...attributes.popper}
      >
        <PopoverRefresher
          isOpen={isOpen}
          scheduleUpdate={update}
        />
        <div className="popover-body">
          <div className="h6 mt-2">
            {translate('SURVEY_COLLECTOR_GREETING')}
            &nbsp;
            {window.lo_platform.user.givenName}
          </div>
          <p>{translate('SURVEY_COLLECTOR_MESSAGE')}</p>
          <div className="flex-row-content flex-nowrap justify-content-end">
            <Button
              id="surveyCollectorNo"
              color="link"
              size="sm"
              onClick={sayNo}
            >
              {translate('SURVEY_COLLECTOR_NO')}
            </Button>
            <Button
              id="surveyCollectorLater"
              color="link"
              size="sm"
              onClick={sayLater}
            >
              {translate('SURVEY_COLLECTOR_LATER')}
            </Button>
            <a
              className="btn btn-primary btn-sm"
              href={userSurveyCollectorUrl}
              target="_blank"
              onClick={sayYes}
            >
              {translate('SURVEY_COLLECTOR_YES')}
            </a>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SurveyCollector;
