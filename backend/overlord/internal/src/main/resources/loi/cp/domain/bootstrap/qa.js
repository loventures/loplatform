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

/**
 * config {
 *   redshiftSchemaName: "qa0"
 * }
 */
function qa(config) {
  return {
    name: 'QA Automation Domain',
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
      'loi.cp.gradebook.notifications.GradeNotification': {
        enabled: true,
      },
      'loi.authoring.Authoring': {
        enabled: true,
      },
      'pcp.course.Student': {
        enabled: true,
      },
      'loi.cp.accesscode.AccessCodeAdminPage': {
        enabled: true,
      },
      'loi.cp.localmail.LocalmailWebControllerImpl': {
        enabled: true,
        configuration: {
          enabled: true,
        },
      },
    },
    bootstrap: [
      {
        phase: 'core.configurate',
        config: [
          {
            key: 'authoring',
            blob: {
              synchronousIndexing: true,
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
        phase: 'core.connector.create',
        config: [
          {
            identifier: 'loi.cp.apikey.ApiKeySystemImpl',
            systemId: 'int-test',
            name: 'Integration Test',
            key: getenv('de.strap.api.key'),
            rights:
              'loi.cp.admin.right.HostingAdminRight,loi.cp.admin.right.AnalyticsConsumerRight',
          },
          {
            identifier: 'loi.cp.lti.BasicLTIProducerSystem',
            systemId: 'test-lti',
            name: 'Test LTI',
            key: getenv('de.strap.lti.secret'),
          },
        ],
      },
      {
        phase: 'core.ltiTool.create',
        config: [
          {
            toolId: 'de-lti-test',
            name: 'DE LTI Test',
            settings: {
              url: 'https://<HOSTNAME>/lti/launch/library/g4epf80s-n1te0jcj/guid/xkfwr4e5-s7ue0jcj',
              key: 'test-lti',
              secret: getenv('de.strap.lti.secret'),
              launchStyle: 'FRAMED',
              includeUsername: true,
              includeRoles: true,
              includeEmailAddress: true,
              includeContextTitle: true,
              useExternalId: true,
              isGraded: true,
              ltiVersion: 'LTI-1p0',
              ltiMessageType: 'basic-lti-launch-request',
              customParameters: {},
            },
          },
          {
            toolId: 'saltire',
            name: 'SALTIRE',
            settings: {
              url: 'https://saltire.lti.app/tool',
              key: 'jisc.ac.uk',
              secret: 'secret',
              launchStyle: 'FRAMED',
              includeUsername: true,
              includeRoles: true,
              includeEmailAddress: true,
              includeContextTitle: true,
              useExternalId: false,
              ltiVersion: 'LTI-1p0',
              ltiMessageType: 'basic-lti-launch-request',
              customParameters: {},
            },
          },
        ],
      },
      {
        phase: 'core.role.create',
        config: {
          idStr: 'role-administrator',
          roleId: 'administrator',
          name: 'Administrator',
          rights: [
            'loi.cp.admin.right.HostingAdminRight',
            'loi.cp.admin.right.AdminRight',
          ],
        },
      },
      {
        phase: 'core.role.create',
        config: {
          idStr: 'role-domainAdministrator',
          roleId: 'domainAdministrator',
          name: 'Domain Administrator',
          rights: ['loi.cp.admin.right.AdminRight'],
        },
      },
      {
        phase: 'core.role.create',
        config: {
          idStr: 'role-advisor',
          roleId: 'advisor',
          name: 'Advisor',
          rights: [],
        },
      },
      {
        phase: 'core.role.create',
        config: {
          idStr: 'role-mediaAdministrator',
          roleId: 'mediaAdministrator',
          name: 'Media Administrator',
          rights: ['loi.cp.admin.right.MediaAdminRight'],
        },
      },
      {
        phase: 'core.user.create',
        config: {
          userName: 'automation_admin',
          givenName: 'Automation',
          familyName: 'Admin',
          emailAddress: 'automation_admin@loqa.com',
          password: getenv('de.strap.user.password'),
          roles: ['administrator', 'mediaAdministrator'],
        },
      },
      {
        phase: 'core.user.create',
        config: {
          userName: 'domain_admin',
          givenName: 'Domain',
          familyName: 'Admin',
          emailAddress: 'domain_admin@loqa.com',
          password: getenv('de.strap.user.password'),
          roles: ['domainAdministrator'],
        },
      },
      {
        phase: 'core.user.create',
        config: {
          userName: 'automationAdvisor01',
          givenName: 'automationAdvisor01',
          middleName: 'automationAdvisor01',
          familyName: 'automationAdvisor01',
          emailAddress: 'LOAutomationAdvisor1@openmail.cc',
          password: getenv('de.strap.user.password'),
          roles: ['advisor'],
        },
      },
      {
        phase: 'core.user.create',
        config: {
          userName: 'automationInstructor01',
          givenName: 'automationInstructor01',
          middleName: 'automationInstructor01',
          familyName: 'automationInstructor01',
          emailAddress: 'automationInstructor01@localmail',
          password: getenv('de.strap.user.password'),
          roles: ['faculty'],
        },
      },
      {
        phase: 'core.user.create',
        config: {
          userName: 'automationInstructor02',
          givenName: 'automationInstructor02',
          middleName: 'automationInstructor02',
          familyName: 'automationInstructor02',
          emailAddress: 'automationInstructor02@localmail',
          password: getenv('de.strap.user.password'),
          roles: ['faculty'],
        },
      },
      {
        phase: 'core.user.create',
        config: {
          userName: 'automationStudent01',
          givenName: 'automationStudent01',
          middleName: 'automationStudent01',
          familyName: 'automationStudent01',
          emailAddress: 'automationStudent01@localmail',
          password: getenv('de.strap.user.password'),
          roles: ['student'],
        },
      },
      {
        phase: 'core.user.create',
        config: {
          userName: 'automationStudent02',
          givenName: 'automationStudent02',
          middleName: 'automationStudent02',
          familyName: 'automationStudent02',
          emailAddress: 'automationStudent02@localmail',
          password: getenv('de.strap.user.password'),
          roles: ['student'],
        },
      },
      {
        phase: 'core.user.create',
        config: {
          userName: 'automationStudent03',
          givenName: 'automationStudent_aa',
          middleName: 'automationStudent_mm',
          familyName: 'automationStudent_zz',
          emailAddress: 'automationStudent03@localmail',
          externalId: 'extId_aa',
          password: getenv('de.strap.user.password'),
          roles: ['student'],
        },
      },
      {
        phase: 'core.user.create',
        config: {
          userName: 'automationStudent04',
          givenName: 'automationStudent_ab',
          middleName: 'automationStudent_mn',
          familyName: 'automationStudent_yz',
          emailAddress: 'automationStudent04@localmail',
          externalId: 'extId_ab',
          password: getenv('de.strap.user.password'),
          roles: ['student'],
        },
      },
      {
        phase: 'core.user.create',
        config: {
          userName: 'automationStudent05',
          givenName: 'automationStudent_bb',
          middleName: 'automationStudent_cc',
          familyName: 'automationStudent_ss',
          emailAddress: 'automationStudent05@localmail',
          externalId: 'extId_bb',
          password: getenv('de.strap.user.password'),
          roles: ['student'],
        },
      },
      {
        phase: 'core.user.create',
        config: {
          userName: 'automationStudent06',
          givenName: 'automationStudent_ee',
          middleName: 'automationStudent_ff',
          familyName: 'automationStudent_gg',
          emailAddress: 'automationStudent06@localmail',
          externalId: 'extId_ee',
          password: getenv('de.strap.user.password'),
          roles: ['student'],
        },
      },
      {
        phase: 'core.user.create',
        config: {
          userName: 'automationStudent07',
          givenName: 'automationStudent_hh',
          middleName: 'automationStudent_ii',
          familyName: 'automationStudent_tt',
          emailAddress: 'automationStudent07@localmail',
          externalId: 'extId_hh',
          password: getenv('de.strap.user.password'),
          roles: ['student'],
        },
      },
      {
        phase: 'core.user.create',
        config: {
          userName: 'automationStudent08',
          givenName: 'automationStudent_kk',
          middleName: 'automationStudent_ll',
          familyName: 'automationStudent_mm',
          emailAddress: 'automationStudent08@localmail',
          externalId: 'extId_kk',
          password: getenv('de.strap.user.password'),
          roles: ['student'],
        },
      },
      {
        phase: 'core.user.create',
        config: {
          userName: 'automationStudent09',
          givenName: 'automationStudent_nn',
          middleName: 'automationStudent_oo',
          familyName: 'automationStudent_pp',
          emailAddress: 'automationStudent09@localmail',
          externalId: 'extId_nn',
          password: getenv('de.strap.user.password'),
          roles: ['student'],
        },
      },
      {
        phase: 'core.user.create',
        config: {
          userName: 'automationStudent10',
          givenName: 'automationStudent_qq',
          middleName: 'automationStudent_rr',
          familyName: 'automationStudent_ss',
          emailAddress: 'automationStudent10@localmail',
          externalId: 'extId_qq',
          password: getenv('de.strap.user.password'),
          roles: ['student'],
        },
      },
      {
        phase: 'core.user.create',
        config: {
          userName: 'automationStudent11',
          givenName: 'automationStudent_tt',
          middleName: 'automationStudent_uu',
          familyName: 'automationStudent_vv',
          emailAddress: 'automationStudent11@localmail',
          externalId: 'extId_tt',
          password: getenv('de.strap.user.password'),
          roles: ['student'],
        },
      },
      {
        phase: 'core.user.create',
        config: {
          userName: 'Project_User',
          givenName: 'ProjectUser',
          middleName: 'ProjectUser',
          familyName: 'ProjectUser',
          emailAddress: 'ProjectUser@loqa.com',
          password: getenv('de.strap.user.password'),
          roles: ['administrator'],
        },
      },
      {
        phase: 'core.user.create',
        config: {
          userName: 'curriculumDeveloper',
          givenName: 'Curriculum',
          middleName: 'CD',
          familyName: 'Developer',
          emailAddress: 'CurriculumDeveloper@loqa.com',
          password: getenv('de.strap.user.password'),
          roles: ['curriculumDeveloper'],
        },
      },
      {
        phase: 'core.user.create',
        config: {
          userName: 'adminTester',
          givenName: 'Admin',
          familyName: 'Tester',
          emailAddress: 'admintester@loqa.com',
          password: getenv('de.strap.user.password'),
          roles: ['administrator'],
        },
      },
      {
        phase: 'core.user.create',
        config: {
          userName: 'studentTester',
          givenName: 'Student',
          familyName: 'Tester',
          emailAddress: 'studenttester@loqa.com',
          password: getenv('de.strap.user.password'),
          roles: ['student'],
        },
      },
      {
        phase: 'core.user.create',
        config: {
          userName: 'instructorTester',
          givenName: 'Instructor',
          familyName: 'Tester',
          emailAddress: 'instructortester@loqa.com',
          password: getenv('de.strap.user.password'),
          roles: ['faculty'],
        },
      },
      {
        phase: 'project.importCourse',
        config: {
          projectName: 'HW_Auto',
          createdBy: 'Project_User',
          importDescription: 'bootstrap import for HW tests',
          zip: getenv('de.strap.asset.path') + '/qaauto-hw-20250621.zip',
          publish: true,
        },
      },
      {
        phase: 'analytics.redshift.initializeEtl',
        config: {
          redshiftSchemaName: config.redshiftSchemaName,
        },
      },
    ],
  };
}

JSON.stringify(qa(config));
