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

import classNames from 'classnames';
import React, { useEffect, useRef } from 'react';

import { useTranslation } from '../../i18n/translationContext.tsx';

interface QuestionTuple {
  state?: { answered?: boolean; skipped?: boolean; correct?: boolean; incorrect?: boolean };
}

interface MpQuestionNavProps {
  questionTuples: QuestionTuple[];
  currentIndex: number;
  goto: (index: number) => void;
  canGoStatus?: boolean[];
}

/**
 * React port of the `mpQuestionNav` component: the multi-page player's question-navigation strip (prev /
 * numbered pages / next). On the current page changing it scrolls the `.pagination` so the active page is
 * centred (the old `centerOnNewPage`). DOM preserved for MultiPageQuizNavTest: `nav.mp-player-questions-nav`,
 * `ul.pagination > li.page-item > button.page-link.question-number`, the `active`/`answered`/`skipped`/
 * `correct`/`incorrect` state classes, and the `icon-chevron-left`/`-right` prev/next buttons.
 */
export const MpQuestionNav: React.FC<MpQuestionNavProps> = ({
  questionTuples,
  currentIndex,
  goto,
  canGoStatus,
}) => {
  const translate = useTranslation();
  const paginationRef = useRef<HTMLUListElement>(null);

  // Centre the active page in the (horizontally-scrollable) pagination strip when it changes.
  useEffect(() => {
    const navEl = paginationRef.current;
    const pageEl = navEl?.querySelectorAll('li')[currentIndex];
    if (!navEl || !pageEl) return;
    const navRect = navEl.getBoundingClientRect();
    const pageRect = pageEl.getBoundingClientRect();
    const navCenter = navRect.left + navRect.width / 2;
    const pageCenter = pageRect.left + pageRect.width / 2;
    if (pageCenter !== navCenter) {
      navEl.scrollLeft += pageCenter - navCenter;
    }
  }, [currentIndex, questionTuples.length]);

  const pageLabel = (index: number) =>
    currentIndex === index
      ? translate('CURR_QUESTION', { pageNumber: index + 1 })
      : translate('GOTO_QUESTION', { pageNumber: index + 1 });

  return (
    <nav
      className="mp-player-questions-nav py-1"
      aria-label={translate('QUIZ_PLAYER_QUESTIONS_NAV')}
    >
      <div className="page-item m-1">
        <button
          className="page-link question-number icon icon-arrow-left icon-chevron-left"
          disabled={!canGoStatus?.[currentIndex - 1]}
          aria-label={translate('PREV_QUESTION')}
          onClick={() => goto(currentIndex - 1)}
        />
      </div>

      <ul
        className="pagination"
        ref={paginationRef}
      >
        {questionTuples.map((tuple, index) => (
          <li
            className="page-item m-1"
            key={index}
          >
            <button
              className={classNames('page-link question-number', {
                active: index === currentIndex,
                answered: tuple.state?.answered,
                skipped: tuple.state?.skipped,
                correct: tuple.state?.correct,
                incorrect: tuple.state?.incorrect,
              })}
              disabled={!canGoStatus?.[index]}
              aria-label={pageLabel(index)}
              onClick={() => goto(index)}
            >
              {index + 1}
            </button>
          </li>
        ))}
      </ul>

      <div className="page-item m-1">
        <button
          className="page-link question-number icon icon-arrow-right icon-chevron-right"
          disabled={!canGoStatus?.[currentIndex + 1]}
          aria-label={translate('NEXT_QUESTION')}
          onClick={() => goto(currentIndex + 1)}
        />
      </div>
    </nav>
  );
};

export default MpQuestionNav;
