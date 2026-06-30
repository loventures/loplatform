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

import Polyglot from 'node-polyglot';
import React from 'react';
import { Button, Col, FormGroup, Input, Label } from 'reactstrap';

import WaitDotGif from '../components/WaitDotGif';

interface LogFormProps {
  inputType: string;
  value?: string;
  type: string;
  onInputChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onDownloadClick: () => void;
  downloading: boolean;
  T: Polyglot;
  inputChildren?: React.ReactNode;
}

const LogForm: React.FC<LogFormProps> = ({
  type,
  value,
  onInputChange,
  onDownloadClick,
  downloading,
  T,
  inputChildren,
  inputType,
}) => {
  const valueProp = value !== null ? { value } : {};
  const id = `logs-${type}`;
  return (
    <div>
      <h4
        className="logs-header"
        id={`${id}-header`}
      >
        {T.t(`adminPage.logs.download.by.${type}`)}
      </h4>
      <FormGroup row>
        <Label
          className="logs-label"
          id={`${id}-label`}
          for={id}
          sm={2}
        >
          {T.t(`adminPage.logs.download.by.${type}.label`)}
        </Label>
        <Col sm={4}>
          <Input
            id={id}
            type={inputType as any}
            {...valueProp}
            onChange={onInputChange}
          >
            {inputChildren}
          </Input>
        </Col>
        <Col sm={2}>
          <Button
            color="primary"
            id={`${id}-download`}
            onClick={onDownloadClick}
          >
            {T.t('adminPage.logs.download.btn')}
            {downloading && (
              <WaitDotGif
                className="ms-2 waiting"
                color="light"
                size={16}
              />
            )}
          </Button>
        </Col>
      </FormGroup>
      <hr className="logs-hr" />
    </div>
  );
};

export default LogForm;
