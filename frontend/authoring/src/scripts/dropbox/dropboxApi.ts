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

import gretchen from '../grfetchen/';

import { ApiQueryResults, encodeQuery } from '../srs/apiQuery';

export type ProfileDto = {
  id: number;
  handle: string;
  givenName: string;
  fullName: string;
  thumbnailId: number | null;
};

export type DropboxFileDto = {
  id: number;
  project: number;
  branch: number;
  asset: string;
  archived: boolean;
  created: string;
  creator?: ProfileDto;
  fileName: string;
  size: number;
  directory: boolean;
  folder?: number;
};

export type NewDropboxFile = {
  branch: number;
  asset: string;
  file: string;
};

export type DropboxParams = {
  branch?: number;
  folder?: number | null | Array<number | 'null'>;
  asset?: string;
  archived?: boolean;
  fileName?: string;
  directory?: true;
  nameCmp?: string;
  offset?: number;
  limit?: number;
};

export const dropboxQuery = (branch: string, params: Partial<DropboxParams>): string =>
  encodeQuery({
    prefilter: [
      { property: 'branch', operator: 'eq', value: branch },
      params.asset && {
        property: 'asset',
        operator: 'eq',
        value: params.asset,
      },
      Array.isArray(params.folder)
        ? {
            property: 'folder',
            operator: 'in',
            value: params.folder.join(','),
          }
        : params.folder
          ? {
              property: 'folder',
              operator: 'eq',
              value: params.folder,
            }
          : params.folder === null && { property: 'folder', operator: 'isNull' },
      params.archived != null && { property: 'archived', operator: 'eq', value: params.archived },
      params.directory && { property: 'isDirectory' },
    ],
    filter: [
      params.fileName && {
        property: 'file.fileName',
        operator: params.nameCmp,
        value: params.fileName,
      },
    ],
    order: [
      { property: 'file.fileName', direction: 'asc' },
      {
        property: 'file.createTime',
        direction: 'desc',
      },
    ],
    offset: params.offset,
    limit: params.limit,
  });

export const loadDropboxFile = (project: number, id: number): Promise<DropboxFileDto> =>
  gretchen.get('/api/v2/authoring/dropbox/:project/:id').params({ project, id }).exec();

export const loadDropboxFiles = (
  project: number,
  branch: string,
  params: DropboxParams
): Promise<ApiQueryResults<DropboxFileDto>> =>
  gretchen
    .get('/api/v2/authoring/dropbox/:project' + dropboxQuery(branch, params))
    .params({ project })
    .exec();

export const uploadDropboxFile = (
  project: number,
  branch: string,
  asset: string,
  folder: number | undefined,
  file: string
): Promise<DropboxFileDto> =>
  gretchen
    .post(`/api/v2/authoring/dropbox/:project`)
    .params({ project })
    .data({ branch: parseInt(branch), asset, folder, file })
    .exec();

export const createDropboxFolder = (
  project: number,
  branch: string,
  asset: string,
  folder: number | undefined,
  directory: string
): Promise<DropboxFileDto> =>
  gretchen
    .post(`/api/v2/authoring/dropbox/:project`)
    .params({ project })
    .data({ branch: parseInt(branch), asset, folder, directory })
    .exec();

export const archiveDropboxFile = (
  project: number,
  id: number,
  archived: boolean
): Promise<DropboxFileDto> =>
  gretchen
    .post(`/api/v2/authoring/dropbox/:project/:id/archive`)
    .params({ project, id })
    .data({ archived })
    .exec();

export const renameDropboxFile = (
  project: number,
  id: number,
  fileName: string
): Promise<DropboxFileDto> =>
  gretchen
    .post(`/api/v2/authoring/dropbox/:project/:id/rename`)
    .params({ project, id })
    .data({ fileName })
    .exec();

export const moveDropboxFile = (
  project: number,
  id: number,
  folder: number | null
): Promise<DropboxFileDto> =>
  gretchen
    .post(`/api/v2/authoring/dropbox/:project/:id/move`)
    .params({ project, id })
    .data({ folder })
    .exec();

export const dropboxFileUrl = (project: number, file: number) =>
  `/api/v2/authoring/dropbox/${project}/${file}/download`;

export const dropboxFilesUrl = (project: number, files: Set<number>) =>
  `/api/v2/authoring/dropbox/${project}/download?${idQs(Array.from(files))}`;

const idQs = (ids: number[]): string => ids.map(id => `id=${id}`).join('&');
