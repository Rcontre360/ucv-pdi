'use client';

import { useRef } from 'react';
import { useParallax } from '../hooks/useParallax';
import Loader from './Loader';
import Slider from './Slider';

interface ParallaxCanvasProps {
  depthMapUrl: string;
  textureImageUrl: string;
}

const ParallaxCanvas: React.FC<ParallaxCanvasProps> = ({ depthMapUrl, textureImageUrl }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const {
    intensity,
    setIntensity,
    loading
  } = useParallax(canvasRef, depthMapUrl, textureImageUrl);

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
              Parallax Controls
            </h3>
            
            <div className="mb-6">
              <div className="flex justify-between items-center mb-2">
                <label className="text-sm font-medium text-gray-700">Movement Intensity</label>
                <span className="text-sm font-mono text-gray-500">{intensity.toFixed(3)}</span>
              </div>
              <Slider
                id="intensitySlider"
                label=""
                min={0}
                max={0.1}
                step={0.001}
                value={intensity}
                onChange={(e) => setIntensity(parseFloat(e.target.value))}
              />
              <p className="text-xs text-gray-500 mt-1">Controls how much the foreground moves relative to the mouse.</p>
            </div>
            
            <div className="mt-4 p-4 bg-blue-50 rounded text-sm text-blue-800">
              <strong>How to use:</strong> Move your mouse over the image to see the 3D parallax effect!
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default ParallaxCanvas;
