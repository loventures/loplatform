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

import Papa from 'papaparse';

import { WriteOp } from '../types/api';

type CsvMeta = { fields: string[] };

export type ImportError = { rowNum: number; err: string } & Record<string, string | number>;
export type ImportResult = {
  ops: WriteOp[];
  errors: ImportError[];
  warnings?: ImportError[];
  added?: number;
  removed?: number;
};

export const generateImportWriteOps = (
  file: File,
  requiredHeaders: Record<string, string>, // friendly name to developer name
  optionalHeaders: Record<string, string>,
  validateRows: (data: Record<string, string>[]) => ImportResult
): Promise<ImportResult> => {
  return readFileAsText(file)
    .then(text => {
      const { data, meta } = generateRowObjects(text, requiredHeaders, optionalHeaders);
      return processRows(data, meta, requiredHeaders, optionalHeaders, validateRows);
    })
    .catch(e => {
      console.error(e);
      throw e;
    });
};

const readFileAsText = (file: File) =>
  new Promise<string>((resolve, reject) => {
    const fr = new FileReader();
    fr.readAsText(file);
    fr.onload = e => resolve(e.target.result as string);
    fr.onerror = e => reject(e);
  });

const generateRowObjects = (
  csvString: string,
  requiredHeaders: Record<string, string>,
  optionalHeaders: Record<string, string>
) => {
  const parsingOptions = {
    skipEmptyLines: true,
    header: true,
    transformHeader: h => requiredHeaders[h] ?? optionalHeaders[h] ?? `invalid-${h}`,
  };

  return Papa.parse(csvString, parsingOptions);
};

// shamefully exposed for shameful tests
export const processRows = (
  data: Record<string, string>[],
  meta: CsvMeta,
  requiredHeaders: Record<string, string>,
  optionalHeaders: Record<string, string>,
  validateRows: (data: Record<string, string>[]) => ImportResult
): ImportResult => {
  const badOrMissing = [
    ...validateBadHeaders(meta),
    ...validateMissingHeaders(meta, requiredHeaders),
  ];
  if (badOrMissing.length > 0) {
    return { ops: [], errors: badOrMissing };
  } else if (!data.length) {
    return { ops: [], errors: [{ err: 'IMPORT_MODAL.NO_DATA', rowNum: 1 }] };
  } else {
    return validateRows(data);
  }
};

const validateBadHeaders = (meta: CsvMeta): ImportError[] => {
  const badHeaders = meta.fields.filter(h => h.indexOf('invalid-') !== -1).map(h => h.slice(8));
  return badHeaders.length > 0
    ? [
        {
          rowNum: 1,
          err: 'IMPORT_MODAL.BAD_COLUMN_HEADERS',
          inputValue: badHeaders.join(', '),
        },
      ]
    : [];
};

const validateMissingHeaders = (
  meta: CsvMeta,
  requiredHeaders: Record<string, string>
): ImportError[] => {
  const missingHeaders = Object.entries(requiredHeaders)
    .map(([key, value]) => {
      if (!meta.fields.includes(value)) {
        return key;
      }
    })
    .filter(v => !!v);
  return missingHeaders.length > 0
    ? [
        {
          rowNum: 1,
          err: 'IMPORT_MODAL.MISSING_COLUMN_HEADERS',
          inputValue: missingHeaders.join(', '),
        },
      ]
    : [];
};
