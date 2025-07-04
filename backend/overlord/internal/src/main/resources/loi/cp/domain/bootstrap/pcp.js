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

/*
 * config: {
 *   testData: true
 * }
 */


function pcp(config) {
  return {
    name: 'PCP',
    s3: {
      identity: getenv('de.strap.s3.identity'),
      credential: getenv('de.strap.s3.credential'),
    },
    archives: {
      'com.learningobjects.cbl': {
        enabled: true,
      },
    },
    components: {
      'loi.cp.redirect.RedirectAdminPage': {
        enabled: false,
      },
      'loi.cp.zip.ZipAdminPage': {
        enabled: false,
      },
      'loi.cp.gradebook.notifications.GradeNotification': {
        enabled: true,
      },
      'com.learningobjects.content.AuthoringPage': {
        enabled: true,
      },
      'pcp.course.Student': {
        enabled: true,
      },
    },
    bootstrap: [
      {
        phase: 'core.domain.settings',
        config: {
          icon: getenv('de.strap.asset.path') + '/favicon.ico',
          logo: getenv('de.strap.asset.path') + '/lo-university.svg',
          styleVariables: {
            primaryColor: '#006699',
            secondaryColor: '#566B10',
            accentColor: '#84382E',
          },
        },
      },
      {
        phase: 'core.configurate',
        config: [
          {
            key: 'authoring',
            blob: {
              synchronousIndexing: config.configureForIntegrationTests,
            },
          },
          {
            key: 'coursePreferences',
            blob: {
              allowPrintingEntireLesson: true,
            },
          },
        ],
      },
    ]
      .concat(config.configureForIntegrationTests ? [
        {
          phase: 'core.connector.create',
          config: {
            identifier: 'loi.cp.apikey.ApiKeySystemImpl',
            systemId: 'int-test',
            name: 'Integration Test',
            key: getenv('de.strap.api.key'),
            rights: 'loi.cp.admin.right.HostingAdminRight',
          },
        },
      ] : [])
      .concat(config.testData ? [
        {
          phase: 'core.user.create',
          config: {
            userName: 'admin',
            givenName: 'System',
            familyName: 'Administrator',
            password: getenv('de.strap.user.password'),
            roles: [
              'administrator',
            ],
          },
        },
        {
          phase: 'core.user.create',
          config: {
            userName: 'student1',
            givenName: 'Lyra',
            familyName: 'Belacqua',
            password: getenv('de.strap.user.password'),
            roles: [
              'student',
            ],
          },
        },
        {
          phase: 'core.user.create',
          config: {
            userName: 'student2',
            givenName: 'Will',
            familyName: 'Parry',
            password: getenv('de.strap.user.password'),
            roles: [
              'student',
            ],
          },
        },
        {
          phase: 'core.user.create',
          config: {
            userName: 'instructor1',
            givenName: 'Mary',
            familyName: 'Malone',
            password: getenv('de.strap.user.password'),
            roles: [
              'faculty',
            ],
          },
        },
        {
          phase: 'core.user.create',
          config: {
            userName: 'instructor2',
            givenName: 'Asriel',
            familyName: 'Belacqua',
            password: getenv('de.strap.user.password'),
            roles: [
              'faculty',
            ],
          },
        },
      ] : []),
  };
}

JSON.stringify(pcp(config));
