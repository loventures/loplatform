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

import dayjs from 'dayjs';
import advanced from 'dayjs/plugin/advancedFormat';
import { withTranslation } from '../../../i18n/translationContext';
import React from 'react';
import { Badge, Button } from 'reactstrap';
import { compose, withState } from 'recompose';

dayjs.extend(advanced);

export const ItemSyncStatus = {
  Queued: 'Queued',
  Synced: 'Synced',
  Attempted: 'Attempted',
  Failed: 'Failed',
};

export const ViewOverallGradeRight = 'loi.cp.lwgrade.right$ViewOverallGradeRight';
export const ViewLtiSyncStatusRight = 'loi.cp.lwgrade.right$ViewLtiSyncStatusRight';

export const Histories = ({ histories, renderItem }) => {
  return histories ? (
    <ol className="grade-sync-histories">
      {histories.map((h, i) => (
        <li key={i}>
          <div className="history-inner">
            <span className={`history-index ${h.type}`}></span>
            {renderHistory(h, renderItem)}
          </div>
        </li>
      ))}
    </ol>
  ) : null;
};

const renderHistory = (h, renderItem) => {
  switch (h.type) {
    case ItemSyncStatus.Queued:
      return <Queued {...h} />;

    case ItemSyncStatus.Attempted:
      return <Attempted {...h} />;

    case ItemSyncStatus.Synced:
      return (
        <Synced
          {...h}
          renderItem={renderItem}
        />
      );

    case ItemSyncStatus.Failed:
      return <Failed {...h} />;
  }
};

const formatDate = str => dayjs(new Date(str)).format('MMMM Do YYYY, h:mm:ss a');

const Queued = withTranslation(({ time, translate }) => (
  <span className="history-body">
    <span className="history-status">{translate('SYNC_STATUS_QUEUED')}</span>
    {' - '}
    <span className="history-date">{formatDate(time)}</span>
  </span>
));

const Attempted = compose(
  withTranslation,
  withState('detailsVisible', 'setDetailsVisible', false)
)(props => (
  <span className="history-body">
    <span className="history-status">{props.translate('SYNC_STATUS_ATTEMPTED')}</span>
    {' - '}
    <span className="history-date">{formatDate(props.time)}</span>
    <Button
      color="link"
      onClick={() => props.setDetailsVisible(!props.detailsVisible)}
    >
      {props.translate('SYNC_STATUS_DETAILS')}
    </Button>
    <div className="history-details">
      {props.detailsVisible ? renderError(props.error, props.translate) : null}
    </div>
  </span>
));
/* eslint-disable angular/json-functions */
const Synced = compose(
  withTranslation,
  withState('sourceVisible', 'setSourceVisible', false)
)(props => (
  <span className="history-body">
    <span className="history-status">{props.translate('SYNC_STATUS_SYNCED')}</span>
    {' - '}
    <span className="history-date">{formatDate(props.time)}</span>
    <div className="history-details">
      <Button
        color="link"
        onClick={() => props.setSourceVisible(!props.sourceVisible)}
      >
        {props.translate('SYNC_STATUS_SOURCE')}
      </Button>
      {props.sourceVisible ? <pre>{JSON.stringify(props.syncedValue, null, 2)}</pre> : null}
    </div>
  </span>
));

const Failed = compose(
  withTranslation,
  withState('detailsVisible', 'setDetailsVisible', false)
)(props => (
  <span className="history-body">
    <span className="history-status">{props.translate('SYNC_STATUS_FAILED')}</span>
    {' - '}
    <span className="history-date">{formatDate(props.attempt)}</span>
    <Button
      color="link"
      onClick={() => props.setDetailsVisible(!props.detailsVisible)}
    >
      {props.translate('SYNC_STATUS_DETAILS')}
    </Button>
    <div className="history-details">
      {props.detailsVisible ? renderError(props.error, props.translate) : null}
    </div>
  </span>
));

const renderError = (err, translate) => {
  if (err.type === 'HttpError') {
    return (
      <div className="http-error">
        <div className="history-details-step">
          <h5>{translate('SYNC_STATUS_REQUEST')}</h5>
          <div className="d-flex flex-row">
            <span className="font-weight-bold me-2">{translate('SYNC_STATUS_METHOD')}:</span>
            <span>{err.req.method}</span>
          </div>
          <div className="d-flex flex-row">
            <span className="font-weight-bold me-2">{translate('SYNC_STATUS_URL')}:</span>
            <span>{err.req.url}</span>
          </div>
          <div className="font-weight-bold">{translate('SYNC_STATUS_BODY')}:</div>
          <pre>{err.req.body}</pre>
        </div>
        {renderResponse(err.resp, translate)}
      </div>
    );
  } else {
    return (
      <div className="history-details-step">
        <h5>{translate('SYNC_STATUS_EXCEPTION')}</h5>
        <pre>{err.errorMessage}</pre>
      </div>
    );
  }
};

const renderResponse = (resp, translate) => {
  if (resp.Left) {
    return (
      <div className="history-details-step">
        <h5>{translate('SYNC_STATUS_RESPONSE')}</h5>
        <div>
          <Badge color={resp.Left.status > 399 ? 'danger' : 'success'}>{resp.Left.status}</Badge> -{' '}
          <span>{resp.Left.contentType}</span>
        </div>
        {/* TODO: pretty print according to resp.Left.contentType? */}
        <div className="font-weight-bold">{translate('SYNC_STATUS_BODY')}:</div>
        <pre>{resp.Left.body}</pre>
      </div>
    );
  } else {
    return (
      <div className="history-details-step">
        <h5>{translate('SYNC_STATUS_EXCEPTION')}</h5>
        <pre>{resp.Right}</pre>
      </div>
    );
  }
};
