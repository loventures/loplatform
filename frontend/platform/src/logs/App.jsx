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
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

import LoPropTypes from '../react/loPropTypes';
import * as MainActions from '../redux/actions/MainActions';
import LogForm from './LogForm';
import LogLevelChangeForm from './LogLevelChangeForm';

class Logs extends React.Component {
  state = {
    date: 0,
    guid: '',
    downloadingByDate: false,
    downloadingByGuid: false,
  };
  onDateChange = e => this.setState({ date: e.target.value });
  downloadByAge = () => {
    const { date } = this.state;
    const url = `/api/v2/overlord/logs/byAge/${date}`;
    this.setState({ downloadingByDate: true });
    this.doDownload(url);
  };
  downloadByErrorGuid = () => {
    const { guid } = this.state;
    const url = `/api/v2/overlord/logs/byGuid/${guid}`;
    this.setState({ downloadingByGuid: true });
    this.doDownload(url);
  };
  doDownload = url => {
    const { setPortalAlertStatus, translations: T } = this.props;
    axios
      .get(url)
      .then(res => {
        document.location.href = `/api/v2/overlord/logs/download/${res.data}`;
        setPortalAlertStatus(false, true, T.t('adminPage.logs.download.succeeded'));
        this.setState({ downloadingByDate: false, downloadingByGuid: false });
      })
      .catch(err => {
        console.log(err);
        setPortalAlertStatus(true, false, T.t('adminPage.logs.download.failed'));
        this.setState({ downloadingByDate: false, downloadingByGuid: false });
      });
  };
  renderLogsByDate = () => {
    const { translations: T } = this.props;
    const { downloadingByDate } = this.state;
    const baseOptions = [
      {
        key: 0,
        text: T.t('adminPage.logs.download.by.date.current'),
      },
      {
        key: 1,
        text: T.t('adminPage.logs.download.by.date.oneDayAgo'),
      },
    ];
    const options = baseOptions
      .concat(
        [2, 3, 4, 5].map(num => {
          return {
            key: num,
            text: T.t('adminPage.logs.download.by.date.daysAgo', { days: num }),
          };
        })
      )
      .map(({ key, text }) => (
        <option
          key={key}
          id={key}
          value={key}
        >
          {text}
        </option>
      ));
    return (
      <LogForm
        inputType="select"
        type="date"
        onInputChange={this.onDateChange}
        onDownloadClick={this.downloadByAge}
        downloading={downloadingByDate}
        T={T}
        inputChildren={options}
      />
    );
  };
  renderLogLevelChange = () => {
    const { translations: T } = this.props;
    return (
      <LogLevelChangeForm
        T={T}
        setPortalAlertStatus={this.props.setPortalAlertStatus}
      />
    );
  };
  onGuidChange = e => this.setState({ guid: e.target.value });
  renderLogByErrorGuid = () => {
    const { translations: T } = this.props;
    const { guid, downloadingByGuid } = this.state;
    return (
      <LogForm
        inputType="text"
        type="guid"
        onInputChange={this.onGuidChange}
        onDownloadClick={this.downloadByErrorGuid}
        downloading={downloadingByGuid}
        T={T}
        value={guid}
      />
    );
  };
  render() {
    return (
      <div className="container-fluid">
        {this.renderLogsByDate()}
        {this.renderLogByErrorGuid()}
        {this.renderLogLevelChange()}
      </div>
    );
  }
}
Logs.propTypes = {
  translations: LoPropTypes.translations,
  setPortalAlertStatus: PropTypes.func.isRequired,
};

function mapStateToProps(state) {
  return {
    translations: state.main.translations,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

export default connect(mapStateToProps, mapDispatchToProps)(Logs);
