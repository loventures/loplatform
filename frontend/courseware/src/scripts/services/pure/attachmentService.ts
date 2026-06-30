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

import dayjs from 'dayjs';
import { each, filter, head, identity, isEmpty, last, map, parseInt } from 'lodash';

import { loConfig } from '../../bootstrap/loConfig.ts';
import { instant } from '../../i18n/pure/i18n.ts';
import UrlBuilder from '../../utilities/UrlBuilder.ts';
import { nativeQ as $q } from '../../utilities/nativeQ.ts';
import { request } from '../../utilities/request.ts';
import { settings as Settings } from '../../utilities/settingsService.ts';

/**
 * Pure AttachmentService — the AngularJS `lo.services.AttachmentService` factory ported off
 * `$q`/`$sce`/`$timeout`/`$translate`/`Settings`/`Request` injection. Transforms (user-approved):
 *  - `$q` -> the native-Promise `nativeQ` (`$q.when/all/reject/defer` call sites unchanged), with a
 *    progress-aware `defer()` (see `deferWithNotify`) because `queueStaging` calls `deferred.notify`.
 *  - `$sce.trustAsResourceUrl(url)` -> `url` (no $sce in React; a no-op).
 *  - `$timeout(fn, delay)` -> `new Promise(r => setTimeout(() => r(fn()), delay))` (the boxview retry
 *    delay is a real computed `readyTime.diff(now)`, not a tight loop).
 *  - `$translate.instant(key)` -> the pure `instant(key)` from `i18n/pure/i18n.ts`.
 *  - `Settings` -> the pure `settings` singleton; `Request` -> the native `request` (X-CSRF / X-UserId
 *    inherited from the global axios defaults). Multipart uploads go through `request.http(config)` with
 *    axios `onUploadProgress` (ported off ng-file-upload in #1588).
 *
 * The Angular adapter (`services/AttachmentService.js`) re-exposes this singleton under the same module +
 * service name so the graders (DI, Phase 5) + FeedbackManager + feedback directives (lojector) keep resolving it.
 */

// $q-style deferred that also supports `.notify(value)` (progress). `nativeQ.defer()` is
// native-Promise-backed and has no `notify`, but `queueStaging` reports upload progress through it and
// `fileStagingUtils` consumes that progress via the third `.then(success, error, progress)` callback.
function deferWithNotify(): any {
  const progressHandlers: Array<(value: any) => void> = [];
  let resolveFn!: (value?: any) => void;
  let rejectFn!: (reason?: unknown) => void;
  const nativePromise = new Promise<any>((res, rej) => {
    resolveFn = res;
    rejectFn = rej;
  });

  // Wrap the native promise so `.then`'s third (progress) argument is honoured the way `$q` did.
  const promise: any = {
    then(onFulfilled?: any, onRejected?: any, onProgress?: any) {
      if (onProgress) {
        progressHandlers.push(onProgress);
      }
      return nativePromise.then(onFulfilled, onRejected);
    },
    catch(onRejected?: any) {
      return nativePromise.catch(onRejected);
    },
    finally(onFinally?: any) {
      return nativePromise.finally(onFinally);
    },
  };

  return {
    promise,
    resolve: resolveFn,
    reject: rejectFn,
    notify(value: any) {
      each(progressHandlers, function (h) {
        h(value);
      });
    },
  };
}

// Multipart upload through the native axios `request`. `request.http` carries the X-CSRF / X-UserId
// global axios defaults, so no manual header shim is needed, and axios `onUploadProgress` replaces
// ng-file-upload's `.progress`.
function uploadFile(url: any, file: any, onUploadProgress: any) {
  const formData = new FormData();
  formData.append('file', file);
  return request.http({ url: url, method: 'POST', data: formData, onUploadProgress: onUploadProgress });
}

/** @alias AttachmentService **/
const AttachmentService: any = {};

/**
 * Determine the list of file extensions we use to load into a page vs provide a download link.
 * Primarily image extensions. Provides a default if not set; you would have to specify an empty
 * values array to have no preview.
 */
const list = Settings.getSettings('EssayUploadPreviewFileTypes') || [
  'png',
  'jpeg',
  'jpg',
  'gif',
  'svg',
  'pdf',
];
AttachmentService.whitelist = {};
each(list, function (l: string) {
  AttachmentService.whitelist[l] = true;
});

