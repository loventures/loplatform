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

import { extend, map, filter, every } from 'lodash';

import { nativeQ as $q } from '../utilities/nativeQ.ts';
import { FileContainer as PureFileContainer } from './pure/fileContainer.ts';
import { attachmentService } from '../services/pure/attachmentService.ts';
import { openFeedbackModal } from './feedbackModal/feedbackModal.tsx';

class FeedbackManager {
  // Instance fields (assigned in the constructor). `declare` so no field init is
  // emitted under useDefineForClassFields — runtime assignment is unchanged.
  declare parentUrl: string;
  declare files: any[];
  declare thumbnailSizes: string[];
  declare ongoingFeedbackStatus: boolean;
  declare stagedForRemoval: any[];
  declare filesInProgress: number;
  declare activeTool: any;
  // Deps wired onto the prototype below (declare = ambient, no emit/shadowing).
  declare $q: typeof $q;
  declare FileContainer: typeof PureFileContainer;
  declare AttachmentService: typeof attachmentService;
  declare defaultThumbnailSizes: string[];

  constructor(files: any, parentUrl?: string, thumbnailSizes?: string[]) {
    this.parentUrl = this.parentUrl || parentUrl;
    if (files instanceof FeedbackManager) {
      extend(this, files);
    } else {
      this.files = map(files, file => {
        return new this.FileContainer(file, this.parentUrl);
      });

      this.thumbnailSizes = thumbnailSizes || this.defaultThumbnailSizes;

      this.ongoingFeedbackStatus = false;
    }
    this.stagedForRemoval = [];
    this.filesInProgress = 0;
    this.activeTool = null;
  }

  signalToolStatus(val: boolean) {
    this.ongoingFeedbackStatus = val;
  }

  getOngoingFeedbackType() {
    return this.ongoingFeedbackStatus ? this.activeTool : null;
  }

  hasOngoingFeedback() {
    return this.ongoingFeedbackStatus;
  }

  hasFiles() {
    return !!this.files.length;
  }

  hasInProgressFiles() {
    return this.filesInProgress > 0;
  }

  hasStagedFiles() {
    return filter(this.files, file => file.info.guid).length > 0;
  }

  hasStagedOrOngoing() {
    return this.hasOngoingFeedback() || this.hasStagedFiles() || this.stagedForRemoval.length;
  }

  confirmResetByModal() {
    if (this.hasOngoingFeedback()) {
      return $q.when(openFeedbackModal(this.getOngoingFeedbackType())).then(() => {
        this.updateActiveTool(null);
      });
    } else {
      return this.$q.when();
    }
  }

  isReady() {
    return (
      !this.files.length ||
      every(this.files, file => {
        return file.isReady();
      })
    );
  }

  updateActiveTool(type: any) {
    this.ongoingFeedbackStatus = false;
    this.activeTool = type;
  }

  addFile(file: any) {
    this.updateActiveTool(null);

    var newFile = new this.FileContainer(file, this.parentUrl);

    this.files.push(newFile);

    return this.stageFile(newFile);
  }

  stageFile(fileContainer: any) {
    var fileData = fileContainer.getData();

    this.filesInProgress += 1;

    return this.AttachmentService.uploadStaging(fileData)
      .then(
        fileInfo => {
          fileInfo.url = this.AttachmentService.getStagingUrl(fileInfo.guid);
          fileInfo.viewUrl = fileContainer.info.viewUrl;
          fileContainer.updateInfo(fileInfo);
        },
        errorProgress => {
          fileContainer.updateProgress(errorProgress);
        },
        progress => {
          fileContainer.updateProgress(progress);
        }
      )
      .finally(() => (this.filesInProgress -= 1));
  }

  toggleRemovalStaging(file: any) {
    if (file.info.guid) {
      //If the file is only in staging
      //remove it without waiting for confirmation
      return this.removeFile(file);
    }
    var stagedStatus = this.stagedForRemoval.indexOf(file);
    if (stagedStatus === -1) {
      var removalIndex = this.files.indexOf(file);

      if (removalIndex !== -1) {
        console.log('Removing the file from the file list. Index == ', removalIndex);
        this.files.splice(removalIndex, 1);
      }

      this.stagedForRemoval.push(file);
    } else {
      this.stagedForRemoval.splice(stagedStatus, 1);
    }

    return this.$q.when();
  }

  commitRemovingFiles() {
    return this._removeFiles(this.stagedForRemoval);
  }

  removeStagedFiles() {
    return this._removeFiles(this.getRawFilesInStaging());
  }

  clearStageFiles() {
    //Does not remove from server, make sure your staged files have been commited!
    this.files = [];
  }

  _removeFiles(files: any[]) {
    return this.$q.all(
      map(files, file => {
        console.log('Removing ', file);
        return this.removeFileFromServer(file);
      })
    );
  }

  removeFile(file: any) {
    var index = this.files.indexOf(file);
    if (index === -1) {
      return this.$q.reject();
    }

    return this.removeFileAt(index);
  }

  removeFileAt(index: number) {
    return this.removeFileFromServer(this.files[index]).then(() => {
      this.files.splice(index, 1);
    });
  }

  removeFileFromServer(file: any) {
    if (file.info.guid) {
      return this.AttachmentService.removeStaging(file.info.guid);
    } else if (file.info.id && file.url) {
      return this.AttachmentService.removeAttachment(file.url);
    } else {
      return this.$q.when();
    }
  }

  getRawFilesInStaging() {
    return this.files.filter(file => file.info.guid);
  }

  getFilesInStaging() {
    return this.getRawFilesInStaging().map(file => {
      return {
        guid: file.info.guid,
        sizes: this.thumbnailSizes,
      };
    });
  }

  getRemovalsInStaging() {
    return this.stagedForRemoval.map(file => file.info.id);
  }

  //this is the real getFilesInStaging, the above one should be getUploadsInStaging
  getFileInfoInStaging() {
    return this.getRawFilesInStaging().map(file => {
      const info = { ...file.info };
      delete info.viewUrl; //this is the $sce trusted object
      return info;
    });
  }

  getRawAttachedFiles() {
    return this.files.filter(file => file.info.id);
  }

  getAttachedFiles() {
    return this.getRawAttachedFiles().map(file => {
      const info = { ...file.info };
      delete info.viewUrl; //this is the $sce trusted object
      return info;
    });
  }
}

// Wire the deps onto the prototype from their pure singletons so the class is a pure,
// directly-importable singleton: `$q` is the native nativeQ; FileContainer/AttachmentService are
// the pure modules (the FeedbackModalService open() call is inlined as `openFeedbackModal` above).
FeedbackManager.prototype.$q = $q;
FeedbackManager.prototype.FileContainer = PureFileContainer;
FeedbackManager.prototype.AttachmentService = attachmentService;
// Was the Angular `.value('DefaultThumbnailSizes', ['medium'])` — inlined (drained off lojector).
FeedbackManager.prototype.defaultThumbnailSizes = ['medium'];

export { FeedbackManager };
