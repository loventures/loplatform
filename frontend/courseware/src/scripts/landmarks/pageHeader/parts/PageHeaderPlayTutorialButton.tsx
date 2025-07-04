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

import classNames from 'classnames';
import * as tutorialSlice from '../../../tutorial/tutorialSlice';
import { useTranslation } from '../../../i18n/translationContext';
import { timeoutEffect } from '../../../utilities/effectUtils';
import React, { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';

const PageHeaderPlayTutorialButton: React.FC<{ glow: boolean }> = ({ glow }) => {
  const translate = useTranslation();
  const dispatch = useDispatch();
  const [glowing, setGlowing] = useState(false);
  useEffect(() => {
    if (glow) {
      setGlowing(true);
      return timeoutEffect(() => setGlowing(false), 1500)();
    }
  }, [glow]);
  return (
    <button
      className={classNames('icon-btn nav-icon icon icon-question', { glow: glowing })}
      onClick={() => dispatch(tutorialSlice.manuallyPlay())}
      title={translate('TUTORIAL_SHOW')}
    />
  );
};

export default PageHeaderPlayTutorialButton;
