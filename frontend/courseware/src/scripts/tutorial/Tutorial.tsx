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

import * as actualUserSlice from '../loRedux/actualUserSlice';
import * as tutorialSlice from '../tutorial/tutorialSlice';
import { isString } from 'lodash';
import { useTranslation } from '../i18n/translationContext';
import { timeoutEffect } from '../utilities/effectUtils';
import { selectCurrentUser } from '../utilities/rootSelectors';
import React, { useEffect, useState } from 'react';
import Joyride, { ACTIONS, CallBackProps, EVENTS, FloaterProps, Styles } from 'react-joyride';
import { useDispatch, useSelector } from 'react-redux';

type TutorialProps = {
  name: string;
};

const Tutorial: React.FC<TutorialProps> = ({ name }) => {
  const tutorials = window.lo_platform.tutorials;
  const tutorialUserInfos = useSelector(actualUserSlice.selectTutorials);
  const manuallyPlaying = useSelector(tutorialSlice.selectManuallyPlaying);
  const enabled = useSelector(tutorialSlice.selectEnabled) && tutorials[name];
  const viewingAs = useSelector(selectCurrentUser);
  const dispatch = useDispatch();
  const translation = useTranslation();
  const [_visibleSteps, setVisibleSteps] = useState('');

  const isStudent = viewingAs.isStudent && !viewingAs.isPreviewing;
  const isPreviewing = viewingAs.user_type === 'Preview';
  const complete = tutorialUserInfos[name]?.status === 'Complete';
  const playing = manuallyPlaying || !complete;

  const [delayed, setDelayed] = useState(true);
  useEffect(
    timeoutEffect(() => setDelayed(false), 300),
    [setDelayed]
  );

  useEffect(() => {
    if (enabled && isStudent) {
      dispatch(tutorialSlice.showManuallyPlayButton());
    }
    return () => {
      dispatch(tutorialSlice.hideManuallyPlayButton());
    };
  }, [enabled, isStudent]);

  useEffect(() => {
    const pageContent = document.getElementById('page-content');
    if (pageContent && enabled && isStudent) {
      const observer = new MutationObserver(() => {
        const visibleSteps = tutorials[name].steps
          .map(step => step.target)
          .filter(isString)
          .filter(target => document.querySelectorAll(target).length > 0)
          .join(',');
        // This is pretty shady but it triggers a re-render if the steps change
        setVisibleSteps(visibleSteps);
      });
      observer.observe(pageContent, { attributes: true, childList: true, subtree: true });
      return () => {
        observer.disconnect();
      };
    }
  }, [enabled, isStudent, tutorials, name]);

  const handleJoyrideCallback = (data: CallBackProps) => {
    // Normally when you close a step it collapses to a beacon. However if you disable
    // the beacon (as is our wont) then it just goes to the next step instead.
    // So if you close the tutorial on a step without a beacon, abandon tracks.
    const isCloseWithoutBeaconBeforeLastStep =
      data.type === EVENTS.STEP_AFTER &&
      data.action === ACTIONS.CLOSE &&
      data.step.disableBeacon &&
      data.index + 1 !== data.size;
    const isTourStart = data.type === EVENTS.TOUR_START;
    const isTourEnd = data.type === EVENTS.TOUR_END;
    if (isTourStart) {
      dispatch(tutorialSlice.play());
    }
    if (isTourEnd || isCloseWithoutBeaconBeforeLastStep) {
      dispatch(
        tutorialSlice.end(name, !manuallyPlaying, data.index + 1, data.index + 1 === data.size)
      );
    }
  };

  if (enabled && !delayed && isStudent && !isPreviewing) {
    const steps = tutorials[name].steps
      .filter(step => {
        if (typeof step.target === 'string') {
          return document.querySelectorAll(step.target).length > 0;
        } else {
          return false;
        }
      })
      .map(step => {
        let nextContent;
        if (typeof step.content === 'string') {
          if (step.content.startsWith('<') && step.content.endsWith('>')) {
            nextContent = <div dangerouslySetInnerHTML={{ __html: step.content }} />;
          } else {
            nextContent = translation(step.content);
          }
        } else {
          nextContent = step.content;
        }

        return {
          disableBeacon: true, // preferred default
          ...step,
          content: nextContent,
        };
      });

    const primaryColor = window.lo_platform.domain.appearance['color-primary'];
    const styles: Partial<Styles> = {
      options: {
        zIndex: 1021, // must be higher than bootstrap variable: zindex-sticky
        ...(primaryColor ? { primaryColor } : {}),
      },
      tooltipTitle: {
        textAlign: 'left',
        fontWeight: 600,
        padding: '20px 10px 0',
        fontSize: '20px',
      },
      tooltipContent: {
        textAlign: 'left',
      },
      buttonBack: {
        outlineColor: '#AAA',
      },
      buttonClose: {
        outlineColor: '#AAA',
      },
      buttonNext: {
        outlineColor: '#AAA',
      },
      buttonSkip: {
        outlineColor: '#AAA',
      },
    };

    const locale = {
      back: translation('TUTORIAL_JOYRIDE_BACK'),
      close: translation('TUTORIAL_JOYRIDE_CLOSE'),
      last: translation('TUTORIAL_JOYRIDE_LAST'),
      next: translation('TUTORIAL_JOYRIDE_NEXT'),
      open: translation('TUTORIAL_JOYRIDE_OPEN'),
      skip: translation('TUTORIAL_JOYRIDE_SKIP'),
    };

    const floaterProps: FloaterProps = {
      disableAnimation: true,
    };

    return (
      <Joyride
        run={playing}
        steps={steps}
        continuous
        showProgress
        callback={handleJoyrideCallback}
        styles={styles}
        locale={locale}
        floaterProps={floaterProps}
        scrollOffset={100}
        disableScrollParentFix={false}
      />
    );
  } else {
    return null;
  }
};

export default Tutorial;
