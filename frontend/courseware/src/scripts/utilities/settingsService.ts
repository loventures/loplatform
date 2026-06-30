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

import { cloneDeep, each, extend, isNil, isObject, isUndefined } from 'lodash';
import lscache from 'lscache';

import Course from '../bootstrap/course';

/**
 * The settings service should be for saving long term user settings and preferences as the app
 * expands. NOTE: USER DATA not app level data!
 *
 * When dealing with system level settings, this is mostly useful for the release of a feature such as
 * a new graph for a particular client (or to turn off a broken feature that is spamming the db).
 * Feature settings should come from a db or property file.
 *
 *   Settings.isFeatureEnabled('SomeFeature');
 *
 * For example some items can be determined on a course and user basis such as self study. This
 * information would be cached in the user course bucket and would be returned by checking
 *
 *   Settings.get('selfStudy');
 *
 * This was an Angular `.provider('Settings')`; it is now a plain singleton (`settings`) — the source of
 * truth. The Angular provider in `settings.jsx` is a thin adapter that delegates here so config-time
 * (`.config`) blocks and existing `'Settings'` injections keep working, while React/TS code can import
 * this module directly instead of going through `lojector.get('Settings')`.
 */
export interface FeatureConfig {
  isEnabled?: boolean;
  userSet?: boolean;
  value?: any;
}

export interface SettingsService {
  LIFESPAN: number;
  userId: any;
  courseId: any;
  userRole: any;
  userRights: any;
  userStorage: any;
  features: Record<string, FeatureConfig>;
  overrides: Record<string, FeatureConfig>;
  change: number;
  initialized: boolean;

  setFeature(feature: any, enabled?: boolean): void;
  setOverrides(overrides: any): void;
  init(userId: any, userRole: any, features: any): SettingsService;
  disableFeature(key: string): void;
  enableFeature(key: string, value?: any): void;
  toggleFeature(key: string, isEnabled?: boolean, value?: any): void;
  set(key: string, val: any): void;
  setUserGlobal(key: string, val: any): any;
  getUserGlobal(key: string, defaultVal?: any): any;
  getCourseLocalValue(key: string, defaultVal?: any): any;
  setCourseLocalValue(key: string, val: any): any;
  setUserSession(key: string, val: any): any;
  clearUserSession(): void;
  getUserSession(): any;
  getGlobalUserKey(key: string): string | null;
  setIfUndefined(key: string, val: any): void;
  get(key: string): any;
  isSelfStudy(): boolean;
  setEternal(key: string, val: any): void;
  getEternal(key: string): any;
  isTrue(key: string): boolean;
  isFalse(key: string): boolean;
  isSet(key: string): boolean;
  isUserEnabled(key: string): boolean;
  isFeatureEnabled(key: string): boolean;
  isFeatureDisabled(key: string): boolean;
  getSettings(key: string, evenWhenDisabled?: boolean): any;
  getBucket(): any;
  setBucket(info: any, lifespan?: number): void;
  getBucketId(): string | null;
  getUserKey(key?: string): string;
  getUserContext(key: string): any;
  setUserContext(key: string, val: any): any;
  clearCache(): void;
  clearAll(): void;
}

const Settings = {
  LIFESPAN: 1 * 24 * 60, // Default to a day
  userId: null, // The current userId
  courseId: null, // The current course selected
  userRole: null, // Is the user an instructor or student?
  userRights: null, // Fine-grained permissions
  userStorage: null, // Load data for this user into this hash for fast lookups
  features: {}, // For system level settings
  overrides: {}, // So you can make EiQ RT and ensure a config step runs after the app init
  change: 0, // For watches
  initialized: false,
} as SettingsService;

Settings.setFeature = function (feature, enabled) {
  if (isObject(feature)) {
    each(feature as any, function (feat: any, key: string) {
      Settings.features[key] = feat; // If the user passes values, you want to preserve that
    });
  } else {
    Settings.toggleFeature(feature, enabled);
  }
};

/**
 * Use setOverrides only if you are doing something like EiQ+R&T, where you don't want the server
 * settings altering what R&T does, but need to hack specific things in EiQ. This will apply into the
 * features init last and let you use a config block. Probably do NOT use.
 */
Settings.setOverrides = function (overrides) {
  Settings.overrides = overrides || {};
};

/**
 * Apply in the settings for keys we have defined.
 */
