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

import PropTypes from 'prop-types';
import React, { Fragment, useState } from 'react';
import { connect } from 'react-redux';
import { Button, Col, Modal, ModalBody, ModalHeader, Row } from 'reactstrap';
import { bindActionCreators } from 'redux';

import * as MainActions from '../redux/actions/MainActions';
import DomainInfo from './01Domain';
import DomainAppearance from './02Appearance';
import AdminAccount from './03Account';
import Status from './04Status';

const App = ({ T }) => {
  const [errors, setErrors] = useState([]);
  const [step, setStep] = useState(1);
  const [domainInfo, setDomainInfo] = useState({});
  const [appearance, setAppearance] = useState({});
  const [account, setAccount] = useState({});

  const comps = [
    DomainInfo(T, setDomainInfo),
    DomainAppearance(T, setErrors, setAppearance),
    AdminAccount(T, setAccount),
    Status(T, step === 4, { domain: domainInfo, appearance, account }, () => {
      setStep(1);
      setErrors([T.t('adminPage.provision.error.generic')]);
    }),
  ];

  const clearErrors = () => setErrors([]);

  let proceed = () => Promise.resolve().then(() => setStep(step + 1));

  const comp = comps[step - 1];
  const advance = () => comp.props.updateData().then(proceed).catch(setErrors);

  return (
    <Fragment>
      <Col className="d-flex flex-column align-items-center provision-container">
        <Row>{comp}</Row>
        <Row className="justify-content-end pe-3">
          <Button
            hidden={step > 3}
            color="primary"
            onClick={advance}
          >
            {T.t(`adminPage.provision.step${step}.button`)}
          </Button>
        </Row>
      </Col>
      <Modal
        id="error-dialog"
        className="provision-modal error modal-dialog-centered"
        backdropClassName="provision-error-bd"
        isOpen={errors.length > 0}
        onClosed={clearErrors}
      >
        <ModalHeader toggle={clearErrors}>{T.t(`adminPortal.error.title`)}</ModalHeader>
        <ModalBody>
          <ul>
            {errors.map((e, i) => (
              <li key={`error-${i}`}>{e}</li>
            ))}
          </ul>
        </ModalBody>
      </Modal>
    </Fragment>
  );
};

App.propTypes = {
  T: PropTypes.object.isRequired,
};

const mapStateToProps = state => {
  return {
    T: state.main.translations,
  };
};

const mapDispatchToProps = dispatch => bindActionCreators(MainActions, dispatch);

export default connect(mapStateToProps, mapDispatchToProps)(App);
