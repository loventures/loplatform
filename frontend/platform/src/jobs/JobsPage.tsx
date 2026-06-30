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

import axios from 'axios';
import React, { Fragment, useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';

import ReactTable from '../components/reactTable/ReactTable';
import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useTranslations } from '../redux/state';
import { asjax, trim } from '../services';
import EditAddForm from './EditAddForm';
import JobTypesComponents, { JobRow, ValidatorResult } from './jobTypes';
import RunLog from './RunLog';

interface JobComponent {
  schema: string;
  name: string;
  interfaces: Record<string, unknown>;
}

const App: React.FC = () => {
  const translations = useTranslations();
  const dispatch = useDispatch();

  const [type, setType] = useState<string | null>(null);
  const [isEmailJob, setEmailJob] = useState(false);
  const [jobTypes, setJobTypes] = useState<JobComponent[] | null>(null);
  const [typeToNameMap, setTypeToNameMap] = useState<Record<string, string>>({});
  const [jobInfo, setJobInfo] = useState<JobRow | null>(null);

  const renderForm = (row: JobRow, validationErrors: Record<string, string>) => {
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

  const parseEntity = (entity: JobRow) => {
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

  const executeJob = (selectedRow: JobRow) => {
    const started = () => {
      dispatch(
        setPortalAlertStatus(false, true, translations.t('adminPage.jobs.jobStartedAlert', selectedRow))
      );
    };
    const progress = () => {};
    return asjax(`/api/v2/jobs/${selectedRow.id}/execute`, null, progress, started)
      .then(() => {
        dispatch(
          setPortalAlertStatus(
            false,
            true,
            translations.t('adminPage.jobs.jobCompletedAlert', selectedRow)
          )
        );
        return false;
      })
      .catch(err => {
        console.log(err);
        dispatch(
          setPortalAlertStatus(
            true,
            false,
            translations.t('adminPage.jobs.failedToExecute', selectedRow)
          )
        );
      });
  };

  const renderModal = () => {
    if (!jobInfo) return null;
    return (
      <RunLog
        T={translations}
        jobInfo={jobInfo as { id: number | string; name: string }}
        close={() => setJobInfo(null)}
        setPortalAlertStatus={(error, success, message) =>
          dispatch(setPortalAlertStatus(error, success, message))
        }
      />
    );
  };

  const getRunLog = (selectedRow: JobRow) => {
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

  const validateForm = (form: JobRow): ValidatorResult => {
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
    const jobType = type ? JobTypesComponents[type] : undefined;
    const customValidation: ValidatorResult =
      (jobType && jobType.validator && jobType.validator(form, translations)) || {};
    if (customValidation.validationErrors) {
      return customValidation;
    }
    const emailData = isEmailJob
      ? {
          emailAddresses: form.emailAddresses
            ? form.emailAddresses.split(',').map((e: string) => e.trim())
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
    return (jobTypes ?? []).map(job => ({
      key: job.schema,
      name: job.name,
      onClick: () => {
        setType(job.schema);
        setEmailJob(!!job.interfaces['loi.cp.job.EmailJob']);
      },
    }));
  };

  const beforeCreateOrUpdate = (row: JobRow) => {
    if (row._type && row._type !== type) {
      setType(row._type);
      setEmailJob(
        !!(jobTypes ?? []).find(j => j.schema === row._type)!.interfaces['loi.cp.job.EmailJob']
      );
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
      dataFormat: (j: string) => typeToNameMap[j],
    },
    { dataField: 'schedule', sortable: false, searchable: false, required: true },
  ];

  useEffect(() => {
    axios
      .get('/api/v2/jobs/components')
      .then(res => {
        const jobComponents: JobComponent[] = res.data.objects;
        const typeToNameMap = jobComponents.reduce<Record<string, string>>((acc, curr) => {
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
        dispatch(setPortalAlertStatus(true, false, translations.t('error.unexpectedError')));
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
        schema={type ?? undefined}
        getModalTitle={() => (type ? typeToNameMap[type] : undefined)}
      />
      {renderModal()}
    </Fragment>
  );
};

export default App;
