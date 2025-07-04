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

import React, { useEffect, useRef } from 'react';
import { BiCommentAdd } from 'react-icons/bi';
import { useDispatch } from 'react-redux';
import { Button } from 'reactstrap';

import { resetHtmlFeedback, setAddFeedbackForAsset } from '../feedback/feedbackActions';
import { useFeedbackOn, useHtmlFeedback } from '../feedback/feedbackHooks';
import { confirmSaveProjectGraphEdits, useCurrentAssetName } from '../graphEdit';
import { usePolyglot } from '../hooks';
import { useContentAccess } from './hooks/useContentAccess';
import {
  clearIFrameSelection,
  useIFrameSelectionListener,
  useMouseSelectionStatus,
} from './storyFeedback';
import { useRevisionCommit } from './storyHooks';

export const HtmlFeedback: React.FC = () => {
  const name = useCurrentAssetName();
  const dispatch = useDispatch();
  const polyglot = usePolyglot();
  const htmlFeedback = useHtmlFeedback();
  const gadgetRef = useRef<HTMLButtonElement>();
  const commit = useRevisionCommit();
  const contentAccess = useContentAccess(name);
  const feedbackOn = useFeedbackOn();
  const feedbackEnabled = contentAccess.AddFeedback && !commit && feedbackOn;

  // If the user selects text in the iframe, place the gadget and allow feedback
  useIFrameSelectionListener(feedbackEnabled);

  // If the user selects text in the main window, place the gadget and allow feedback
  useMouseSelectionStatus(gadgetRef, feedbackEnabled);

  useEffect(() => {
    dispatch(resetHtmlFeedback());
  }, [name]);

  useEffect(() => {
    if (htmlFeedback) {
      const listener = (e: MouseEvent) => {
        if (!gadgetRef.current?.contains(e.target as any)) {
          clearIFrameSelection();
          dispatch(resetHtmlFeedback());
        }
      };
      document.addEventListener('mousedown', listener);
      return () => document.removeEventListener('mousedown', listener);
    }
  }, [htmlFeedback, gadgetRef]);

  return htmlFeedback ? (
    <div
      className="feedback-gadget-holder"
      style={{
        left: htmlFeedback.x,
        top: htmlFeedback.y,
      }}
    >
      <Button
        id="feedback-gadget"
        color="primary"
        className="feedback-gadget"
        innerRef={gadgetRef}
        onClick={() => {
          if (htmlFeedback) {
            // Pedantically I only need to save if the current asset or an ancestor is added
            // and so doesn't have a real UUID, but it would be weird to add feedback on
            // unsaved content so just always save.
            dispatch(
              confirmSaveProjectGraphEdits(() =>
                dispatch(
                  setAddFeedbackForAsset(htmlFeedback.path, htmlFeedback.quote, htmlFeedback.id)
                )
              )
            );
          }
        }}
        aria-label={polyglot.t('FEEDBACK_POPOVER_TEXT')}
      >
        <BiCommentAdd
          aria-hidden={true}
          size="1.5rem"
        />
      </Button>
    </div>
  ) : null;
};
