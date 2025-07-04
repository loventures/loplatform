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

import gretchen from '../../../grfetchen/';

import { BlobRef, NodeName } from '../../../types/asset';
import { User } from '../../../types/user';
import { ProjectsResponse } from '../../NarrativeMultiverse';
import { openToast, TOAST_TYPES } from '../../../toast/actions.ts';

export const loadProjectProps = (
  property: 'category' | 'subCategory' | 'productType'
): Promise<string[]> =>
  gretchen
    .post(`/api/v2/authoring/projectProps`)
    .data({ property })
    .exec()
    .then(data => data.values);

export type PutProject = {
  projectName: string;
  code?: string;
  productType?: string;
  category?: string;
  subCategory?: string;
  revision?: number;
  launchDate?: string;
  liveVersion?: string;
  s3?: string;
};

export type CreateProject = PutProject & {
  projectStatus?: string;
  courseStatus?: string;
};

export type CopyProject = CreateProject & {
  branchId: number;
  targetDomain?: number;
};

export type ImportProject = CreateProject & {
  source: BlobRef;
};

export type ImportReceipt = {
  data: { targetBranchId: number };
  importedRoots: Array<{ name: NodeName }>;
  // much more
};

export type ProjectContributors = {
  owner: number;
  contributors: Record<string | number, string | null>;
};

export const createProject = (project: CreateProject): Promise<ProjectsResponse> =>
  gretchen.post(`/api/v2/authoring/projects`).data(project).exec();

export const copyProject = (
  project: CopyProject
): Promise<{ branchId: number; homeNodeName: NodeName }> =>
  gretchen.post(`/api/v2/authoring/admin/project`).data(project).exec();

export const importProject = (
  project: ImportProject
): Promise<{
  receipts: Array<ImportReceipt>;
  users: Record<number, User>;
}> => gretchen.post('/api/v2/authoring/importProject').data(project).exec()
  .then(data => {
    if (data.status === 'async') {
      return new Promise((resolve, reject) => {
        const channel = data.channel;
        const msgs = new EventSource(`/event${channel}`);
        msgs.addEventListener(channel, event => {
          const data = JSON.parse(event.data);
          if (data.status === 'ok') {
            msgs.close();
            resolve(data.body);
          } else {
            msgs.close();
            reject(data.body);
          }
        });
      });
    } else {
      throw 'Unexpected success.';
    }
  });

export const getProject = (id: number): Promise<ProjectsResponse> =>
  gretchen.get(`/api/v2/authoring/projects/${id}`).exec();

export const deleteProject = (id: number): Promise<void> =>
  gretchen.delete(`/api/v2/authoring/projects/${id}`).exec();

export const putProject = (id: number, project: PutProject): Promise<ProjectsResponse> =>
  gretchen.put(`/api/v2/authoring/projects/${id}`).data(project).exec();

export const archiveProject = (id: number): Promise<ProjectsResponse> =>
  gretchen.post(`/api/v2/authoring/projects/${id}/archive`).exec();

export const unarchiveProject = (id: number): Promise<ProjectsResponse> =>
  gretchen.post(`/api/v2/authoring/projects/${id}/unarchive`).exec();

export const putProjectContributors = (
  id: number,
  cf: ProjectContributors
): Promise<ProjectsResponse> =>
  gretchen.put(`/api/v2/authoring/projects/${id}/contributors`).data(cf).exec();

export type CoursePreferences = {
  enableAnalyticsPage?: boolean;
  enableQna?: boolean;
  CBLPROD16934InstructorResources?: string;
  ltiISBN?: string;
  ltiCourseKey?: string;
};

export type ConfigOut<T> = {
  effective: T;
  defaults: T;
  overrides: T;
};

export type CourseConfig = ConfigOut<CoursePreferences>;

export const getProjectConfig = (id: number): Promise<CourseConfig> =>
  gretchen.get(`/api/v2/config/coursePreferences/project/${id}`).exec();

export const putProjectConfig = (id: number, cf: CoursePreferences): Promise<CourseConfig> =>
  gretchen.put(`/api/v2/config/coursePreferences/project/${id}`).data(cf).exec();

export const getOfferingConfig = (id: number): Promise<CourseConfig> =>
  gretchen.get(`/api/v2/config/coursePreferences/item/${id}`).exec();
