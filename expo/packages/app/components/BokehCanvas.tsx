'use client';

import { useRef } from 'react';
import { useBokeh } from '../hooks/useBokeh';
import Loader from './Loader';
import Slider from './Slider';

interface BokehCanvasProps {
  depthMapUrl: string;
  textureImageUrl: string;
}

const BokehCanvas: React.FC<BokehCanvasProps> = ({ depthMapUrl, textureImageUrl }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const {
    focusDepth,
    setFocusDepth,
    loading
  } = useBokeh(canvasRef, depthMapUrl, textureImageUrl);

  return (
    <>
      {loading && <Loader />}
      <div className="flex flex-col lg:flex-row gap-8 h-full">
        {/* Canvas Section */}
        <div className="w-full lg:w-2/3 bg-gray-50 rounded-lg border border-gray-200 flex items-center justify-center overflow-hidden min-h-[400px]">
          <canvas 
            ref={canvasRef} 
            className="max-w-full max-h-[70vh] object-contain block"
          ></canvas>
        </div>

        {/* Controls Section */}
        <div className="w-full lg:w-1/3">
          <div className="bg-gray-50 rounded-lg p-6 border border-gray-200">
            <h3 className="text-lg font-semibold text-gray-900 mb-4 border-b border-gray-200 pb-2">
              Bokeh Controls
            </h3>
            
            <div className="mb-6">
              <div className="flex justify-between items-center mb-2">
                <label className="text-sm font-medium text-gray-700">Focus Depth</label>
                <span className="text-sm font-mono text-gray-500">{focusDepth.toFixed(2)}</span>
              </div>
              <Slider
                id="focusSlider"
                label=""
                min={0}
                max={1}
                step={0.01}
                value={focusDepth}
                onChange={(e) => setFocusDepth(parseFloat(e.target.value))}
              />
              <p className="text-xs text-gray-500 mt-1">Adjust to change the focal plane distance.</p>
            </div>

            <div className="mt-4 p-4 bg-blue-50 rounded text-sm text-blue-800">
              <p>Blur strength is fixed at a constant level for cinematic consistency.</p>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default BokehCanvas;
