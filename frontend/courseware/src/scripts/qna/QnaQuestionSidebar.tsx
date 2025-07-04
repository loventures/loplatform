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

import classNames from 'classnames';
import copyToClipboard from 'copy-to-clipboard';
import AnswerQuestion from '../qna/AnswerQuestion';
import { fetchQnaQuestionIds, QnaQuestionDto } from '../qna/qnaApi';
import { useQnaQuery } from '../qna/qnaHooks';
import QnaLink from '../qna/QnaLink';
import { useTranslation } from '../i18n/translationContext';
import { history } from '../utilities/history';
import React, { useEffect, useMemo, useRef, useState } from 'react';
import { FiCopy } from 'react-icons/fi';
import { TfiClose } from 'react-icons/tfi';
import { useScroll } from 'react-use';
import { Button, Tooltip } from 'reactstrap';
import { MdChevronLeft, MdChevronRight } from 'react-icons/md';

type QnaQuestionSidebarProps = {
  question: QnaQuestionDto;
};

const QnaQuestionSidebar: React.FC<QnaQuestionSidebarProps> = ({ question }) => {
  const translate = useTranslation();

  const messagesRef = useRef<HTMLDivElement>(null);
  const { y } = useScroll(messagesRef);

  const scrollToBottom = () => {
    const messageDiv = messagesRef?.current;
    if (messageDiv) {
      messageDiv.scrollTop = messageDiv.scrollHeight;
    }
  };

  const { offset: _1, limit: _2, ...query } = useQnaQuery();
  const [questionIds, setQuestionIds] = useState(new Array<number>());
  const [idCopied, setIdCopied] = useState(false);
  const [tooltipOpen, setTooltipOpen] = useState(false);

  useEffect(() => {
    fetchQnaQuestionIds(query).then(({ objects: questionIds }) => setQuestionIds(questionIds));
  }, []);

  const [index, lag, lead] = useMemo((): [number, number | undefined, number | undefined] => {
    const idx = questionIds.indexOf(question.id);
    return [idx, questionIds[idx - 1], questionIds[idx + 1]];
  }, [question.id, questionIds.length]);

  const copyId = (externalId: string) => {
    copyToClipboard(externalId);
    setIdCopied(true);
    setTimeout(() => {
      setIdCopied(false);
      setTooltipOpen(false);
    }, 1500);
  };

  return (
    <div
      id="QnaSidebar"
      className={classNames('qna-sidebar opened', { scrolled: y > 0 })}
    >
      <div className="feedback-container">
        <div className="feedback-sidebar-header p-2 p-sm-3">
          <div className="d-flex align-items-center">
            <Button
              color="medium"
              outline
              className="p-1 border-0 flex-grow-0 text-muted"
              style={{ lineHeight: 1 }}
              onClick={() => history.push('/instructor/qna')}
              title={translate('QNA_SIDEBAR_CLOSE')}
              aria-label={translate('QNA_SIDEBAR_CLOSE')}
            >
              <TfiClose
                aria-hidden={true}
                size="1rem"
                style={{ strokeWidth: 0.75 }}
              />
            </Button>
            {question.instructorMessage ? (
              <h6 className="h6 mb-0 ms-2 text-truncate flex-grow-1 font-weight-bold">
                {translate('QNA_INSTRUCTOR_SIDEBAR_TITLE_SENT')}:{' '}
                {!question.recipients?.length ? (
                  <span>{translate('QNA_ENTIRE_CLASS')}</span>
                ) : (
                  question.recipients.map(r => (
                    <>
                      <span
                        id={`qna-user-detail-${r.id}`}
                        className={classNames({ 'qna-user-detail': r.externalId })}
                      >
                        {r.fullName}
                      </span>
                      {r.externalId && (
                        <Tooltip
                          target={`qna-user-detail-${r.id}`}
                          placement="bottom"
                          autohide={false}
                          style={{ maxWidth: '40em' }}
                          isOpen={tooltipOpen}
                          toggle={() => setTooltipOpen(!tooltipOpen)}
                        >
                          {idCopied ? (
                            translate('QNA_EXTERNAL_ID_COPIED')
                          ) : (
                            <>
                              {translate('QNA_EXTERNAL_ID', { externalId: r.externalId })}
                              <button
                                onClick={() => copyId(r.externalId)}
                                className="btn btn-link text-white"
                              >
                                <FiCopy aria-label={translate('QNA_COPY_EXTERNAL_ID')} />
                              </button>
                            </>
                          )}
                        </Tooltip>
                      )}
                    </>
                  ))
                )}
              </h6>
            ) : (
              <h6 className="h6 mb-0 ms-2 text-truncate flex-grow-1 font-weight-bold">
                {translate('QNA_INSTRUCTOR_SIDEBAR_TITLE')}:{' '}
                <span
                  id="qna-user-detail"
                  className={classNames({ 'qna-user-detail': question.creator.externalId })}
                >
                  {question.creator.fullName}
                </span>
                {question.creator.externalId && (
                  <Tooltip
                    target="qna-user-detail"
                    placement="bottom"
                    autohide={false}
                    style={{ maxWidth: '40em' }}
                    isOpen={tooltipOpen}
                    toggle={() => setTooltipOpen(!tooltipOpen)}
                  >
                    {idCopied ? (
                      translate('QNA_EXTERNAL_ID_COPIED')
                    ) : (
                      <>
                        {translate('QNA_EXTERNAL_ID', { externalId: question.creator.externalId })}
                        <button
                          onClick={() => copyId(question.creator.externalId)}
                          className="btn btn-link text-white"
                        >
                          <FiCopy aria-label={translate('QNA_COPY_EXTERNAL_ID')} />
                        </button>
                      </>
                    )}
                  </Tooltip>
                )}
              </h6>
            )}
            {index >= 0 ? (
              <div className="d-flex align-items-center">
                <QnaLink
                  questionId={lag}
                  onClick={evt => {
                    if (!lag) {
                      evt.preventDefault();
                    }
                  }}
                  className={classNames(
                    'btn btn-outline-primary border-0 p-1 br-50 me-1 d-flex',
                    !lag && 'disabled'
                  )}
                  aria-label="Previous Question"
                >
                  <MdChevronLeft />
                </QnaLink>
                <span
                  className=""
                  style={{ width: 'max-content' }}
                >
                  {index != null ? `${index + 1} of ${questionIds.length}` : ''}
                </span>
                <QnaLink
                  questionId={lead}
                  onClick={evt => {
                    if (!lead) {
                      evt.preventDefault();
                    }
                  }}
                  className={classNames(
                    'btn btn-outline-primary border-0 p-1 br-50 ms-1 d-flex',
                    !lead && 'disabled'
                  )}
                  aria-label="Next Question"
                >
                  <MdChevronRight />
                </QnaLink>
              </div>
            ) : (
              <div
                className="d-none d-sm-block"
                style={{ width: '1.5rem' }}
              ></div>
            )}
          </div>
        </div>
        <div
          className="d-flex flex-column overflow-auto mh-100"
          ref={messagesRef}
        >
          <AnswerQuestion
            question={question}
            onAddMessage={scrollToBottom}
          />
        </div>
      </div>
    </div>
  );
};

export default QnaQuestionSidebar;
