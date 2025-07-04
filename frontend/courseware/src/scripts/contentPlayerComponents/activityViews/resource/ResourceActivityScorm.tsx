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

import { EmbeddedContent } from '../../../richContent/embeddedContent.ts';
import { buildScormUrl, loadScormData, setScormData, submitScorm } from '../../../api/scormApi.ts';
import Course from '../../../bootstrap/course.ts';
import ERContentTitle from '../../../commonPages/contentPlayer/ERContentTitle.tsx';
import TooltippedGradeBadge from '../../../components/TooltippedGradeBadge.tsx';
import dayjs from 'dayjs';
import localized from 'dayjs/plugin/localizedFormat';
import { debounce } from 'lodash';
import { ActivityProps } from '../ActivityProps.ts';
import ActivityCompetencies from '../../parts/ActivityCompetencies.tsx';
import { reportProgressActionCreator } from '../../../courseActivityModule/actions/activityActions.ts';
import { ContentWithRelationships } from '../../../courseContentModule/selectors/assembleContentView.ts';
import { CONTENT_TYPE_SCORM } from '../../../utilities/contentTypes.ts';
import React, { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';

dayjs.extend(localized);

const isProdLike = window.lo_platform.environment.isProdLike;

const buildUrl = (content: ContentWithRelationships, activityData: any) => {
  return buildScormUrl(Course.commitId, content.node_name, activityData.resourcePath || '');
};

declare global {
  interface Window {
    API: any;
    API_1484_11: any;
  }
}

const ResourceActivityScorm: React.FC<ActivityProps<CONTENT_TYPE_SCORM>> = ({
  content,
  printView,
}) => {
  const dispatch = useDispatch();
  const [activityData, setActivityData] = useState<null | any>(null);
  const [localApiData, setLocalApiData] = useState<null | any>(null);
  const [doManualLaunch, setDoManualLaunch] = useState(0);
  const scormDataLoaded = Boolean(activityData && localApiData);

  useEffect(() => {
    /**
     * Custom shared data from the ADL 1.2 standard is represented in a unique format. Specifically, instead of storing
     *
     * { 'a.custom.value': 'thevalue' }
     *
     * it stores
     *
     * {
     *   'adl.data.0.id': 'a.custom.value',
     *   'adl.data.0.store': 'thevalue'
     * }
     *
     * additional variables are stored similarly as 'adl.data.1' and so on. On the backend we store something like the
     * former, as it is easier to process, and we merge/split the two in this file here.
     */
    loadScormData(content.id).then(loadedData => {
      //general important values for this activity
      setActivityData({
        contentHeight: loadedData.contentHeight,
        contentWidth: loadedData.contentWidth,
        launchNewWindow: loadedData.launchNewWindow,
        resourcePath: loadedData.resourcePath,
        allRefs: loadedData.allRefs,
      });

      //values specifically going into the window.API that SCORM needs
      const data = loadedData.apiData;

      let i = 0;
      for (const key in loadedData.sharedData) {
        const value = loadedData.sharedData[key];
        data['adl.data.' + i + '.id'] = key;
        data['adl.data.' + i + '.store'] = value;
        i++;
      }

      setLocalApiData(data);
    });

    return () => {
      //reset so this reloads when we navigate back to this activity
      setActivityData(null);
      setLocalApiData(null);
    };
  }, [content.id]);

  useEffect(() => {
    if (!scormDataLoaded) {
      return;
    }

    const contentId = content.id;

    const CORE_SCORE_RAW = 'cmi.core.score.raw';
    const CORE_SCORE_MIN = 'cmi.core.score.min';
    const CORE_SCORE_MAX = 'cmi.core.score.max';
    const SCORE_RAW = 'cmi.score.raw';
    const SCORE_MIN = 'cmi.score.min';
    const SCORE_MAX = 'cmi.score.max';
    const LESSON_STATUS = 'cmi.core.lesson_status';
    const COMPLETION_STATUS = 'cmi.completion_status';
    const SUCCESS_STATUS = 'cmi.success_status';

    const COMPLETION_TIMESTAMP = 'lo.completion_date'; //our own special field

    //Full list is "passed", "completed", "failed", "incomplete", "browsed", "not attempted", but these are done-like
    const COMPLETED_VALUES = ['passed', 'completed', 'failed'];

    const COUNT = '_count';

    const getCountForPrefix = (prefix: string) => {
      let count = 0;

      for (const key in localApiData) {
        if (key && key.length > 0 && key.startsWith(prefix)) {
          //we have a series of keys that look like 'cmi.objectives.0.id'; skip 'cmi.objectives.' and parseInt the end
          count = Math.max(count, parseInt(key.substr(prefix.length), 10) + 1 || 0);
        }
      }
      return count;
    };

    const getValue = (key: string) => {
      let ret: any;
      if (key.endsWith(COUNT)) {
        const prefix: string = key.substr(0, key.length - COUNT.length);
        ret = getCountForPrefix(prefix);
      } else {
        ret = localApiData[key] || ''; //default for scorm is an empty string, things check for this (and not null)
      }
      return ret;
    };

    const isGradeTrigger = (key: string) => key.includes('score') || key.includes('status');
    const getScoreRaw = () => localApiData[SCORE_RAW] || localApiData[CORE_SCORE_RAW];
    const getScoreMin = () => localApiData[SCORE_MIN] || localApiData[CORE_SCORE_MIN];
    const getScoreMax = () => localApiData[SCORE_MAX] || localApiData[CORE_SCORE_MAX];
    const isCompletedStatus = (statusValue: string) => COMPLETED_VALUES.includes(statusValue);
    const isCompleted = () =>
      isCompletedStatus(localApiData[LESSON_STATUS]) ||
      isCompletedStatus(localApiData[COMPLETION_STATUS]) ||
      isCompletedStatus(localApiData[SUCCESS_STATUS]);

    //This function debounces, and also performs a processing step to split out the 'adl.data.*' custom data.
    const saveApiData = debounce((contentId: string, apiData: any) => {
      const apiDataCopy = Object.assign({}, apiData);

      const sharedCount = getValue('adl.data._count');
      const sharedData: any = {};
      for (let i = 0; i < sharedCount; i++) {
        const adlIdKey = 'adl.data.' + i + '.id';
        const adlStoreKey = 'adl.data.' + i + '.store';

        sharedData[apiDataCopy[adlIdKey]] = apiDataCopy[adlStoreKey];
        delete apiDataCopy[adlIdKey];
        delete apiDataCopy[adlStoreKey];
      }

      setScormData(contentId, apiDataCopy, sharedData);
    }, 500);

    //Debounce and submit grade for this scorm activity
    const submitGrade = debounce((raw: number, min: number, max: number, contentId: string) => {
      submitScorm(raw, min, max, contentId);
    }, 500);

    window.API = {
      LMSInitialize: function (str: string) {
        if (!isProdLike) console.log('LMSInitialize ' + str);
        return 'true';
      },
      LMSFinish: function (str: string) {
        if (!isProdLike) console.log('LMSFinish ' + str);
        if (localApiData['adl.nav.request']) {
          //if this is a nav, we acknowledge that, and do not close the window when the session briefly suspends
          delete localApiData['adl.nav.request']; //but also only prevent this once, so it goes away
        } else {
          closeScormWindow();
        }
        return 'true';
      },
      LMSGetValue: function (key: string) {
        const ret = getValue(key);
        if (!isProdLike) {
          console.log('LMSGetValue ' + key + ' => ' + ret);
        }
        return ret;
      },
      LMSSetValue: function (key: string, value: any) {
        if (!isProdLike) {
          console.log('LMSSetValue ' + key + ' = ' + value);
        }

        if (key == 'adl.nav.request') {
          const targetMatch = value.match(/\{target=(.*)\}/);
          if (targetMatch && targetMatch[1]) {
            let newRef = activityData.allRefs[targetMatch[1]];
            //if this is an <item>, it points to an identifierref, which needs to be looked up one more time
            if (activityData.allRefs[newRef]) {
              newRef = activityData.allRefs[newRef];
            }

            //this causes a full reload, but should be totally fine if the SCORM activity is using API data properly
            setActivityData({ ...activityData, resourcePath: newRef });
          }
        }
        localApiData[key] = value;

        if (isCompleted() && !localApiData[COMPLETION_TIMESTAMP]) {
          localApiData[COMPLETION_TIMESTAMP] = new Date();
          dispatch(reportProgressActionCreator(content, true));
        }

        /**
         * I really wanted a numerator and denominator, but apparently some SCORM implementations just give you a 'raw'
         * value and not the 'max'. I also know those SCORM archives to immediately set 'raw' to the value of '0' on
         * launch, so to avoid panicking students we wait to submit until the activity marks itself as completed-ish*
         *
         * * completed here means 'passed', 'failed', or the ambivalent 'completed' status
         *
         * If there is no max, assume it is out of 100 (sorry I tried)
         */
        if (isGradeTrigger(key) && getScoreRaw() && (getScoreMax() || isCompleted())) {
          if (!isProdLike) {
            console.log('Submitting grade (raw=' + getScoreRaw() + ', max=' + getScoreMax() + ')');
          }
          submitGrade(getScoreRaw(), getScoreMin(), getScoreMax() || 100, contentId);
        }

        saveApiData(contentId, localApiData);
      },
      LMSCommit: function (str: string) {
        if (!isProdLike) {
          console.log('LMSCommit ' + str);
        }
        saveApiData(contentId, localApiData);
        return 'true';
      },
      LMSGetLastError: function () {
        return 0; /* we serve content, not errors */
      },
      LMSGetErrorString: function (errorCode: any) {
        return errorCode ? 'No error string available.' : '';
      },
      LMSGetDiagnostic: function (errorCode: any) {
        return errorCode ? 'No diagnostic information available.' : '';
      },
    };

    window.API_1484_11 = {
      Initialize: window.API.LMSInitialize,
      Terminate: window.API.LMSFinish,
      GetValue: window.API.LMSGetValue,
      SetValue: window.API.LMSSetValue,
      Commit: window.API.LMSCommit,
      GetLastError: window.API.LMSGetLastError,
      GetErrorString: window.API.LMSGetErrorString,
      GetDiagnostic: window.API.LMSGetDiagnostic,
    };

    return () => {
      //unfortunately the useState openedWindow is (probably) cleaned up before here, so I use a disgraceful global
      closeScormWindow();
      delete window.API;
      delete window.API_1484_11;
    };
  }, [scormDataLoaded, content.id]);

  const openScormWindow = (content: ContentWithRelationships, activityData: any) => {
    //size should always be specified, but provide defaults just in case
    const height = activityData.contentHeight || 600;
    const width = activityData.contentWidth || 960;
    //offsets are a little arbitrary, but seemed helpful
    const left = (window.screenLeft || window.screenX) + (window.outerWidth - width) / 2; //center it
    const top = (window.screenTop || window.screenY) + 200; //a little arbitrary, but seems alright
    const features = `popup,height=${height},width=${width},left=${left},top=${top}`;
    const url = buildUrl(content, activityData);
    const popup = open(location.origin + url, 'scorm' + content.id, features);
    window.API.openedWindow = popup; // I apologize, needed for the return () => {...} cleanup above
  };

  const closeScormWindow = () => {
    window.API?.openedWindow?.close();
  };

  const relaunchWindow = () => setDoManualLaunch(ml => ml + 1);

  // initial launch (scormDataLoaded) + re-launching (doManualLaunch) + navigation events (activityData)
  useEffect(() => {
    if (scormDataLoaded && activityData.launchNewWindow && !printView) {
      openScormWindow(content, activityData);
    }
  }, [scormDataLoaded, doManualLaunch, activityData]);

  return (
    <div className="card er-content-wrapper">
      <div className="card-body">
        <ERContentTitle
          content={content}
          printView={printView}
        />
        <TooltippedGradeBadge />
        {scormDataLoaded ? (
          activityData?.launchNewWindow ? (
            <div className="er-expandable-activity">
              <div className="card mb-4 er-instructions">
                <div className="card-body d-flex align-items-center">
                  Activity has been launched in a new window. You may need to disable popup blockers
                  to view the content.
                </div>
              </div>
              {!printView && (
                <div className="flex-column-center d-print-none">
                  <button
                    className="btn btn-success btn-lg"
                    onClick={relaunchWindow}
                  >
                    Relaunch Activity
                  </button>
                </div>
              )}
            </div>
          ) : (
            <EmbeddedContent
              url={buildUrl(content, activityData)}
              title={content.name}
              contentWidth={activityData.contentWidth}
              contentHeight={activityData.contentHeight}
            />
          )
        ) : null}
        <ActivityCompetencies content={content} />
      </div>
    </div>
  );
};

export default ResourceActivityScorm;
