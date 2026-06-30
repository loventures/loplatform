import { LoPlatform } from 'loPlatform.ts';

export {};

declare global {
  interface Window {
    lo_platform: LoPlatform;
    lo_base_url: string;
    lo_static_url: string;
    lo_error_file?: string;
    lo_error_title?: string;
    lo_error_body?: string;
    locale?: string;
  }
}
