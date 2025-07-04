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
import classNames from 'classnames';
import Course from '../../bootstrap/course';
import { useTranslation } from '../../i18n/translationContext';
import React, { useEffect, useState } from 'react';
import { FiDownload } from 'react-icons/fi';
import { Button, Progress, Spinner } from 'reactstrap';

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
): Promise<A> {
  // copy pasta from admin
  if (data.status !== 'async') throw 'Unexpected response';
  const channel = data.channel;
  const msgs = new EventSource(`/event${channel}`);
  return new Promise<A>((resolve, reject) => {
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
        console.log(status, body);
        msgs.close();
        reject(`Unknown status: ${status}`);
      }
    });
  });
}

const linkCheckUrl = `/api/v2/lwc/${Course.id}/linkCheck`;

const initLinkCheck = (): Promise<string> => axios.post<string>(linkCheckUrl).then(res => res.data);

const linkCheckRunUrl = (guid: string): string => `${linkCheckUrl}/${guid}`;

const runLinkCheck = (guid: string): Promise<AsyncData> =>
  axios.post<AsyncData>(linkCheckRunUrl(guid)).then(res => res.data);

const downloadLinkCheckUrl = (guid: string): string => linkCheckRunUrl(guid) + '.csv';

const LinkCheckerPagelet = () => {
  const [modalState, setModalState] = useState<
    'Start' | 'Init' | 'Generating' | 'Download' | 'Failed'
  >('Start');
  const [retry, setRetry] = useState(0);
  const [progress, setProgress] = useState(0);
  const onProgress = ({ done, todo }: InProgress) => {
    setProgress((done / todo) * 100);
  };
  const [reportGuid, setReportGuid] = useState<string | '' | undefined>(undefined);
  const [downloaded, setDownloaded] = useState(false);
  const translate = useTranslation();
  useEffect(() => {
    if (modalState === 'Init') {
      setReportGuid(undefined);
      initLinkCheck()
        .then(guid => {
          setReportGuid(guid);
          if (guid === '') {
            setTimeout(() => setRetry(1 + retry), 10000);
          } else {
            setModalState('Generating');
          }
        })
        .catch(() => setModalState('Failed'));
    } else if (modalState === 'Generating') {
      runLinkCheck(reportGuid!)
        .then(data => srsAsync<void>(data, onProgress))
        .then(() => {
          setDownloaded(false);
          setModalState('Download');
        })
        .catch(() => setModalState('Failed'));
    }
  }, [modalState, retry]);
  return (
    <div className="link-checker-page mt-3">
      {modalState === 'Start' || modalState === 'Init' ? (
        <>
          <p>
            Link Check is intended to help you identify and test the existing external links in your
            course. This tool searches all the course content for hyperlinks and connects to the
            target servers to validate whether the linked content still exists.
          </p>
          <p>
            This process can take several minutes after which a report will be generated for your
            review. This report may identify the following statuses:
          </p>
          <ul>
            <li>200 OK</li>
            <li>301 Moved Permanently, 302 Found / Moved Temporarily</li>
            <li>401 Unauthorized, 403 Forbidden, 404 Not Found</li>
            <li>
              <a
                href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status"
                rel="noopener noreferrer nofollow"
                target="_blank"
              >
                Other statuses
              </a>{' '}
              are possible, depending on the Web server. The numeric status codes are always the
              same, but the text description may vary.
            </li>
          </ul>
          <p>
            Generally speaking, 2xx (e.g. 200 OK) status codes are good; 3xx status codes may need
            attention, and any 4xx status codes almost certainly need to be verified. More details
            about the status codes are provided once the report has been generated.
          </p>
          <p>
            As you are reviewing the report, you will want to manually verify and determine the
            appropriate revisions that need to be made to provide learners with an updated resource.
          </p>
          <p>
            Click the {translate('COURSE_CHECK_EXTERNAL_LINKS_START')} button below if you are sure
            you want to start this process now.
          </p>
          <div>
            <Button
              color="primary"
              onClick={() => setModalState('Init')}
              id="link-check-start"
              disabled={modalState === 'Init'}
            >
              {translate('COURSE_CHECK_EXTERNAL_LINKS_START')}
              {modalState === 'Init' && (
                <Spinner
                  bsSize="sm"
                  className="ms-2"
                />
              )}
            </Button>
            {reportGuid === '' && (
              <span className="ms-3">The link check service is currently busy, please wait...</span>
            )}
          </div>
        </>
      ) : modalState === 'Generating' ? (
        <>
          <p>
            Link Check is running an external link test. A report will be available upon completion.
            Please stay on this page, while the Link Report is generating.
          </p>
          <p>
            This process may take several minutes, depending on the number of external links in the
            course and the status of each external link.
          </p>
          <Progress
            className="my-3"
            animated
            color="primary"
            value={progress}
          >
            {Math.round(progress)}%
          </Progress>
          <div>
            <Button
              disabled
              color="primary"
              id="link-check-generating"
            >
              {translate('COURSE_CHECK_EXTERNAL_LINKS_GENERATING')}
              <Spinner
                bsSize="sm"
                className="ms-2"
              />
            </Button>
          </div>
        </>
      ) : modalState === 'Download' ? (
        <>
          <p>
            Link Check has completed testing the external links in the course; the report can be
            downloaded here:
          </p>
          <div className="pt-1 mb-3 text-center">
            <a
              id="link-check-download"
              href={downloadLinkCheckUrl(reportGuid!)}
              target="_blank"
              className={classNames(
                'd-inline-flex align-items-center search-download',
                downloaded && 'disabled'
              )}
              onClick={() => {
                if (downloaded) {
                  return false;
                }
                setDownloaded(true);
              }}
            >
              Download Link Check CSV <FiDownload className="ms-2" />
            </a>
          </div>
          <p>
            <strong>Okay</strong> links must meet one of the following criteria:
          </p>
          <ul>
            <li>
              200s code
              <ul>
                <li>
                  The link returns a success status code. You <strong>do not</strong> need to update
                  these links.
                </li>
              </ul>
            </li>
            <li>
              300s code
              <ul>
                <li>
                  The link returns a redirect from an insecure <code>http://</code> URL to a secure{' '}
                  <code>https://</code> URL and the rest of the URL remains the same. Even though
                  this is okay, you should update these links to improve security. You{' '}
                  <strong>do</strong> need to update these links.
                </li>
                <li>
                  The link returns a redirect from a URL with a <code>www.</code> prefix to one
                  without, or vice versa, or to a URL that looks like a load balancer such as{' '}
                  <code>www-1</code>, and the rest of the URL remains the same. You{' '}
                  <strong>do not</strong> need to update these links.
                </li>
                <li>
                  The link returns a redirect from a URL that ends in <code>/</code> to a child
                  page; for example, from <code>/</code> to <code>/index.html</code>. You{' '}
                  <strong>do not</strong> need to update these links, the original <code>/</code>{' '}
                  form is preferred.
                </li>
              </ul>
            </li>
          </ul>
          <p>
            <strong>Not Okay</strong> links meet one of the following criteria:
          </p>
          <ul>
            <li>
              400s code
              <ul>
                <li>
                  The link returns an error. You <strong>do</strong> need to update these links.
                  <ul>
                    <li>The link may be permanently unavailable.</li>
                    <li>Sometimes a server may be temporarily offline or inaccessible.</li>
                    <li>
                      Other times, servers may refuse to respond to an automated link checker, and
                      instead return an error or redirect. These links will need to be manually
                      checked.
                    </li>
                  </ul>
                </li>
              </ul>
            </li>
            <li>
              300s code
              <ul>
                <li>
                  The link redirects to a completely different URL. You <strong>do</strong> need to
                  update these links.
                </li>
              </ul>
            </li>
          </ul>
        </>
      ) : modalState === 'Failed' ? (
        <>
          <div className="mb-3">
            The link check operation failed. Please contact your administrator for assistance.
          </div>
          <div>
            <Button
              color="danger"
              id="link-check-failure"
              disabled
            >
              {translate('COURSE_CHECK_EXTERNAL_LINKS_FAILED')}
            </Button>
          </div>
        </>
      ) : null}
    </div>
  );
};

export default LinkCheckerPagelet;
