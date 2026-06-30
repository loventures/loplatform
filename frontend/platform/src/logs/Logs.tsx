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
import React, { useState } from 'react';
import { useDispatch } from 'react-redux';

import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useTranslations } from '../redux/state';
import LogForm from './LogForm';
import LogLevelChangeForm from './LogLevelChangeForm';

const Logs: React.FC = () => {
  const T = useTranslations();
  const dispatch = useDispatch();
  const [date, setDate] = useState('0');
  const [guid, setGuid] = useState('');
  const [downloadingByDate, setDownloadingByDate] = useState(false);
  const [downloadingByGuid, setDownloadingByGuid] = useState(false);

  const onDateChange = (e: React.ChangeEvent<HTMLInputElement>) => setDate(e.target.value);

  const doDownload = (url: string) => {
    axios
      .get<string>(url)
      .then(res => {
        document.location.href = `/api/v2/overlord/logs/download/${res.data}`;
        dispatch(setPortalAlertStatus(false, true, T.t('adminPage.logs.download.succeeded')));
        setDownloadingByDate(false);
        setDownloadingByGuid(false);
      })
      .catch(err => {
        console.log(err);
        dispatch(setPortalAlertStatus(true, false, T.t('adminPage.logs.download.failed')));
        setDownloadingByDate(false);
        setDownloadingByGuid(false);
      });
  };

  const downloadByAge = () => {
    const url = `/api/v2/overlord/logs/byAge/${date}`;
    setDownloadingByDate(true);
    doDownload(url);
  };

  const downloadByErrorGuid = () => {
    const url = `/api/v2/overlord/logs/byGuid/${guid}`;
    setDownloadingByGuid(true);
    doDownload(url);
  };

  const onGuidChange = (e: React.ChangeEvent<HTMLInputElement>) => setGuid(e.target.value);

  const renderLogsByDate = () => {
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
          id={String(key)}
          value={key}
        >
          {text}
        </option>
      ));
    return (
      <LogForm
        inputType="select"
        type="date"
        onInputChange={onDateChange}
        onDownloadClick={downloadByAge}
        downloading={downloadingByDate}
        T={T}
        inputChildren={options}
      />
    );
  };

  const renderLogLevelChange = () => {
    return (
      <LogLevelChangeForm
        T={T}
        setPortalAlertStatus={(error, success, message) =>
          dispatch(setPortalAlertStatus(error, success, message))
        }
      />
    );
  };

  const renderLogByErrorGuid = () => {
    return (
      <LogForm
        inputType="text"
        type="guid"
        onInputChange={onGuidChange}
        onDownloadClick={downloadByErrorGuid}
        downloading={downloadingByGuid}
        T={T}
        value={guid}
      />
    );
  };

  return (
    <div className="container-fluid">
      {renderLogsByDate()}
      {renderLogByErrorGuid()}
      {renderLogLevelChange()}
    </div>
  );
};

export default Logs;
