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

export const branchPath = '/branch/:branchId';

export const storyPath = '/branch/:branchId/story/:name';
export const revisionPath = '/branch/:branchId/revision/:name';

export const competenciesPath = '/branch/:branchId/story/objectives';
export const feedbackPath = '/branch/:branchId/feedback';
export const feedbackItemPath = '/branch/:branchId/feedback/:feedback';
export const dropboxPath = '/branch/:branchId/dropbox';
export const dropboxFolderPath = '/branch/:branchId/dropbox/:folder';
export const gradebookPath = '/branch/:branchId/story/gradebook';
export const timelinePath = '/branch/:branchId/story/timeline';
export const launchPath = '/branch/:branchId/launch/:name';

export const rootPath = '/';
export const contentSearchPath = '/search';

export const dcmPaths = [branchPath, rootPath, contentSearchPath];

export const feedbackPaths = [storyPath, feedbackItemPath];

const allPaths = [
  branchPath,
  storyPath,
  revisionPath,
  competenciesPath,
  dropboxPath,
  dropboxFolderPath,
  gradebookPath,
  feedbackPath,
  feedbackItemPath,
  timelinePath,
  launchPath,
];

export default allPaths;
