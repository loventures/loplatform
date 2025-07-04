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

function generateMatching(asset) {
  var defaultEssay = {
    questionContent: {
      Assess_Question_Term: [],
      Assess_Question_Definition: [],
      'AssessQuestion.pointsPossible': '1',
      'AssessQuestion.questionText': asset.title,
      'AssessQuestion.complexQuestionText': {
        partType: 'html',
        html: asset.title,
      },
      'AssessQuestion.contentBlockQuestionText': {
        partType: 'block',
        parts: [
          {
            partType: 'html',
            html: asset.title,
          },
        ],
      },
      'AssessQuestion.richCorrectAnswerFeedback': {
        partType: 'html',
        html: '',
      },
      'AssessQuestion.richIncorrectAnswerFeedback': {
        partType: 'html',
        html: '',
      },
    },
  };

  return Object.assign({}, defaultEssay, asset);
}
export default generateMatching;
