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

import { Asset, RatingScaleQuestion1 } from '../../authoring';
import _ from 'lodash';
import { TranslationContext } from '../../i18n/translationContext';
import React, { useContext, useState } from 'react';
import { Button, ButtonGroup } from 'reactstrap';

export interface SurveyRatingScaleProps {
  question: Asset<RatingScaleQuestion1>;
  setResponse: (r: string | number) => void;
  disabled: boolean;
}

const SurveyRatingScaleQuestion1Body: React.FC<SurveyRatingScaleProps> = ({
  question,
  setResponse,
  disabled,
}) => {
  const translate = useContext(TranslationContext);

  const [rating, setRating] = useState<number | null>(null);

  const onRatingBtnClick = (rating: number) => {
    setRating(rating);
    setResponse(`${rating}`);
  };

  const ratingGroupAriaLabel = translate('CONTENT_SURVEY_RATING_BUTTON_GROUP', {
    max: question.data.max,
    lowRatingText: question.data.lowRatingText,
    highRatingText: question.data.highRatingText,
  });

  function getButtons() {
    return (
      <>
        {_.range(1, question.data.max + 1).map(i => (
          <Button
            key={i}
            outline
            color="primary"
            onClick={() => onRatingBtnClick(i)}
            active={rating === i}
            disabled={disabled}
          >
            {i}
          </Button>
        ))}
      </>
    );
  }

  if (question.data.max > 10) {
    return (
      <div className="position-relative">
        <div
          className="position-absolute"
          style={{
            top: '7px',
            left: '50px',
          }}
        >
          {question.data.lowRatingText}
        </div>
        <ButtonGroup
          vertical
          aria-label={ratingGroupAriaLabel}
        >
          {getButtons()}
        </ButtonGroup>
        <div
          className="position-absolute"
          style={{
            bottom: '7px',
            left: '50px',
          }}
        >
          {question.data.highRatingText}
        </div>
      </div>
    );
  } else {
    return (
      <>
        <div className="d-flex flex-row justify-content-between">
          <div>{question.data.lowRatingText}</div>
          <div>{question.data.highRatingText}</div>
        </div>
        <ButtonGroup
          style={{
            display: 'grid',
            gridTemplateColumns: `repeat(${question.data.max}, 1fr)`,
          }}
          size="sm"
          aria-label={ratingGroupAriaLabel}
        >
          {getButtons()}
        </ButtonGroup>
      </>
    );
  }
};

export default SurveyRatingScaleQuestion1Body;
