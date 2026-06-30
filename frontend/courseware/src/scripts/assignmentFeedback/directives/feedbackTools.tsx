/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import React from 'react';

import { FeedbackFileList } from './feedbackFileList.tsx';
import { FeedbackToolbar } from './feedbackToolbar.tsx';

interface FeedbackToolsProps {
  /** The Angular `FeedbackManager` model (stays Angular) — mutated in place. */
  feedbackManager: any;
}

/**
 * React port of the `feedbackTools` directive (assignmentFeedback): the attachment-tools container —
 * the staged-file list plus the upload/record toolbar — under a feedback or answer editor. Was a thin,
 * logic-free Angular template wrapping `<feedback-file-list>` + `<feedback-toolbar>`; both are now
 * React, so this composes them directly. Bridged back via react2angular so the still-Angular consumers
 * (essay answer view, `gradeFeedbackPanel`) keep rendering `<feedback-tools>`; React consumers
 * (discussionReply, fullMessageCreator) import `FeedbackTools` directly, retiring their angular2react
 * bridges.
 *
 * Deliberately stateless — like the original directive, it holds no watch. The `FeedbackManager` is
 * mutated in place (files pushed/spliced, progress updated by `$q` callbacks on the same array ref);
 * re-rendering is driven by whoever owns the editor (e.g. discussionReply's submit-readiness poll), so
 * the file list refreshes with the surrounding form rather than this leaf forcing reflows on every
 * upload tick — an extra watch here caused a scroll/layout regression in the discussion reply flow.
 * `FeedbackToolbar` self-watches its own `activeTool`. DOM preserved for Selenide: `.attachment-tools-container`.
 */
export const FeedbackTools: React.FC<FeedbackToolsProps> = ({ feedbackManager }) => {
  if (!feedbackManager) return null;

  return (
    <div className="attachment-tools-container">
      <FeedbackFileList
        files={feedbackManager.files}
        feedbackManager={feedbackManager}
      />
      <FeedbackToolbar feedbackManager={feedbackManager} />
    </div>
  );
};

