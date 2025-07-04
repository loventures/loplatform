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

import axios from 'axios';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { Button, Col, FormFeedback, FormGroup, FormText, Input, Label } from 'reactstrap';
import _ from 'underscore';

import { ContentTypeMultipart } from '../../services';
import { formatSize } from '../../services/formatSize';
import WaitDotGif from '../WaitDotGif';

const uploadsUrl = '/api/v2/uploads';

class AdminFormFile extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: this.props.value,
      guid: '',
      error: null,
      uploading: false,
    };
  }

  clearForm = () => {
    this.props.onClear();
    this.setState({ value: null, guid: 'remove', error: null });
  };

  resetForm = () => {
    this.props.onReset();
    this.setState({ value: this.props.value, guid: '', error: null });
  };

  // inexplicably in the production bundle the event that gets passed is the
  // string name of the file and nothing else so we can't use e.target.files
  onChange = () => {
    const { entity, field, onChange: cb, noUpload } = this.props;
    const id = `${entity}-${field}`;
    const target = document.getElementById(id);
    if (noUpload) return cb(target.files[0]);
    const formData = new FormData();
    formData.append('upload', target.files[0]);
    const startState = { error: null, uploading: true };
    this.setState(startState);
    if (cb) cb(startState);
    axios
      .post(uploadsUrl, formData, ContentTypeMultipart)
      .then(r => {
        const data = r.data;
        const newState = { value: data, guid: data.guid, uploading: false };
        if (cb) cb(newState);
        this.setState(newState);
      })
      .catch(e => {
        console.log(e);
        const newState = {
          error: this.props.T.t('adminForm.file.error.uploadFile'),
          uploading: false,
        };
        if (cb) cb(newState);
        this.setState(newState);
        target.value = '';
      });
  };

  formatFileInfo = (file, T) => {
    const params = {
      mimeType: file.mimeType,
      size: formatSize(file.size, T),
      dimensions: T.t('format.dimensions.pixels', file),
    };
    return T.t(
      file.width && file.height ? 'adminForm.file.imageInfo' : 'adminForm.file.fileInfo',
      params
    );
  };

  render() {
    const {
      accept,
      entity,
      field,
      fieldUrl,
      help,
      invalid,
      label,
      labelWidth,
      required,
      image,
      T,
    } = this.props;
    const { value, guid, error, uploading } = this.state;
    const id = `${entity}-${field}`;
    const url = guid ? `/api/v2/uploads/${guid}` : `${fieldUrl}/view`;
    const changed = !!guid;
    const problem = error || invalid;
    const acceptProp = !_.isEmpty(accept) ? { accept: accept.join(',') } : {};
    const name = T.t(`adminPage.${entity}.fieldName.${field}`);
    return (
      <FormGroup
        row
        className={classNames({ 'has-danger': problem, 'is-required': required })}
      >
        {label && (
          <Label
            id={id + '-label'}
            lg={labelWidth}
            for={id}
          >
            {name}
          </Label>
        )}
        <Col lg={label ? 12 - labelWidth : 12}>
          <Input
            type="hidden"
            name={field + 'Upload'}
            value={this.state.guid}
          />
          {value && (
            <div className="upload-file-current mb-2">
              {image && (
                <img
                  className="upload-file-preview mb-2"
                  src={url}
                  alt=""
                />
              )}
              <span className="upload-file-info">
                <a
                  id={id + '-fileName'}
                  className="upload-file-fileName"
                  href={url + '?download=true'}
                  title={T.t('adminForm.file.downloadFile', { name })}
                >
                  {value.fileName}
                </a>
                <span
                  id={id + '-fileInfo'}
                  className="upload-file-mimeType ms-1 me-2"
                >
                  {this.formatFileInfo(value, T)}
                </span>
              </span>
              {changed ? (
                <Button
                  id={id + '-reset'}
                  size="sm"
                  onClick={this.resetForm}
                  title={T.t('adminForm.file.resetFile', { name })}
                >
                  {T.t('adminForm.file.action.reset')}
                </Button>
              ) : (
                <Button
                  id={id + '-remove'}
                  size="sm"
                  onClick={this.clearForm}
                  title={T.t('adminForm.file.removeFile', { name })}
                >
                  {T.t('adminForm.file.action.remove')}
                </Button>
              )}
            </div>
          )}
          {changed && !value && (
            <div>
              <span className="upload-file-removeLabel me-2">
                {T.t('adminForm.file.label.removeThisFile')}
              </span>
              <Button
                id={id + '-reset'}
                size="sm"
                onClick={this.resetForm}
              >
                Reset
              </Button>
            </div>
          )}
          {!changed && (
            <div className="upload-file-new position-relative">
              {uploading && (
                <WaitDotGif
                  color="muted"
                  size={16}
                  style={{ position: 'absolute', top: '15px', right: '11px', zIndex: '1' }}
                />
              )}
              <Input
                className="upload-file-input"
                type="file"
                id={id}
                name={field}
                onChange={this.onChange}
                {...acceptProp}
                label={T.t('adminForm.file.uploadFile', { name })}
                aria-required={required}
              />
            </div>
          )}
          {problem && (
            <FormFeedback
              id={id + '-problem'}
              style={{ display: 'block' }}
            >
              {problem}
            </FormFeedback>
          )}
          {help && <FormText id={id + '-help'}>{help}</FormText>}
        </Col>
      </FormGroup>
    );
  }
}

AdminFormFile.propTypes = {
  defaultUrl: PropTypes.string,
  entity: PropTypes.string,
  field: PropTypes.string,
  image: PropTypes.bool,
  accept: PropTypes.array,
  label: PropTypes.bool,
  labelWidth: PropTypes.number.isRequired,
  help: PropTypes.oneOfType([PropTypes.string, PropTypes.element]),
  fieldUrl: PropTypes.string,
  invalid: PropTypes.string,
  onChange: PropTypes.func,
  noUpload: PropTypes.bool,
  removable: PropTypes.bool,
  required: PropTypes.bool,
  value: PropTypes.object,
  T: PropTypes.shape({
    t: PropTypes.func,
  }),
};

const noop = () => '';
AdminFormFile.defaultProps = {
  label: true,
  labelWidth: 2,
  onChange: noop,
  onClear: noop,
  onReset: noop,
  removable: true,
};

export default AdminFormFile;
