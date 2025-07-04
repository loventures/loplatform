
const tsRecommended = require('@typescript-eslint/eslint-plugin/dist/configs/recommended.js');

module.exports = {
  parser: '@babel/eslint-parser',
  extends: [
    'eslint-config-lo/react',
    'plugin:react/recommended',
    'prettier'
  ],
  plugins: [
    'react',
    'redux-constants',
    'import',
    // 'react-hooks',
    '@typescript-eslint',
  ],
  env: {
    es6: true,
    browser: true,
    node: true,
    commonjs: true
  },
  globals: {
    fetch: true,
    require: true
  },
  parserOptions: {
    ecmaVersion: 11,
    ecmaFeatures: {
      jsx: true,
    }
  },
  rules: {
    'no-func-assign': 0,
    'no-irregular-whitespace': 0,
    'valid-jsdoc': 0,
    'accessor-pairs': 0,
    'block-scoped-var': 0,
    'consistent-return': 0,
    'default-case': 0,
    'dot-location': 0,
    'no-implicit-coercion': 0,
    'no-invalid-this': 0,
    'no-loop-func': 0,
    'radix': 1,
    'vars-on-top': 0,
    'no-undef-init': 1,
    'no-undefined': 0,
    'no-unused-vars': 2,
    'array-bracket-spacing': [0, 'always'],
    'redux-constants/redux-constants': 2,

    'react/display-name': 0,
    'react/no-direct-mutation-state': 2,

    'no-console': 'off',
    'no-warning-comments': 'off',
    'complexity': 'off',
    'eqeqeq': 2,
    'no-unneeded-ternary': 2,
    'no-var': 2,
    'react/prop-types': 'off',
    'react/jsx-no-comment-textnodes': 2,
    'no-prototype-builtins': 'off'
  },
  overrides: [{
    files: ['**/*.ts', '**/*.tsx'],
    parser: '@typescript-eslint/parser',
    rules: {
      ...tsRecommended.rules,
      '@typescript-eslint/no-explicit-any': 'off',
      '@typescript-eslint/explicit-function-return-type': 'off',

      // these are already handled by TS
      'no-undef': 'off',
      'no-undefined': 'off',
      'no-redeclare': 'off',
      'import/named': 'off',
      'import/no-unresolved': 'off',
      'import/no-named-as-default': 'off',
      'import/no-named-as-default-member': 'off',
      '@typescript-eslint/no-unused-vars': 'off',
      // 'this rule is dumb. If I wanted the null safety I wouldn’t have used it in the first place' - wmao
      '@typescript-eslint/no-non-null-assertion': 'off',
      // doesn't work for `import * as foo from 'foo'` usages
      'import/no-duplicates': 'off',
      // noop is a thing.
      '@typescript-eslint/no-empty-function': 'off',
      // progressive typing is a thing we need so let ts-ignore exist
      '@typescript-eslint/ban-ts-comment': 'warn',
    },
  },],
  settings: {
    react: {
      version: 'detect'
    }
  }
};
