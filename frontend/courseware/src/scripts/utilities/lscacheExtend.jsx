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

import dayjs from 'dayjs';
import { bind, isFunction, isObject, isString, isUndefined, now } from 'lodash';
import Sanity from './Sanitize.js';
import lscache from 'lscache';

window.lscache = lscache; // expose for debugging purposes

export default angular
  .module('lo.utilities.lscacheExtend', [])
  .factory('lscacheExtend', [
    '$q',
    'Settings',
    'Request',
    /**
     * @ngdoc service
     * @alias lscache
     * @memberof lo.utilities
     *
     * @description  IMPORTANT READ:
     *
     * Extends lscache to provide our most common use case.
     * This is _not_ a pure angular module and to use it you include lscacheExtend
     * into your run block so that it augments the globally available lscache
     * window object.   This is a bit hacky, but we would rather augment lscache
     * and just use it rather than include lscacheExtend in every service etc.
     *
     *  You want to use the methods for user data vs common global data.
     *  <pre>
     *  //For user specific data like assignments
     *  lscache.userLoad(
     *      'http://something/assignments', //Url to request, typically from loConfig
     *      {assignment: id},               //The params to pass
     *      'get',                          //get, post etc
     *      5                               //cache timeout, 5 minutes
     *  );
     *
     *  //For data like taxons information that is global across users
     *  lscache.globalLoad(
     *      'http://something/taxonomy', //Url to request, typically from loConfig
     *      {taxonomy: id},              //params
     *      'post'                       //get, post etc
     *      null,                        //No cache timeout if null
     *  );
     *  </pre>
     *
     *  Debugging: In production you can call lscache.setDebug(true) and
     *  then all further keys will be stored in lscache.keyInfo so you can
     *  look over the response information.
     *  @requires lscache from https://github.com/pamelafox/lscache
     */
    function ($q, Settings, Request) {
      lscache.offline = false;

      /**
       * @description  Check to see if localStorage actually works on this device.
       * In private browsing mode on an iPad it will not exception, but still not
       * actually work (silent failures).  This catches that.
       * @returns {boolean} true if supported
       */
      lscache.isAvailable = function () {
        try {
          var val = 'test';
          var key = 'lscache_test';
          lscache.set(key, val);
          if (val != lscache.get(key)) {
            return false;
          }
        } catch (e) {
          return false;
        }
        return true;
      };

      var cfg = {
        DEBUG: false,
        DEFAULT_LIFE: 5,
        GET_LIFE: 1 * 24 * 60, //Minute lifespan to cache for
        POST_LIFE: 1 * 1 * 60,
      };

      /**
       * @description
       * Set to true and it will keep a list of all the key information in memory, NOTE
       * it will not expire this information, but should make it easier to debug when
       * you have 'freshish' data
       * @param {boolean} truthiness turn it on or off
       */
      lscache.setDebug = function (truthiness) {
        if (cfg.DEBUG || truthiness) {
          cfg.DEBUG = truthiness;
          lscache.keyInfo = {};
        } else {
          lscache.keyInfo = null;
        }
      };
      lscache.setDebug(cfg.DEBUG); //Init the keystore if needed.

      /**
       * @description
       * Search cache for the key + id, if not create an http get
       * with the url provided.
       *
       * @param {String} url     Location you want to call
       * @param {Object} params  http params / body.  Used to make cache keys unique
       * @param {String} [method=get] 'get|post|delete|put'
       * @param {Integer} lifespan  the lifetime in minutes for the call
       * @param {boolean} [forceLoad=false] If true will skip the cache
       * @param  {function} [call] function to call to load in PLACE of any caching
       *    logic, you will have to implement all that, primarily here for testing.
       *    SEE lscache.call for what is pased in.
       * @returns {Promise} a $q promise, the promise has .cacheKey on it to tell you
       * where the cached data is if you want to unset / delete a single cache item
       * instead of calling flush.   It also has returnPromise.CACHED=true if the results
       * came from cache and returnPromise.HTTP = true if it made an ajax call.
       * @param {Object} config Extra configuration options to be added to the Angular $http request.
       */
      lscache.load = function (keyPrefix, url, params, method, lifespan, forceLoad, call, config) {
        if (!url) {
          console.error(
            'NO URL provided to load (url, params, method, lifespan)',
            url,
            params,
            method,
            lifespan
          );
          return;
        }
        method = method || 'get';
        lifespan = lifespan || (method == 'post' ? cfg.POST_LIFE : cfg.GET_LIFE);
        call = isFunction(call) ? call : lscache.call;

        var cacheKey = lscache.getKey(keyPrefix, url, params);
        var deferred = $q.defer();
        var promise = deferred.promise;
        promise.cacheKey = cacheKey;

        var ref = lscache.get(cacheKey);
        if (ref && !forceLoad) {
          if (config && (config.DO_STALE_CHECK || config.STALE_IF_BEFORE)) {
            var st = config.STALE_IF_BEFORE || lscache.getStaleTime(config.DO_STALE_CHECK);
            var check = dayjs(config.STALE_IF_BEFORE || st);

            if (check.isAfter(dayjs(ref.CACHED_AT_TIME))) {
              cfg.DEBUG &&
                console.warn('This information is stale for url, ref (reload)', url, ref);
              promise.HTTP = true;
              call(url, params, method, deferred, cacheKey, lifespan, config);
              return promise;
            }
            delete config.DO_STALE_CHECK; //Don't want this crap going to http
            delete config.STALE_IF_BEFORE;
          } else if (config && config.DO_STALE_CHECK) {
            cfg.DEBUG && console.log('Cached information good for url, ref', url, ref);
          }

          cfg.DEBUG && console.log('Cached version.', cacheKey);
          if (lscache.keyInfo) {
            lscache.keyInfo[cacheKey] = true;
          }
          if (ref.CACHED_AT_TIME) {
            //Awkwaaard
            delete ref.CACHED_AT_TIME;
          }
          Sanity.dates(ref);
          deferred.resolve(ref);
          promise.CACHED = true;
        } else {
          cfg.DEBUG && console.log('Not in cache, looking up.', cacheKey);
          promise.HTTP = true;
          call(url, params, method, deferred, cacheKey, lifespan, config);
        }

        //Safety for legacy calls, some of them use the cached info or the cacheKey
        var p = promise.then(function (data) {
          return Request.getActualData(data);
        });
        p.CACHED = promise.CACHED;
        p.HTTP_CALL = promise.HTTP_CALL;
        p.HTTP = promise.HTTP;
        p.cacheKey = promise.cacheKey;
        return p;
      };

      /**
       * @description If you want to expire certain things that care about a time update,
       * then set this time and any cache entry cached before this time will be thrown out
       * For example taking a quiz should set something that invalidates certain statistic calls.
       *
       * To activate this test on a particular call you must pass config: {DO_STALE_CHECK: true} in
       * order to use this option.  Optionally to specify the time you can
       * use {STALE_IF_BEFORE: dayjs}
       *
       * If you want to provide the time for a particular call rather than a global, then in your
       * config argument to an lscache.userload or .globalLoad you can specify config.STALE_IF_BEFORE
       * and give it a valid dayjs or string date.
       * @params {Object} time where if the cache entry is before this we will make the
       * call again (Defaults to now)
       * @params {string}  a different key to use in doing this time specific check.
       */
      const DEFAULT_TIMEOUT_KEY = 'DEFAULT_TIMEOUT_KEY';
      lscache.setStaleTime = function (key, time) {
        cfg.DEBUG && console.log('Setting stale time', key, time);
        key = key || DEFAULT_TIMEOUT_KEY;
        time = time || dayjs();
        lscache.set(key, time);
        return time;
      };

      /**
       *  @description Will get the currently considered stale time out of the cache and return it as a
       *  dayjs.
       *  @params {String} [key] a key to use in returning the time to compare against.   If you
       *  need specific timeouts (probably shouldn't) then set the time you want using setStaleTime
       *  and the key you used to get it out.
       */
      lscache.getStaleTime = function (key) {
        if (!key) return null;

        key = isString(key) ? key : DEFAULT_TIMEOUT_KEY;
        var val = lscache.get(key);
        if (val) {
          val = dayjs(val);
        }
        return val;
      };

      /**
       * @description
       * Provide a key to cache on based on url and params.  Internal method,
       * sometimes used to check what key is used.  The promise returned from
       * most load files will have cacheKey=return of this method on them.
       *
       * @param {String} keyPrefix often the combination of user+role information
       * @param {String} url url to load
       * @param {Object} params - json version becomes the suffix
       * @returns {String} a key to use when calling lscache.set(cacheKey, obj)
       */
      lscache.getKey = function (keyPrefix, url, params) {
        var key = keyPrefix || '';
        key += params ? url + angular.toJson(params) : url;
        return key;
      };

      /**
       * For ease of overriding all test cases.
       */
      lscache.call = function (url, params, method, deferred, cacheKey, lifespan, config) {
        var success = bind(lscache.cacheSuccess, lscache, cacheKey, lifespan);
        var error = lscache.reject;
        return Request.promiseRequest(url, method, params, success, error, deferred, null, config);
      };

      /**
       * @description
       *  User and course bucket in local storage (many people, same browser)
       *  See {@link lo.utilities.lscache#load} for parameters
       * @param {String} url     Location you want to call
       * @param {Object} params  http params / body.  Used to make cache keys unique
       * @param {String} [method=get] 'get|post|delete|put'
       * @param {Integer} lifespan  the lifetime in minutes for the call
       * @param {boolean} [forceLoad=false] If true will skip the cache
       * @param  {function} [call] function to call to load in PLACE of any caching
       *    logic, you will have to implement all that, primarily here for testing.
       *    SEE lscache.call for what is pased in.
       * @param {Object} config Extra configuration options to be added to the Angular $http request.
       */
      lscache.userLoad = function (url, params, method, lifespan, forceLoad, call, config) {
        if (!Settings.courseId || !Settings.userId) {
          throw 'Cannot userLoad without Settings initialized.';
        }
        return lscache.load(
          Settings.getUserKey(),
          url,
          params,
          method,
          lifespan,
          forceLoad,
          call,
          config
        );
      };

      /**
       * @description
       * Assign the cache entry, key is typically the method of load + id
       * sent into it (tempting to use the url)
       *
       * @param {string} key The key to store into the cache with
       * @param {Object} data The data to store into lscache / local storage
       * @param {int} [lifespan=cfg.DEFAULT_LIFE] How long should it live in cache in minutes.  Set to null to cache forever.
       */
      lscache.setCache = function (key, data, lifespan) {
        if (lscache.keyInfo) {
          lscache.keyInfo[key] = true;
        }
        //If offline no expire.
        if (lscache.offline) {
          lifespan = null;
        } else if (isUndefined(lifespan)) {
          lifespan = cfg.DEFAULT_LIFE;
        }
        lscache.set(key, data, lifespan);
        return data;
      };

      //Top level rejection handler, but here for test cases.
      lscache.reject = Request.reject;
      lscache.resolve = Request.resolve;

      /**
       * @description
       * On successfully returning data with actual results, we want to try and cache
       * it.  This is the default callback passed from the load method.
       *
       * @param {String} cacheKey Cache key to use
       * @param {int}    lifespan Time to live in minutes
       * @param {$q.defer} deferred the defer object to resolve on success
       * @returns {function(data)} This function will resolve the promise when results are considered
       * non-empty / good.
       */
      lscache.cacheSuccess = function (cacheKey, lifespan, deferred) {
        return function (data, status) {
          try {
            if (Request.isValid(data, status)) {
              if (Request.hasResults(data)) {
                cfg.DEBUG && console.log('Setting cache with: ', cacheKey, data, lifespan);
                if (isObject(data)) {
                  data.CACHED_AT_TIME = now();
                }
                lscache.setCache(cacheKey, data, lifespan);
                Sanity.dates(data);
                if (data.CACHED_AT_TIME) {
                  //Awkwaaard
                  delete data.CACHED_AT_TIME;
                }
                deferred.resolve(data);
              } else {
                cfg.DEBUG && console.log('Not caching empty response: ', cacheKey, data, lifespan);
                if (data.CACHED_AT_TIME) {
                  //Awkwaaard
                  delete data.CACHED_AT_TIME;
                }
                deferred.resolve(data); //Still need to resolve even if not caching.
              }
            } else {
              cfg.DEBUG && console.warn('Invalid result returned from the server.', cacheKey, data);
              deferred.reject(data); //Error occurred or results are bad
            }
          } catch (e) {
            console.error('Exception in caching.', e);
            deferred.reject(data);
          }
        };
      };
      return lscache;
    },
  ])
  .run([
    'lscacheExtend',
    function (lscacheExtend) {
      lscacheExtend.isAvailable();
    },
  ]);
