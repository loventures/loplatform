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

/**
 * The composite DiscussionBoardAPI, migrated verbatim from the AngularJS service:
 * a flat merge of the four discussion sub-APIs into one facade. Take the four
 * already-built sub-API objects and spread them (later spreads win, matching the
 * original object-literal order).
 */
export const makeDiscussionBoardAPI = (
  DiscussionBoardAPILight: any,
  DiscussionPostAPI: any,
  DiscussionPostReplyAPI: any,
  DiscussionPostStateAPI: any
) => ({
  ...DiscussionBoardAPILight,
  ...DiscussionPostAPI,
  ...DiscussionPostReplyAPI,
  ...DiscussionPostStateAPI,
});

export type DiscussionBoardAPI = ReturnType<typeof makeDiscussionBoardAPI>;
