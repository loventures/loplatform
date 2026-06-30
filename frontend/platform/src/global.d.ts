/// <reference types="vite/client" />

declare const process: {
  env: {
    APP: string;
    DEBUG?: string;
  };
};
