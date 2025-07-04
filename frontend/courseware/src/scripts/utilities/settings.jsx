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

import { isObject, each, extend, isUndefined, isNil } from 'lodash';
import lscache from 'lscache';
import Course from '../bootstrap/course.js';

export default angular.module('lo.utilities.Settings', []).provider(
  'Settings',
  /**
   * @ngdoc provider
   * @memberof lo.utilities
   * @alias SettingsProvider
   */
  function SettingsProvider() {
    /**
     * @ngdoc service
     * @memberof lo.utilities
     *
     * @description The settings service should be for saving long term user
     * settings and preferences as the app expands.  NOTE: USER DATA not app
     * level data!
     *
     *  If you need to observe when the user changes settings, you can setup a
     *  watch on Settings.change.  This is altered when the user sets a new value.
     *
     *  When dealing with system level settings, this is mostly useful for the
     *  release of a feature such as a new graph for a particular client (or to
     *  turn off a broken feature that is spamming the db).  Feature settings
     *  should come from a db or property file, all features used in the library
     *  will appear in the documentation for properties of this service.
     *
     *  Settings.isFeatureEnabled('SomeFeature');
     *
     *  For example some items can be determined on a course and user basis
     *  such as self study.  This information would be cached in the user
     *  course bucket and would be returned by checking
     *
     *  Settings.get('selfStudy');
     *
     *  This is a service that probably should get stuffed into the rootScope
     *  so that you can enable and disable directives by configuration.
     */
    var Settings = {
      LIFESPAN: 1 * 24 * 60, //Default to a day
      userId: null, //The current userId
      courseId: null, //The current course selected
      userRole: null, //Is the user an instructor or student?
      userRights: null, //Fine-grained permissions
      userStorage: null, //Load data for this user into this hash for fast lookups
      features: {}, //For system level settings
      overrides: {}, //So you can make EiQ RT and ensure a config step runs after the app init
      change: 0, //For watches
      initialized: false,
    };

    this.Settings = Settings;

    this.feature = {};
    this.setFeature = function (feature, enabled) {
      if (isObject(feature)) {
        each(feature, function (feat, key) {
          Settings.features[key] = feat; //If the user passes values, you want to preserve that
        });
      } else {
        Settings.toggleFeature(feature, enabled);
      }
    };
    /**
     *  Use setOverrides only if you are doing something like EiQ+R&T, where you
     *  don't want the server settings altering what R&T does, but need to hack
     *  specific thigns in EiQ.    This will apply into the features init last
     *  and let you use a config block.  Probably do NOT use.
     */
    this.setOverrides = function (overrides) {
      Settings.overrides = overrides || {};
    };

    /**
     * @description Apply in the settings for keys we have defined.
     *
     * @param {String|int} userId the id of the user
     * @param {String|int} courseId The id of the primary course
     * @param {String} userRole the user role (instructor etc)
     * @param {Hash} features the currently enabled features in the system
     * <pre>
     *    {
     *      KeyName: {isEnabled: true|false},
     *      RenderNewFeature: {isEnabled: false},
     *      ExpensiveServiceCall: {isEnabled: false}
     *    }
     * </pre>
     *
     */
    this.init = function (userId, userRole, features) {
      Settings.userId = userId;
      Settings.courseId = Course.id || 'Global';
      Settings.userRole = userRole;

      //Load from local storage if possible.
      Settings.userStorage = null; //Unset local bucket before getting it.
      Settings.userStorage = Settings.getBucket();
      Settings.features = extend(Settings.features, features || {}, Settings.overrides); //Allow provider config settings

      Settings.initialized = true;

      return Settings;
    };
    Settings.init = this.init;

    /**
     * @description Convenience method to disable a feature.
     * @param {String} key the feature to disable.
     */
    Settings.disableFeature = function (key) {
      Settings.toggleFeature(key, false);
    };
    this.disableFeature = Settings.disableFeature;

    /**
     * @description Convenience method to enable a feature.
     * @param {String} key the feature to enable.
     */
    Settings.enableFeature = function (key, value) {
      Settings.toggleFeature(key, true, value);
    };
    this.enableFeature = Settings.enableFeature;

    /**
     * @description Enable or disable a feature.
     * @param {String} key the feature to disable.
     * @param {boolean} whether to enable or disable
     * @param {Any} value override for this feature
     */
    Settings.toggleFeature = function (key, isEnabled, value) {
      if (!isObject(Settings.features[key])) {
        Settings.features[key] = {};
      }
      Settings.features[key].isEnabled = !!isEnabled;
      if (isEnabled && arguments.length > 2) {
        Settings.features[key].value = value;
      }
    };
    this.toggleFeature = Settings.toggleFeature;

    //Alias this to the provider so we can init in a config block.
    this.init = Settings.init;

    /**
     * @description
     * Apply in the settings for keys we have defined.  Note that the
     * code can watch Settings.change to determine if a redraw based
     * on user settings should kick off.  This is a course specific
     * bucket.
     *
     * @param {String} key The user key to set into the hash
     * @param {Object} val Value ot set
     */
    Settings.set = function (key, val) {
      var bucket = Settings.getBucket();
      bucket[key] = val;
      Settings.setBucket(bucket);

      Settings.change += 1; //Because people like to watch
    };

    /**
     * @description
     * Per user, global across courses but not roles (use for seen tours etc)
     *
     * @param {String} key store into this hash location (string
     * @param {Object} val the value or object to store into the pp
     */
    Settings.setUserGlobal = function (key, val) {
      var gUserKey = Settings.getGlobalUserKey(key);
      if (gUserKey && !isNil(val)) {
        lscache.set(gUserKey, val);
        return val;
      }
    };

    /**
     * @description
     *  Get the globally set user key value
     * @param {String} key The key to lookup.
     * @returns {Object} The item stored into that user global
     */
    Settings.getUserGlobal = function (key, defaultVal) {
      var gUserKey = Settings.getGlobalUserKey(key);
      if (gUserKey) {
        var val = lscache.get(gUserKey);
        if (!isNil(val)) {
          return val;
        }
      }
      return defaultVal;
    };

    /**
     * @description Get a course-localized value of a piece of data for a given key.
     * @param {String} key The key to lookup.
     * @param defaultVal Value to return if the key could not be found.
     * @returns {*}
     */
    Settings.getCourseLocalValue = function (key, defaultVal) {
      if (!Settings.courseId) {
        return defaultVal;
      }
      var userKey = Settings.getUserKey(key);
      if (userKey) {
        var val = lscache.get(userKey);
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
      var userKey = Settings.getUserKey(key);
      if (userKey && !isUndefined(val)) {
        lscache.set(userKey, val);
        return val;
      }
    };

    /**
     *  The user session variables are set only for the length of the
     *  session.   We will use it for things like changing a default
     *  date range.  Only use this for per session user variables.
     */
    Settings.setUserSession = function (key, val) {
      var sUserKey = Settings.getUserKey('session');
      var session = Settings.getUserSession() || {};
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
      var sUserKey = Settings.getUserKey('session');
      if (sUserKey) {
        return lscache.get(sUserKey);
      }
    };

    /**
     * @description
     * A simple key created by adding the userId + userRole + key passed
     * in.  Internal method, exposed for testing.
     *
     * @param {String} key The key to use to namespace into this user space
     */
    Settings.getGlobalUserKey = function (key) {
      if (Settings.userId && key != null) {
        return Settings.userId + '_' + (Settings.userRole || 'student') + '_' + key;
      }
      return null;
    };

    /**
     * @description
     *  Sets something only if the setting is already not created for
     *  this user (safety method for preventing default setting stomps)
     * @param {String} key - The key
     * @param {Object} val - The value / object to run angular.toJson() on
     */
    Settings.setIfUndefined = function (key, val) {
      if (!Settings.isSet(key)) {
        Settings.set(key, val);
      }
    };

    /**
     * @description Get the info once, return the key lookup
     * @param {String} key get the user key from their bucket.
     */
    Settings.get = function (key) {
      var bucket = Settings.getBucket();
      return bucket[key];
    };

    /**
     * @description
     * Determines if a user is in an instructor-led course or not.  Requires the 'selfStudy' feature to be
     *   enabled.
     */
    Settings.isSelfStudy = function () {
      return Settings.isFeatureEnabled('selfStudy') && window.lo_platform.instructor_led === false;
    };

    /**
     * @description
     * PROBABLY NOT WHAT YOU WANT, USE set()
     * Set a distinct key for the bucketId in a seperate key val bucket that will
     * never expire.  Specifically used to ensure bad service loads do not break
     * important pieces of the UI.
     *
     * @param {String} key the key
     * @param {Object} val the value object to JSON.stringify
     */
    Settings.setEternal = function (key, val) {
      var bId = Settings.getBucketId();
      if (bId && key) {
        lscache.set(bId + key, val);
      }
    };

    /**
     * @description
     * PROBABLY NOT WHAT YOU WANT, USE get() Retrieve an Eternal set key
     *
     * @param {String} key to lookup
     */
    Settings.getEternal = function (key) {
      var bId = Settings.getBucketId();
      if (bId && key) {
        lscache.get(bId + key);
      }
    };

    /**
     * @description
     * Is this key SET and true
     * @param {String} key to lookup
     */
    Settings.isTrue = function (key) {
      var val = Settings.get(key);
      if (!isUndefined(val)) {
        return !!val;
      }
      return false;
    };

    /**
     * @description  Is key SET and false
     * @param {String} key to lookup
     * @returns {boolean} !val of the key set in the user info
     */
    Settings.isFalse = function (key) {
      var val = Settings.get(key);
      if (!isUndefined(val)) {
        return !val;
      }
      return false;
    };

    /**
     * @description  Is the value set
     * @returns {boolean} true if it is not undefined or null
     */
    Settings.isSet = function (key) {
      var val = Settings.get(key);
      if (typeof val != 'undefined' && val !== null) {
        return true;
      }
      return false;
    };

    /**
     * @description  The feature must be enabled AND the user must have set it to true
     * @param {String} key is this named feature enabled in the system (permissions / cfg)
     * and has the user turned it on?
     * returns {boolean} true if enabled
     */
    Settings.isUserEnabled = function (key) {
      if (Settings.isFeatureEnabled(key) && Settings.get(key)) {
        return true;
      }
      return false;
    };

    /**
     * @description
     * Check to see if a feature is enabled, see if the user can set the
     * preference, return the result
     * @returns {boolean} Is the feature enabled on this system
     */
    this.isFeatureEnabled = Settings.isFeatureEnabled = function (key) {
      var feature = Settings.features[key];
      if (feature && feature.isEnabled) {
        if (feature.userSet) {
          var val = Settings.get(key);
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
     * @description Check to see if the feature is explicitly disabled, this
     * allows you to assume a feature is default on without copying setting
     * configuration into every project.
     * @returns {boolean} Is the feature explicitly disabled
     * */
    this.isFeatureDisabled = Settings.isFeatureDisabled = function (key) {
      if (!isUndefined(Settings.features[key])) {
        return !Settings.isFeatureEnabled(key);
      }
      return false;
    };

    /**
     * @description
     *   Returns custom settings for a feature, defined by setting a `.value` property
     *   on the feature object.
     * @returns {value} Feature data.
     *   Returns null if feature doesn't exist, is disabled, or has no custom data.
     */
    Settings.getSettings = function (key, evenWhenDisabled) {
      var feature = Settings.features[key];

      // If feature is not enabled, doesn't exist, or doesn't have a data property, return null.
      if (
        !feature ||
        !(Settings.isFeatureEnabled(key) || evenWhenDisabled) ||
        isUndefined(Settings.features[key].value)
      ) {
        return null;
      }

      return angular.copy(feature.value);
    };

    this.getSettings = Settings.getSettings;

    /**
     * @description  Get the actual storage bucket for this user. (Internal Method)
     * @returns {Object} The hash of keys that we set for this particular user in a role.
     */
    Settings.getBucket = function () {
      if (Settings.userStorage) {
        return Settings.userStorage;
      }
      var bId = Settings.getBucketId();
      if (bId) {
        return lscache.get(bId) || {};
      }
      return {}; //If local userStorage is not supported.
    };

    /**
     * @description set the user bucket into lscache / local storage
     * @param {Hash} info the result of getBucket or a hash of settings
     * @param {int} [lifespan=Settings.LIFESPAN] time to keep in the cache in minutes
     */
    Settings.setBucket = function (info, lifespan) {
      var bId = Settings.getBucketId();
      if (bId && info) {
        lscache.set(bId, info, lifespan || Settings.LIFESPAN);
      } else {
        console.error('Attempted to save into the user bucket without an id(info, bId)', info, bId);
      }
    };

    /**
     * @description internal method for keeping track of where to put user Settings
     *
     * TODO: Update to better support cross course settings / smarter buckets
     * TODO: Make courseId optional?
     */
    Settings.getBucketId = function () {
      if (Settings.userId && Settings.courseId) {
        return Settings.userId + Settings.courseId;
      }
      return null;
    };

    /**
     * @description Create a bucket based on the user information + key
     *
     * @param {String} key The suffix to use for this bucket, ie key: hello would
     * create a user / course associated element with key on the end.
     * returns {String} Your new key to use for storing events.
     */
    Settings.getUserKey = function (key) {
      return (
        Settings.courseId + '' + (Settings.userId + '') + (Settings.userRole || '') + (key || '')
      );
    };

    Settings.getUserContext = function (key) {
      var gUserKey = Settings.getUserKey(key);
      if (gUserKey) {
        var val = lscache.get(gUserKey);
        return val;
      }
    };

    Settings.setUserContext = function (key, val) {
      var gUserKey = Settings.getUserKey(key);
      if (gUserKey && !isUndefined(val)) {
        lscache.set(gUserKey, val);
        return val;
      }
    };

    /**
     * @description  For clearing out user settings, consider using it on logout.
     */
    Settings.clearCache = function () {
      var bId = Settings.getBucketId();
      if (bId) {
        lscache.set(bId, null);
      }
    };

    /**
     * @description  In case it all needs to die, use this based on a config version?
     */
    Settings.clearAll = function () {
      lscache.flush();
    };

    this.$get = function () {
      return Settings;
    };
  }
);
