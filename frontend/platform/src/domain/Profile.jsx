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
import PropTypes from 'prop-types';
import React from 'react';
import Dropzone from 'react-dropzone';
import { connect } from 'react-redux';
import { Button, Col, Row } from 'reactstrap';

import WaitDotGif from '../components/WaitDotGif';
import EtcLoading from '../etc/EtcLoading';
import LoPropTypes from '../react/loPropTypes';
import { ContentTypeMultipart, ContentTypeURLEncoded } from '../services';

const uploadsUrl = '/api/v2/uploads';
const selfImageUrl = '/api/v2/users/self/image';

class Profile extends React.Component {
  state = {
    loaded: true,
    submitting: false,
    guid: null,
  };

  onDrop = files => {
    if (files && files.length) {
      const formData = new FormData();
      formData.append('upload', files[0]);
      axios
        .post(uploadsUrl, formData, ContentTypeMultipart)
        .then(r => {
          const data = r.data;
          const newState = { value: data, guid: data.guid };
          this.setState(newState);
        })
        .catch(e => {
          console.log(e);
        });
    }
  };

  onSubmit = () => {
    const { guid } = this.state;
    const data = 'image=' + encodeURIComponent(guid);
    this.setState({ submitting: true });
    axios
      .post(selfImageUrl, data, ContentTypeURLEncoded)
      .then(() => document.location.reload())
      .catch(e => {
        console.log(e);
      });
  };

  onCancel = () => {
    this.setState({ guid: null });
  };

  render() {
    const {
      state: { loaded, guid, submitting },
      props: { T },
    } = this;
    if (!loaded) {
      return <EtcLoading />;
    } else {
      return (
        <div
          id="profile-page"
          className="container"
        >
          <Row>
            <Col>
              <h3 className="block-header row mt-md-5">{T.t('page.profile.profileImage')}</h3>
              {!guid && (
                <Dropzone
                  multiple={false}
                  accept={{
                    'image/png': ['.png'],
                    'image/jpeg': ['.jpeg', '.jpg'],
                    'text/svg+xml': ['.svg'],
                  }}
                  onDrop={this.onDrop}
                >
                  {({ getRootProps, getInputProps }) => (
                    <div className="drop-zone">
                      <div {...getRootProps()}>
                        <input {...getInputProps()} />
                        <p>Drop an image here, or click to select one.</p>
                      </div>
                    </div>
                  )}
                </Dropzone>
              )}
              {!!guid && (
                <div>
                  <img
                    className="profile-image"
                    src={`/api/v2/uploads/${guid}`}
                  />
                </div>
              )}
              <div className="mt-3">
                <Button
                  id="profile-image-cancel-btn"
                  disabled={submitting || !guid}
                  onClick={this.onCancel}
                >
                  {T.t('page.profile.cancel')}
                </Button>{' '}
                <Button
                  id="react-table-submit-modal-btn"
                  type="submit"
                  color="primary"
                  disabled={submitting || !guid}
                  onClick={this.onSubmit}
                >
                  {T.t(`page.profile.submit`)}
                  {submitting && (
                    <WaitDotGif
                      className="ms-2 waiting"
                      color="light"
                      size={16}
                    />
                  )}
                </Button>
              </div>
            </Col>
          </Row>
        </div>
      );
    }
  }
}

Profile.propTypes = {
  T: LoPropTypes.translations,
  lop: PropTypes.object.isRequired,
};

function mapStateToProps(state) {
  return {
    T: state.main.translations,
    lop: state.main.lo_platform,
  };
}

export default connect(mapStateToProps, null)(Profile);
