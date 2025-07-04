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

import { connectRouter, routerMiddleware } from 'connected-react-router';
import { createBrowserHistory } from 'history';
import { has, omit } from 'lodash';
import Polyglot from 'node-polyglot';
import { applyMiddleware, combineReducers, compose, createStore, Reducer, Action } from 'redux';
import { createLogger } from 'redux-logger';
import promise from 'redux-promise-middleware';
import { thunk } from 'redux-thunk';

import { initializeGoogleAnalytics } from './analytics';
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

export const history = createBrowserHistory({
  basename: window.lo_platform.isDev && +window.location.port < 8080 ? undefined : '/Authoring',
  getUserConfirmation(dialogKey, callback) {
    // use "message" as Symbol-based key
    const dialogTrigger = window[Symbol.for(dialogKey)];

    if (dialogTrigger) {
      // delegate to dialog and pass callback through
      return dialogTrigger(callback);
    }

    // Fallback to allowing navigation
    callback(true);
  },
});

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
  announcement: announcementReducer as Reducer<any>,
  projectGraph: projectGraph as Reducer<ProjectGraph>,
  graphEdits: graphEditReducer as Reducer<ProjectGraphEditState>,
  projectStructure: projectStructure as Reducer<any>,
  router: connectRouter(history),
  feedback: feedbackReducer as Reducer<FeedbackState>,
  story: storyReducer as Reducer<StoryState>,
  data: dataReducer as Reducer<DataState>,
  dropbox: dropboxReducer as Reducer<DropboxState>,
};

const rootReducer = combineReducers(dcmApplicationReducers);

const middlewares = [routerMiddleware(history), thunk, promise];

if (process.env.NODE_ENV === 'development') {
  const logger = createLogger({
    duration: true,
  });
  middlewares.push(logger);
}

const storeEnhancer = compose(applyMiddleware(...middlewares));

export const dcmStore = createStore(rootReducer, storeEnhancer);

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
