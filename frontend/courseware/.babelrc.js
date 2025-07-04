
/**
 * Currently this is only used for IDEs, webpack configs have a copy of this config.
 * */
module.exports = {
  "plugins": [
    '@babel/plugin-proposal-class-properties',
    '@babel/plugin-proposal-nullish-coalescing-operator',
    '@babel/plugin-proposal-numeric-separator',
    '@babel/plugin-proposal-optional-chaining',
  ],
  "presets": [
    "@babel/preset-react",
    "@babel/preset-typescript",
    ["@babel/preset-env", {
      "modules": false,
      "targets": {
        "ie": "11"
      }
    }]
  ]
};