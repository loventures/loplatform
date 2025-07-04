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

import Course from '../bootstrap/course.js';
import { head } from 'lodash';
import UrlBuilder from '../utilities/UrlBuilder.js';

export default angular.module('lo.services.GradebookAPI', []).service('GradebookAPI', [
  'Settings',
  'Request',
  /**
   * @ngdoc service
   * @alias GradebookAPI
   * @memberof lo.services
   * @description Service for helping to set and get various bits of gradebook data
   * using the newer gradebook apis.
   */
  function GradebookAPI(Settings, Request) {
    /** @alias GradebookAPI **/
    var GradebookAPI = {};

    GradebookAPI.urls = {
      grades: '/api/v2/lwgrade2/{courseId}/gradebook/grades?syncHistory',
      oneGrade: '/api/v2/lwgrade2/{courseId}/gradebook/grades/grade',
      removeOverride: '/api/v2/lwgrade2/{courseId}/gradebook/grades/removeOne',
      columns: '/api/v2/lwgrade2/{courseId}/gradebook/columns',
      oneColumn: '/api/v2/lwgrade2/{courseId}/gradebook/columns/{columnId}',
      categories: '/api/v2/lwgrade2/{courseId}/gradebook/categories',
      oneCategory: '/api/v2/lwgrade2/{courseId}/gradebook/categories/{categoryId}',
      settings: '/api/v2/lwgrade2/{courseId}/gradebook/settings',
      download: '/api/v2/lwgrade2/{courseId}/gradebook/export?config',
      downloadStudent: '/api/v2/lwgrade2/{courseId}/gradebook/{studentId}/export',
      syncStudentHistory:
        '/api/v2/lwgrade2/{courseId}/gradebook/grades/syncHistory/{edgePath}/{studentId}',
      syncEdgeHistory: '/api/v2/lwgrade2/{courseId}/gradebook/grades/syncHistory/{edgePath}',
      syncAllEdgeHistory: '/api/v2/lwgrade2/{courseId}/gradebook/grades/syncHistory/{edgePath}/all',
      syncAllHistory: '/api/v2/lwgrade2/{courseId}/gradebook/grades/syncHistory/all',
    };

    GradebookAPI.downloadStudentGrades = function (courseId, studentId) {
      return new UrlBuilder(GradebookAPI.urls.downloadStudent, {
        courseId: courseId || Course.id,
        studentId,
      }).toString();
    };

    GradebookAPI.downloadGrades = function (courseId) {
      var useRollup = !Settings.isFeatureDisabled('GradebookExportRollup');

      var config = {
        userOrder: [
          { property: 'familyName', direction: 'ASC' },
          { property: 'givenName', direction: 'ASC' },
        ],
        userAttributes: ['userName', 'familyName', 'givenName', 'externalId'],
        categoryAverage: useRollup,
      };

      return new UrlBuilder(GradebookAPI.urls.download, {
        courseId: courseId || Course.id,
        config: window.JSON.stringify(config),
      }).toString();
    };

    // unused
    GradebookAPI.getGrades = function (searchQuery, courseId) {
      var url = new UrlBuilder(
        GradebookAPI.urls.grades,
        {
          courseId: courseId || Course.id,
          syncHistory: false,
        },
        searchQuery
      );

      return Request.promiseRequest(url, 'get');
    };

    GradebookAPI.getGradesForUsers = function (userIds, courseId, syncHistory) {
      var url = new UrlBuilder(
        GradebookAPI.urls.grades,
        {
          courseId: courseId || Course.id,
          syncHistory: !!syncHistory,
        },
        {
          filter: ['user_id', 'in', userIds],
        }
      );

      return Request.promiseRequest(url, 'get');
    };

    GradebookAPI.getOverallGrades = function (users, courseId) {
      var filters = [['column_id', 'eq', '_root_']];

      if (users) {
        filters.push(['user_id', 'in', users.join(',')]);
      }

      var url = new UrlBuilder(
        GradebookAPI.urls.grades,
        {
          courseId: courseId || Course.id,
          syncHistory: false,
        },
        {
          filters: filters,
        }
      );

      return Request.promiseRequest(url, 'get');
    };

    GradebookAPI.getCategoryByName = function (name, courseId) {
      var url = new UrlBuilder(
        GradebookAPI.urls.categories,
        {
          courseId: courseId || Course.id,
        },
        {
          filter: ['name', 'eq', name],
        }
      );

      return Request.promiseRequest(url, 'get').then(head);
    };

    GradebookAPI.getCategories = function (courseId) {
      var url = new UrlBuilder(GradebookAPI.urls.categories, {
        courseId: courseId || Course.id,
      });

      return Request.promiseRequest(url, 'get');
    };

    GradebookAPI.getGradebookCategories = GradebookAPI.getCategories;

    GradebookAPI.createCategoryGrading = function (category, courseId) {
      var url = new UrlBuilder(GradebookAPI.urls.categories, {
        courseId: courseId || Course.id,
      });

      return Request.promiseRequest(url, 'post', category);
    };

    GradebookAPI.createGradebookCategory = GradebookAPI.createCategoryGrading;

    GradebookAPI.updateCategoryGrading = function (category, courseId) {
      var url = new UrlBuilder(GradebookAPI.urls.categories + '/' + category.id, {
        courseId: courseId || Course.id,
      });

      return Request.promiseRequest(url, 'put', category);
    };

    GradebookAPI.getColumns = function (courseId, embeds) {
      var url = new UrlBuilder(
        GradebookAPI.urls.columns,
        {
          courseId: courseId || Course.id,
        },
        {
          embeds: embeds,
        }
      );

      return Request.promiseRequest(url, 'get');
    };

    GradebookAPI.getColumnsBy = function (filters, courseId) {
      var url = new UrlBuilder(GradebookAPI.urls.columns, {
        courseId: courseId || Course.id,
      });

      url.query.setFilters(filters);

      return Request.promiseRequest(url, 'get');
    };

    GradebookAPI.getColumnsByContent = function (contentIds, courseId) {
      return GradebookAPI.getColumnsBy(
        {
          filter: ['contentItem_id', 'in', contentIds.join(',')],
        },
        courseId
      );
    };

    GradebookAPI.getColumnsById = function (columnIds, courseId) {
      return GradebookAPI.getColumnsBy(
        {
          filter: ['id', 'in', columnIds.join(',')],
        },
        courseId
      );
    };

    GradebookAPI.getColumnById = function (columnId, courseId) {
      return GradebookAPI.getColumnsById([columnId], courseId).then(head);
    };

    GradebookAPI.createAssignmentGrading = function (assignment, courseId) {
      var url = new UrlBuilder(GradebookAPI.urls.columns, {
        courseId: courseId || Course.id,
      });
      return Request.promiseRequest(url, 'post', assignment);
    };

    GradebookAPI.updateGradebookColumn = function (assignment, courseId) {
      var url = new UrlBuilder(GradebookAPI.urls.columns + '/' + assignment.id, {
        courseId: courseId || Course.id,
      });
      return Request.promiseRequest(url, 'put', assignment);
    };

    // unused
    GradebookAPI.getColumnGrades = function (columnId, courseId) {
      var url = new UrlBuilder(GradebookAPI.urls.grades, {
        courseId: courseId || Course.id,
        syncHistory: false,
      });
      url.query.setFilters([['column_id', 'eq', columnId]]);
      return Request.promiseRequest(url, 'get');
    };

    // unused
    GradebookAPI.getAssignmentGrades = function (assignmentId, courseId) {
      var url = new UrlBuilder(GradebookAPI.urls.grades, {
        courseId: courseId || Course.id,
        syncHistory: false,
      });
      url.query.setFilters([['column.contentItem_id', 'eq', assignmentId]]);
      return Request.promiseRequest(url, 'get');
    };

    GradebookAPI.getAssignmentColumn = function (assignmentId, courseId) {
      var url = new UrlBuilder(GradebookAPI.urls.oneColumn, {
        courseId: courseId || Course.id,
        columnId: assignmentId.split('.')[1] || '',
      });
      return Request.promiseRequest(url, 'get').then(function (columns) {
        return columns;
      });
    };

    /**
     *  @description this will remove the overridden flag on the column, if you
     *  provide a grade it will reset the current grade and later grade operations
     *  will be able to set the fields.
     *  @returns {Promise} The new column settings
     */
    GradebookAPI.removeOverride = function (userId, columnId, grade, courseId) {
      return GradebookAPI.setScore(userId, columnId, grade || 0, courseId, false);
    };

    /**
     *  @description Set a score in the gradebook for a user
     *  @param {int} userId User you want to set the score for
     *  @param {edgePath} columnId the column you want to set, columnId corresponts to content Items
     *  @param {int} grade number of points to put in the column
     *  @param {int} [courseId] the course you are in
     *  @param {int} override If you want the grade to stomp any further scores in this column
     *  @returns {Promise} If it is set it returns the updated column info in the promise
     */
    // TODO: does "score" comport with modern gradebook nomenclature? maybe?
    GradebookAPI.setScore = function (userId, columnId, grade, courseId, override) {
      var url = new UrlBuilder(GradebookAPI.urls.oneGrade, {
        courseId: courseId || Course.id,
      });
      var params = {
        studentId: userId,
        grade: grade,
        columnId: columnId,
        override: override === true ? override : false,
      };
      return Request.promiseRequest(url, 'post', params);
    };

    GradebookAPI.getSettings = function (courseId) {
      var url = new UrlBuilder(GradebookAPI.urls.settings, {
        courseId: courseId || Course.id,
      });

      return Request.promiseRequest(url, 'get').then(function (settings) {
        return GradebookAPI.featureOverwrites(settings);
      });
    };

    GradebookAPI.featureOverwrites = function (settings) {
      if (settings.gradeDisplayDefault) {
        settings.gradeDisplayMethod = Settings.getUserGlobal(
          'grade-book-display-method',
          settings.gradeDisplayDefault
        );
        if (settings.gradeDisplayMethod === 'Points') {
          settings.gradeDisplayMethod = 'points';
        }
      } else {
        settings.gradeDisplayMethod = Settings.getUserGlobal(
          'grade-book-display-method',
          Settings.getSettings('GradebookSettings').gradeDisplayMethod
        );
      }

      return settings;
    };

    GradebookAPI.updateSettings = function (settings, courseId) {
      var url = new UrlBuilder(GradebookAPI.urls.settings, {
        courseId: courseId || Course.id,
      });
      return Request.promiseRequest(url, 'put', settings);
    };

    GradebookAPI.getSingleGradeSyncHistory = function (courseId, studentId, edgePath) {
      const url = new UrlBuilder(GradebookAPI.urls.syncStudentHistory, {
        courseId,
        studentId,
        edgePath,
      });
      return Request.promiseRequest(url, 'get');
    };

    GradebookAPI.syncExternalGrade = function (courseId, studentId, edgePath) {
      const url = new UrlBuilder(GradebookAPI.urls.syncStudentHistory, {
        courseId,
        studentId,
        edgePath,
      });
      return Request.promiseRequest(url, 'post');
    };

    GradebookAPI.getSingleColumnSyncHistory = function (courseId, edgePath) {
      const url = new UrlBuilder(GradebookAPI.urls.syncEdgeHistory, { courseId, edgePath });
      return Request.promiseRequest(url, 'get');
    };

    GradebookAPI.syncExternalColumn = function (courseId, edgePath) {
      const url = new UrlBuilder(GradebookAPI.urls.syncEdgeHistory, { courseId, edgePath });
      return Request.promiseRequest(url, 'post');
    };

    GradebookAPI.syncExternalGradesForColumn = function (courseId, edgePath) {
      const url = new UrlBuilder(GradebookAPI.urls.syncAllEdgeHistory, { courseId, edgePath });
      return Request.promiseRequest(url, 'post');
    };

    GradebookAPI.syncAllColumnsForCourse = function (courseId) {
      const url = new UrlBuilder(GradebookAPI.urls.syncAllEdgeHistory, { courseId });
      return Request.promiseRequest(url, 'post');
    };

    return GradebookAPI;
  },
]);
