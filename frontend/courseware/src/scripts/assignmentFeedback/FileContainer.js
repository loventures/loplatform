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

import { extend, isObject } from 'lodash';

import FileUtils from './FileUtils.js';
import UrlBuilder from '../utilities/UrlBuilder.js';

class FileContainer {
  constructor(fileInfo, fileUrl) {
    this.info = {};

    if (!fileUrl) {
      this.url = fileInfo.url;
    } else if (typeof fileUrl === 'string') {
      this.url = fileUrl + '/attachments/' + fileInfo.id;
    }

    this.updateInfo(fileInfo);

    if (this.isValidFile(fileInfo)) {
      this.setData(fileInfo);
    }

    this.canPreview = FileContainer.AttachmentService.isWhitelisted(this.info);

    this.progress = null;
  }
  updateInfo(fileInfo) {
    if (fileInfo instanceof FileContainer) {
      extend(this, fileInfo);
    } else if (isObject(fileInfo.info)) {
      extend(this.info, fileInfo.info);
    } else if (fileInfo) {
      extend(this.info, FileContainer.FileUtils.processFile(fileInfo, this.url));
    }
  }
  updateProgress(progress) {
    if (progress.type === 'success') {
      this.progress = null;
    } else {
      this.progress = progress;
    }
  }
  isReady() {
    return !this.progress;
  }
  getData() {
    return this.fileData;
  }
  isValidFile(data) {
    return data instanceof window.File || data instanceof window.Blob;
  }
  setData(data) {
    if (this.isValidFile(data)) {
      this.fileData = data;
    } else {
      throw new Error('Not a window.File instance', data);
    }
  }
  setMovedFromStaging() {
    delete this.info.guid;
  }
}

export default angular
  .module('lo.feedback.FileContainer', [FileUtils.name])
  .service('FileContainer', [
    'FileUtils',
    'AttachmentService',
    function (FileUtils, AttachmentService) {
      FileContainer.FileUtils = FileUtils;
      FileContainer.UrlBuilder = UrlBuilder; // crazy this is added here. Fortunately it means anywhere
      // it's used will also reference UrlBuilder directly.
      FileContainer.AttachmentService = AttachmentService;

      return FileContainer;
    },
  ]);
