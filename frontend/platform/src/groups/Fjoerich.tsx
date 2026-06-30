/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import classnames from 'classnames';
import React from 'react';

import { useTranslations } from '../redux/state';

interface FjoerichProps {
  className?: string;
  fjœr: boolean;
  label?: boolean;
  setFjœr: (fjœr: boolean) => void;
}

const Fjœrich: React.FC<FjoerichProps> = ({ className = '', fjœr, label = true, setFjœr }) => {
  const T = useTranslations();

  const fjoerClick = () => setFjœr(fjœr);
  const containerCls = classnames({
    'flame-container': true,
    fjœr: fjœr,
  });
  const keydown = (e: React.KeyboardEvent<HTMLDivElement>) => {
    if (e.keyCode === 32) {
      e.preventDefault();
      fjoerClick();
    }
  };

  return (
    <div
      id="flame-check"
      className={className}
      role="checkbox"
      tabIndex={0}
      aria-checked={fjœr}
      onKeyDown={keydown}
      onClick={fjoerClick}
    >
      {!!label && (
        <label
          id="flame-label"
          htmlFor="flame-check"
        >
          {T.t('adminPage.groups.fjœrosityToggle')}
        </label>
      )}
      <div className={containerCls}>
        <div className="red flame"></div>
        <div className="orange flame"></div>
        <div className="yellow flame"></div>
        <div className="white flame"></div>
        <div className="blue circle"></div>
        <div className="white circle"></div>
        <div className="check circle"></div>
      </div>
    </div>
  );
};

export default Fjœrich;
