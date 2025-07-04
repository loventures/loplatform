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

import { push } from 'connected-react-router';
import { assign } from 'lodash';
import qs from 'qs';
import { matchPath } from 'react-router';

import { reloadAssetEditor } from '../editor/assetEditorActions';
import { useDcmSelector } from '../hooks';
import { FullEdge } from '../types/edge';
import allPaths from './routes';

import { dcmStore } from '../dcmStore';

class ReactRouterService {
  getState() {
    return dcmStore.getState();
  }

  dispatch(e) {
    return dcmStore.dispatch(e);
  }

  reload() {
    window.location.reload();
  }

  reloadAssetEditor() {
    this.dispatch(reloadAssetEditor());
  }

  goToHome() {
    const { branchId, project } = this.getState().layout;
    this.dispatch(push(`/branch/${branchId}/asset/${project.homeNodeName}`));
  }

  goToAsset(asset: { name: string; typeId: string }, params?, clear?) {
    const { name, typeId } = asset;
    const { branchId } = this.getState().layout;

    switch (typeId) {
      case 'competencySet.1':
        this.dispatch(push(this.buildUrl(`/branch/${branchId}/competencies`, params, clear)));
        break;
      case 'rubric.1':
        this.dispatch(
          push(this.buildUrl(`/branch/${branchId}/rubric-editor/${name}`, params, clear))
        );
        break;
      default:
        this.dispatch(push(this.buildUrl(`/branch/${branchId}/asset/${name}`, params, clear)));
    }
  }

  goToEdgeTarget(edge: FullEdge) {
    const { contextPath, search } = this.getCurrentParams<{
      contextPath: string;
      search: string;
    }>();

    if (contextPath) {
      reactRouterService.goToAsset(edge.target, {
        contextPath: contextPath + '.' + edge.source.name,
      });
    } else if (edge.source.typeId == 'course.1' && !search) {
      reactRouterService.goToAsset(edge.target, { contextPath: edge.source.name });
    } else {
      reactRouterService.goToAsset(edge.target, {}, true);
    }
  }

  /* TODO: we can generalize this and make the contextPath stuff a utility. */
  goToRubricEditor(rubricName, parent) {
    const currentParams = this.getCurrentParams<{ contextPath: string; branchId: string }>();

    const currentPath = currentParams.contextPath ? currentParams.contextPath.split('.') : [];
    const newContext = [...currentPath, parent].join('.');

    const params = {
      contextPath: newContext,
    };
    this.dispatch(
      push(this.buildUrl(`/branch/${currentParams.branchId}/rubric-editor/${rubricName}`, params))
    );
  }

  goToBranchError(error) {
    console.error(error);
    const { branchId } = this.getState().layout;
    this.dispatch(
      push(!branchId || parseInt(branchId) < 0 ? '/error' : `/branch/${branchId}/error`)
    );
  }

  goToRootError(error) {
    console.error(error);
    this.dispatch(push('/error'));
  }

  logout() {
    window.location.href = '/';
  }

  isRevisionRoute(state?) {
    if (!state) state = this.getState();
    return state.router.location?.pathname.includes('/revision/'); // turd
  }

  buildUrl(url, queryParams = {}, clear?) {
    /*
     * TODO: Can this build URL method also do path variables please?
     * */

    const { search } = this.getState().router.location;
    const currentParams = parseQueryParams(search);

    if (clear) {
      return url;
    }

    assign(currentParams, queryParams);

    return url + '?' + qs.stringify(currentParams);
  }

  getCurrentParams<Params extends { [K in keyof Params]?: string }>(state?): Params {
    if (!state) {
      state = this.getState();
    }
    if (!state.router.location) {
      return {} as Params;
    }
    const { search, pathname } = state.router.location;
    const searchParams = parseQueryParams(search);
    const match = matchPath<Params>(pathname, { path: allPaths, exact: true });
    return Object.assign({}, searchParams, match?.params);
  }
}

export function parseQueryParams(search) {
  return qs.parse(search, { ignoreQueryPrefix: true, arrayLimit: 50 }); // max array size is 50 because we have ~33 searchable asset types
}

export const useRouteParameter = (param: string) =>
  useDcmSelector(
    state => reactRouterService.getCurrentParams<Record<string, string>>(state)[param]
  );

export const useNumericRouteParameter = (param: string) => {
  const value = useRouteParameter(param);
  return value == null ? undefined : parseInt(value);
};

const reactRouterService = new ReactRouterService();

export function getBranchId() {
  return reactRouterService.getCurrentParams<{ branchId: string }>().branchId;
}

export default reactRouterService;
