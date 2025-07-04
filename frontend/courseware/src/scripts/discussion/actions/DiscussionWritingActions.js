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

import { batchActions } from 'redux-batched-actions';

import {
  DISCUSSION_WRITING_START,
  DISCUSSION_WRITING_SAVE_START,
  DISCUSSION_WRITING_SAVE_SUCCESS,
  DISCUSSION_WRITING_SAVE_ERROR,
  DISCUSSION_WRITING_DISCARD,
  DISCUSSION_WRITING_KEEP_WORKING,
} from '../actionTypes.js';

import DiscussionBoardAPI from '../../services/DiscussionBoardAPI.js';

import DiscussionDataActions from './DiscussionDataActions.js';
import DiscussionJumperActions from './DiscussionJumperActions.js';
import DiscussionBoardActions from './DiscussionBoardActions.js';

export default angular
  .module('lo.discussionBoard.DiscussionWritingActions', [
    DiscussionBoardAPI.name,
    DiscussionBoardActions.name,
    DiscussionJumperActions.name,
    DiscussionDataActions.name,
  ])
  .service('DiscussionWritingActions', [
    'DiscussionBoardAPI',
    'DiscussionBoardActions',
    'DiscussionDataActions',
    function DiscussionWritingActions(
      DiscussionBoardAPI,
      DiscussionBoardActions,
      DiscussionDataActions
    ) {
      const service = {};

      service.makeWritingStartActionCreator = config => replyToId => ({
        type: DISCUSSION_WRITING_START,
        ...config,
        data: { replyToId },
      });

      service.makeWritingDiscardActionCreator = config => () => ({
        type: DISCUSSION_WRITING_DISCARD,
        ...config,
      });

      service.makeWritingKeepWorkingActionCreator = config => () => ({
        type: DISCUSSION_WRITING_KEEP_WORKING,
        ...config,
      });

      service.makeWritingSaveStartActionCreator = config => () => ({
        type: DISCUSSION_WRITING_SAVE_START,
        ...config,
      });

      service.makeWritingSaveSuccessActionCreator = config => (newItemId, discussionId) => ({
        type: DISCUSSION_WRITING_SAVE_SUCCESS,
        ...config,
        data: { newItemId, discussionId },
      });

      service.makeWritingSaveErrorActionCreator = config => error => ({
        type: DISCUSSION_WRITING_SAVE_ERROR,
        ...config,
        data: { error },
      });

      service.makeEditStartActionCreator = (discussionId, postId) =>
        service.makeWritingStartActionCreator({ postId });

      service.makeEditDiscardActionCreator = (discussionId, postId) =>
        service.makeWritingDiscardActionCreator({ postId });

      service.makeEditSaveActionCreator = (discussionId, postId) => {
        const startAction = service.makeWritingSaveStartActionCreator({ postId });
        const successAction = service.makeWritingSaveSuccessActionCreator({
          postId,
        });
        const errorAction = service.makeWritingSaveErrorActionCreator({ postId });

        return (title, content, uploads, removals, attachments) => dispatch => {
          dispatch(startAction());

          DiscussionBoardAPI.updateReply(discussionId, postId, {
            title,
            content,
            uploads,
            removals,
            attachments,
          }).then(
            post => {
              const actions = [DiscussionDataActions.createPostUpdateAction(post)];
              if (post.depth === 0) {
                actions.push(DiscussionDataActions.createThreadUpdateAction(post));
              }
              actions.push(successAction());
              dispatch(batchActions(actions));
            },
            error => {
              DiscussionBoardAPI.getErrorDetails(discussionId, error).then(details => {
                dispatch(errorAction(details || {}));
              });
            }
          );
        };
      };

      service.makeReplyStartActionCreator = (discussionId, threadId) =>
        service.makeWritingStartActionCreator({ threadId });

      service.makeReplyDiscardActionCreator = (discussionId, threadId) =>
        service.makeWritingDiscardActionCreator({ threadId });

      service.makeReplySaveActionCreator = (discussionId, threadId, parentPostId) => {
        const startAction = service.makeWritingSaveStartActionCreator({
          threadId,
        });
        const successAction = service.makeWritingSaveSuccessActionCreator({
          threadId,
        });
        const errorAction = service.makeWritingSaveErrorActionCreator({
          threadId,
        });

        return (title, content, uploads) => dispatch => {
          dispatch(startAction());

          DiscussionBoardAPI.newReply(discussionId, {
            title,
            content,
            uploads,
            parentPostId,
          }).then(
            post =>
              dispatch(
                batchActions([
                  DiscussionDataActions.createPostUpdateAction(post),
                  successAction(post.id, discussionId),
                ])
              ),
            error => {
              DiscussionBoardAPI.getErrorDetails(discussionId, error).then(details => {
                dispatch(errorAction(details || {}));
              });
            }
          );
        };
      };

      service.makeThreadSaveActionCreator = discussionId => {
        const startAction = service.makeWritingSaveStartActionCreator({
          discussionId,
        });
        const successAction = service.makeWritingSaveSuccessActionCreator({
          discussionId,
        });
        const errorAction = service.makeWritingSaveErrorActionCreator({
          discussionId,
        });

        return (
            title,
            content,
            uploads,
            removaldiscussionStudentPickerModals,
            attachedFiles,
            visitAfter
          ) =>
          dispatch => {
            dispatch(startAction());

            DiscussionBoardAPI.newThread(discussionId, {
              title,
              content,
              uploads,
            }).then(
              thread => {
                dispatch(
                  batchActions([
                    DiscussionDataActions.createThreadUpdateAction(thread),
                    DiscussionDataActions.createPostUpdateAction(thread.rootPost),
                    successAction(thread.id),
                  ])
                );
                if (visitAfter) {
                  const visitAction =
                    DiscussionBoardActions.makeVisitBoardActionCreator(discussionId);
                  dispatch(visitAction());
                }
              },
              error => {
                DiscussionBoardAPI.getErrorDetails(discussionId, error).then(details => {
                  dispatch(errorAction(details || {}));
                });
              }
            );
          };
      };

      return service;
    },
  ]);
