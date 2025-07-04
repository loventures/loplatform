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

import { get, set } from 'lodash';
import { ChangeEvent, useCallback, useEffect, useState } from 'react';

import AssetInEditService from '../editor/AssetInEditService';
import useDcmSelector from './useDcmSelector';

type Option<A = any> = { value: A; label: string };

interface FormProps<T> {
  value: T;
  onChange: (a: ChangeEvent<InputElement> | T | Option<T>) => void;
}

type InputElement = HTMLInputElement | HTMLTextAreaElement;

function isEvent(e): e is ChangeEvent<InputElement> {
  return e?.target instanceof HTMLElement;
}

function isCheckbox(e): e is ChangeEvent<HTMLInputElement> {
  return e?.target?.type.toLowerCase() === 'checkbox';
}

function isNumeric(e): e is ChangeEvent<HTMLInputElement> {
  return e?.target?.type.toLowerCase() === 'number';
}

function isSelect(e): e is Option {
  return e != null && typeof e === 'object' && 'label' in e && 'value' in e;
}

export const useAssetDataWithDep = <T = string>(
  dependencies: any[],
  ...path: string[]
): FormProps<T> => {
  const { assetNode: asset, dirty } = useDcmSelector(state => state.assetEditor);

  const dataPath = ['data', ...path];

  const [value, setValue] = useState<T>(get(asset, dataPath));

  useEffect(() => {
    setValue(get(asset, dataPath));
  }, dependencies ?? []);

  const onChange = useCallback(
    updatedValue => {
      if (isEvent(updatedValue)) {
        if (isCheckbox(updatedValue)) {
          /** NOTE: we narrow to HTMLInputElement because textarea does not have "checked" */
          updatedValue = updatedValue.target.checked;
        } else if (isNumeric(updatedValue)) {
          updatedValue = updatedValue.target.valueAsNumber;
          if (isNaN(updatedValue)) {
            updatedValue = null;
          }
        } else {
          updatedValue = updatedValue.target.value;
        }
      } else if (isSelect(updatedValue)) {
        updatedValue = updatedValue.value;
      }
      const trimmedValue = typeof updatedValue === 'string' ? updatedValue.trim() : updatedValue;
      set(asset, dataPath, trimmedValue);
      AssetInEditService.updateAsset(asset, !dirty);
      setValue(updatedValue);
    },
    [asset, dataPath, dirty]
  );

  return { value, onChange };
};
export const useAssetDataField = <T = string>(...path: string[]): FormProps<T> => {
  const { assetNode: asset, dirty } = useDcmSelector(state => state.assetEditor);

  const dataPath = ['data', ...path];

  const [value, setValue] = useState<T>(get(asset, dataPath));

  const onChange = useCallback(
    updatedValue => {
      if (isEvent(updatedValue)) {
        if (isCheckbox(updatedValue)) {
          /** NOTE: we narrow to HTMLInputElement because textarea does not have "checked" */
          updatedValue = updatedValue.target.checked;
        } else if (isNumeric(updatedValue)) {
          updatedValue = updatedValue.target.valueAsNumber;
          if (isNaN(updatedValue)) {
            updatedValue = null;
          }
        } else {
          updatedValue = updatedValue.target.value;
        }
      } else if (isSelect(updatedValue)) {
        updatedValue = updatedValue.value;
      }
      set(asset, dataPath, updatedValue);
      AssetInEditService.updateAsset(asset, !dirty);
      setValue(updatedValue);
    },
    [asset, dataPath, dirty]
  );

  return { value, onChange };
};

export default useAssetDataField;
