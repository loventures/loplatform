declare module '*.scss';
declare module '*.css';

declare module '@bprogress/core/css';

declare module 'browser-md5-file' {
  export default class BMF {
    md5(
      file: Blob,
      callback: (err: Error | null, md5: string) => void,
      progress?: (progress: number) => void
    ): void;
  }
}
