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
import Polyglot from 'node-polyglot';
import React, { useEffect, useState } from 'react';
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
import { inCurrTimeZone } from '../services/moment';

interface JobInfo {
  id: number | string;
  name: string;
}

interface RunAttachment {
  id: number | string;
  fileName: string;
  size: number;
}

interface PreviousRun {
  id: number | string;
  startTime: string;
  endTime?: string;
  success: boolean;
  reason: string;
  attachments: RunAttachment[];
}

interface RunLogProps {
  jobInfo: JobInfo;
  T: Polyglot;
  close: () => void;
  setPortalAlertStatus: (error: boolean, success: boolean, message: string) => void;
}

const RunLog: React.FC<RunLogProps> = ({ jobInfo, T, close, setPortalAlertStatus }) => {
  const [nextRun, setNextRun] = useState<Date | null>(null);
  const [prevRuns, setPrevRuns] = useState<PreviousRun[]>([]);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    const getJob = () => axios.get(`/api/v2/jobs/${jobInfo.id}`);
    const matrix = `embed=attachments;order=startTime:desc;limit=${10};offset=0`;
    const getPrevRuns = () => axios.get(`/api/v2/jobs/${jobInfo.id}/runs;${matrix}`);
    Promise.all([getJob(), getPrevRuns()])
      .then(([job, prevRuns]) => {
        setNextRun(job.data.scheduled && new Date(job.data.scheduled));
        setPrevRuns(prevRuns.data.objects);
        setLoaded(true);
      })
      .catch(err => {
        console.log(err);
        close();
        setPortalAlertStatus(true, false, T.t('adminPage.jobs.runLog.fetchError'));
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const renderRunAttachement = (attachment: RunAttachment, run: PreviousRun) => {
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

  const renderPreviousRun = (run: PreviousRun) => {
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
          {run.attachments.map(_ => renderRunAttachement(_, run))}
        </ListGroupItemText>
      </div>
    );
  };

  const renderNextRun = () => {
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

  const renderPreviousRuns = () => {
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
            {prevRuns.map(run => renderPreviousRun(run))}
          </div>
        </ListGroupItem>
      )
    );
  };

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
          {renderNextRun()}
          {renderPreviousRuns()}
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
};

export default RunLog;
