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
import cookies from 'browser-cookies';
import Course from '../../bootstrap/course';
import { FadeInLoader } from '../../directives/loadingSpinner/FadeInLoader';
import { withTranslation } from '../../i18n/translationContext';
import React, { useRef, useState } from 'react';
import {
  Button,
  Card,
  CardBody,
  CardHeader,
  Col,
  Form,
  FormGroup,
  Input,
  Label,
  Row,
} from 'reactstrap';
import { lifecycle, withHandlers, withState } from 'recompose';
import { compose } from 'redux';

import NumberField from '../../components/forms/NumberField';

const gradingPolicies = [
  'Highest',
  'FirstAttempt',
  'MostRecent',
  'Average',
  'FullCreditOnCompletion',
];

const AssessmentPoliciesEditor = ({ translate, policies, saveUpdatedPolices }: any) => {
  const initialValue = useRef(policies);
  const [value, setValue] = useState(policies);
  const [isSubmitting, setSubmitting] = useState(false);
  const dirty = value !== initialValue.current;

  return (
    <Form
      onSubmit={e => {
        e.preventDefault();
        saveUpdatedPolices(value, {
          setSubmitting,
          resetForm: v => {
            initialValue.current = v;
            setValue(v);
          },
        });
      }}
    >
      {value.map((policy: any, idx: number) => (
        <Row
          form
          key={policy.assessmentType}
          className="policy-editor"
        >
          <legend>{translate(`ASSESSMENT_POLICY_${policy.assessmentType}`)}</legend>
          <Col md="5">
            <Row form>
              <Col>
                <FormGroup>
                  <NumberField
                    id={`policy-${idx}-limit`}
                    value={policy.attemptLimit}
                    disabled={policy.unlimited}
                    onChange={e =>
                      setValue(
                        value.map((v, i) => ({
                          ...v,
                          ...(i === idx ? { attemptLimit: e.target.valueAsNumber } : {}),
                        }))
                      )
                    }
                    label={translate('ASSESSMENT_POLICY_EDIT_ATTEMPT_LIMIT')}
                  />
                </FormGroup>
              </Col>
            </Row>
            <Row
              form
              className="mb-4"
            >
              <Col>
                <FormGroup check>
                  <Label check>
                    <Input
                      type="checkbox"
                      checked={!!policy.unlimited}
                      onChange={e =>
                        setValue(
                          value.map((v, i) => ({
                            ...v,
                            ...(i === idx ? { unlimited: e.target.checked } : {}),
                          }))
                        )
                      }
                    />
                    {translate('ASSESSMENT_POLICY_INFINITE_ATTEMPTS')}
                  </Label>
                </FormGroup>
              </Col>
            </Row>
          </Col>
          <Col md="7">
            <FormGroup>
              <Label>{translate('ASSESSMENT_POLICY_GRADING_STRATEGY')}</Label>
              <Input
                type="select"
                value={policy.assessmentGradingPolicy}
                onChange={e =>
                  setValue(
                    value.map((v, i) => ({
                      ...v,
                      ...(i === idx ? { assessmentGradingPolicy: e.target.value } : {}),
                    }))
                  )
                }
              >
                {gradingPolicies.map(strategy => (
                  <option value={strategy}>{translate(`ASSESSMENT_POLICY_${strategy}`)}</option>
                ))}
              </Input>
            </FormGroup>
          </Col>
        </Row>
      ))}
      <Button
        type="submit"
        color="primary"
        disabled={isSubmitting || !dirty}
      >
        {translate('SAVE')}
      </Button>
    </Form>
  );
};

const AssessmentPolicies = (props: any) => (
  <Card id="course-assessment-policies">
    <CardHeader>{props.translate('ASSESSMENT_POLICY_HEADER')}</CardHeader>
    <CardBody>
      {props.loading ? <FadeInLoader message="LOADING" /> : <AssessmentPoliciesEditor {...props} />}
    </CardBody>
  </Card>
);

export default compose<React.ComponentType>(
  withTranslation,
  withState('loading', 'setLoading', true),
  withState('policies', 'setPolicies', []),
  lifecycle({
    componentDidMount() {
      const { setPolicies, setLoading } = this.props as any;
      // loConfig.instructorCustomization.assessmentPolicies
      axios
        .get(`/api/v2/contentConfig/${Course.id}/assessmentPolicies`, {
          headers: { 'X-CSRF': cookies.get('CSRF')! },
        })
        .then(policies => {
          setPolicies(
            policies.data.objects.map((raw: any) => {
              return {
                ...raw,
                unlimited: raw.attemptLimit === null,
                attemptLimit: raw.attemptLimit || 1,
              };
            })
          );
          setLoading(false);
        });
    },
  }),
  withHandlers({
    saveUpdatedPolices:
      () =>
      (policies: any, { setSubmitting, resetForm }: any) => {
        const policyDto = policies.map((p: any) => {
          return {
            assessmentGradingPolicy: p.assessmentGradingPolicy,
            attemptLimit: p.unlimited ? null : p.attemptLimit,
            assessmentType: p.assessmentType,
          };
        });
        axios
          .put(`/api/v2/contentConfig/${Course.id}/assessmentPolicies`, policyDto, {
            headers: { 'X-CSRF': cookies.get('CSRF')! },
          })
          .then(() => {
            resetForm(
              policyDto.map((raw: any) => {
                return {
                  ...raw,
                  unlimited: raw.attemptLimit === null,
                  attemptLimit: raw.attemptLimit || 1,
                };
              })
            );
          })
          .catch(() => {
            setSubmitting(false);
          });
      },
  })
)(AssessmentPolicies);