Settings.init = function (userId, userRole, features) {
  Settings.userId = userId;
  Settings.courseId = Course.id || 'Global';
  Settings.userRole = userRole;

  // Load from local storage if possible.
  Settings.userStorage = null; // Unset local bucket before getting it.
  Settings.userStorage = Settings.getBucket();
  Settings.features = extend(Settings.features, features || {}, Settings.overrides); // Allow provider config settings

  Settings.initialized = true;

  return Settings;
};

/** Convenience method to disable a feature. */
Settings.disableFeature = function (key) {
  Settings.toggleFeature(key, false);
};

/** Convenience method to enable a feature. */
Settings.enableFeature = function (key, value) {
  Settings.toggleFeature(key, true, value);
};

/** Enable or disable a feature. */
Settings.toggleFeature = function (key, isEnabled, value) {
  if (!isObject(Settings.features[key])) {
    Settings.features[key] = {};
  }
  Settings.features[key].isEnabled = !!isEnabled;
  if (isEnabled && arguments.length > 2) {
    Settings.features[key].value = value;
  }
};

/**
 * Apply in the settings for keys we have defined. Note that the code can watch Settings.change to
 * determine if a redraw based on user settings should kick off. This is a course specific bucket.
 */
Settings.set = function (key, val) {
  const bucket = Settings.getBucket();
  bucket[key] = val;
  Settings.setBucket(bucket);

  Settings.change += 1; // Because people like to watch
};

/** Per user, global across courses but not roles (use for seen tours etc) */
Settings.setUserGlobal = function (key, val) {
  const gUserKey = Settings.getGlobalUserKey(key);
  if (gUserKey && !isNil(val)) {
    lscache.set(gUserKey, val);
    return val;
  }
};

/** Get the globally set user key value */
Settings.getUserGlobal = function (key, defaultVal) {
  const gUserKey = Settings.getGlobalUserKey(key);
  if (gUserKey) {
    const val = lscache.get(gUserKey);
    if (!isNil(val)) {
      return val;
    }
  }
  return defaultVal;
};

/** Get a course-localized value of a piece of data for a given key. */
Settings.getCourseLocalValue = function (key, defaultVal) {
  if (!Settings.courseId) {
    return defaultVal;
  }
  const userKey = Settings.getUserKey(key);
  if (userKey) {
    const val = lscache.get(userKey);
    if (!isUndefined(val)) {
      return val;
    }
  }
  return defaultVal;
};

Settings.setCourseLocalValue = function (key, val) {
  if (!Settings.courseId) {
    throw 'Cannot set value outside course context.';
  }
  const userKey = Settings.getUserKey(key);
  if (userKey && !isUndefined(val)) {
    lscache.set(userKey, val);
    return val;
  }
};

/**
 * The user session variables are set only for the length of the session. We will use it for things
 * like changing a default date range. Only use this for per session user variables.
 */
Settings.setUserSession = function (key, val) {
  const sUserKey = Settings.getUserKey('session');
  const session = Settings.getUserSession() || {};
  if (sUserKey && !isUndefined(val)) {
    session[key] = val;
    lscache.set(sUserKey, session);
  }
  return session;
};

Settings.clearUserSession = function () {
  lscache.remove(Settings.getUserKey('session'));
};

Settings.getUserSession = function () {
  const sUserKey = Settings.getUserKey('session');
  if (sUserKey) {
    return lscache.get(sUserKey);
  }
};

/**
 * A simple key created by adding the userId + userRole + key passed in. Internal method, exposed for
 * testing.
 */
Settings.getGlobalUserKey = function (key) {
  if (Settings.userId && key != null) {
    return Settings.userId + '_' + (Settings.userRole || 'student') + '_' + key;
  }
  return null;
};

/**
 * Sets something only if the setting is already not created for this user (safety method for
 * preventing default setting stomps)
 */
Settings.setIfUndefined = function (key, val) {
  if (!Settings.isSet(key)) {
    Settings.set(key, val);
  }
};

/** Get the info once, return the key lookup */
Settings.get = function (key) {
  const bucket = Settings.getBucket();
  return bucket[key];
};

/**
 * Determines if a user is in an instructor-led course or not. Requires the 'selfStudy' feature to be
 * enabled.
 */
Settings.isSelfStudy = function () {
  return Settings.isFeatureEnabled('selfStudy') && (window.lo_platform as any).instructor_led === false;
};

/**
 * PROBABLY NOT WHAT YOU WANT, USE set(). Set a distinct key for the bucketId in a separate key val
 * bucket that will never expire. Specifically used to ensure bad service loads do not break important
 * pieces of the UI.
 */
