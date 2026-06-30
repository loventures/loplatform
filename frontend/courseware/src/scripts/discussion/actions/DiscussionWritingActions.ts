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

import { batchActions } from 'redux-batched-actions';

import { discussionBoardAPI } from '../../services/discussionBoardAPI.ts';

import {
  DISCUSSION_WRITING_START,
  DISCUSSION_WRITING_SAVE_START,
  DISCUSSION_WRITING_SAVE_SUCCESS,
  DISCUSSION_WRITING_SAVE_ERROR,
  DISCUSSION_WRITING_DISCARD,
  DISCUSSION_WRITING_KEEP_WORKING,
} from '../actionTypes.js';

import * as DiscussionDataActions from './DiscussionDataActions.ts';
import * as DiscussionBoardActions from './DiscussionBoardActions.ts';

export const makeWritingStartActionCreator = (config: any) => (replyToId: any) => ({
  type: DISCUSSION_WRITING_START,
  ...config,
  data: { replyToId },
});

export const makeWritingDiscardActionCreator = (config: any) => () => ({
  type: DISCUSSION_WRITING_DISCARD,
  ...config,
});

export const makeWritingKeepWorkingActionCreator = (config: any) => () => ({
  type: DISCUSSION_WRITING_KEEP_WORKING,
  ...config,
});

export const makeWritingSaveStartActionCreator = (config: any) => () => ({
  type: DISCUSSION_WRITING_SAVE_START,
  ...config,
});

export const makeWritingSaveSuccessActionCreator =
  (config: any) => (newItemId?: any, discussionId?: any) => ({
    type: DISCUSSION_WRITING_SAVE_SUCCESS,
    ...config,
    data: { newItemId, discussionId },
  });

export const makeWritingSaveErrorActionCreator = (config: any) => (error: any) => ({
  type: DISCUSSION_WRITING_SAVE_ERROR,
  ...config,
  data: { error },
});

export const makeEditStartActionCreator = (discussionId: any, postId: any) =>
  makeWritingStartActionCreator({ postId });

export const makeEditDiscardActionCreator = (discussionId: any, postId: any) =>
  makeWritingDiscardActionCreator({ postId });

export const makeEditSaveActionCreator = (discussionId: any, postId: any) => {
  const startAction = makeWritingSaveStartActionCreator({ postId });
  const successAction = makeWritingSaveSuccessActionCreator({
    postId,
  });
  const errorAction = makeWritingSaveErrorActionCreator({ postId });

  return (title: any, content: any, uploads: any, removals: any, attachments: any) =>
    (dispatch: any) => {
      dispatch(startAction());

      discussionBoardAPI
        .updateReply(discussionId, postId, {
          title,
          content,
          uploads,
          removals,
          attachments,
        })
        .then(
          (post: any) => {
            const actions = [DiscussionDataActions.createPostUpdateAction(post)];
            if (post.depth === 0) {
              actions.push(DiscussionDataActions.createThreadUpdateAction(post));
            }
            actions.push(successAction());
            dispatch(batchActions(actions));
          },
          (error: any) => {
            discussionBoardAPI
              .getErrorDetails(discussionId, error)
              .then((details: any) => {
                dispatch(errorAction(details || {}));
              });
          }
        );
    };
};

export const makeReplyStartActionCreator = (discussionId: any, threadId: any, _postId?: any) =>
  makeWritingStartActionCreator({ threadId });

export const makeReplyDiscardActionCreator = (discussionId: any, threadId: any, _postId?: any) =>
  makeWritingDiscardActionCreator({ threadId });

export const makeReplySaveActionCreator = (discussionId: any, threadId: any, parentPostId: any) => {
  const startAction = makeWritingSaveStartActionCreator({
    threadId,
  });
  const successAction = makeWritingSaveSuccessActionCreator({
    threadId,
  });
  const errorAction = makeWritingSaveErrorActionCreator({
    threadId,
  });

  return (title: any, content: any, uploads: any) => (dispatch: any) => {
    dispatch(startAction());

    discussionBoardAPI
      .newReply(discussionId, {
        title,
        content,
        uploads,
        parentPostId,
      })
      .then(
        (post: any) =>
          dispatch(
            batchActions([
              DiscussionDataActions.createPostUpdateAction(post),
              successAction(post.id, discussionId),
            ])
          ),
        (error: any) => {
          discussionBoardAPI
            .getErrorDetails(discussionId, error)
            .then((details: any) => {
              dispatch(errorAction(details || {}));
            });
        }
      );
  };
};

export const makeThreadSaveActionCreator = (discussionId: any) => {
  const startAction = makeWritingSaveStartActionCreator({
    discussionId,
  });
  const successAction = makeWritingSaveSuccessActionCreator({
    discussionId,
  });
  const errorAction = makeWritingSaveErrorActionCreator({
    discussionId,
  });

  return (...args: any[]) =>
    (dispatch: any) => {
      // original positional args: title, content, uploads,
      // removaldiscussionStudentPickerModals, attachedFiles, visitAfter
      const [title, content, uploads] = args;
      const visitAfter = args[5];
      dispatch(startAction());

      discussionBoardAPI
        .newThread(discussionId, {
          title,
          content,
          uploads,
        })
        .then(
          (thread: any) => {
            dispatch(
              batchActions([
                DiscussionDataActions.createThreadUpdateAction(thread),
                DiscussionDataActions.createPostUpdateAction(thread.rootPost),
                successAction(thread.id),
              ])
            );
            if (visitAfter) {
              const visitAction = DiscussionBoardActions.makeVisitBoardActionCreator(discussionId);
              dispatch(visitAction());
            }
          },
          (error: any) => {
            discussionBoardAPI
              .getErrorDetails(discussionId, error)
              .then((details: any) => {
                dispatch(errorAction(details || {}));
              });
          }
        );
    };
};
