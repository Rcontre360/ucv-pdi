'use client';

import { useRef } from 'react';
import { useSSAO } from '../hooks/useSSAO';
import Loader from './Loader';
import Slider from './Slider';

interface SSAOCanvasProps {
  depthMapUrl: string;
  textureImageUrl: string;
}

const SSAOCanvas: React.FC<SSAOCanvasProps> = ({ depthMapUrl, textureImageUrl }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const {
    radius,
    bias,
    intensity,
    setRadius,
    setBias,
    setIntensity,
    loading
  } = useSSAO(canvasRef, depthMapUrl, textureImageUrl);

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
              Ambient Occlusion (Clay Mode)
            </h3>
            
            <div className="mb-6">
              <div className="flex justify-between items-center mb-2">
                <label className="text-sm font-medium text-gray-700">Shadow Radius</label>
                <span className="text-sm font-mono text-gray-500">{radius.toFixed(3)}</span>
              </div>
              <Slider
                id="radiusSlider"
                label=""
                min={0.001}
                max={0.1}
                step={0.001}
                value={radius}
                onChange={(e) => setRadius(parseFloat(e.target.value))}
              />
              <p className="text-xs text-gray-500 mt-1">Controls the size of the contact shadows.</p>
            </div>

            <div className="mb-6">
              <div className="flex justify-between items-center mb-2">
                <label className="text-sm font-medium text-gray-700">Darkness Intensity</label>
                <span className="text-sm font-mono text-gray-500">{intensity.toFixed(1)}</span>
              </div>
              <Slider
                id="intensitySlider"
                label=""
                min={0}
                max={5}
                step={0.1}
                value={intensity}
                onChange={(e) => setIntensity(parseFloat(e.target.value))}
              />
            </div>

            <div className="mb-6">
              <div className="flex justify-between items-center mb-2">
                <label className="text-sm font-medium text-gray-700">Depth Bias</label>
                <span className="text-sm font-mono text-gray-500">{bias.toFixed(3)}</span>
              </div>
              <Slider
                id="biasSlider"
                label=""
                min={0.001}
                max={0.05}
                step={0.001}
                value={bias}
                onChange={(e) => setBias(parseFloat(e.target.value))}
              />
              <p className="text-xs text-gray-500 mt-1">Reduces self-shadowing artifacts on flat surfaces.</p>
            </div>
            
            <div className="mt-4 p-4 bg-gray-100 rounded text-sm text-gray-700">
              <p>This mode visualizes purely the <strong>geometric shadows</strong> calculated from the depth map, ignoring the original colors. It looks like a white clay sculpture.</p>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default SSAOCanvas;
