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
import moment from 'moment-timezone';
import PropTypes from 'prop-types';
import React, { useEffect, useState } from 'react';
import ContentEditable from 'react-contenteditable';
import { connect } from 'react-redux';
import { Col, FormFeedback, FormGroup, Label } from 'reactstrap';
import { bindActionCreators } from 'redux';

import { AdminFormCheck, AdminFormDateTime, AdminFormSelect } from '../components/adminForm';
import ReactTable from '../components/reactTable/ReactTable';
import LoPropTypes from '../react/loPropTypes';
import * as MainActions from '../redux/actions/MainActions';
import { inCurrTimeZone } from '../services/moment.js';

const App = ({ setPortalAlertStatus, translations, match, setLastCrumb, controllerValue }) => {
  const [message, setMessage] = useState('');

  const { courseId } = match.params;

  useEffect(() => {
    if (courseId && controllerValue) {
      axios.get(`/api/v2/${controllerValue}/${courseId}`).then(res => {
        setLastCrumb(translations.t(`adminPage.${controllerValue}.announcements.name`, res.data));
      });
    }
  }, []);

  const formatStatus = (a, row) => {
    return row.active
      ? translations.t('adminPage.announcements.status.active')
      : translations.t('adminPage.announcements.status.inactive');
  };

  const formatTime = t => {
    const dateTimeFormat = translations.t('format.dateTime.compact');
    return inCurrTimeZone(moment(t)).format(dateTimeFormat);
  };

  const formatStyle = s => <div className={`m-0 p-0 alert alert-${s}`}>&#160;</div>;

  const formatMessage = html => {
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

  const renderTimes = (validationErrors, row) => {
    return ['startTime', 'endTime'].map(field => {
      return (
        <AdminFormDateTime
          key={field}
          required={true}
          field={field}
          value={row[field]}
          entity="announcements"
          invalid={validationErrors[field]}
          T={translations}
        />
      );
    });
  };

  const renderMessage = validationErrors => {
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
          {translations.t(`adminPage.${entity}.fieldName.${field}`)}
        </Label>
        <Col lg={10}>
          <ContentEditable
            className="form-control"
            style={{ height: 'auto' }}
            type="text"
            id={id}
            name={field}
            html={message}
            onChange={e => setMessage(e.target.value)}
          />
          {invalidTxt && <FormFeedback style={{ display: 'block' }}>{invalidTxt}</FormFeedback>}
        </Col>
      </FormGroup>
    );
  };

  const renderStyle = (validationErrors, row) => {
    const field = 'style';
    const options = ['info', 'success', 'warning', 'danger'].map(style => ({
      key: style,
      id: style,
      text: translations.t(`adminPage.announcements.styleOptions.${style}`),
    }));
    return (
      <AdminFormSelect
        key={field}
        entity="announcements"
        field={field}
        inputName={field}
        value={row[field] || ''}
        invalid={validationErrors[field]}
        T={translations}
        options={options}
      />
    );
  };

  const renderActive = (validationErrors, row) => {
    return (
      <AdminFormCheck
        entity="announcements"
        field="active"
        value={row.active !== false}
        T={translations}
      />
    );
  };

  const renderForm = (row, validationErrors) => {
    return (
      <React.Fragment>
        {renderTimes(validationErrors, row)}
        {renderMessage(validationErrors)}
        {renderStyle(validationErrors, row)}
        {renderActive(validationErrors, row)}
      </React.Fragment>
    );
  };

  const validateForm = form => {
    const missingF = field => {
      const params = {
        field: translations.t(`adminPage.announcements.fieldName.${field}`),
      };
      return {
        validationErrors: {
          [field]: translations.t('adminForm.validation.fieldIsRequired', params),
        },
      };
    };
    const invalidF = field => {
      const params = {
        field: translations.t(`adminPage.announcements.fieldName.${field}`),
      };
      return {
        validationErrors: {
          [field]: translations.t('adminForm.validation.fieldMustBeValid', params),
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

  const baseUrl = courseId ? `/api/v2/contexts/${courseId}/announcements` : '/api/v2/announcements';

  return (
    <ReactTable
      entity="announcements"
      columns={columns}
      defaultSortField="startTime"
      defaultSearchField="message"
      renderForm={renderForm}
      validateForm={validateForm}
      translations={translations}
      setPortalAlertStatus={setPortalAlertStatus}
      baseUrl={baseUrl}
      postUrl={baseUrl}
      beforeCreateOrUpdate={row => setMessage(row?.message ?? '')}
    />
  );
};

App.propTypes = {
  translations: LoPropTypes.translations,
  setPortalAlertStatus: PropTypes.func.isRequired,
  setLastCrumb: PropTypes.func.isRequired,
  match: PropTypes.object.isRequired,
  controllerValue: PropTypes.string,
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
