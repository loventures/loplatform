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

import 'codemirror/mode/clike/clike';
import 'codemirror/mode/meta';

import axios from 'axios';
import PropTypes from 'prop-types';
import React from 'react';
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

class ScriptedReport extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      code: props.row.script,
      debug: false,
      loading: false,
      result: null,
    };
  }

  runDebug = () => {
    this.setState({ debug: true, loading: true });
    axios
      .post('/api/v2/jobs/components/loi.cp.script.ScriptedReportImpl/instance/debug', {
        script: this.state.code,
      })
      .then(({ data }) => {
        this.setState({ loading: false, result: JSON.stringify(data, null, 2) });
      })
      .catch(e => {
        console.log(e);
        this.setState({
          loading: false,
          result: e.response.data.throwable + '\n\n' + e.response.data.logs,
        });
      });
  };

  unDebug = () => {
    if (!this.state.loading) this.setState({ debug: false });
  };

  render() {
    const { T } = this.props;
    const { code, debug, loading, result } = this.state;
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
            onClick={this.runDebug}
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
            value={this.state.code}
            className="job-script-editor"
            onChange={code => this.setState({ code })}
            options={options}
          />
        </Col>
        <Modal
          id="modal-job-debug-report"
          isOpen={debug}
          toggle={this.unDebug}
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
              onClick={this.unDebug}
            >
              {T.t('crudTable.modal.closeButton')}
            </Button>
          </ModalFooter>
        </Modal>
      </FormGroup>
    );
  }
}

ScriptedReport.propTypes = {
  T: PropTypes.object.isRequired,
  row: PropTypes.object.isRequired,
  validationErrors: PropTypes.object.isRequired,
};

const validator = form => {
  return { data: { script: form.script } };
};

export default {
  id: 'scriptedReport',
  component: ScriptedReport,
  validator: validator,
};
