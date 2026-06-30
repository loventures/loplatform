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
import { SELECTION_TYPE_HOTSPOT } from '../../../utilities/questionTypes.js';
import { BasicQuestionTemplate } from '../../questionTemplates/BasicQuestionTemplate.tsx';

const SELECTION_RADIUS = 10;

// Module-level image cache: the React analogue of the Angular HotspotQuestionImageCache
// service (still used by the Angular print view), keyed by question id.
const imageCache = new Map<any, HTMLImageElement>();

interface Shape {
  type?: string;
  radius?: number;
  width?: number;
  height?: number;
}
interface Choice {
  shape?: Shape;
  x?: number;
  y?: number;
  correct?: boolean;
  selected?: boolean;
  response?: boolean;
}
interface Question {
  id?: any;
  imageUrl?: string;
  image?: { nodeName?: string };
  choices?: Choice[];
}
interface Response {
  selection?: { point?: { x?: number; y?: number }; responseType?: string } | null;
}

export interface HotspotQuestionBaseViewProps {
  index: number;
  assessment?: { settings?: { isCheckpoint?: boolean } };
  questionCount?: number;
  question: Question;
  response?: Response;
  changeAnswer: (index: number, response: any) => void;
  canEditAnswer?: boolean;
}

const getImageUrl = (question: Question) => {
  if (question.imageUrl) return domainUrl + question.imageUrl;
  if (question.image) return getHotspotImageUrl(question.image.nodeName);
  return undefined;
};

const choiceClasses = (choice: Choice) =>
  [
    'hotspot-choice',
    choice.correct === true ? 'correct' : '',
    choice.correct === false ? 'incorrect' : '',
    choice.selected ? 'selected' : '',
    choice.response ? 'response' : '',
  ]
    .filter(Boolean)
    .join(' ');

/**
 * React port of the learner `hotspotQuestionBaseView` (B2-quiz): an SVG "paper"
 * showing the image with the answer choices (circles/rects) and the learner's
 * click marker. The image is loaded + cached (module-level, replacing the Angular
 * HotspotQuestionImageCache service), the click point is computed from the SVG's
 * bounding rect, and the selection round-trips through `changeAnswer`. DOM
 * preserved from hotspotQuestionBaseView.html: `.hotspot-question-play`,
 * `.hotspot-container`, `svg.hotspot-paper`, `g.hotspot-choice` (with
 * correct/incorrect/selected/response) and `g.hotspot-choice.selection`. The
 * print view stays Angular.
 */
export const HotspotQuestionBaseView: React.FC<HotspotQuestionBaseViewProps> = ({
  index,
  assessment,
  questionCount,
  question,
  response,
  changeAnswer,
  canEditAnswer,
}) => {
  const translate = useTranslation();
  const [image, setImage] = useState<{ width: number; height: number; src: string } | null>(null);

  useEffect(() => {
    const cached = imageCache.get(question.id);
    if (cached) {
      setImage({ width: cached.width, height: cached.height, src: cached.src });
      return;
    }
    const url = getImageUrl(question);
    const img = new window.Image();
    img.onload = () => {
      imageCache.set(question.id, img);
      setImage({ width: img.width, height: img.height, src: img.src });
    };
    if (url) img.src = url;
  }, [question.id]);

  const point = response?.selection?.point;
  const hasSelection = !isEmpty(point);

  const onClickPaper = (e: React.MouseEvent<SVGSVGElement>) => {
    if (!canEditAnswer) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const newPoint = { x: e.clientX - rect.left, y: e.clientY - rect.top };
    const resp: any = response || {};
    const selection = (response && response.selection) || { responseType: SELECTION_TYPE_HOTSPOT };
    changeAnswer(index, { ...resp, selection: { ...selection, point: newPoint } });
  };

  return (
    <BasicQuestionTemplate
      className="question hotspot-question"
      index={index}
      assessment={assessment}
      questionCount={questionCount}
      question={question as any}
      response={response as any}
    >
      <div className="hotspot-question-play">
        {!image && <div className="alert alert-info">{translate('HOTSPOT_IMAGE_LOADING')}</div>}
        {image && (
          <div className="hotspot-container mt-3">
            <svg
              className="hotspot-paper"
              width={image.width}
              height={image.height}
              onClick={onClickPaper}
              style={{ backgroundImage: `url(${image.src})` }}
            >
              {(question.choices ?? []).map(
                (choice, i) =>
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
                    cx={point!.x}
                    cy={point!.y}
                    r={SELECTION_RADIUS}
                  />
                </g>
              )}
            </svg>
          </div>
        )}
      </div>
    </BasicQuestionTemplate>
  );
};
