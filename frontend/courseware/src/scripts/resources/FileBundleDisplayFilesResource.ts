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

import { SrsList } from '../api/commonTypes';
import Course from '../bootstrap/course';
import { loConfig } from '../bootstrap/loConfig';
import { queryClient } from './queryClient';
import { Resource, useSuspenseQuery } from './Resource';
import { DisplayFile } from '../utilities/assetTypes';

import { createDataListUpdateMergeAction } from '../utilities/apiDataActions';
import { UrlAndParamsKey } from './ContentsResource';
import { courseReduxStore } from '../loRedux';

/**
 * React-Query Resource for FileBundle Display Files
 * */
class FileBundleDisplayFilesResource<TData extends SrsList<DisplayFile>> extends Resource<
  TData,
  [number, string],
  UrlAndParamsKey
> {
  urlTemplate = loConfig.contentItem.fileBundle.files;

  getKey(commit: number, name: string): UrlAndParamsKey {
    return [
      {
        commit: +commit,
        name: `${name}`,
      },
      this.urlTemplate,
    ];
  }

  pushToRedux(data: TData, config: Record<string, any> = {}) {
    if (data.objects.length > 0) {
      const action = createDataListUpdateMergeAction('displayFilesByContent', {
        [config.contentId]: data.objects,
      });
      courseReduxStore.dispatch(action);
    }
  }

  fetch(key: UrlAndParamsKey, config: Record<string, any> = {}) {
    return queryClient.fetchQuery(key, this.fetcher(config));
  }

  read(commit: number, name: string, config?: Record<string, any>) {
    const key = this.getKey(commit, name);
    const promise = this.fetch(key, config);
    const data = queryClient.getQueryData<TData>(key);
    const fetching = queryClient.isFetching(key);

    return { promise, fetching, data, key };
  }

  isFetching(courseId: number, name: string) {
    return queryClient.isFetching(this.getKey(courseId, name));
  }
}

const fileBundleDisplayFilesResource = new FileBundleDisplayFilesResource();

export default fileBundleDisplayFilesResource;

export const useFileBundleDisplayFilesResource = (
  name: string,
  commit: number = Course.commitId
) => {
  const key = fileBundleDisplayFilesResource.getKey(commit, name);
  const fetcher = fileBundleDisplayFilesResource.fetcher({ redux: true, contentId: name });
  return useSuspenseQuery(key, fetcher, { select: data => data.objects });
};
