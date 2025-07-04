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

import { Omit } from '../types/omit.ts';
import React, { useContext } from 'react';
import { lojector } from '../loject.ts';

export interface Translate {
  (key: string): string;
  (key: string, params: Record<string, any>): string;
  (key: string, params: Record<string, any>, format: 'messageformat'): string;
}
export const TranslationContext = React.createContext<Translate>((a: string) => a);

export const WithTranslate = TranslationContext.Consumer;

export type WithTranslateProps = {
  translate: (key: string, params?: any) => string;
};

export class TranslationProvider extends React.Component<React.PropsWithChildren> {
  state = {
    isReady: false,
    translate: (key: string) => key,
  };

  translateService: {
    instant: (key?: string, params?: any) => string;
    onReady(callback: () => void): Promise<any>;
  } = lojector.get('$translate');

  translate = (...args: any[]) => {
    return this.translateService.instant(...args);
  };

  componentDidMount() {
    this.translateService.onReady(() => {
      this.setState({
        isReady: true,
        translate: this.translate,
      });
    });
  }

  render() {
    return (
      <TranslationContext.Provider value={this.state.translate}>
        {this.props.children}
      </TranslationContext.Provider>
    );
  }
}

export const withTranslation = <P extends WithTranslateProps>(
  BaseComponent: React.ComponentType<P>
) => {
  const TranslatedComponent: React.FunctionComponent<Omit<P, keyof WithTranslateProps>> = props => (
    <TranslationContext.Consumer>
      {translate => (
        <BaseComponent
          {...(props as P)}
          translate={translate}
        />
      )}
    </TranslationContext.Consumer>
  );

  TranslatedComponent.displayName = `WithTranslation(${
    BaseComponent.displayName || BaseComponent.name
  })`;

  return TranslatedComponent;
};

//Use this for react2angular components
//because the layer of angular in between
//breaks the provider/consumer hierarchy
export const withTranslationFor2Angular = <P extends WithTranslateProps>(
  BaseComponent: React.ComponentType<P>
) => {
  const TranslatedComponent: React.FunctionComponent<Omit<P, keyof WithTranslateProps>> = props => (
    <TranslationProvider>
      <TranslationContext.Consumer>
        {translate => (
          <BaseComponent
            {...(props as P)}
            translate={translate}
          />
        )}
      </TranslationContext.Consumer>
    </TranslationProvider>
  );

  TranslatedComponent.displayName = `WithTranslationFor2Angular(${
    BaseComponent.displayName || BaseComponent.name
  })`;

  return TranslatedComponent;
};

export const useTranslation = () => useContext(TranslationContext);
