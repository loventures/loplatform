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

import {
  CONTENT_ITEM_TYPE_QUIZ,
  CONTENT_TYPE_ASSESSMENT,
  CONTENT_TYPE_ASSIGNMENT,
  CONTENT_TYPE_OBSERVATION_ASSESSMENT,
  CONTENT_TYPE_CHECKPOINT,
  CONTENT_TYPE_DIAGNOSTIC,
  CONTENT_TYPE_DISCUSSION,
  CONTENT_TYPE_FILE_BUNDLE,
  CONTENT_TYPE_HTML,
  CONTENT_TYPE_LESSON,
  CONTENT_TYPE_LTI,
  CONTENT_TYPE_MODULE,
  CONTENT_TYPE_POOLED_ASSESSMENT,
  CONTENT_TYPE_RESOURCE,
  CONTENT_TYPE_SCORM,
  CONTENT_TYPE_UNIT,
} from './contentTypes.ts';
import {
  AUDIO_EMBED,
  AUDIO_UPLOAD,
  EXTERNAL_LINK,
  READING_INSTRUCTIONS,
  READING_MATERIAL,
  VIDEO_EMBED,
  VIDEO_UPLOAD,
} from './resource1Types';

const contentDisplayInfoValues: { [key: string]: ContentDisplayInfo } = {
  activity: {
    displayIcon: 'icon-book',
    displayKey: 'ACTIVITY',
  },

  pretest: {
    displayIcon: 'icon-EKG-line',
    displayKey: 'PRETEST',
  },

  quiz: {
    displayIcon: 'icon-text',
    displayKey: 'QUIZ',
  },

  checkpoint: {
    displayIcon: 'icon-stack-check',
    displayKey: 'CHECKPOINT',
  },

  project: {
    displayIcon: 'icon-design',
    displayKey: 'FINAL_PROJECT',
  },

  observationAssessment: {
    displayIcon: 'icon-stack',
    displayKey: 'OBSERVATION_ASSESSMENT',
  },

  discussion: {
    displayIcon: 'icon-bubbles',
    displayKey: 'DISCUSSION',
  },

  unit: {
    displayIcon: 'icon-cube',
    displayKey: 'UNIT',
  },

  module: {
    displayIcon: 'icon-stack',
    displayKey: 'MODULE',
  },

  lesson: {
    displayIcon: 'icon-text',
    displayKey: 'LESSON',
  },

  fileBased: {
    displayIcon: 'icon-book',
    displayKey: 'FILE_BASED',
  },

  ltiResource: {
    displayIcon: 'icon-earth',
    displayKey: 'LTI_LINK',
  },

  link: {
    displayIcon: 'icon-earth',
    displayKey: 'ACTIVITY',
  },

  externalLink: {
    displayIcon: 'icon-earth',
    displayKey: 'EXTERNAL_LINK',
  },

  audio: {
    displayIcon: 'icon-mic',
    displayKey: 'AUDIO',
  },

  video: {
    displayIcon: 'icon-play',
    displayKey: 'VIDEO',
  },

  readingText: {
    displayIcon: 'icon-book',
    displayKey: 'READING',
  },

  readingUpload: {
    displayIcon: 'icon-text',
    displayKey: 'READING_UPLOAD',
  },
};

export type ContentDisplayInfo = {
  displayIcon: string;
  displayKey: string;
};

const getDisplayInfoValueKey = <C extends { typeId: string; subType?: string | null }>(
  content: C
) => {
  switch (content.typeId) {
    case CONTENT_TYPE_UNIT:
      return 'unit';
    case CONTENT_TYPE_MODULE:
      return 'module';
    case CONTENT_TYPE_LESSON:
      return 'lesson';

    case CONTENT_ITEM_TYPE_QUIZ:
    case CONTENT_TYPE_ASSESSMENT:
    case CONTENT_TYPE_POOLED_ASSESSMENT:
      return 'quiz';
    case CONTENT_TYPE_CHECKPOINT:
      return 'checkpoint';
    case CONTENT_TYPE_DIAGNOSTIC:
      return 'pretest';
    case CONTENT_TYPE_ASSIGNMENT:
      return 'project';
    case CONTENT_TYPE_OBSERVATION_ASSESSMENT:
      return 'observationAssessment';
    case CONTENT_TYPE_DISCUSSION:
      return 'discussion';

    case CONTENT_TYPE_FILE_BUNDLE:
    case CONTENT_TYPE_SCORM:
    case CONTENT_TYPE_HTML:
      return 'fileBased';
    case CONTENT_TYPE_LTI:
      return 'ltiResource';

    case CONTENT_TYPE_RESOURCE:
      switch (content.subType) {
        case AUDIO_EMBED:
        case AUDIO_UPLOAD:
          return 'audio';

        case VIDEO_EMBED:
        case VIDEO_UPLOAD:
          return 'video';

        case READING_INSTRUCTIONS:
          return 'readingText';

        case READING_MATERIAL:
          return 'readingUpload';

        case EXTERNAL_LINK:
          return 'externalLink';

        default:
          return 'activity';
      }

    default:
      return 'activity';
  }
};

export const getContentDisplayInfo = <C extends { typeId: string; subType?: string | null }>(
  content: C
) => {
  return contentDisplayInfoValues[getDisplayInfoValueKey(content)];
};