AttachmentService.isWhitelisted = function (attachment: any) {
  if (!attachment) {
    return false;
  }
  if (!attachment.loaded && !attachment.viewUrl) {
    return false;
  }

  if (attachment.fileName && AttachmentService.whitelist[last(attachment.fileName.split('.')) as string]) {
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
AttachmentService._ensureType = function (file: any) {
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

AttachmentService.guessMimeType = function (file: any) {
  return file.mimeType || file.type;
};

AttachmentService.generateFileName = function (file: any) {
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

AttachmentService._completeFileProperties = function (file: any) {
  if (!file.fileName && !file.name) {
    //Name is what the browser uses, fileName is our internal name.
    throw new Error('File has no file name');
  }
  file.fileName = file.fileName || file.name;
  file.name = file.fileName || file.name;

  // default to octet-stream if browser can't determine type
  file.mimeType = file.mimeType || file.type || 'application/octet-stream';

  AttachmentService._ensureType(file);
};

AttachmentService.getIntegrationSession = function (attachment: any, integration: any) {
  return request.promiseRequest(
    attachment.url + '/integrations/' + integration.id + '/session',
    'post'
  );
};

AttachmentService.getIntegrationDetail = function (attachment: any, integration: any) {
  return request.promiseRequest(attachment.url + '/integrations/' + integration.id);
};

AttachmentService._checkBoxviewStatus = function (attachment: any, boxviewIntegration: any) {
  var readyTime = dayjs(boxviewIntegration.retryAfter);
  var now = dayjs();

  attachment.url = attachment.url.replace(';embed=integrations', '');
  if (readyTime.isBefore(now)) {
    return AttachmentService.getIntegrationSession(attachment, boxviewIntegration).then(function (
      boxviewSession: any
    ) {
      if (boxviewSession && boxviewSession.urls && boxviewSession.urls.view) {
        // $sce.trustAsResourceUrl -> raw url (no-op in React).
        attachment.viewUrl = boxviewSession.urls.view;
      }
      attachment.loaded = true;
      return attachment;
    });
  } else {
    // $timeout(fn, delay) -> Promise that runs fn after `delay`, resolving its (promise) return value.
    return new Promise(resolve =>
      setTimeout(
        () =>
          resolve(
            AttachmentService.getIntegrationDetail(attachment, boxviewIntegration).then(function (
              updatedIntegration: any
            ) {
              return AttachmentService._checkBoxviewStatus(attachment, updatedIntegration);
            })
          ),
        readyTime.diff(now)
      )
    );
  }
};

AttachmentService.afterGet = function (attachmentUrl: any, attachment: any) {
  if (attachment) {
    AttachmentService._completeFileProperties(attachment);
  }

  attachmentUrl = (UrlBuilder as any).create(attachmentUrl);
  attachment.url = attachmentUrl.baseUrl() + '/' + attachment.id;
  attachment.viewUrl = attachment.url + '/view';
  attachment.downloadUrl = attachment.viewUrl + '?download=true';

  var integrationLoading: any = false;
  each(attachment.integrations, function (integration: any) {
    if (integration._type.match(/boxView/i)) {
      integrationLoading = AttachmentService._checkBoxviewStatus(attachment, integration);
    }
  });
  if (isEmpty(attachment.integrations) || !integrationLoading) {
    attachment.loaded = true;
  }

  return attachment;
};

AttachmentService.afterGetAttachments = function (attachmentUrl: any, attachments: any) {
  return map(attachments, function (attachment: any) {
    return AttachmentService.afterGet(attachmentUrl, attachment);
  });
};

AttachmentService.getAttachments = function (attachmentUrl: any) {
  attachmentUrl = (UrlBuilder as any).create(attachmentUrl);

  attachmentUrl.query.addEmbeds('integrations');
  return request.promiseRequest(attachmentUrl, 'get').then(function (data: any) {
    return AttachmentService.afterGetAttachments(attachmentUrl, data);
  });
};

AttachmentService.uploadAttachments = function (attachmentUrl: any, files: any) {
  if (isEmpty(files)) {
    return $q.when([]);
  } else {
    files = filter(files, identity);

    var url = attachmentUrl.toString() + '/upload';

    each(
      map(files, function (file: any) {
        return file.data;
      }),
      AttachmentService._completeFileProperties
    );

    var uploads = map(files, function (file: any) {
      return uploadFile(url, file.data, function (evt: any) {
        var progress = parseInt(((100.0 * evt.loaded) / evt.total) as any);
        file.uploadProgress = progress;
        console.log(file.name + ' upload progress: ' + progress);
        if (parseInt(progress as any) === 100) {
          file.serverProcessing = true;
        }
      }).then(
        function (response: any) {
          file.serverProcessing = false;
          console.log('upload success');
          return head(response.data.objects);
        },
        function (e: any) {
          console.error('upload failed', e);
          return $q.reject(e);
        }
      );
    });

    return $q.all(uploads).then(function (responses: any) {
      return AttachmentService.afterGetAttachments(attachmentUrl, responses);
    });
  }
};

AttachmentService.removeAttachment = function (attachmentUrl: any) {
  return request.promiseRequest(attachmentUrl, 'delete');
};

AttachmentService.createSuccessProgress = function () {
  return {
    percent: 100,
    type: 'success',
    status: instant('UPLOAD_SUCCESSFUL'),
  };
};

AttachmentService.createErrorProgress = function () {
  return {
    percent: 0,
    type: 'error',
    status: instant('UPLOAD_ERROR'),
  };
};

AttachmentService.createProgress = function (event: any) {
  var progress: any = {};
  progress.percent = Math.round((100 * event.loaded) / event.total);
  progress.type = 'info';

  if (progress.percent < 100) {
    progress.status = progress.percent + '%';
  } else {
    progress.status = instant('UPLOAD_SERVER_PROCESSING');
  }
  return progress;
};

AttachmentService.getStagingUrl = function (guid: any) {
  return loConfig.fileUpload.upload + '/' + guid;
};

AttachmentService.uploadStaging = function (file: any) {
  return AttachmentService.queueStaging(file);
};

AttachmentService.removeStaging = function (guid: any) {
  return request.promiseRequest(AttachmentService.getStagingUrl(guid), 'delete');
};

AttachmentService.stagingQueue = $q.when();

AttachmentService.queueStaging = function (fileData: any) {
  var deferred = deferWithNotify();

  AttachmentService.stagingQueue = AttachmentService.stagingQueue.then(function () {
    return uploadFile(loConfig.fileUpload.upload, fileData, function (event: any) {
      deferred.notify(AttachmentService.createProgress(event));
    }).then(
      function (response: any) {
        deferred.notify(AttachmentService.createSuccessProgress());
        if (response.status >= 400) {
          deferred.reject(response.data);
        } else {
          deferred.resolve(response.data);
        }
        return $q.when();
      },
      function (error: any) {
        console.error('upload failed', error);
        deferred.reject(AttachmentService.createErrorProgress());
        //still need to continue the queue
        return $q.when();
      }
    );
  });

  return deferred.promise;
};

export const attachmentService = AttachmentService;

export default attachmentService;
