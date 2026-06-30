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

import { extend, isObject } from 'lodash';

import UrlBuilder from '../../utilities/UrlBuilder.ts';
import { attachmentService } from '../../services/pure/attachmentService.ts';
import { fileUtils } from './fileUtils.ts';

/**
 * Pure FileContainer — the AngularJS `lo.feedback.FileContainer` model ported off DI. Where the Angular
 * adapter set `FileContainer.FileUtils` / `FileContainer.AttachmentService` (and `FileContainer.UrlBuilder`)
 * statics at construction time, this imports the pure `fileUtils` + `attachmentService` (and `UrlBuilder`)
 * singletons directly. The Angular adapter (FileContainer.js) re-exposes this class under the same module +
 * service name, keeping those statics for any code that read them off the injected constructor.
 */
export class FileContainer {
  info: any;
  url: any;
  fileData: any;
  canPreview: any;
  progress: any;

  constructor(fileInfo: any, fileUrl?: any) {
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

    this.canPreview = attachmentService.isWhitelisted(this.info);

    this.progress = null;
  }
  updateInfo(fileInfo: any) {
    if (fileInfo instanceof FileContainer) {
      extend(this, fileInfo);
    } else if (isObject(fileInfo.info)) {
      extend(this.info, fileInfo.info);
    } else if (fileInfo) {
      extend(this.info, fileUtils.processFile(fileInfo, this.url));
    }
  }
  updateProgress(progress: any) {
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
  isValidFile(data: any) {
    return data instanceof window.File || data instanceof window.Blob;
  }
  setData(data: any) {
    if (this.isValidFile(data)) {
      this.fileData = data;
    } else {
      throw new Error('Not a window.File instance');
    }
  }
  setMovedFromStaging() {
    delete this.info.guid;
  }
}

// crazy these are added here. Fortunately it means anywhere they're used will also reference the
// underlying singletons directly. Preserved from the Angular adapter for any code reading them off the
// constructor.
(FileContainer as any).FileUtils = fileUtils;
(FileContainer as any).UrlBuilder = UrlBuilder;
(FileContainer as any).AttachmentService = attachmentService;

export const fileContainer = FileContainer;

export default FileContainer;
