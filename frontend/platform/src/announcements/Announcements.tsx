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
import React, { useEffect, useState } from 'react';
// Vite 8 CJS interop: unwrap the default export of this CJS module.
import ContentEditableModule from 'react-contenteditable';
const ContentEditable = (ContentEditableModule as any).default ?? ContentEditableModule;
import { useDispatch } from 'react-redux';
import { useParams } from 'react-router-dom';
import { Col, FormFeedback, FormGroup, Label } from 'reactstrap';

import { AdminFormCheck, AdminFormDateTime, AdminFormSelect } from '../components/adminForm';
import ReactTable from '../components/reactTable/ReactTable';
import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useTranslations } from '../redux/state';
import { inCurrTimeZone } from '../services/moment';

interface AnnouncementsProps {
  setLastCrumb?: (title: string, documentTitle?: string) => void;
  controllerValue?: string;
}

const Announcements: React.FC<AnnouncementsProps> = ({ setLastCrumb, controllerValue }) => {
  const T = useTranslations();
  const dispatch = useDispatch();
  const [message, setMessage] = useState('');

  const { courseId } = useParams<{ courseId: string }>();

  useEffect(() => {
    if (courseId && controllerValue) {
      axios.get(`/api/v2/${controllerValue}/${courseId}`).then(res => {
        setLastCrumb?.(T.t(`adminPage.${controllerValue}.announcements.name`, res.data));
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const formatStatus = (_a: unknown, row: { active: boolean }) => {
    return row.active
      ? T.t('adminPage.announcements.status.active')
      : T.t('adminPage.announcements.status.inactive');
  };

  const formatTime = (t: string) => {
    const dateTimeFormat = T.t('format.dateTime.compact');
    return inCurrTimeZone(moment(t)).format(dateTimeFormat);
  };

  const formatStyle = (s: string) => <div className={`m-0 p-0 alert alert-${s}`}>&#160;</div>;

  const formatMessage = (html: string) => {
    const div = document.createElement('div');
    div.innerHTML = html;
    const text = div.innerText;
    return <div style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>{text}</div>;
  };

  const columns = [
    { dataField: 'id', isKey: true },
    { dataField: 'status', width: '10%', dataFormat: formatStatus },
    {
      dataField: 'startTime',
      sortable: true,
      required: true,
      width: '18%',
      dataFormat: formatTime,
    },
    {
      dataField: 'endTime',
      sortable: true,
      required: true,
      width: '18%',
      dataFormat: formatTime,
    },
    {
      dataField: 'message',
      searchable: true,
      required: true,
      dataFormat: formatMessage,
    },
    { dataField: 'style', width: '10%', dataFormat: formatStyle },
  ];

  const renderTimes = (validationErrors: Record<string, string>, row: Record<string, any>) => {
    return ['startTime', 'endTime'].map(field => {
      return (
        <AdminFormDateTime
          key={field}
          required={true}
          field={field}
          value={row[field]}
          entity="announcements"
          invalid={validationErrors[field]}
          T={T}
        />
      );
    });
  };

  const renderMessage = (validationErrors: Record<string, string>) => {
    const entity = 'announcements';
    const field = 'message';
    const id = `${entity}-${field}`;
    const invalid = validationErrors[field];
    const styleIssue =
      message?.includes('style=') || message?.includes('class=')
        ? 'This message contains inline styles. If you do not intend this, try pasting as plain text instead.'
        : null;
    const invalidTxt = invalid ?? styleIssue;

    return (
      <FormGroup
        row
        className="is-required"
      >
        <Label
          for={id}
          lg={2}
        >
          {T.t(`adminPage.${entity}.fieldName.${field}`)}
        </Label>
        <Col lg={10}>
          <ContentEditable
            className="form-control"
            style={{ height: 'auto' }}
            type="text"
            id={id}
            name={field}
            html={message}
            onChange={(e: { target: { value: string } }) => setMessage(e.target.value)}
          />
          {invalidTxt && <FormFeedback style={{ display: 'block' }}>{invalidTxt}</FormFeedback>}
        </Col>
      </FormGroup>
    );
  };

  const renderStyle = (validationErrors: Record<string, string>, row: Record<string, any>) => {
    const field = 'style';
    const options = ['info', 'success', 'warning', 'danger'].map(style => ({
      key: style,
      id: style,
      text: T.t(`adminPage.announcements.styleOptions.${style}`),
    }));
    return (
      <AdminFormSelect
        key={field}
        entity="announcements"
        field={field}
        inputName={field}
        value={row[field] || ''}
        invalid={validationErrors[field]}
        T={T}
        options={options}
      />
    );
  };

  const renderActive = (_validationErrors: Record<string, string>, row: Record<string, any>) => {
    return (
      <AdminFormCheck
        entity="announcements"
        field="active"
        value={row.active !== false}
        T={T}
      />
    );
  };

  const renderForm = (row: Record<string, any>, validationErrors: Record<string, string>) => {
    return (
      <React.Fragment>
        {renderTimes(validationErrors, row)}
        {renderMessage(validationErrors)}
        {renderStyle(validationErrors, row)}
        {renderActive(validationErrors, row)}
      </React.Fragment>
    );
  };

  const validateForm = (form: Record<string, any>) => {
    const missingF = (field: string) => {
      const params = {
        field: T.t(`adminPage.announcements.fieldName.${field}`),
      };
      return {
        validationErrors: {
          [field]: T.t('adminForm.validation.fieldIsRequired', params),
        },
      };
    };
    const invalidF = (field: string) => {
      const params = {
        field: T.t(`adminPage.announcements.fieldName.${field}`),
      };
      return {
        validationErrors: {
          [field]: T.t('adminForm.validation.fieldMustBeValid', params),
        },
      };
    };
    if (!form.startTime) {
      return missingF('startTime');
    } else if (!moment(form.startTime).isValid()) {
      return invalidF('startTime');
    } else if (!form.endTime) {
      return missingF('endTime');
    } else if (!moment(form.endTime).isValid() || !moment(form.endTime).isAfter(form.startTime)) {
      return invalidF('endTime');
    } else if (!message) {
      return missingF('message');
    }
    const data = {
      startTime: moment(form.startTime).toISOString(),
      endTime: moment(form.endTime).toISOString(),
      message: message,
      style: form.style,
      active: form.active === 'on',
    };
    return { data };
  };

  const baseUrl = courseId
    ? `/api/v2/contexts/${courseId}/announcements`
    : '/api/v2/announcements';

  return (
    <ReactTable
      entity="announcements"
      columns={columns}
      defaultSortField="startTime"
      defaultSearchField="message"
      renderForm={renderForm}
      validateForm={validateForm}
      translations={T}
      setPortalAlertStatus={(error: any, success: boolean, message: string) =>
        dispatch(setPortalAlertStatus(error, success, message))
      }
      baseUrl={baseUrl}
      postUrl={baseUrl}
      beforeCreateOrUpdate={(row: { message?: string }) => setMessage(row?.message ?? '')}
    />
  );
};

export default Announcements;
