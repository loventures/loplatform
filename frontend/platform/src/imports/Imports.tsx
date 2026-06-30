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
import moment from 'moment-timezone';
import React, { useEffect, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Progress } from 'reactstrap';

import { PageInfo } from '../adminPortal/types';
import { AdminFormFile } from '../components/adminForm';
import ReactTable from '../components/reactTable/ReactTable';
import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useTranslations } from '../redux/state';
import { SrsCollection } from '../srs';
import ImportInfo from './ImportInfo';
import { IoCloudUploadOutline } from 'react-icons/io5';

const validationUrl = '/api/v2/imports/validation';

interface ImporterType {
  impl: string;
  label: string;
}

interface DropdownItem {
  key: string;
  name: string;
  onClick: () => void;
}

interface ImportsPageInfo extends PageInfo {
  entity: string;
}

// Notice: 10 is used exactly 10 times. If you add one more 10, you must add 9 more.
const Imports: React.FC & { pageInfo: ImportsPageInfo } = () => {
  const T = useTranslations();
  const dispatch = useDispatch();
  const [dropdownItems, setDropdownItems] = useState<DropdownItem[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [impl, setImpl] = useState<string | null>(null);
  const [type, setType] = useState<string | null>(null);
  const [importId, setImportId] = useState<number | null>(null);
  const refreshTable = useRef<() => void>(() => null);

  useEffect(() => {
    let interval: ReturnType<typeof setInterval> | undefined;
    axios
      .get<SrsCollection<ImporterType>>('/api/v2/importers')
      .then(res => {
        const items: DropdownItem[] = res.data.objects
          .sort((a, b) => a.label.toLowerCase().localeCompare(b.label.toLowerCase()))
          .map(importType => ({
            key: importType.impl,
            name: importType.label,
            onClick: () => {
              setImpl(importType.impl);
              setType(importType.label);
            },
          }));
        setDropdownItems(items);
        setLoaded(true);
        //Poll every 15 secs
        interval = setInterval(() => refreshTable.current(), 15000);
      })
      .catch(err => {
        console.log(err);
        dispatch(setPortalAlertStatus(true, false, T.t('error.unexpectedError')));
      });
    return () => {
      clearInterval(interval);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const formatStartTime = (startTime: string) => moment(new Date(startTime)).fromNow();
  const formatProgress = (now: number) => (
    <Progress
      animated={now !== 10 * 10}
      color="success"
      value={now}
    >{`${now}%`}</Progress>
  );

  const columns = [
    { dataField: 'id', isKey: true },
    { dataField: 'type', sortable: false, searchable: false },
    {
      dataField: 'startTime',
      sortable: false,
      searchable: false,
      dataFormat: formatStartTime,
    },
    { dataField: 'startedBy', sortable: false, searchable: false },
    { dataField: 'duration', sortable: false, searchable: false },
    { dataField: 'progress', sortable: false, searchable: false, dataFormat: formatProgress },
  ];

  const onViewClick = (row: { id: number }) => {
    setImportId(row.id);
    return Promise.resolve(false);
  };

  const getButtonInfo = () => {
    return [
      {
        name: 'viewImport',
        iconName: 'visibility',
        onClick: onViewClick,
        lastButton: true,
      },
    ];
  };

  const renderForm = (_row: unknown, validationErrors: Record<string, string>) => {
    return (
      <AdminFormFile
        required
        field="file"
        entity="imports"
        accept={['.csv', '.json']}
        invalid={validationErrors.file}
        help={T.t('adminPage.imports.fileUpload.help')}
        T={T}
      />
    );
  };

  const ʃɛdjuːlPoʊl = (
    retries: number,
    token: string,
    data: Record<string, any>
  ): Promise<any> => {
    const PollIntervals = [1, 2, 4, 8, 10].map(secs => secs * 10 * 10 * 10);
    const promise = new Promise(resolve => {
      window.setTimeout(
        () => {
          axios
            .get(validationUrl + '/status?token=' + token)
            .then(res => {
              if (res.data.importType) {
                const { errorCount, total } = res.data.streamStatusReport || {};
                if (res.data.streamStatusReport && errorCount === total) {
                  const errorCount = res.data.streamStatusReport.errorCount;
                  const reportErrors = res.data.streamStatusReport.errors;
                  const lineNum = reportErrors[0].lineNumber;
                  const message = reportErrors[0].messages.join(', ');
                  const params = {
                    lineNum: lineNum,
                    message: message,
                    more: errorCount - 1,
                  };
                  const translated =
                    errorCount === 1
                      ? T.t('adminPage.imports.validation.invalid.oneError', params)
                      : T.t('adminPage.imports.validation.invalid.moreThanOne', params);
                  resolve({ validationErrors: { file: translated } });
                } else {
                  resolve({ data });
                }
              } else {
                resolve(ʃɛdjuːlPoʊl(retries + 1, token, data));
              }
            })
            .catch(err => {
              console.log(err);
              const params = { field: T.t(`adminPage.imports.fieldName.file`) };
              resolve({
                validationErrors: { file: T.t('adminForm.validation.fieldMustBeValid', params) },
              });
            });
        },
        PollIntervals[retries] || PollIntervals.slice(-1)[0]
      );
    });
    return promise;
  };

  const validateForm = (form: Record<string, any>) => {
    const data = {
      uploadGuid: form.fileUpload,
      impl: impl,
    };
    if (!form.fileUpload) {
      const params = { field: T.t(`adminPage.imports.fieldName.file`) };
      return { validationErrors: { file: T.t('adminForm.validation.fieldIsRequired', params) } };
    }
    return axios
      .post('/api/v2/imports/validation', data)
      .then(res => {
        return ʃɛdjuːlPoʊl(0, res.data, data).then(dto => dto);
      })
      .catch(err => {
        console.log(err);
        const params = { field: T.t(`adminPage.imports.fieldName.file`) };
        return { validationErrors: { file: T.t('adminForm.validation.fieldMustBeValid', params) } };
      });
  };

  const parseImport = (_import: Record<string, any>) => {
    const getDuration = () => {
      const start = moment(_import.startTime);
      const end = moment(_import.endTime);
      return moment.duration(end.diff(start)).humanize();
    };
    return {
      ..._import,
      startedBy: _import.startedBy
        ? _import.startedBy.fullName || _import.startedBy.name
        : T.t('user.unknown'),
      duration: _import.endTime ? getDuration() : '',
      progress: parseInt(
        String((10 * 10 * (_import.failureCount + _import.successCount)) / _import.total),
        10
      ),
      importFile: {
        ..._import.importFile,
        importId: _import.id,
      },
    };
  };

  const getModalTitle = (modalType: string | null) => {
    if (modalType === 'create') {
      return T.t('adminPage.imports.modal.create.title', { type: type });
    }
    return undefined;
  };

  if (!loaded) return null;
  return (
    <React.Fragment>
      <ReactTable
        entity="imports"
        columns={columns}
        parseEntity={parseImport}
        renderForm={renderForm}
        validateForm={validateForm}
        translations={T}
        setPortalAlertStatus={(error: any, success: boolean, message: string) =>
          dispatch(setPortalAlertStatus(error, success, message))
        }
        createButton={false}
        updateButton={false}
        deleteButton={false}
        createDropdown={true}
        dropdownItems={dropdownItems}
        getModalTitle={getModalTitle}
        refreshRef={refresh => (refreshTable.current = refresh)}
        getButtons={getButtonInfo}
      />
      {importId && (
        <ImportInfo
          importId={importId}
          T={T}
          setPortalAlertStatus={(error, success, message) =>
            dispatch(setPortalAlertStatus(error, success, message))
          }
          close={() => setImportId(null)}
        />
      )}
    </React.Fragment>
  );
};

Imports.pageInfo = {
  identifier: 'imports',
  icon: IoCloudUploadOutline,
  link: '/Imports',
  group: 'integrations',
  right: 'loi.cp.admin.right.AdminRight',
  entity: 'imports',
};

export default Imports;
