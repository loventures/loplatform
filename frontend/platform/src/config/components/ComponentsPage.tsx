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

import React, { useEffect, useState } from 'react';
// Vite 8 CJS interop: unwrap the default export of this deep CJS module.
import FormModule from 'react-jsonschema-form/lib/components/Form';
const Form = FormModule.default ?? FormModule;
import { useDispatch } from 'react-redux';
import { Col, Row } from 'reactstrap';

import { setLoPlatform } from '../../redux/actions/MainActions';
import { getPlatform } from '../../services';
import * as configApi from '../configApi';
import { Schemata } from '../configApi';
import fields from './fields/index';
import LabellingTemplate from './misc/LabellingTemplate';
import SchemaDropdown from './SchemaDropdown';
import widgets from './widgets/index';

interface ComponentsPageProps {
  item?: number;
  path: string;
  schema: string;
  schemata: Schemata;
  search?: string;
}

const ComponentsPage = ({ item, path, schema, schemata, search }: ComponentsPageProps) => {
  const dispatch = useDispatch();
  const [currentSchema, setCurrentSchema] = useState('');
  const [currentDefaults, setCurrentDefaults] = useState<any>({});
  const [currentOverrides, setCurrentOverrides] = useState<any>({});
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    configApi
      .getConfig(schema, item)
      .then(res => {
        setCurrentSchema(schema);
        setCurrentDefaults(res.data.defaults);
        setCurrentOverrides(res.data.overrides);
        setSaved(false);
      })
      .catch((err: any) => {
        window.alert(`error getting configuration for ${schema}: ${err.text}`); // shame
      });
  }, [schema, item]);

  const refreshFormData = (ev: { formData: any }) => {
    setCurrentOverrides(ev.formData);
    setSaved(false);
  };

  const submitData = () => {
    const jefreshPlatform = () =>
      getPlatform(true).then(({ data }: { data: any }) =>
        dispatch(setLoPlatform(data))
      );

    /* if we send up a raw string it doesn't get stringified and therefore looks like bad json
     * if we send up nothing it is weird --m */
    const adapted =
      typeof currentOverrides === 'undefined' ? 'null' : JSON.stringify(currentOverrides);
    configApi
      .putConfig(currentSchema, item, adapted)
      .then(() => {
        setSaved(true);
        jefreshPlatform();
      })
      .catch((err: any) => {
        window.alert(
          `error setting configuration on ${currentSchema}:\n${
            err.response?.data?.message ?? err.message
          }`
        ); // shame
      });
  };

  const ctx = {
    defaults: currentDefaults,
  };

  const form = currentSchema && (
    <React.Fragment>
      <Form
        id={`schema-form-${currentSchema}`}
        schema={schemata[currentSchema]}
        className="form form-inline schema-form"
        formData={currentOverrides}
        formContext={ctx}
        FieldTemplate={LabellingTemplate}
        widgets={widgets}
        fields={fields}
        noValidate={true}
        onChange={refreshFormData}
      >
        {saved ? (
          <button
            id="config-saved-alert"
            className="btn btn-success"
            disabled
          >
            Saved!
          </button>
        ) : (
          <button
            id="config-save-btn"
            className="btn btn-success"
            type="submit"
            onClick={submitData}
          >
            Save
          </button>
        )}
      </Form>
    </React.Fragment>
  );

  const singular = Object.keys(schemata).length === 1;

  return (
    <Row>
      {!singular && (
        <Col sm={3}>
          <SchemaDropdown
            path={path}
            search={search}
            schemata={schemata}
            current={currentSchema}
          />
        </Col>
      )}
      <Col sm={singular ? 12 : 9}>
        {form}
        <div className="preview">
          {currentSchema && (
            <pre
              id="config-pre"
              className="json-value"
            >
              {JSON.stringify(currentOverrides, null, 2)}
            </pre>
          )}
        </div>
      </Col>
    </Row>
  );
};

export default ComponentsPage;
