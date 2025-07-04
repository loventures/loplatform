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

import { useCourseSelector } from '../../loRedux';
import { setFullscreenActionCreator } from '../../courseContentModule/actions/contentPageActions';
import { selectFullscreenState } from '../../courseContentModule/selectors/contentLandmarkSelectors';
import { useTranslation } from '../../i18n/translationContext';
import React from 'react';
import { FiMaximize, FiMinimize } from 'react-icons/fi';
import { useDispatch } from 'react-redux';
import { Button } from 'reactstrap';

const ERFullScreenButton: React.FC = () => {
  const translate = useTranslation();
  const dispatch = useDispatch();

  const fullscreen = !!useCourseSelector(selectFullscreenState);
  const msg = fullscreen ? 'EXIT_FULL_SCREEN_DESC' : 'VIEW_FULL_SCREEN_DESC';

  return (
    <Button
      id="full-screen-button"
      color="primary"
      outline
      className="border-white px-2"
      aria-label={translate(msg)}
      title={translate(msg)}
      onClick={() => dispatch(setFullscreenActionCreator(!fullscreen))}
      aria-expanded={fullscreen}
    >
      {fullscreen ? (
        <FiMinimize
          size="2rem"
          strokeWidth={1.2}
          aria-hidden={true}
        />
      ) : (
        <FiMaximize
          size="2rem"
          strokeWidth={1.2}
          aria-hidden={true}
        />
      )}
    </Button>
  );
};

export default ERFullScreenButton;
