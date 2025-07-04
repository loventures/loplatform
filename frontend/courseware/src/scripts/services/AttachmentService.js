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

import { loConfig } from '../bootstrap/loConfig.js';
import dayjs from 'dayjs';
import { each, filter, head, identity, isEmpty, last, map, parseInt } from 'lodash';
import ngFileUpload from 'ng-file-upload';
import UrlBuilder from '../utilities/UrlBuilder.js';

export default angular
  .module('lo.services.AttachmentService', [ngFileUpload])
  .factory('AttachmentService', [
    '$q',
    '$sce',
    'Upload',
    '$timeout',
    '$translate',
    'Settings',
    'Request',
    /**
     * @ngdoc service
     * @alias AttachmentService
     * @memberof lo.services
     * @description
     *  uploading and getting attachments, with some file properties guessing
     *  also deals with boxview
     */
    function AttachmentService($q, $sce, Upload, $timeout, $translate, Settings, Request) {
      /** @alias AttachmentService **/
      var AttachmentService = {};

      /**
       * @ngdoc property
       * @name EssayUploadPreviewFileTypes{Feature}
       * @memberof lo.utilities.Settings
       * @description Determine the list of file extensions we use to load into a page vs provide
       * a download link.   Primarily image extensions.  Provides a default if not set, you would have
       * to specify an empty values array to have no preview
       */
      var list = Settings.getSettings('EssayUploadPreviewFileTypes') || [
        'png',
        'jpeg',
        'jpg',
        'gif',
        'svg',
        'pdf',
      ];
      AttachmentService.whitelist = {};
      each(list, function (l) {
        AttachmentService.whitelist[l] = true;
      });

      /**
       * @description This will try and help the UI to determine when attachments which can be
       * dynamically loaded and post processed are ready to view, or even should be viewed.
       * @params {Object} attachment an attachment that came back from the available APIs
       * @returns {boolean} true if the attachment is loaded and has a viewUrl, in addition
       * it will set the whitelisted = true flag on the attachment to allow you to save on
       * service calls.
       */
      AttachmentService.isWhitelisted = function (attachment) {
        if (!attachment) {
          return false;
        }
        if (!attachment.loaded && !attachment.viewUrl) {
          return false;
        }

        if (
          attachment.fileName &&
          AttachmentService.whitelist[last(attachment.fileName.split('.'))]
        ) {
          attachment.whitelisted = true;
        }

        if (!isEmpty(attachment.integrations)) {
          var integ = null;
          for (var i = 0; i < attachment.integrations.length; ++i) {
            integ = attachment.integrations[i];
            if (integ._type && integ._type.match(/boxView/i)) {
              attachment.whitelisted = true;
            }
          }
        }
        return attachment.whitelisted || false;
      };

      //this type is the type used by our app
      //to determine what to do with it
      AttachmentService._ensureType = function (file) {
        if (file.type) {
          return;
        }

        console.warn('Guessing file type!', file);

        if (
          file.fileName === 'undefined' || //chrome
          file.fileName === 'blob' || //firefox
          (file.mimeType && file.mimeType.match('audio'))
        ) {
          file.type = 'audio';
        } else {
          file.type = 'file';
        }
      };

      /**
       * @description
       *     make a guess on the file's mimeType
       * @param {Object} file the file to be checked
       */
      AttachmentService.guessMimeType = function (file) {
        return file.mimeType || file.type;
      };

      /**
       * @description
       *     generate a filename with extension that makes sense
       * @param {Object} file the file to be named
       */
      AttachmentService.generateFileName = function (file) {
        if (file.fileName) {
          return;
        }

        if (file.name) {
          return file.name;
        }

        console.warn('File without a name!');
        var ext = '';
        if (!file.name && file.type && file.type.match('wav')) {
          ext = '.wav';
        }
        return 'lo-attachment-' + dayjs() + ext;
      };

      /**
       * @description
       *      checks various properties
       *      if some property doesn't exist, guess its value based on other properties
       *      should modify the original file
       * @param {Object} file the file to be checked
       */
      AttachmentService._completeFileProperties = function (file) {
        if (!file.fileName && !file.name) {
          //Name is what the browser uses, fileName is our internal name.
          throw new Error('File has no file name', file);
        }
        file.fileName = file.fileName || file.name;
        file.name = file.fileName || file.name;

        // default to octet-stream if browser can't determine type
        file.mimeType = file.mimeType || file.type || 'application/octet-stream';

        AttachmentService._ensureType(file);
      };

      /**
       * @description
       *      Ask for the session info between our server and the integrated service
       *      Should contain everything we need from the integrated service
       * @param {Object} attachment the attachment file to be processed
       * @param {Object} integration the integration info for this service
       * @return {Promise} resolves the integration session info
       */
      AttachmentService.getIntegrationSession = function (attachment, integration) {
        return Request.promiseRequest(
          attachment.url + '/integrations/' + integration.id + '/session',
          'post'
        );
      };

      /**
       * @description
       *      Ask for the integration info on the integration process
       *      Should contain information about the status and progress of the external service
       * @param {Object} attachment the attachment file to be processed
       * @param {Object} integration the integration info for this service
       * @return {Promise} resolves the integration info
       */
      AttachmentService.getIntegrationDetail = function (attachment, integration) {
        return Request.promiseRequest(attachment.url + '/integrations/' + integration.id);
      };

      /**
       * @description
       *      checks if boxview is ready
       *      change the viewUrl if it is ready,
       *      otherwise kicks off polling
       * @param {Object} attachment the attachment file to be processed
       * @return {Promise} eventually resolves the attachment after boxview is ready
       */
      AttachmentService._checkBoxviewStatus = function (attachment, boxviewIntegration) {
        var readyTime = dayjs(boxviewIntegration.retryAfter);
        var now = dayjs();

        attachment.url = attachment.url.replace(';embed=integrations', '');
        if (readyTime.isBefore(now)) {
          return AttachmentService.getIntegrationSession(attachment, boxviewIntegration).then(
            function (boxviewSession) {
              if (boxviewSession && boxviewSession.urls && boxviewSession.urls.view) {
                attachment.viewUrl = $sce.trustAsResourceUrl(boxviewSession.urls.view);
              }
              attachment.loaded = true;
              return attachment;
            }
          );
        } else {
          return $timeout(function () {
            return AttachmentService.getIntegrationDetail(attachment, boxviewIntegration).then(
              function (updatedIntegration) {
                return AttachmentService._checkBoxviewStatus(attachment, updatedIntegration);
              }
            );
          }, readyTime.diff(now));
        }
      };

      /**
       * @description
       *      process an attachments from the server
       *      ensures important properties are present
       *      attaches a viewUrl and checks boxview
       * @param {String} attachmentUrl url of the parent of the attachments
       * @param {Object} attachment the attachment file to be processed
       * @return {Object} the same attachment after processing
       */
      AttachmentService.afterGet = function (attachmentUrl, attachment) {
        if (attachment) {
          AttachmentService._completeFileProperties(attachment);
        }

        attachmentUrl = UrlBuilder.create(attachmentUrl);
        attachment.url = attachmentUrl.baseUrl() + '/' + attachment.id;
        attachment.viewUrl = attachment.url + '/view';
        attachment.downloadUrl = attachment.viewUrl + '?download=true';

        var integrationLoading = false;
        each(attachment.integrations, function (integration) {
          if (integration._type.match(/boxView/i)) {
            integrationLoading = AttachmentService._checkBoxviewStatus(attachment, integration);
          }
        });
        if (isEmpty(attachment.integrations) || !integrationLoading) {
          attachment.loaded = true;
        }

        return attachment;
      };

      /**
       * @description
       *      batch version of attachmentService.afterGet
       *      called implicitly by attachmentService.getAttachmentsFor
       *      should be called explicitly for embeded attachments
       * @param {String} attachmentUrl url of the parent of the attachments
       * @param {Array} attachments array of files to process
       * @return {Array} the array of files after processing
       */
      AttachmentService.afterGetAttachments = function (attachmentUrl, attachments) {
        return map(attachments, function (attachment) {
          return AttachmentService.afterGet(attachmentUrl, attachment);
        });
      };

      /**
       * @description
       *      get all attachments for a resource object
       *      usually attachments are gotten through embed=attachments
       *      but there are exceptions like essay question uploads
       * @param {String} attachmentUrl url of the resource object to get attachments for
       * @return {Promise} resolves the files that are the attachments
       */
      AttachmentService.getAttachments = function (attachmentUrl) {
        attachmentUrl = UrlBuilder.create(attachmentUrl);

        attachmentUrl.query.addEmbeds('integrations');
        return Request.promiseRequest(attachmentUrl, 'get').then(function (data) {
          return AttachmentService.afterGetAttachments(attachmentUrl, data);
        });
      };

      /**
       * @description
       *      upload a list of files as attachments to some resource
       * @param {String} attachmentUrl url of the resource object to attach to
       * @param {Array} files array of files to upload
       * @return {Promise} resolves the files that are uploaded
       */
      AttachmentService.uploadAttachments = function (attachmentUrl, files) {
        if (isEmpty(files)) {
          return $q.when([]);
        } else {
          files = filter(files, identity);

          var url = attachmentUrl.toString() + '/upload';

          each(
            map(files, function (file) {
              return file.data;
            }),
            AttachmentService._completeFileProperties
          );

          var uploads = map(files, function (file) {
            return Upload.upload({
              url: url,
              file: file.data,
            })
              .progress(function (evt) {
                var progress = parseInt((100.0 * evt.loaded) / evt.total);
                file.uploadProgress = progress;
                console.log(file.name + ' upload progress: ' + progress);
                if (parseInt(progress) === 100) {
                  file.serverProcessing = true;
                }
              })
              .success(function () {
                file.serverProcessing = false;
                console.log('upload success');
              })
              .error(function (e) {
                //this error is in a separate resolve chain
                console.error('upload failed', e);
              })
              .then(function (response) {
                return head(response.data.objects);
              });
          });

          return $q.all(uploads).then(function (responses) {
            return AttachmentService.afterGetAttachments(attachmentUrl, responses);
          });
        }
      };

      AttachmentService.removeAttachment = function (attachmentUrl) {
        return Request.promiseRequest(attachmentUrl, 'delete');
      };

      AttachmentService.createSuccessProgress = function () {
        return {
          percent: 100,
          type: 'success',
          status: $translate.instant('UPLOAD_SUCCESSFUL'),
        };
      };

      AttachmentService.createErrorProgress = function () {
        return {
          percent: 0,
          type: 'error',
          status: $translate.instant('UPLOAD_ERROR'),
        };
      };

      AttachmentService.createProgress = function (event) {
        var progress = {};
        progress.percent = Math.round((100 * event.loaded) / event.total);
        progress.type = 'info';

        if (progress.percent < 100) {
          progress.status = progress.percent + '%';
        } else {
          progress.status = $translate.instant('UPLOAD_SERVER_PROCESSING');
        }
        return progress;
      };

      AttachmentService.getStagingUrl = function (guid) {
        return loConfig.fileUpload.upload + '/' + guid;
      };

      AttachmentService.uploadStaging = function (file) {
        return AttachmentService.queueStaging(file);
      };

      AttachmentService.removeStaging = function (guid) {
        return Request.promiseRequest(AttachmentService.getStagingUrl(guid), 'delete');
      };

      AttachmentService.stagingQueue = $q.when();

      AttachmentService.queueStaging = function (fileData) {
        var deferred = $q.defer();

        AttachmentService.stagingQueue = AttachmentService.stagingQueue.then(function () {
          return Upload.upload({
            url: loConfig.fileUpload.upload,
            file: fileData,
          })
            .progress(function (event) {
              deferred.notify(AttachmentService.createProgress(event));
            })
            .success(function () {
              deferred.notify(AttachmentService.createSuccessProgress());
            })
            .error(function (error) {
              console.error('upload error', error);
              deferred.reject(AttachmentService.createErrorProgress());
            })
            .then(
              function (response) {
                if (response.status >= 400) {
                  deferred.reject(response.data);
                } else {
                  deferred.resolve(response.data);
                }
                return $q.when();
              },
              function (error) {
                console.error('upload failed', error);
                deferred.reject(AttachmentService.createErrorProgress());
                //still need to continue the queue
                return $q.when();
              }
            );
        });

        return deferred.promise;
      };

      return AttachmentService;
    },
  ]);
