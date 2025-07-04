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

import Course from '../bootstrap/course.js';
import { loConfig } from '../bootstrap/loConfig.js';
import { cdnWebAssets } from './preferences.js';
import UrlBuilder from './UrlBuilder.js';

export const getAssetRenderUrl = (name, branch = Course.branch_id, commit = Course.commitId) => {
  const hasOffering = !!Course.offering_id;
  const cdn = cdnWebAssets && hasOffering; // only use the CDN in a real section

  return new UrlBuilder(loConfig.contentItem.assetRender, {
    branch,
    commit,
    name,
    cdn,
  }).toString();
};

export const getCourseAssetRenderUrl = path => {
  const hasOffering = !!Course.offering_id;
  const cdn = cdnWebAssets && hasOffering; // only use the CDN in a real section

  return new UrlBuilder(loConfig.contentItem.courseAssetRender, {
    context: Course.id,
    path,
    cdn,
  }).toString();
};

export const getFileBundleUrl = (fileInfo, content, commit = Course.commitId) => {
  const url = new UrlBuilder(loConfig.contentItem.fileBundle.serve, {
    commit,
    name: content.node_name,
  });

  return `${url.toString()}/${fileInfo.path}`;
};

export const getHotspotImageUrl = (name, commit = Course.commitId) => {
  return new UrlBuilder(loConfig.contentItem.asset.serve, {
    commit,
    name,
  }).toString();
};

export const getLessonPrintUrl = (name, branch = Course.branch_id, commit = Course.commitId) => {
  return new UrlBuilder(loConfig.contentItem.asset.lessonPrint, {
    branch,
    commit,
    name,
  }).toString();
};
