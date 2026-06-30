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
 * Pure FileUtils — the AngularJS `lo.feedback.FileUtils` service ported off the `$sce` injection.
 * `$sce.trustAsResourceUrl(url)` becomes the raw `url` (no $sce trusted-types system in React; a no-op).
 * `FileTypeIcons` is now a plain constant imported directly. The Angular adapter (FileUtils.js) re-exposes
 * the `fileUtils` singleton + the `FileTypeIcons` constant under the same module + names.
 */

const max_filename_length = 12;
const filler = '... ';
const imageTypes = ['jpg', 'jpeg', 'png'];

export const FileTypeIcons: Record<string, string> = {
  zip: 'icon-file-zip',
  xml: 'icon-file-xml',
  csv: 'icon-file-spreadsheet',
  txt: 'icon-file-text',
  doc: 'icon-file-text',
  docx: 'icon-file-text',
};

export class FileUtils {
  FileTypeIcons: Record<string, string>;
  supportedImageTypes: string[];
  filenameFiller: string;
  maxFilenameLength: number;

  constructor(fileTypeIcons: Record<string, string>) {
    this.FileTypeIcons = fileTypeIcons;

    this.supportedImageTypes = imageTypes;
    this.filenameFiller = filler;
    this.maxFilenameLength = max_filename_length;
  }
  processFile(fileData: any, parentUrl: any) {
    var file: any = {};

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
  unstagedFileUrls(file: any, fileData: any) {
    file.url = fileData.viewUrl;
    file.viewUrl = this.entrust(fileData.viewUrl);
  }
  stagedFileUrls(file: any, fileData: any) {
    file.url = fileData.url;
    file.viewUrl = this.entrust(fileData.viewUrl || file.url);
    file.downloadUrl = fileData.downloadUrl || file.viewUrl;
    if (this.hasThumbnail(file.extension)) {
      file.thumbnailUrl = this.getThumbnailUrl(file.viewUrl);
    }
  }
  attachmentFileUrls(file: any, fileData: any) {
    //file.url = fileData.url;
    file.viewUrl = this.entrust(fileData.viewUrl || this.getViewUrl(file.url));
    file.downloadUrl = fileData.downloadUrl || this.getDownloadUrl(file.viewUrl.toString());

    if (this.hasThumbnail(file.extension)) {
      file.thumbnailUrl = fileData.thumbnailUrl || this.getThumbnailUrl(file.viewUrl.toString());
    }
  }
  entrust(urlOrHolder: any) {
    if (urlOrHolder && urlOrHolder.toString) {
      // $sce.trustAsResourceUrl -> raw url string (no-op in React).
      return urlOrHolder.toString();
    } else {
      return '';
    }
  }
  getExtension(name: string) {
    return name.slice(name.lastIndexOf('.') + 1).toLowerCase();
  }
  getDisplayName(fileName: string) {
    var lastDot = fileName.lastIndexOf('.');
    var extension = lastDot < 0 ? '' : fileName.slice(lastDot);
    var name = lastDot < 0 ? fileName : fileName.slice(0, lastDot);

    if (name.length > this.maxFilenameLength) {
      name = name.substring(0, this.maxFilenameLength) + this.filenameFiller;
    }

    return name + extension;
  }
  getViewUrl(url: any) {
    return url + '/view';
  }
  getDownloadUrl(viewUrl: any) {
    return viewUrl + '?download=true';
  }
  getThumbnailUrl(viewUrl: any) {
    return viewUrl + ';size=medium';
  }
  hasThumbnail(extension: string) {
    return this.supportedImageTypes.indexOf(extension) !== -1;
  }
  getPlayType(file: any) {
    if (file.fileName === 'blob' || (file.mimeType && file.mimeType.match('audio'))) {
      return 'audio';
    } else {
      return 'file';
    }
  }
  getIcon(file: any) {
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

  iconMap(extension: string) {
    return this.FileTypeIcons[extension] || 'icon-file-empty';
  }
}

export const fileUtils = new FileUtils(FileTypeIcons);

export default fileUtils;
