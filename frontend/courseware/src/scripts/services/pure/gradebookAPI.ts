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

import { head } from 'lodash';
import Course from '../../bootstrap/course.ts';
import UrlBuilder from '../../utilities/UrlBuilder.js';

/** The Request object this service needs (only promiseRequest). */
export interface RequestLike {
  promiseRequest(url: any, method?: string, ...rest: any[]): PromiseLike<any>;
}

/** The Settings surface this service reads. */
export interface SettingsLike {
  isFeatureDisabled(feature: string): boolean;
  getUserGlobal(key: string, fallback: any): any;
  getSettings(name: string): any;
}

/**
 * Gradebook (lwgrade2) data API, migrated verbatim from the AngularJS
 * `GradebookAPI` service to plain TS taking the injected `Settings` and `Request`.
 */
export const makeGradebookAPI = (Settings: SettingsLike, Request: RequestLike) => {
  /** @alias GradebookAPI **/
  const GradebookAPI: any = {};

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
    syncStudentHistory: '/api/v2/lwgrade2/{courseId}/gradebook/grades/syncHistory/{edgePath}/{studentId}',
    syncEdgeHistory: '/api/v2/lwgrade2/{courseId}/gradebook/grades/syncHistory/{edgePath}',
    syncAllEdgeHistory: '/api/v2/lwgrade2/{courseId}/gradebook/grades/syncHistory/{edgePath}/all',
    syncAllHistory: '/api/v2/lwgrade2/{courseId}/gradebook/grades/syncHistory/all',
  };

  GradebookAPI.downloadStudentGrades = function (courseId: any, studentId: any) {
    return new (UrlBuilder as any)(GradebookAPI.urls.downloadStudent, {
      courseId: courseId || Course.id,
      studentId,
    }).toString();
  };

  GradebookAPI.downloadGrades = function (courseId: any) {
    const useRollup = !Settings.isFeatureDisabled('GradebookExportRollup');

    const config = {
      userOrder: [
        { property: 'familyName', direction: 'ASC' },
        { property: 'givenName', direction: 'ASC' },
      ],
      userAttributes: ['userName', 'familyName', 'givenName', 'externalId'],
      categoryAverage: useRollup,
    };

    return new (UrlBuilder as any)(GradebookAPI.urls.download, {
      courseId: courseId || Course.id,
      config: window.JSON.stringify(config),
    }).toString();
  };

  // unused
  GradebookAPI.getGrades = function (searchQuery: any, courseId: any) {
    const url = new (UrlBuilder as any)(
      GradebookAPI.urls.grades,
      {
        courseId: courseId || Course.id,
        syncHistory: false,
      },
      searchQuery
    );

    return Request.promiseRequest(url, 'get');
  };

  GradebookAPI.getGradesForUsers = function (userIds: any, courseId: any, syncHistory: any) {
    const url = new (UrlBuilder as any)(
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

  GradebookAPI.getOverallGrades = function (users: any, courseId: any) {
    const filters = [['column_id', 'eq', '_root_']];

    if (users) {
      filters.push(['user_id', 'in', users.join(',')]);
    }

    const url = new (UrlBuilder as any)(
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

  GradebookAPI.getCategoryByName = function (name: any, courseId: any) {
    const url = new (UrlBuilder as any)(
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

  GradebookAPI.getCategories = function (courseId: any) {
    const url = new (UrlBuilder as any)(GradebookAPI.urls.categories, {
      courseId: courseId || Course.id,
    });

    return Request.promiseRequest(url, 'get');
  };

  GradebookAPI.getGradebookCategories = GradebookAPI.getCategories;

  GradebookAPI.createCategoryGrading = function (category: any, courseId: any) {
    const url = new (UrlBuilder as any)(GradebookAPI.urls.categories, {
      courseId: courseId || Course.id,
    });

    return Request.promiseRequest(url, 'post', category);
  };

  GradebookAPI.createGradebookCategory = GradebookAPI.createCategoryGrading;

  GradebookAPI.updateCategoryGrading = function (category: any, courseId: any) {
    const url = new (UrlBuilder as any)(GradebookAPI.urls.categories + '/' + category.id, {
      courseId: courseId || Course.id,
    });

    return Request.promiseRequest(url, 'put', category);
  };

  GradebookAPI.getColumns = function (courseId: any, embeds: any) {
    const url = new (UrlBuilder as any)(
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

  GradebookAPI.getColumnsBy = function (filters: any, courseId: any) {
    const url = new (UrlBuilder as any)(GradebookAPI.urls.columns, {
      courseId: courseId || Course.id,
    });

    url.query.setFilters(filters);

    return Request.promiseRequest(url, 'get');
  };

  GradebookAPI.getColumnsByContent = function (contentIds: any, courseId: any) {
    return GradebookAPI.getColumnsBy(
      {
        filter: ['contentItem_id', 'in', contentIds.join(',')],
      },
      courseId
    );
  };

  GradebookAPI.getColumnsById = function (columnIds: any, courseId: any) {
    return GradebookAPI.getColumnsBy(
      {
        filter: ['id', 'in', columnIds.join(',')],
      },
      courseId
    );
  };

  GradebookAPI.getColumnById = function (columnId: any, courseId: any) {
    return GradebookAPI.getColumnsById([columnId], courseId).then(head);
  };

  GradebookAPI.createAssignmentGrading = function (assignment: any, courseId: any) {
    const url = new (UrlBuilder as any)(GradebookAPI.urls.columns, {
      courseId: courseId || Course.id,
    });
    return Request.promiseRequest(url, 'post', assignment);
  };

  GradebookAPI.updateGradebookColumn = function (assignment: any, courseId: any) {
    const url = new (UrlBuilder as any)(GradebookAPI.urls.columns + '/' + assignment.id, {
      courseId: courseId || Course.id,
    });
    return Request.promiseRequest(url, 'put', assignment);
  };

  // unused
  GradebookAPI.getColumnGrades = function (columnId: any, courseId: any) {
    const url = new (UrlBuilder as any)(GradebookAPI.urls.grades, {
      courseId: courseId || Course.id,
      syncHistory: false,
    });
    url.query.setFilters([['column_id', 'eq', columnId]]);
    return Request.promiseRequest(url, 'get');
  };

  // unused
  GradebookAPI.getAssignmentGrades = function (assignmentId: any, courseId: any) {
    const url = new (UrlBuilder as any)(GradebookAPI.urls.grades, {
      courseId: courseId || Course.id,
      syncHistory: false,
    });
    url.query.setFilters([['column.contentItem_id', 'eq', assignmentId]]);
    return Request.promiseRequest(url, 'get');
  };

  GradebookAPI.getAssignmentColumn = function (assignmentId: any, courseId: any) {
    const url = new (UrlBuilder as any)(GradebookAPI.urls.oneColumn, {
      courseId: courseId || Course.id,
      columnId: assignmentId.split('.')[1] || '',
    });
    return Request.promiseRequest(url, 'get').then(function (columns: any) {
      return columns;
    });
  };

  /**
   *  @description this will remove the overridden flag on the column, if you
   *  provide a grade it will reset the current grade and later grade operations
   *  will be able to set the fields.
   *  @returns {Promise} The new column settings
   */
  GradebookAPI.removeOverride = function (userId: any, columnId: any, grade: any, courseId: any) {
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
  GradebookAPI.setScore = function (userId: any, columnId: any, grade: any, courseId: any, override: any) {
    const url = new (UrlBuilder as any)(GradebookAPI.urls.oneGrade, {
      courseId: courseId || Course.id,
    });
    const params = {
      studentId: userId,
      grade: grade,
      columnId: columnId,
      override: override === true ? override : false,
    };
    return Request.promiseRequest(url, 'post', params);
  };

  GradebookAPI.getSettings = function (courseId: any) {
    const url = new (UrlBuilder as any)(GradebookAPI.urls.settings, {
      courseId: courseId || Course.id,
    });

    return Request.promiseRequest(url, 'get').then(function (settings: any) {
      return GradebookAPI.featureOverwrites(settings);
    });
  };

  GradebookAPI.featureOverwrites = function (settings: any) {
    if (settings.gradeDisplayDefault) {
      settings.gradeDisplayMethod = Settings.getUserGlobal('grade-book-display-method', settings.gradeDisplayDefault);
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

  GradebookAPI.updateSettings = function (settings: any, courseId: any) {
    const url = new (UrlBuilder as any)(GradebookAPI.urls.settings, {
      courseId: courseId || Course.id,
    });
    return Request.promiseRequest(url, 'put', settings);
  };

  GradebookAPI.getSingleGradeSyncHistory = function (courseId: any, studentId: any, edgePath: any) {
    const url = new (UrlBuilder as any)(GradebookAPI.urls.syncStudentHistory, {
      courseId,
      studentId,
      edgePath,
    });
    return Request.promiseRequest(url, 'get');
  };

  GradebookAPI.syncExternalGrade = function (courseId: any, studentId: any, edgePath: any) {
    const url = new (UrlBuilder as any)(GradebookAPI.urls.syncStudentHistory, {
      courseId,
      studentId,
      edgePath,
    });
    return Request.promiseRequest(url, 'post');
  };

  GradebookAPI.getSingleColumnSyncHistory = function (courseId: any, edgePath: any) {
    const url = new (UrlBuilder as any)(GradebookAPI.urls.syncEdgeHistory, { courseId, edgePath });
    return Request.promiseRequest(url, 'get');
  };

  GradebookAPI.syncExternalColumn = function (courseId: any, edgePath: any) {
    const url = new (UrlBuilder as any)(GradebookAPI.urls.syncEdgeHistory, { courseId, edgePath });
    return Request.promiseRequest(url, 'post');
  };

  GradebookAPI.syncExternalGradesForColumn = function (courseId: any, edgePath: any) {
    const url = new (UrlBuilder as any)(GradebookAPI.urls.syncAllEdgeHistory, { courseId, edgePath });
    return Request.promiseRequest(url, 'post');
  };

  GradebookAPI.syncAllColumnsForCourse = function (courseId: any) {
    const url = new (UrlBuilder as any)(GradebookAPI.urls.syncAllEdgeHistory, { courseId });
    return Request.promiseRequest(url, 'post');
  };

  return GradebookAPI;
};

export type GradebookAPI = ReturnType<typeof makeGradebookAPI>;
