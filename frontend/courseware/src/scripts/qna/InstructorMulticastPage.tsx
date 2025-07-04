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

import ERBasicTitle from '../commonPages/contentPlayer/ERBasicTitle';
import ERContentContainer from '../landmarks/ERContentContainer';
import { useTranslation } from '../i18n/translationContext';
import InstructorQnaSendMessage from './InstructorQnaSendMessage';

const InstructorMulticastPage: React.FC = () => {
  const translate = useTranslation();

  return (
    <ERContentContainer title={translate('QNA_SEND_MESSAGE')}>
      <ERBasicTitle label={translate('QNA_SEND_MESSAGE')} />
      <div className="col p-4">
        <InstructorQnaSendMessage />
      </div>
    </ERContentContainer>
  );
};

export default InstructorMulticastPage;
