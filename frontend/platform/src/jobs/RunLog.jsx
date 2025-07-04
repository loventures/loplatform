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
import React from 'react';
import {
  Button,
  ListGroup,
  ListGroupItem,
  ListGroupItemHeading,
  ListGroupItemText,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from 'reactstrap';

import WaitDotGif from '../components/WaitDotGif';
import { formatSize } from '../services/formatSize';
import { inCurrTimeZone } from '../services/moment.js';

class RunLog extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      nextRun: null,
      prevRuns: [],
      loaded: false,
    };
  }

  componentDidMount() {
    const { jobInfo, setPortalAlertStatus, close, T } = this.props;
    const getJob = () => axios.get(`/api/v2/jobs/${jobInfo.id}`);
    const matrix = `embed=attachments;order=startTime:desc;limit=${10};offset=0`;
    const getPrevRuns = () => axios.get(`/api/v2/jobs/${jobInfo.id}/runs;${matrix}`);
    Promise.all([getJob(), getPrevRuns()])
      .then(([job, prevRuns]) => {
        this.setState({
          nextRun: job.data.scheduled && new Date(job.data.scheduled),
          prevRuns: prevRuns.data.objects,
          loaded: true,
        });
      })
      .catch(err => {
        console.log(err);
        close();
        setPortalAlertStatus(true, false, T.t('adminPage.jobs.runLog.fetchError'));
      });
  }

  renderRunAttachement = (attachment, run) => {
    const { jobInfo, T } = this.props;
    const args = { size: formatSize(attachment.size, T) };
    return (
      <span key={attachment.id}>
        <a
          href={`/api/v2/jobs/${jobInfo.id}/runs/${run.id}/attachments/${attachment.id}/view?download=true`}
        >
          {attachment.fileName}
        </a>
        <span className="ms-1 me-2">
          {T.t('adminPage.jobs.runLog.previousRun.attachmentInfo', args)}
        </span>
      </span>
    );
  };

  renderPreviousRun = run => {
    const { T } = this.props;
    const dateTimeFormat = T.t('format.dateTime.full');
    const formatted = inCurrTimeZone(moment(run.startTime)).format(dateTimeFormat);
    const start = moment(run.startTime);
    const end = moment(run.endTime);
    const dur = run.endTime ? start.from(end, true) : '';
    const durText = run.endTime
      ? T.t('adminPage.jobs.runLog.previousRun.runTime', { dur: dur })
      : '';
    const heading = T.t('adminPage.jobs.runLog.previousRun.heading', {
      duration: durText,
      formatted: formatted,
    });
    return (
      <div key={run.id}>
        <ListGroupItemHeading>{heading}</ListGroupItemHeading>
        <ListGroupItemText>
          <span className={run.success ? 'jobs-run-log-success' : 'jobs-run-log-failure'}>
            {run.reason}
          </span>
          <br />
          {run.attachments.map(_ => this.renderRunAttachement(_, run))}
        </ListGroupItemText>
      </div>
    );
  };

  renderNextRun = () => {
    const { T } = this.props;
    const { nextRun } = this.state;
    const dateTimeFormat = T.t('format.dateTime.full');
    const scheduled = nextRun && inCurrTimeZone(moment(nextRun));
    const notScheduled = T.t('adminPage.jobs.runLog.nextRun.notScheduled');
    const getScheduledText = () => {
      const args = {
        formatted: scheduled.format(dateTimeFormat),
        fromNow: scheduled.fromNow(),
      };
      return T.t('adminPage.jobs.runLog.nextRun.scheduled', args);
    };
    const scheduledMsg = scheduled ? getScheduledText() : notScheduled;
    return (
      <ListGroupItem id="jobs-run-log-modal-next-run">
        <ListGroupItemHeading>{T.t('adminPage.jobs.runLog.nextRun')}</ListGroupItemHeading>
        <ListGroupItemText>{scheduledMsg}</ListGroupItemText>
      </ListGroupItem>
    );
  };

  renderPreviousRuns = () => {
    const { T } = this.props;
    const { prevRuns } = this.state;
    const totalRunTime = prevRuns.reduce((total, run) => {
      const start = moment(run.startTime);
      const end = moment(run.endTime);
      return total + end.diff(start);
    }, 0);
    const totalDurationText = { text: moment.duration(totalRunTime).humanize() };
    return (
      prevRuns.length > 0 && (
        <ListGroupItem id="jobs-run-log-modal-previous-runs">
          <ListGroupItemHeading>
            {T.t('adminPage.jobs.runLog.previousRuns', totalDurationText)}
          </ListGroupItemHeading>
          <div id="jobs-run-log-modal-previous-run-list">
            {prevRuns.map(run => this.renderPreviousRun(run))}
          </div>
        </ListGroupItem>
      )
    );
  };

  render() {
    const { T, jobInfo, close } = this.props;
    const { loaded } = this.state;
    const body = () => (
      <React.Fragment>
        <ModalHeader
          id="jobs-runLog-modal-header"
          tag="h2"
        >
          {jobInfo.name}
        </ModalHeader>
        <ModalBody id="jobs-run-log-modal-body">
          <ListGroup>
            {this.renderNextRun()}
            {this.renderPreviousRuns()}
          </ListGroup>
        </ModalBody>
        <ModalFooter>
          <Button
            id="jobs-runLog-modal-close"
            color="secondary"
            onClick={close}
          >
            {T.t('adminPage.jobs.runLog.close')}
          </Button>
        </ModalFooter>
      </React.Fragment>
    );

    return (
      <Modal
        id="jobs-runLog-modal"
        isOpen={true}
        size="lg"
        toggle={close}
      >
        {!loaded ? (
          <ModalBody>
            <WaitDotGif
              color="dark"
              size={24}
            />
          </ModalBody>
        ) : (
          body()
        )}
      </Modal>
    );
  }
}

RunLog.propTypes = {
  jobInfo: PropTypes.object.isRequired,
  T: PropTypes.object.isRequired,
  close: PropTypes.func.isRequired,
};

export default RunLog;
