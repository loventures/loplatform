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

import classnames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Button, FormFeedback, Input, Label, Modal, ModalBody, ModalFooter } from 'reactstrap';
import { bindActionCreators } from 'redux';

import LoPropTypes from '../react/loPropTypes';
import * as MainActions from '../redux/actions/MainActions';
import { getPlatform, login } from '../services';

class Login extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      submitting: false,
      error: null,
      success: false,
    };
  }

  onSubmit = e => {
    e.preventDefault();
    const form = e.target;
    this.setState({ submitting: true, error: null });
    login(form.elements.username.value, form.elements.password.value, false, true)
      .then(response => {
        this.setState({ submitting: false });
        if (response.status === 202) {
          this.setState({ submitting: false, error: response.data.reason });
        } else {
          this.setState({ success: true });
        }
      })
      .catch(() => {
        this.setState({ submitting: false, error: 'UnknownError' });
      });
  };

  onClosed = () => {
    getPlatform(true).then(res => this.props.setLoPlatform(res.data));
  };

  render() {
    const { T } = this.props;
    const { submitting, error, success } = this.state;
    const validProp = error ? { invalid: true } : {};
    return (
      <div className="container-fluid">
        <div>
          <Modal
            id="overlord-login"
            isOpen={!success}
            backdrop="static"
            autoFocus={false}
            backdropClassName="no-backdrop"
            onClosed={this.onClosed}
            fade={false}
          >
            <form
              onSubmit={this.onSubmit}
              autoComplete="off"
              method="POST"
            >
              <ModalBody className={classnames({ 'has-danger': !!error })}>
                <Label for="overlord-login-username">{T.t('overlord.login.username')}</Label>
                <Input
                  id="overlord-login-username"
                  name="username"
                  bsSize="lg"
                  {...validProp}
                  className="mb-3"
                  autoComplete="username"
                />
                <Label for="overlord-login-password">{T.t('overlord.login.password')}</Label>
                <Input
                  id="overlord-login-password"
                  type="password"
                  bsSize="lg"
                  {...validProp}
                  name="password"
                  autoComplete="current-password"
                />
                {error && <FormFeedback>{T.t(`error.login.${error}`)}</FormFeedback>}
              </ModalBody>
              <ModalFooter>
                <Button
                  id="overlord-login-submit"
                  color="primary"
                  size="lg"
                  block
                  disabled={submitting}
                >
                  {T.t('overlord.login.action.login')}
                </Button>
              </ModalFooter>
            </form>
          </Modal>
        </div>
      </div>
    );
  }
}

Login.propTypes = {
  lo_platform: LoPropTypes.lo_platform,
  T: LoPropTypes.translations,
  setLoPlatform: PropTypes.func.isRequired,
};

function mapStateToProps(state) {
  return {
    T: state.main.translations,
    lo_platform: state.main.lo_platform,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

export default connect(mapStateToProps, mapDispatchToProps)(Login);
