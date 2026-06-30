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

import { createBrowserHistory } from 'history';
import { has, omit } from 'lodash';
import Polyglot from 'node-polyglot';
import { applyMiddleware, combineReducers, compose, createStore, Reducer, Action } from 'redux';
import { createLogger } from 'redux-logger';
import promise from 'redux-promise-middleware';
import { thunk } from 'redux-thunk';

import { initializeGoogleAnalytics } from './analytics';
import { createRouterReducer, locationChange, RouterState } from './router/routerReducer';
import announcementReducer from './announcement/AnnouncementReducer';
import { INITIALIZE_DCM } from './dcmStoreConstants';
import dropboxReducer, { DropboxState } from './dropbox/dropboxReducer';
import assetEditor, { AssetEditorState } from './editor/assetEditorReducer';
import feedbackReducer, { FeedbackState } from './feedback/feedbackReducer';
import graphEditReducer, { ProjectGraphEditState } from './graphEdit/graphEditReducer';
import dcmLayoutReducer, { LayoutState } from './layout/dcmLayoutReducer';
import modal from './modals/modalReducer';
import presenceReducer, { PresenceState } from './presence/PresenceReducer';
import {
  computeContentAccessByRoleAndStatus,
  computeProjectAccessByRoleAndStatus,
  stockConfiguration,
} from './story/contentStatus';
import dataReducer, { DataState } from './story/dataReducer';
import storyReducer, { StoryState } from './story/storyReducer';
import projectGraph, { ProjectGraph } from './structurePanel/projectGraphReducer';
import projectStructure from './structurePanel/projectStructureReducer';
import toast, { ToastState } from './toast/reducer';
import user, { UserState } from './user/reducers';
import { TypeId } from './types/asset';

type ConfigurationState = {
  configuration?: any;
  domain?: { id: number };
  translations?: Polyglot;
};

type ConfigurationAction = {
  type: typeof INITIALIZE_DCM;
} & ConfigurationState;

const configurationReducer: Reducer<ConfigurationState, ConfigurationAction> = (
  state = {},
  action
) => {
  switch (action.type) {
    case INITIALIZE_DCM: {
      return {
        ...state,
        ...action.configuration,
      };
    }
    default:
      return state;
  }
};

// history v5 dropped both `basename` and `getUserConfirmation` from createBrowserHistory: the
// basename now lives on the <HistoryRouter> in DcmRoot, and the unsaved-changes prompt that used
// getUserConfirmation is handled by PreventNavAndUnsavedChangesPrompt via history.block.
export const basename =
  window.lo_platform.isDev && +window.location.port < 8080 ? undefined : '/Authoring';

export const history = createBrowserHistory();

// history v5 has no basename, so location.pathname is absolute; strip the router basename to match
// what connected-react-router/history v4 stored (a basename-relative pathname the routes match on).
const stripBasename = (pathname: string): string =>
  basename && (pathname === basename || pathname.startsWith(basename + '/'))
    ? pathname.slice(basename.length) || '/'
    : pathname;

const routerLocation = () => ({
  ...history.location,
  pathname: stripBasename(history.location.pathname),
});

// Programmatic navigation helpers. <Link>/useNavigate go through <HistoryRouter basename>, which
// prepends the basename for us — but a raw `history.push('/branch/…')` would not, so it would escape
// the app's /Authoring mount. These prepend the basename (no-op in dev / search-only updates) so
// the connected-react-router `push`/`replace` call sites keep working.
const withBasename = (to: any) => {
  if (!basename) return to;
  if (typeof to === 'string') return basename + to;
  if (to && typeof to === 'object' && to.pathname != null)
    return { ...to, pathname: basename + to.pathname };
  return to;
};

export const pushPath = (to: any, state?: any) => history.push(withBasename(to), state);
export const replacePath = (to: any, state?: any) => history.replace(withBasename(to), state);

const probableAdminRights = new Set([
  'loi.cp.admin.right.AdminRight',
  'loi.cp.admin.right.UserAdminRight',
  'loi.cp.course.right.ManageCoursesReadRight',
]);

