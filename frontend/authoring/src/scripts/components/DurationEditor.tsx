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

import * as React from 'react';
import { useState } from 'react';
import { FormGroup, Input, Label } from 'reactstrap';

import { useAssetDataField, usePolyglot } from '../hooks';

interface DurationEditorProps {
  disabled: boolean;
  useHours?: boolean;
}

export const getDurationInHoursAndMinutes = (value: number | undefined) => {
  if (value == null) return '';
  const minutes = value % 60;
  const hours = (value - minutes) / 60;
  return minutes === 0 ? `${hours}h` : hours === 0 ? `${minutes}m` : `${hours}h ${minutes}m`;
};

// the "h?" is because if you type in "25m" it goes "2h" "25h" "25mh"
const HMRegex = /^(?:([0-9][0-9.]*)|(?:([0-9][0-9.]*)(?:h|\s)+)?(?:([0-9]+)(?:mh?|\s)*)?)$/;

export const getMinutesByHoursAndMinutes = (hours: string) => {
  if (hours === '') return null;
  const match = hours.match(HMRegex);
  if (!match) throw Error(`Invalid hours: ${hours}`);
  const [, hh, h, m] = match;
  if (hh == null && h == null && m == null) throw Error(`Invalid hours: ${hours}`);
  return Math.round(parseFloat(hh ?? h ?? '0') * 60 + parseInt(m ?? '0'));
};

const DurationEditor: React.FC<DurationEditorProps> = ({ disabled, useHours = false }) => {
  const { value: duration, onChange: setDuration } = useAssetDataField<number>('duration');
  const [badHours, setBadHours] = useState(false);
  const polyglot = usePolyglot();
  const invalid = duration <= 0 && duration !== null;

  return (
    <FormGroup className="row asset-duration-editor">
      <Label className="col-2 pt-2">{polyglot.t('ASSET_DURATION')}</Label>
      <div className="col-10">
        <Input
          name="duration"
          type={useHours ? 'text' : 'number'}
          step={useHours ? undefined : '1'}
          disabled={disabled}
          invalid={invalid || badHours}
          aria-invalid={invalid || badHours}
          title={polyglot.t(
            useHours ? 'ASSET_DURATION_IN_HOURS_HELP_TEXT' : 'ASSET_DURATION_HELP_TEXT'
          )}
          placeholder={polyglot.t(
            useHours ? 'ASSET_DURATION_IN_HOURS_HELP_TEXT' : 'ASSET_DURATION_HELP_TEXT'
          )}
          defaultValue={
            duration == null
              ? null
              : useHours
                ? getDurationInHoursAndMinutes(duration)
                : duration.toString()
          }
          onChange={e => {
            if (!useHours) {
              setDuration(e);
            } else {
              try {
                setDuration(getMinutesByHoursAndMinutes(e.target.value));
                setBadHours(false);
              } catch (e) {
                setBadHours(true);
              }
            }
          }}
        />
        {invalid ? (
          <div className="form-text text-danger">{polyglot.t('ASSET_DURATION_ERROR')}</div>
        ) : useHours ? (
          <div className={badHours ? 'form-text text-danger' : 'form-text text-muted'}>
            {polyglot.t('ASSET_DURATION_HOURS_EXTRA_HELP')}
          </div>
        ) : null}
      </div>
    </FormGroup>
  );
};

export default DurationEditor;
