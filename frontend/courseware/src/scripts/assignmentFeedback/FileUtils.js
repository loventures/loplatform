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

var max_filename_length = 12;
var filler = '... ';
var imageTypes = ['jpg', 'jpeg', 'png'];

class FileUtils {
  constructor($sce, FileTypeIcons) {
    this.$sce = $sce;
    this.FileTypeIcons = FileTypeIcons;

    this.supportedImageTypes = imageTypes;
    this.filenameFiller = filler;
    this.maxFilenameLength = max_filename_length;
  }
  processFile(fileData, parentUrl) {
    var file = {};

    if (fileData.guid) {
      file.guid = fileData.guid;
    } else if (fileData.id) {
      file.id = fileData.id;
    }

    if (fileData.name || fileData.fileName) {
      file.fileName = fileData.name || fileData.fileName;
      file.extension = this.getExtension(file.fileName);
      file.displayName = this.getDisplayName(file.fileName);
    }

    file.playType = this.getPlayType(fileData);

    file.url = parentUrl;

    if (fileData.guid) {
      this.stagedFileUrls(file, fileData);
    } else if (fileData.id) {
      this.attachmentFileUrls(file, fileData);
    } else {
      this.unstagedFileUrls(file, fileData);
    }

    file.iconClass = this.getIcon(file);

    return file;
  }
  unstagedFileUrls(file, fileData) {
    file.url = fileData.viewUrl;
    file.viewUrl = this.entrust(fileData.viewUrl);
  }
  stagedFileUrls(file, fileData) {
    file.url = fileData.url;
    file.viewUrl = this.entrust(fileData.viewUrl || file.url);
    file.downloadUrl = fileData.downloadUrl || file.viewUrl;
    if (this.hasThumbnail(file.extension)) {
      file.thumbnailUrl = this.getThumbnailUrl(file.viewUrl);
    }
  }
  attachmentFileUrls(file, fileData) {
    //file.url = fileData.url;
    file.viewUrl = this.entrust(fileData.viewUrl || this.getViewUrl(file.url));
    file.downloadUrl = fileData.downloadUrl || this.getDownloadUrl(file.viewUrl.toString());

    if (this.hasThumbnail(file.extension)) {
      file.thumbnailUrl = fileData.thumbnailUrl || this.getThumbnailUrl(file.viewUrl.toString());
    }
  }
  entrust(urlOrHolder) {
    if (urlOrHolder && urlOrHolder.toString) {
      return this.$sce.trustAsResourceUrl(urlOrHolder.toString());
    } else {
      return '';
    }
  }
  getExtension(name) {
    return name.slice(name.lastIndexOf('.') + 1).toLowerCase();
  }
  getDisplayName(fileName) {
    var lastDot = fileName.lastIndexOf('.');
    var extension = lastDot < 0 ? '' : fileName.slice(lastDot);
    var name = lastDot < 0 ? fileName : fileName.slice(0, lastDot);

    if (name.length > this.maxFilenameLength) {
      name = name.substring(0, this.maxFilenameLength) + this.filenameFiller;
    }

    return name + extension;
  }
  getViewUrl(url) {
    return url + '/view';
  }
  getDownloadUrl(viewUrl) {
    return viewUrl + '?download=true';
  }
  getThumbnailUrl(viewUrl) {
    return viewUrl + ';size=medium';
  }
  hasThumbnail(extension) {
    return this.supportedImageTypes.indexOf(extension) !== -1;
  }
  getPlayType(file) {
    if (file.fileName === 'blob' || (file.mimeType && file.mimeType.match('audio'))) {
      return 'audio';
    } else {
      return 'file';
    }
  }
  getIcon(file) {
    if (file.thumbnailUrl) {
      return;
    } else if (file.playType === 'audio') {
      return 'icon-file-music';
    } else if (file.mimeType && file.mimeType.match('video')) {
      return 'icon-file-video';
    } else {
      return this.iconMap(file.extension);
    }
  }

  iconMap(extension) {
    return this.FileTypeIcons[extension] || 'icon-file-empty';
  }
}

export default angular
  .module('lo.feedback.FileUtils', [])
  .service('FileUtils', ['$sce', 'FileTypeIcons', FileUtils])
  .constant('FileTypeIcons', {
    zip: 'icon-file-zip',
    xml: 'icon-file-xml',
    csv: 'icon-file-spreadsheet',
    txt: 'icon-file-text',
    doc: 'icon-file-text',
    docx: 'icon-file-text',
  });
