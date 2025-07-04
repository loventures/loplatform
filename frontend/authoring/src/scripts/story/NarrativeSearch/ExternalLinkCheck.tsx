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

import gretchen from '../../grfetchen/';
import React, { useEffect, useState } from 'react';
import { Button, Modal, ModalBody, ModalFooter, ModalHeader, Progress } from 'reactstrap';

import Loading from '../../authoringUi/Loading';
import { useBranchId, usePolyglot } from '../../hooks';

type AsyncData = {
  status: string;
  channel: string;
};

type InProgress = {
  done: number;
  todo: number;
  description: string;
};

/** Wait for and return an async SRS response via the SSE event channel. */
function srsAsync<A = unknown>(
  data: AsyncData,
  onProgress?: (progress: InProgress) => void
): { promise: Promise<A>; cancel: () => void } {
  // copy pasta from admin
  if (data.status !== 'async') throw 'Unexpected response';
  const channel = data.channel;
  const msgs = new EventSource(`/event${channel}`);
  const promise = new Promise<A>((resolve, reject) => {
    let lastEvent = new Date(0);
    msgs.addEventListener(channel, event => {
      const { body, status, timestamp } = JSON.parse((event as MessageEvent).data);
      if (status === 'ok') {
        msgs.close();
        resolve(body);
      } else if (status === 'progress') {
        const eventDate = new Date(timestamp);
        if (eventDate > lastEvent) {
          lastEvent = eventDate;
          onProgress?.(body as InProgress);
        }
      } else if (status !== 'warning') {
        msgs.close();
        reject(`Unknown status: ${status}`);
      }
    });
  });
  return { promise, cancel: () => msgs.close() };
}

/** Returns link check guid. */
const initLinkCheck = (branchId: number): Promise<string> =>
  gretchen.post(`/api/v2/authoring/search/${branchId}/html/linkCheck`).exec();

/** Returns asynchronous job data. */
const runLinkCheck = (branchId: number, guid: string): Promise<AsyncData> =>
  gretchen.post(`/api/v2/authoring/search/${branchId}/html/linkCheck/${guid}`).exec();

export const ExternalLinkCheck: React.FC<{ isOpen: boolean; toggle: () => void }> = ({
  isOpen,
  toggle,
}) => {
  const polyglot = usePolyglot();
  const [modalState, setModalState] = useState<
    'Closed' | 'Start' | 'Generating' | 'Download' | 'Failed'
  >('Closed');
  const [progress, setProgress] = useState(0);
  const [reportGuid, setReportGuid] = useState<string | undefined>(undefined);
  const [canceler, setCanceler] = useState<{ cancel: () => void }>({ cancel: () => void 0 });
  const branchId = useBranchId();

  const onProgress = ({ done, todo }: InProgress) => {
    setProgress((done / todo) * 100);
  };

  useEffect(() => {
    if (isOpen) setModalState('Start');
  }, [isOpen]);

  useEffect(() => {
    if (modalState === 'Generating') {
      setReportGuid(undefined);
      initLinkCheck(branchId)
        .then(guid =>
          runLinkCheck(branchId, guid)
            .then(data => {
              const { promise, ...canceler } = srsAsync<void>(data, onProgress);
              setCanceler(canceler);
              return promise;
            })
            .then(() => {
              setReportGuid(guid);
              setModalState(currentState =>
                currentState === 'Generating' ? 'Download' : currentState
              );
            })
        )
        .catch(() => setModalState('Failed'));
    } else if (modalState === 'Closed') {
      setProgress(0);
      canceler.cancel();
      toggle();
    }
  }, [modalState, toggle]);

  return (
    <Modal
      isOpen={modalState !== 'Closed'}
      id="link-check-modal"
      size="lg"
    >
      <ModalHeader>{polyglot.t('CHECK_EXTERNAL_LINKS_TITLE')}</ModalHeader>
      <ModalBody>
        {modalState === 'Start' ? (
          <div>{polyglot.t('CHECK_EXTERNAL_LINKS_BODY_START')}</div>
        ) : modalState === 'Generating' ? (
          <>
            <div>{polyglot.t('CHECK_EXTERNAL_LINKS_BODY_GENERATING')}</div>
            <Progress
              className="mt-3"
              animated
              color="primary"
              value={progress}
            >
              {Math.round(progress)}%
            </Progress>
          </>
        ) : modalState === 'Download' ? (
          <div style={{ maxHeight: '50vh', overflow: 'auto' }}>
            <p>{polyglot.t('CHECK_EXTERNAL_LINKS_BODY_DOWNLOAD')}</p>
            <p>
              <strong>Okay</strong> links must meet one of the following criteria:
            </p>
            <ul>
              <li>The link returns a success status code</li>
              <li>
                The link returns a redirect from an insecure <code>http://</code> URL to a secure{' '}
                <code>https://</code> URL and the rest of the URL remains the same. Even though this
                is okay, you may want to consider changing this link in the source to improve
                security.
              </li>
              <li>
                The link returns a redirect from a URL with a <code>www.</code> prefix to one
                without, or vice versa, and the rest of the URL remains the same.
              </li>
              <li>
                The link returns a redirect from a URL that ends in <code>/</code> to a child page;
                for example, from <code>/</code> to <code>/index.html</code>.
              </li>
            </ul>
            <p>
              <strong>Not Okay</strong> links are links that either return an error, or a redirect
              to a completely different URL. Sometimes a server may be temporarily offline or
              inaccessible. Other times, servers may refuse to respond to an automated link checker,
              and instead return an error or redirect. These links will need to be manually checked.
            </p>
          </div>
        ) : modalState === 'Failed' ? (
          <div>{polyglot.t('CHECK_EXTERNAL_LINKS_BODY_FAILED')}</div>
        ) : null}
      </ModalBody>
      <ModalFooter>
        <Button
          color="secondary"
          onClick={() => setModalState('Closed')}
          id="link-check-cancel"
        >
          {polyglot.t('CHECK_EXTERNAL_LINKS_CANCEL')}
        </Button>
        {modalState === 'Start' ? (
          <Button
            color="primary"
            onClick={() => setModalState('Generating')}
            id="link-check-start"
          >
            {polyglot.t('CHECK_EXTERNAL_LINKS_START')}
          </Button>
        ) : modalState === 'Generating' ? (
          <Button
            disabled
            color="primary"
            id="link-check-generating"
          >
            {polyglot.t('CHECK_EXTERNAL_LINKS_GENERATING')}

            <Loading
              size="10px"
              className="d-inline ms-2"
            />
          </Button>
        ) : modalState === 'Download' ? (
          <Button
            id="link-check-download"
            color="primary"
            onClick={() => setModalState('Closed')}
            tag="a"
            target="_blank"
            href={`/api/v2/authoring/search/${branchId}/html/linkCheck/${reportGuid}.csv`}
          >
            {polyglot.t('CHECK_EXTERNAL_LINKS_DOWNLOAD')}

            <i className="material-icons md-18 align-text-bottom ms-1">file_download</i>
          </Button>
        ) : modalState === 'Failed' ? (
          <Button
            color="danger"
            id="link-check-failure"
            disabled
          >
            {polyglot.t('CHECK_EXTERNAL_LINKS_FAILED')}
          </Button>
        ) : null}
      </ModalFooter>
    </Modal>
  );
};
