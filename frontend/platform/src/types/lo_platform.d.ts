export {};

declare global {
  interface Window {
    lo_platform: any;
    lo_base_url: string;
    lo_static_url: string;
  }
}