Settings.setEternal = function (key, val) {
  const bId = Settings.getBucketId();
  if (bId && key) {
    lscache.set(bId + key, val);
  }
};

/** PROBABLY NOT WHAT YOU WANT, USE get(). Retrieve an Eternal set key */
Settings.getEternal = function (key) {
  const bId = Settings.getBucketId();
  if (bId && key) {
    lscache.get(bId + key);
  }
};

/** Is this key SET and true */
Settings.isTrue = function (key) {
  const val = Settings.get(key);
  if (!isUndefined(val)) {
    return !!val;
  }
  return false;
};

/** Is key SET and false */
Settings.isFalse = function (key) {
  const val = Settings.get(key);
  if (!isUndefined(val)) {
    return !val;
  }
  return false;
};

/** Is the value set */
Settings.isSet = function (key) {
  const val = Settings.get(key);
  if (typeof val != 'undefined' && val !== null) {
    return true;
  }
  return false;
};

/** The feature must be enabled AND the user must have set it to true */
Settings.isUserEnabled = function (key) {
  if (Settings.isFeatureEnabled(key) && Settings.get(key)) {
    return true;
  }
  return false;
};

/**
 * Check to see if a feature is enabled, see if the user can set the preference, return the result.
 */
Settings.isFeatureEnabled = function (key) {
  const feature = Settings.features[key];
  if (feature && feature.isEnabled) {
    if (feature.userSet) {
      const val = Settings.get(key);
      if (isUndefined(val)) {
        return feature.isEnabled;
      } else {
        return !!val;
      }
    }
    return feature.isEnabled;
  }
  return false;
};

/**
 * Check to see if the feature is explicitly disabled, this allows you to assume a feature is default
 * on without copying setting configuration into every project.
 */
Settings.isFeatureDisabled = function (key) {
  if (!isUndefined(Settings.features[key])) {
    return !Settings.isFeatureEnabled(key);
  }
  return false;
};

/**
 * Returns custom settings for a feature, defined by setting a `.value` property on the feature object.
 * Returns null if feature doesn't exist, is disabled, or has no custom data.
 */
Settings.getSettings = function (key, evenWhenDisabled) {
  const feature = Settings.features[key];

  // If feature is not enabled, doesn't exist, or doesn't have a data property, return null.
  if (
    !feature ||
    !(Settings.isFeatureEnabled(key) || evenWhenDisabled) ||
    isUndefined(Settings.features[key].value)
  ) {
    return null;
  }

  return cloneDeep(feature.value);
};

/** Get the actual storage bucket for this user. (Internal Method) */
Settings.getBucket = function () {
  if (Settings.userStorage) {
    return Settings.userStorage;
  }
  const bId = Settings.getBucketId();
  if (bId) {
    return lscache.get(bId) || {};
  }
  return {}; // If local userStorage is not supported.
};

/** set the user bucket into lscache / local storage */
Settings.setBucket = function (info, lifespan) {
  const bId = Settings.getBucketId();
  if (bId && info) {
    lscache.set(bId, info, lifespan || Settings.LIFESPAN);
  } else {
    console.error('Attempted to save into the user bucket without an id(info, bId)', info, bId);
  }
};

/** internal method for keeping track of where to put user Settings */
Settings.getBucketId = function () {
  if (Settings.userId && Settings.courseId) {
    return Settings.userId + Settings.courseId;
  }
  return null;
};

/**
 * Create a bucket based on the user information + key, ie key: hello would create a user / course
 * associated element with key on the end. Returns your new key to use for storing events.
 */
Settings.getUserKey = function (key) {
  return Settings.courseId + '' + (Settings.userId + '') + (Settings.userRole || '') + (key || '');
};

Settings.getUserContext = function (key) {
  const gUserKey = Settings.getUserKey(key);
  if (gUserKey) {
    const val = lscache.get(gUserKey);
    return val;
  }
};

Settings.setUserContext = function (key, val) {
  const gUserKey = Settings.getUserKey(key);
  if (gUserKey && !isUndefined(val)) {
    lscache.set(gUserKey, val);
    return val;
  }
};

/** For clearing out user settings, consider using it on logout. */
Settings.clearCache = function () {
  const bId = Settings.getBucketId();
  if (bId) {
    lscache.set(bId, null);
  }
};

/** In case it all needs to die, use this based on a config version? */
Settings.clearAll = function () {
  lscache.flush();
};

/** The settings singleton — the source of truth, importable directly from React/TS. */
export const settings = Settings;

export default settings;
