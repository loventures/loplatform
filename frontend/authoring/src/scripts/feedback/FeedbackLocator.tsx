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

import React from 'react';
import { TfiLocationPin } from 'react-icons/tfi';
import { Button } from 'reactstrap';

import { trackAuthoringEvent } from '../analytics';
import { FeedbackDto } from './FeedbackApi';

export const FeedbackLocator: React.FC<{ feedback: FeedbackDto }> = ({ feedback }) =>
  feedback.identifier ? (
    <Button
      size="sm"
      color="primary"
      outline
      className="d-flex p-1 br-50 border-0 me-2 locator"
      title="Locate Feedback"
      onClick={() => {
        trackAuthoringEvent('Narrative Editor - Locate Feedback');
        window.postMessage(
          {
            fn: 'highlightElement',
            name: feedback.assetName,
            id: feedback.identifier,
          },
          '*'
        );
      }}
    >
      <TfiLocationPin />
    </Button>
  ) : null;
