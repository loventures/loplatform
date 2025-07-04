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

import ERContentContainer from '../../landmarks/ERContentContainer';
import { useTranslation } from '../../i18n/translationContext';
import React from 'react';

import SendMessage from './SendMessage';
import ERNonContentTitle from '../contentPlayer/ERNonContentTitle.tsx';

const ERSendMessagePage: React.FC = () => {
  const translate = useTranslation();
  return (
    <ERContentContainer title={translate('MESSAGING_SEND_MESSAGE')}>
      <div className="container p-0">
        <div className="card er-content-wrapper mb-2 m-md-3 m-lg-4">
          <div className="card-body">
            <ERNonContentTitle label={translate('MESSAGING_SEND_MESSAGE')} />
            <div className="px-4 py-3">
              <SendMessage />
            </div>
          </div>
        </div>
      </div>
    </ERContentContainer>
  );
};

export default ERSendMessagePage;
