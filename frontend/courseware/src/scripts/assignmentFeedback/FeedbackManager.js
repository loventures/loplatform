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

import { extend, map, filter, every } from 'lodash';

import FileContainer from './FileContainer.js';
import AttachmentService from '../services/AttachmentService.js';
import FeedbackModalService from './feedbackModal/FeedbackModalService.js';
import DefaultThumbnailSizes from './DefaultThumbnailSizes.js';

class FeedbackManager {
  constructor(files, parentUrl, thumbnailSizes) {
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

  signalToolStatus(val) {
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
      return this.FeedbackModalService.open(this.getOngoingFeedbackType()).then(() => {
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

  updateActiveTool(type) {
    this.ongoingFeedbackStatus = false;
    this.activeTool = type;
  }

  addFile(file) {
    this.updateActiveTool(null);

    var newFile = new this.FileContainer(file, this.parentUrl);

    this.files.push(newFile);

    return this.stageFile(newFile);
  }

  stageFile(fileContainer) {
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

  toggleRemovalStaging(file) {
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

  _removeFiles(files) {
    return this.$q.all(
      map(files, file => {
        console.log('Removing ', file);
        return this.removeFileFromServer(file);
      })
    );
  }

  removeFile(file) {
    var index = this.files.indexOf(file);
    if (index === -1) {
      return this.$q.reject();
    }

    return this.removeFileAt(index);
  }

  removeFileAt(index) {
    return this.removeFileFromServer(this.files[index]).then(() => {
      this.files.splice(index, 1);
    });
  }

  removeFileFromServer(file) {
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

export default angular
  .module('lo.feedback.FeedbackManager', [
    FileContainer.name,
    AttachmentService.name,
    FeedbackModalService.name,
    DefaultThumbnailSizes.name,
  ])
  .service('FeedbackManager', [
    '$q',
    'FileContainer',
    'AttachmentService',
    'FeedbackModalService',
    'DefaultThumbnailSizes',
    function ($q, FileContainer, AttachmentService, FeedbackModalService, DefaultThumbnailSizes) {
      FeedbackManager.prototype.$q = $q;
      FeedbackManager.prototype.FileContainer = FileContainer;
      FeedbackManager.prototype.AttachmentService = AttachmentService;
      FeedbackManager.prototype.FeedbackModalService = FeedbackModalService;
      FeedbackManager.prototype.defaultThumbnailSizes = DefaultThumbnailSizes;

      return FeedbackManager;
    },
  ]);
