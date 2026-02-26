'use client';

import { useRef } from 'react';
import { useUnderwater } from '../hooks/useUnderwater';
import Loader from './Loader';
import Slider from './Slider';
import SaveImageButton from './SaveImageButton';

interface UnderwaterCanvasProps {
  depthMapUrl: string;
  textureImageUrl: string;
}

const UnderwaterCanvas: React.FC<UnderwaterCanvasProps> = ({ depthMapUrl, textureImageUrl }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const {
    waterDensity,
    redAbsorb,
    aspectRatio,
    loading,
    setWaterDensity,
    setRedAbsorb,
  } = useUnderwater(canvasRef, depthMapUrl, textureImageUrl);

  return (
    <>
      {loading && <Loader />}
      <div className="flex flex-col lg:flex-row gap-8 h-full flex-1 w-full">
        {/* Canvas Section */}
        <div className="w-full lg:w-2/3 bg-gray-50 rounded-lg border border-gray-200 flex items-center justify-center overflow-hidden min-h-[400px]">
          <div className="relative w-full h-full flex items-center justify-center">
            <canvas 
              ref={canvasRef} 
              style={{ aspectRatio: `${aspectRatio}` }}
              className="max-w-full max-h-[70vh] object-contain block shadow-md"
            ></canvas>
          </div>
        </div>

        {/* Controls Section */}
        <div className="w-full lg:w-1/3">
          <div className="bg-gray-50 rounded-lg p-6 border border-gray-200">
            <h3 className="text-lg font-semibold text-gray-900 mb-4 border-b border-gray-200 pb-2">
              Underwater Sim
            </h3>
            
            <p className="text-sm text-gray-500 mb-6">
              Simulates light absorption and refraction in water. Red light is absorbed first, creating a blue/green tint.
            </p>

            <div className="mb-6">
              <div className="flex justify-between items-center mb-2">
                <label className="text-sm font-medium text-gray-700">Water Density (Fog)</label>
                <span className="text-sm font-mono text-gray-500">{waterDensity.toFixed(2)}</span>
              </div>
              <Slider
                id="waterDensitySlider"
                label=""
                min={0}
                max={1.5}
                step={0.01}
                value={waterDensity}
                onChange={(e) => setWaterDensity(parseFloat(e.target.value))}
              />
            </div>

            <div className="mb-6">
              <div className="flex justify-between items-center mb-2">
                <label className="text-sm font-medium text-gray-700">Red Absorption</label>
                <span className="text-sm font-mono text-gray-500">{redAbsorb.toFixed(2)}</span>
              </div>
              <Slider
                id="redAbsorbSlider"
                label=""
                min={0}
                max={1.5}
                step={0.01}
                value={redAbsorb}
                onChange={(e) => setRedAbsorb(parseFloat(e.target.value))}
              />
            </div>

            <SaveImageButton 
              canvasRef={canvasRef} 
              filenamePrefix="underwater-image" 
              disabled={loading} 
            />
          </div>
        </div>
      </div>
    </>
  );
};

export default UnderwaterCanvas;

