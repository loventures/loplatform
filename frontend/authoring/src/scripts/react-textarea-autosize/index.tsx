import * as React from 'react';

import calculateNodeHeight from './calculateNodeHeight';
import getSizingData, { SizingData } from './getSizingData';
import { useComposedRef, useFontsLoadedListener } from './hooks';
import { noop } from './utils';

const isDevelopment = false;

type TextareaProps = React.TextareaHTMLAttributes<HTMLTextAreaElement>;

type Style = Omit<NonNullable<TextareaProps['style']>, 'maxHeight' | 'minHeight'> & {
  height?: number;
};

export type TextareaHeightChangeMeta = {
  rowHeight: number;
};

export interface TextareaAutosizeProps extends Omit<TextareaProps, 'style'> {
  maxRows?: number;
  minRows?: number;
  onHeightChange?: (height: number, meta: TextareaHeightChangeMeta) => void;
  cacheMeasurements?: boolean;
  style?: Style;
}

const TextareaAutosize: React.ForwardRefRenderFunction<
  HTMLTextAreaElement,
  TextareaAutosizeProps
> = (
  { cacheMeasurements, maxRows, minRows, onChange = noop, onHeightChange = noop, ...props },
  userRef: React.Ref<HTMLTextAreaElement>
) => {
  if (isDevelopment && props.style) {
    if ('maxHeight' in props.style) {
      throw new Error(
        'Using `style.maxHeight` for <TextareaAutosize/> is not supported. Please use `maxRows`.'
      );
    }
    if ('minHeight' in props.style) {
      throw new Error(
        'Using `style.minHeight` for <TextareaAutosize/> is not supported. Please use `minRows`.'
      );
    }
  }
  const isControlled = props.value !== undefined;
  const libRef = React.useRef<HTMLTextAreaElement | null>(null);
  const ref = useComposedRef(libRef, userRef);
  const heightRef = React.useRef(0);
  const measurementsCacheRef = React.useRef<SizingData>();

  const resizeTextarea = () => {
    const node = libRef.current!;
    const nodeSizingData =
      cacheMeasurements && measurementsCacheRef.current
        ? measurementsCacheRef.current
        : getSizingData(node);

    if (!nodeSizingData) {
      return;
    }

    measurementsCacheRef.current = nodeSizingData;

    const [height, rowHeight] = calculateNodeHeight(
      nodeSizingData,
      node.value || node.placeholder || 'x',
      minRows,
      maxRows
    );

    if (heightRef.current !== height) {
      heightRef.current = height;
      node.style.setProperty('height', `${height}px`, 'important');
      onHeightChange(height, { rowHeight });
    }
  };

  const handleChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    if (!isControlled) {
      resizeTextarea();
    }
    onChange(event);
  };

  React.useLayoutEffect(resizeTextarea);
  useFontsLoadedListener(resizeTextarea);
  // LOV added
  React.useLayoutEffect(() => {
    // monitor for changes to width
    let knownWidth = libRef.current.clientWidth;
    const ro = new ResizeObserver(entries => {
      const newWidth = entries[entries.length - 1].contentRect.width;
      if (newWidth !== knownWidth) {
        knownWidth = newWidth;
        resizeTextarea();
      }
    });
    ro.observe(libRef.current);
    return () => {
      ro.disconnect();
    };
  }, []);
  // end LOV added
  return (
    <textarea
      {...props}
      onChange={handleChange}
      ref={ref}
    />
  );
};

export default /* #__PURE__ */ React.forwardRef(TextareaAutosize);
