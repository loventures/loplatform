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

import { PureComponent } from 'react';
import { withTranslation } from '../../i18n/translationContext';
import { describeArc } from './circleUtils';

const start = -150;
const end = 150;
const start2end = end - start;

class HorseshoeGraphic extends PureComponent {
  render() {
    const total = this.props.correct + this.props.incorrect;
    const correctEnd = (this.props.correct / total) * start2end + start;
    const incorrectStart = end - (this.props.incorrect / total) * start2end;
    const arcCorrect = describeArc(100, 100, 85, start, correctEnd);
    const arcIncorrect = describeArc(100, 100, 85, incorrectStart, end);
    return (
      <svg
        className="horseshoe-svg"
        width="100%"
        height="100%"
        viewBox="0 0 200 200"
        version="1.1"
        xmlns="http://www.w3.org/2000/svg"
      >
        <g
          strokeWidth="25"
          fill="none"
        >
          <path
            className="horseshoe-arc-correct"
            d={arcCorrect}
            stroke="lime"
          />
          <path
            className="horseshoe-arc-incorrect"
            d={arcIncorrect}
            stroke="red"
          />
        </g>
      </svg>
    );
  }
}

const ScoreHorseshoe = ({ correct, incorrect, children }) => (
  <div
    className="horseshoe-chart"
    aria-hidden="true"
  >
    <div>
      <HorseshoeGraphic
        correct={correct}
        incorrect={incorrect}
      />
    </div>
    <div className="horseshoe-chart-content">{children}</div>
  </div>
);

export default withTranslation(ScoreHorseshoe);
