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

import { AssetNode, NewAsset, SurveyEssayQuestionData } from '../types/asset';
import { EdgeGroup } from '../types/edge';
import { SurveyChoiceQuestion, SurveyEssayQuestion } from '../types/typeIds';

export const createEmbedTag = (
  group: EdgeGroup,
  edge: {
    target: NewAsset<any>;
    edgeId?: string;
  }
) => {
  const target = edge.target;
  const edgeUrl = `loEdgeId://${edge.edgeId}`;
  const constant = {
    cssResources: {
      'image.1': `url("${edgeUrl}")`,
    },
    resources: {
      'image.1': `<img src="${edgeUrl}" data-asset-title="${target.data.title}"/>`,
      'video.1': `<video src="${edgeUrl}" data-asset-title="${target.data.title}" controls></video>`,
      'audio.1': `<audio src="${edgeUrl}" data-asset-title="${target.data.title}" controls></audio>`,
      'pdf.1': `<a href="${edgeUrl}" data-asset-title="${target.data.title}">Download</a>`,
      'file.1': `<a href="${edgeUrl}" data-asset-title="${target.data.title}" target="_blank">Download</a>`,
      'videoCaption.1': `<track  src="${edgeUrl}" label="${target.data.label}"
                        data-asset-title="${target.data.title}" kind="subtitles" srclang="${target.data.language}" >`,
    },
  };
  return constant[group][target.typeId];
};

export const extractTitle = (assetData: Partial<AssetNode>) => {
  if (assetData.data.title) {
    return assetData.data.title;
  } else {
    switch (assetData.typeId) {
      // TODO: isTypeOf type narrowing
      case SurveyChoiceQuestion:
      case SurveyEssayQuestion: {
        const html = (assetData.data as SurveyEssayQuestionData).prompt.renderedHtml;
        const temp = document.createElement('span');
        temp.innerHTML = html;
        return temp.textContent.substring(0, 254);
      }
      default:
        return; // return undefined.
    }
  }
};
