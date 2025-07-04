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

import axios from 'axios';
import React, { Fragment, useEffect, useState } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

import ReactTable from '../components/reactTable/ReactTable';
import * as MainActions from '../redux/actions/MainActions';
import { asjax, trim } from '../services';
import EditAddForm from './EditAddForm';
import JobTypesComponents from './jobTypes';
import RunLog from './RunLog';

const App = ({ translations, setPortalAlertStatus }) => {
  const [type, setType] = useState(null);
  const [isEmailJob, setEmailJob] = useState(false);
  const [jobTypes, setJobTypes] = useState(null);
  const [typeToNameMap, setTypeToNameMap] = useState({});
  const [jobInfo, setJobInfo] = useState(null);

  const renderForm = (row, validationErrors) => {
    return (
      <EditAddForm
        T={translations}
        row={row}
        validationErrors={validationErrors}
        type={type}
        isEmailJob={isEmailJob}
      />
    );
  };

  const parseEntity = entity => {
    const state = entity.disabled ? 'suspended' : 'active';
    const emailData = isEmailJob
      ? {
          emailAddresses: entity.emailAddresses?.join(', ') ?? [],
        }
      : {};
    return {
      ...entity,
      job: entity._type,
      state: translations.t(`adminPage.jobs.state.${state}`),
      ...emailData,
    };
  };

  const executeJob = selectedRow => {
    const started = () => {
      setPortalAlertStatus(
        false,
        true,
        translations.t('adminPage.jobs.jobStartedAlert', selectedRow)
      );
    };
    const progress = () => {};
    return asjax(`/api/v2/jobs/${selectedRow.id}/execute`, null, progress, started)
      .then(() => {
        setPortalAlertStatus(
          false,
          true,
          translations.t('adminPage.jobs.jobCompletedAlert', selectedRow)
        );
        return false;
      })
      .catch(err => {
        console.log(err);
        setPortalAlertStatus(
          true,
          false,
          translations.t('adminPage.jobs.failedToExecute', selectedRow)
        );
      });
  };

  const renderModal = () => {
    if (!jobInfo) return null;
    return (
      <RunLog
        T={translations}
        jobInfo={jobInfo}
        close={() => setJobInfo(null)}
        setPortalAlertStatus={setPortalAlertStatus}
      />
    );
  };

  const getRunLog = selectedRow => {
    setJobInfo(selectedRow);
    return Promise.resolve(false);
  };

  const getButtonInfo = () => {
    return [
      {
        name: 'runJob',
        iconName: 'play_arrow',
        onClick: executeJob,
      },
      {
        name: 'jobRunLog',
        iconName: 'list',
        onClick: getRunLog,
      },
    ];
  };

  const validateForm = form => {
    if (trim(form.name) === '') {
      const params = { field: translations.t(`adminPage.jobs.fieldName.name`) };
      return {
        validationErrors: { name: translations.t('adminForm.validation.fieldIsRequired', params) },
      };
    } else if (trim(form.schedule) === '') {
      const params = { field: translations.t(`adminPage.jobs.fieldName.schedule`) };
      return {
        validationErrors: {
          schedule: translations.t('adminForm.validation.fieldIsRequired', params),
        },
      };
    }
    const customValidation =
      (JobTypesComponents[type] &&
        JobTypesComponents[type].validator &&
        JobTypesComponents[type].validator(form, translations)) ||
      {};
    if (customValidation.validationErrors) {
      return customValidation;
    }
    const emailData = isEmailJob
      ? {
          emailAddresses: form.emailAddresses
            ? form.emailAddresses.split(',').map(e => e.trim())
            : [],
        }
      : {};
    const customData = customValidation.data;
    const data = {
      schedule: trim(form.schedule),
      name: trim(form.name),
      disabled: form.active !== 'on',
      ...emailData,
      ...customData,
    };
    return { data };
  };

  const generateDropdownItems = () => {
    return jobTypes.map(job => ({
      key: job.schema,
      name: job.name,
      onClick: () => {
        setType(job.schema);
        setEmailJob(!!job.interfaces['loi.cp.job.EmailJob']);
      },
    }));
  };

  const beforeCreateOrUpdate = row => {
    if (row._type && row._type !== type) {
      setType(row._type);
      setEmailJob(!!jobTypes.find(j => j.schema === row._type).interfaces['loi.cp.job.EmailJob']);
    }
  };

  const columns = [
    { dataField: 'id', isKey: true },
    { dataField: 'state', sortable: false, searchable: false },
    { dataField: 'name', sortable: true, searchable: true, required: true },
    {
      dataField: 'job',
      sortable: false,
      searchable: false,
      required: false,
      dataFormat: j => typeToNameMap[j],
    },
    { dataField: 'schedule', sortable: false, searchable: false, required: true },
  ];

  useEffect(() => {
    axios
      .get('/api/v2/jobs/components')
      .then(res => {
        const jobComponents = res.data.objects;
        const typeToNameMap = jobComponents.reduce((acc, curr) => {
          acc[curr.schema] = curr.name;
          return acc;
        }, {});
        const sorted = jobComponents.sort((a, b) =>
          a.name.toLowerCase().localeCompare(b.name.toLowerCase())
        );
        setJobTypes(sorted);
        setTypeToNameMap(typeToNameMap);
      })
      .catch(err => {
        console.log(err);
        setPortalAlertStatus(true, false, translations.t('error.unexpectedError'));
      });
  }, []);

  return !jobTypes ? null : (
    <Fragment>
      <ReactTable
        entity="jobs"
        columns={columns}
        defaultSortField="name"
        defaultSearchField="name"
        renderForm={renderForm}
        parseEntity={parseEntity}
        createButton={false}
        createDropdown={true}
        dropdownItems={generateDropdownItems()}
        beforeCreateOrUpdate={beforeCreateOrUpdate}
        validateForm={validateForm}
        translations={translations}
        getButtons={getButtonInfo}
        setPortalAlertStatus={setPortalAlertStatus}
        schema={type}
        getModalTitle={() => type && typeToNameMap[type]}
      />
      {renderModal()}
    </Fragment>
  );
};

function mapStateToProps(state) {
  return {
    translations: state.main.translations,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

export default connect(mapStateToProps, mapDispatchToProps)(App);