const dcmApplicationReducers = {
  layout: dcmLayoutReducer as Reducer<LayoutState>,
  configuration: configurationReducer as Reducer<ConfigurationState>,
  modal: modal as Reducer<any>,
  user: user as Reducer<UserState>,
  assetEditor: assetEditor as Reducer<AssetEditorState>,
  toast: toast as Reducer<ToastState>,
  presence: presenceReducer as Reducer<PresenceState>,
  announcement: announcementReducer as unknown as Reducer<any>,
  projectGraph: projectGraph as Reducer<ProjectGraph>,
  graphEdits: graphEditReducer as Reducer<ProjectGraphEditState>,
  projectStructure: projectStructure as Reducer<any>,
  router: createRouterReducer(routerLocation(), history.action) as Reducer<RouterState>,
  feedback: feedbackReducer as Reducer<FeedbackState>,
  story: storyReducer as Reducer<StoryState>,
  data: dataReducer as Reducer<DataState>,
  dropbox: dropboxReducer as Reducer<DropboxState>,
};

const rootReducer = combineReducers(dcmApplicationReducers);

const middlewares = [thunk, promise];

if (process.env.NODE_ENV === 'development') {
  const logger = createLogger({
    duration: true,
  });
  middlewares.push(logger);
}

const storeEnhancer = compose(applyMiddleware(...middlewares));

export const dcmStore = createStore(rootReducer, storeEnhancer);

// Keep state.router in sync with the history singleton (connected-react-router used to do this).
history.listen(({ location, action }) =>
  dcmStore.dispatch(
    locationChange({ ...location, pathname: stripBasename(location.pathname) }, action)
  )
);

export const noBranch = {
  id: -1,
  name: '',
  active: true,
  project: {
    id: 0,
    name: '',
    ownedBy: 0,
    contributedBy: {},
  },
};

export const initializeStore = (domain, authoring, i18n, branch, lo_platform) => {
  return dispatch => {
    const user = domain.user;

    const features = Object.assign(stockConfiguration, lo_platform.features);

    // If you have a domain role mapped to an authoring role then that is your default
    // role, otherwise either Viewer or no limits.
    const canEdit = userCanEdit(branch, user);
    const defaultRole =
      Object.entries(features.domainRoleMapping).find(([r]) => user.roles.includes(r))?.[1] ??
      (!canEdit ? 'Viewer' : undefined);

    const configs = Object.assign(
      {
        translations: new Polyglot({ phrases: i18n }),
        projectRightsByRoleAndStatus: computeProjectAccessByRoleAndStatus(features),
        contentRightsByRoleAndStatus: computeContentAccessByRoleAndStatus(features),
      },
      features,
      authoring.effective,
      domain
    );

    dispatch({
      type: INITIALIZE_DCM,
      layout: {
        project: branch.project,
        branchName: branch.name,
        branchId: branch.id,
        userCanEdit: canEdit,
        userCanEditSettings: userCanEditSettings(branch, user),
        probableAdmin: user.rights.some(r => probableAdminRights.has(r)),
        role: userRole(branch, user) ?? defaultRole,
        platform: lo_platform,
      },
      configuration: configs,
      user: {
        preferences: user.preferences,
        roles: user.roles,
        rights: user.rights,
        profile: omit(user, 'preferences', 'roles', 'rights'),
      },
    });

    initializeGoogleAnalytics({ title: branch.name });
  };
};

const userCanEdit = (branch, user) =>
  branch.project.ownedBy === user.id ||
  has(branch.project.contributedBy, user.id) ||
  user.rights.includes('loi.authoring.security.right$EditContentAnyProjectRight');

const userCanEditSettings = (branch, user) =>
  branch.project.ownedBy === user.id ||
  user.rights.includes('loi.authoring.security.right$EditSettingsAnyProjectRight');

const userRole = (branch, user) => branch.project.contributedBy[user.id] ?? null;
