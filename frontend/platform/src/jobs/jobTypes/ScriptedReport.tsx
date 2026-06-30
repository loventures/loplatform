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

import 'codemirror/mode/clike/clike';
import 'codemirror/mode/meta';

import axios from 'axios';
import React, { useState } from 'react';
import CodeMirror from 'react-codemirror';
import {
  Button,
  Col,
  FormGroup,
  Input,
  Label,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from 'reactstrap';

import WaitDotGif from '../../components/WaitDotGif';
import { JobTypeProps, JobValidator } from './index';

const ScriptedReport: React.FC<JobTypeProps> = ({ T, row }) => {
  const [code, setCode] = useState<string>(row.script);
  const [debug, setDebug] = useState(false);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<string | null>(null);

  const runDebug = () => {
    setDebug(true);
    setLoading(true);
    axios
      .post('/api/v2/jobs/components/loi.cp.script.ScriptedReportImpl/instance/debug', {
        script: code,
      })
      .then(({ data }) => {
        setLoading(false);
        setResult(JSON.stringify(data, null, 2));
      })
      .catch(e => {
        console.log(e);
        setLoading(false);
        setResult(e.response.data.throwable + '\n\n' + e.response.data.logs);
      });
  };

  const unDebug = () => {
    if (!loading) setDebug(false);
  };

  const options = {
    lineNumbers: true,
    mode: 'text/x-scala',
  };
  return (
    <FormGroup
      row
      className="is-required"
    >
      <Col lg={2}>
        <div>
          <Label
            id="script-label"
            for="job-script"
          >
            {T.t(`adminPage.jobs.fieldName.scriptReport.script`)}
          </Label>
        </div>
        <Button
          id="job-debug-button"
          size="sm"
          color="outline-success"
          onClick={runDebug}
        >
          <i
            className="material-icons md-18"
            aria-hidden="true"
          >
            {'play_arrow'}
          </i>
        </Button>
      </Col>
      <Col lg={10}>
        <Input
          type="hidden"
          name="script"
          value={code}
        />
        <CodeMirror
          value={code}
          className="job-script-editor"
          onChange={code => setCode(code)}
          options={options}
        />
      </Col>
      <Modal
        id="modal-job-debug-report"
        isOpen={debug}
        toggle={unDebug}
        size="lg"
      >
        <ModalHeader tag="h2">Debug</ModalHeader>
        <ModalBody>
          {loading ? (
            <WaitDotGif
              color="dark"
              size={48}
            />
          ) : (
            <pre
              id="job-debug-report"
              className="my-0"
            >
              {result}
            </pre>
          )}
        </ModalBody>
        <ModalFooter>
          <Button
            id="job-debug-modal-close-btn"
            disabled={loading}
            onClick={unDebug}
          >
            {T.t('crudTable.modal.closeButton')}
          </Button>
        </ModalFooter>
      </Modal>
    </FormGroup>
  );
};

const validator: JobValidator = form => {
  return { data: { script: form.script } };
};

export default {
  id: 'scriptedReport',
  component: ScriptedReport,
  validator: validator,
};
