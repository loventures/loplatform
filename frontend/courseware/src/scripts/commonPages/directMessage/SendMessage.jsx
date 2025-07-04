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

import { map } from 'lodash';
import { connect } from 'react-redux';
import { createSelector } from 'reselect';
import { FullMessageCreator } from './messageCreator';
import { b64Decode } from '../../utilities/b64Utils';
import { selectRouter } from '../../utilities/rootSelectors';
import { lojector } from '../../loject';

const SendMessage = ({ entireClass, recipients }) => (
  <FullMessageCreator
    entireClass={entireClass}
    recipients={recipients}
  />
);

const selectSendMessage = createSelector(selectRouter, router => {
  const UserClass = lojector.get('UserClass');
  return {
    entireClass: router.searchParams.entireClass,
    recipients: map(b64Decode(router.searchParams.recipients), l => new UserClass('student', l)),
  };
});

export default connect(selectSendMessage)(SendMessage);
