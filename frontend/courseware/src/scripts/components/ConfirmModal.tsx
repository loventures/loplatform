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

import { TranslationContext } from '../i18n/translationContext';
import { isString } from 'lodash';
import React, { useContext, useEffect, useState } from 'react';
import { Alert, Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import LoadingSpinner from '../directives/loadingSpinner';

const ConfirmModal: React.FC<
  {
    isOpen: boolean;
    confirm: (isConfirmed: boolean) => Promise<any>;
  } & React.PropsWithChildren
> = ({ isOpen, confirm, children }) => {
  const translate = useContext(TranslationContext);
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState<any>(null);

  useEffect(() => {
    let isMounted = true;
    if (confirming) {
      confirm(true).then(
        () => {
          if (isMounted) {
            setConfirming(false);
          }
        },
        error => {
          if (isMounted) {
            setConfirming(false);
            setError(error);
          }
          throw error;
        }
      );
    }
    return () => {
      isMounted = false;
    };
  }, [confirming]);

  return (
    <Modal
      isOpen={isOpen}
      toggle={() => {
        if (!confirming) {
          setError(null);
          confirm(false);
        }
      }}
    >
      <ModalHeader>{translate('CONFIRM_MODAL_HEADER')}</ModalHeader>
      <ModalBody>{children}</ModalBody>
      <ModalFooter>
        <Button
          color="primary"
          disabled={confirming}
          onClick={() => {
            setConfirming(true);
            setError(null);
          }}
        >
          {confirming ? <LoadingSpinner /> : translate('CONFIRM_MODAL_CONFIRM')}
        </Button>
        <Button
          color="secondary"
          disabled={confirming}
          onClick={() => {
            setError(null);
            confirm(false);
          }}
        >
          {translate('CONFIRM_MODAL_CANCEL')}
        </Button>
      </ModalFooter>
      {error && (
        <Alert
          className="mx-3"
          color="danger"
        >
          {isString(error) ? <span>{error}</span> : translate('GENERIC_LOADING_ERROR')}
        </Alert>
      )}
    </Modal>
  );
};

export default ConfirmModal;
