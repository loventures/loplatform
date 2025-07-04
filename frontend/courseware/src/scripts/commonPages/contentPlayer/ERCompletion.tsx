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

import classnames from 'classnames';
import { ActivityProps } from '../../contentPlayerComponents/activityViews/ActivityProps';
import { reportProgressActionCreator } from '../../courseActivityModule/actions/activityActions';
import { useTranslation } from '../../i18n/translationContext';
import {
  CONTENT_TYPE_COURSE_LINK,
  CONTENT_TYPE_LTI,
  CONTENT_TYPE_SCORM,
} from '../../utilities/contentTypes';
import { timeoutEffect } from '../../utilities/effectUtils';
import React, { useEffect, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Button } from 'reactstrap';

const SelfProgressTypeIds = new Set([
  CONTENT_TYPE_SCORM,
  CONTENT_TYPE_LTI,
  CONTENT_TYPE_COURSE_LINK,
]);

/** A completion widget that automatically emits progress for content when it is scrolled on screen. */
const ERCompletion: React.FC<ActivityProps<any> & { loaded: boolean }> = ({ content, loaded }) => {
  const dispatch = useDispatch();
  const translate = useTranslation();
  const ref = useRef<HTMLDivElement | null>(null);

  const { progress } = content;
  const complete = progress?.total > 0 && progress.completions >= progress.total;

  // Was the activity already complete when initially viewed? If so, no animation.
  const [initiallyComplete, setInitiallyComplete] = useState(false);
  useEffect(() => setInitiallyComplete(complete), [content.id]);

  // Is the activity currently complete.
  const [currentlyComplete, setCurrentlyComplete] = useState(false);
  useEffect(() => {
    if (complete) {
      // half second delay so the animation kicks in a moment after scrolling on-screen
      return timeoutEffect(() => setCurrentlyComplete(true), 500)();
    } else {
      setInitiallyComplete(false);
      setCurrentlyComplete(false);
    }
  }, [content.id, complete]);

  // Is this widget visible on screen.
  const [visible, setVisible] = useState(false);
  useEffect(() => {
    if (ref.current) {
      const observer = new IntersectionObserver(
        ([entry]) => {
          setVisible(entry.isIntersecting);
        },
        { threshold: 1 }
      );
      observer.observe(ref.current);
      return () => observer.disconnect();
    }
  }, [ref.current]);

  // SCORM and LTI and such trigger progress internally
  const noAutoProgress = SelfProgressTypeIds.has(content.typeId);

  // Emit progress if this content is loaded and the completer is visible and content doesn't grant progress.
  const emitProgress = loaded && visible && !noAutoProgress;
  useEffect(() => {
    if (emitProgress) dispatch(reportProgressActionCreator(content, true));
  }, [content.id, emitProgress]);

  const undoCompletion = () => {
    dispatch(reportProgressActionCreator(content, false));
  };

  // For SCORM and LTI undo is questionable but for now just hide it for graded activities
  const noUndo = content.hasGradebookEntry;

  return (
    <div
      className="mt-4 mt-md-4 mt-lg-5 completion-widget"
      ref={ref}
    >
      <div className=" d-flex justify-content-center align-items-center">
        <svg
          className={classnames(
            'completion-checkmark',
            initiallyComplete ? 'already-completed' : currentlyComplete && 'just-completed'
          )}
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 52 52"
        >
          <circle
            className="checkmark__circle"
            cx="26"
            cy="26"
            r="25"
            fill="none"
          />
          <path
            className="checkmark__check"
            fill="none"
            d="M14.1 27.2l7.1 7.2 16.7-16.8"
          />
        </svg>
        <div
          className="flex-column ms-3"
          style={{ opacity: currentlyComplete ? 1 : 0.5, transition: 'opacity linear .3s' }}
        >
          <div className="mb-0 completion-label">
            {currentlyComplete
              ? translate('ER_COMPLETION_COMPLETE')
              : translate('ER_COMPLETION_INCOMPLETE')}
          </div>
          {!noUndo ? (
            <Button
              color="link"
              className="completion-undo-btn"
              onClick={undoCompletion}
              disabled={!currentlyComplete}
            >
              {translate('ER_COMPLETION_UNDO')}
            </Button>
          ) : null}
        </div>
      </div>
    </div>
  );
};

export default ERCompletion;
