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

import React, { useEffect, useMemo, useReducer, useRef, useState } from 'react';

import { FeedbackTools } from '../../assignmentFeedback/directives/feedbackTools.tsx';
import { FeedbackManager } from '../../assignmentFeedback/FeedbackManager.js';
import { RichTextEditor } from '../../contentEditor/directives/richTextEditor.tsx';
import { richTextEditor } from '../../contentEditor/index.js';
import { errorMessage } from '../../filters/pure/errorMessage.ts';
import { useTranslation } from '../../i18n/translationContext';
import settings from '../../utilities/settingsService';
import { openDiscussionReplyForbiddenModal } from '../modals/discussionReplyForbiddenModal.tsx';

interface DiscussionReplyProps {
  /** The writing slice: `{ saving, error }`. */
  state: any;
  saveAction: (
    title: string | null,
    content: string,
    filesInStaging: any,
    removals: any,
    attached: any
  ) => void;
  discardAction?: () => void;
  keepWorkingAction?: () => void;
  showTitle?: boolean;
  /** discussion-write-reply: prefill a "+name" mention. */
  replyToName?: string;
  /** discussion-edit-post: */
  cannotEdit?: boolean;
  post?: any;
  attachments?: any;
}

/**
 * React port of the shared discussion writing editor (B2, discussion subsystem — leaf 5): the
 * controller + template behind the three Angular components `discussionWriteThread` (new thread),
 * `discussionWriteReply` (reply), and `discussionEditPost` (edit). Now native React, composing the
 * already-React `RichTextEditor` (#1461) + the now-React `FeedbackTools` (file attachments; the audio
 * recorder within it stays Angular via an angular2react bridge). Each of
 * the three variants is bridged back via react2angular for its still-Angular consumer
 * (discussionBoard.html / discussionItem.html / discussionItemContent.html). The `FeedbackManager`
 * stays Angular (lojector); the forbidden-edit modal opens the already-React
 * `openDiscussionReplyForbiddenModal` directly. DOM preserved: `.discussion-write-reply`, the title
 * `input.form-control`, the editor, `.alert-danger`, the `.btn-outline-dark` cancel + `.btn-success`
 * submit.
 */
export const DiscussionReplyEditor: React.FC<DiscussionReplyProps> = ({
  state,
  saveAction,
  discardAction,
  keepWorkingAction,
  showTitle,
  replyToName,
  cannotEdit,
  post,
  attachments,
}) => {
  const translate = useTranslation();

  const feedbackManager = useMemo(
    () => new FeedbackManager(attachments),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    []
  );

  const [contentInEdit, setContentInEdit] = useState<string>(() => {
    if (post && post.content) return post.content;
    if (replyToName) return `<p><strong>+${replyToName}</strong>&nbsp;</p>`;
    return '';
  });
  const [contentTitle, setContentTitle] = useState<string>(() => (post && post.title) || '');
  const [error, setError] = useState<any>(null);
  const submitting = !!state.saving;
  const prevSaving = useRef(false);

  // `canSubmitReply` reads the mutable `feedbackManager`, which the Angular `<feedback-tools>` child
  // stages files into outside React's knowledge (and whose uploads complete asynchronously). The old
  // Angular component re-evaluated `ng-disabled` on every digest; mirror that here by polling the
  // manager's submit-relevant signature and forcing a re-render only when it actually changes (so the
  // Submit button enables once a staged file is ready, even with no text — CBLPROD-17003).
  const [, forceRender] = useReducer((c: number) => c + 1, 0);
  const feedbackSignature = useRef('');
  useEffect(() => {
    const id = window.setInterval(() => {
      const staged = feedbackManager.hasStagedOrOngoing();
      const sig = `${staged}:${staged && feedbackManager.isReady()}`;
      if (sig !== feedbackSignature.current) {
        feedbackSignature.current = sig;
        forceRender();
      }
    }, 200);
    return () => window.clearInterval(id);
  }, [feedbackManager]);

  const discard = () => {
    discardAction?.();
    setContentTitle('');
    setContentInEdit('');
  };

  // The old `$onChanges({ state })`.
  useEffect(() => {
    if (state.error && state.error.type === 'UNAUTHORIZED_ERROR') {
      openDiscussionReplyForbiddenModal(state.error).then((keepWork: any) => {
        if (keepWork) keepWorkingAction?.();
        else discard();
      });
    } else if (state.error) {
      setError(state.error);
    } else if (!state.saving && prevSaving.current) {
      setContentTitle('');
      setContentInEdit('');
      feedbackManager.clearStageFiles();
    }
    prevSaving.current = !!state.saving;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state]);

  const canSubmitReply = () => {
    if (cannotEdit) return false;
    if (state.saving) return false;
    // CBLPROD-997: advisors (neither Learn nor Teach right) cannot submit.
    if (!settings.isFeatureEnabled('LearnCourseRight') && !settings.isFeatureEnabled('TeachCourseRight')) {
      return false;
    }
    // CBLPROD-17003: a file without input text is allowed.
    if (feedbackManager.hasStagedOrOngoing()) return feedbackManager.isReady();
    return !!contentInEdit && !!contentInEdit.length;
  };

  const submit = () =>
    saveAction(
      contentTitle,
      contentInEdit,
      feedbackManager.getFilesInStaging(),
      feedbackManager.getRemovalsInStaging(),
      feedbackManager.getAttachedFiles()
    );

  return (
    <div className="discussion-write-reply">
      <div className="discussion-reply-row">
        {showTitle && (
          <input
            className="form-control input-group mb-1"
            type="text"
            disabled={cannotEdit}
            value={contentTitle}
            onChange={e => setContentTitle(e.target.value)}
            placeholder={translate('ENTER_TITLE')}
            maxLength={255}
          />
        )}

        <RichTextEditor
          content={contentInEdit}
          onChange={setContentInEdit}
          isDisabled={cannotEdit}
          minHeight={60}
        />

        <div className="media-items d-print-none">
          <FeedbackTools feedbackManager={feedbackManager} />
        </div>

        {error && <div className="alert alert-danger">{errorMessage(error, translate)}</div>}
      </div>

      <div className="flex-row-content justify-content-end mt-2 d-print-none">
        {discardAction && (
          <button
            className="btn btn-outline-dark me-0"
            type="button"
            onClick={discard}
          >
            <span>{translate('DISCUSSION_CANCEL_REPLY')}</span>
          </button>
        )}

        <button
          className="btn btn-success d-flex align-items-center gap-1"
          type="button"
          onClick={submit}
          disabled={!canSubmitReply()}
        >
          {!submitting && (
            <>
              {/*
               * Always "Submit". The original template gated the "Save Changes" (DISCUSSION_SUBMIT_EDIT)
               * label on `ng-if="item"`, but `item` was never a binding (the components bind `post`), so
               * that label never rendered and the button always read "Submit" — behavior the E2E codifies.
               */}
              <span>{translate('DISCUSSION_SUBMIT_REPLY')}</span>
              <span
                className="icon icon-circle-right"
                aria-hidden="true"
              />
            </>
          )}
          {submitting && (
            <>
              <span>{translate('DISCUSSION_SUBMITTING_REPLY')}</span>
              <span className="icon de-spin icon-spinner-dots" />
            </>
          )}
        </button>
      </div>
    </div>
  );
};

