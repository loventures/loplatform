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

import './0_module';

import angular from 'angular';
import classNames from 'classnames';
import { TranslationContext, TranslationProvider } from '../i18n/translationContext.tsx';
import { react2angularWithDigest } from '../utilities/react2angularWithDigest';
import React, { useContext } from 'react';

type LoCheckboxProps = {
  state: boolean;
  checkboxFor?: string;
  checkboxLabel: string;
  checkboxTitle?: string;
  onToggle: (checked: boolean) => void;
  disabled?: boolean;
  tabIndex?: number;
};

export const LoCheckbox: React.FC<LoCheckboxProps> = ({
  state,
  checkboxFor,
  checkboxLabel,
  checkboxTitle = checkboxLabel,
  onToggle,
  tabIndex,
  disabled = false,
}) => {
  const translate = useContext(TranslationContext);
  return (
    <label // eslint-disable-line jsx-a11y/label-has-associated-control
      className={classNames(
        'btn m-0 flex-shrink-0 lo-checkbox',
        state ? 'btn-primary' : 'btn-outline-primary'
      )}
      htmlFor={checkboxFor}
    >
      <div className="custom-control custom-checkbox">
        <input // eslint-disable-line jsx-a11y/label-has-associated-control
          tabIndex={tabIndex ? tabIndex : undefined}
          type="checkbox"
          className="custom-control-input"
          id={checkboxFor ? checkboxFor : undefined}
          disabled={disabled}
          checked={state}
          onChange={() => onToggle(!state)}
          title={translate(checkboxTitle)}
        />
        <span className="custom-control-label">{translate(checkboxLabel)}</span>
      </div>
    </label>
  );
};

//layer of angular in between react breaks the provider/consumer hierarchy
const LoCheckbox2Angular = (props: LoCheckboxProps) => (
  <TranslationProvider>
    <LoCheckbox {...props} />
  </TranslationProvider>
);
angular
  .module('lo.directives')
  .component(
    'loCheckbox',
    react2angularWithDigest(
      LoCheckbox2Angular,
      ['state', 'checkboxFor', 'checkboxLabel', 'onToggle', 'disabled'],
      [],
      ['onToggle']
    )
  );
