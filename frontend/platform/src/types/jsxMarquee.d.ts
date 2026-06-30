import 'react';

// React 19's @types/react dropped the deprecated <marquee> intrinsic element,
// which the non-production announcement bar (announcementBar.tsx) still renders.
// Re-declare it with standard HTML element attributes.
declare module 'react' {
  namespace JSX {
    interface IntrinsicElements {
      marquee: DetailedHTMLProps<HTMLAttributes<HTMLElement>, HTMLElement>;
    }
  }
}
