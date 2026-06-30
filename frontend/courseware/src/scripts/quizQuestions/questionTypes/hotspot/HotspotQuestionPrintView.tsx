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

import { isEmpty } from 'lodash';
import React, { useEffect, useState } from 'react';

import { getHotspotImageUrl } from '../../../utilities/assetRendering.js';
import domainUrl from '../../../utilities/domainUrl.js';
import { useTranslation } from '../../../i18n/translationContext.tsx';
import { PrintQuestionTemplate } from '../../questionTemplates/PrintQuestionTemplate.tsx';

const SELECTION_RADIUS = 10;
const MAX_PRINT_IMAGE_SIZE = 5 * 96; // assume 5 inches at 96 DPI

const getImageUrl = (question: any) => {
  if (question.imageUrl) return domainUrl + question.imageUrl;
  if (question.image) return getHotspotImageUrl(question.image.nodeName);
  return undefined;
};

const choiceClasses = (choice: any) =>
  [
    'hotspot-choice',
    choice.correct === true ? 'correct' : '',
    choice.correct === false ? 'incorrect' : '',
    choice.selected ? 'selected' : '',
    choice.response ? 'response' : '',
  ]
    .filter(Boolean)
    .join(' ');

export interface HotspotQuestionPrintViewProps {
  index: number;
  assessment?: { settings?: { isCheckpoint?: boolean } };
  questionCount?: number;
  question: any;
  response?: any;
}

/**
 * React port of the learner `hotspotQuestionPrintView` (B2-quiz print). Read-only: the image scaled to
 * a print-friendly size (5in @ 96DPI) under an SVG `viewBox`, the answer-choice shapes (circle/rect with
 * correct/incorrect/selected/response classes), and the learner's click marker. DOM preserved from
 * hotspotQuestionPrintView.html (`.print-hotspot-question`, `.hotspot-container`, `svg.hotspot-paper`,
 * `g.hotspot-choice`, `.hotspot-image`). No Selenide print coverage for hotspot — verified via build.
 */
export const HotspotQuestionPrintView: React.FC<HotspotQuestionPrintViewProps> = ({
  index,
  assessment,
  questionCount,
  question,
  response,
}) => {
  const translate = useTranslation();
  const [image, setImage] = useState<{ width: number; height: number; src: string } | null>(null);

  useEffect(() => {
    const url = getImageUrl(question);
    const img = new window.Image();
    img.onload = () => setImage({ width: img.width, height: img.height, src: img.src });
    if (url) img.src = url;
  }, [question.id]);

  const point = response?.selection?.point;
  const hasSelection = !isEmpty(point);

  const printScale = image && image.width > MAX_PRINT_IMAGE_SIZE ? MAX_PRINT_IMAGE_SIZE / image.width : 1;
  const printFriendlyWidth = image ? image.width * printScale : 0;
  const printFriendlyHeight = image ? image.height * printScale : 0;

  return (
    <PrintQuestionTemplate
      className="question hotspot-question print-hotspot-question"
      index={index}
      assessment={assessment}
      questionCount={questionCount}
      question={question}
      response={response}
    >
      {!image && <div className="alert alert-info">{translate('HOTSPOT_IMAGE_LOADING')}</div>}

      {image && (
        <div className="hotspot-container mt-3 position-relative">
          <svg
            className="hotspot-paper"
            viewBox={`0 0 ${image.width} ${image.height}`}
            width={printFriendlyWidth}
            height={printFriendlyHeight}
          >
            {(question.choices ?? []).map(
              (choice: any, i: number) =>
                choice.shape && (
                  <g
                    key={i}
                    className={choiceClasses(choice)}
                  >
                    {choice.shape.type === 'circle' && (
                      <circle
                        cx={choice.x}
                        cy={choice.y}
                        r={choice.shape.radius}
                      />
                    )}
                    {choice.shape.type === 'rect' && (
                      <rect
                        x={choice.x}
                        y={choice.y}
                        width={choice.shape.width}
                        height={choice.shape.height}
                      />
                    )}
                  </g>
                )
            )}
            {hasSelection && (
              <g className="hotspot-choice selection">
                <circle
                  cx={point.x}
                  cy={point.y}
                  r={SELECTION_RADIUS}
                />
              </g>
            )}
          </svg>
          <img
            className="hotspot-image"
            src={image.src}
            width={printFriendlyWidth}
            height={printFriendlyHeight}
            alt=""
          />
        </div>
      )}
    </PrintQuestionTemplate>
  );
};
