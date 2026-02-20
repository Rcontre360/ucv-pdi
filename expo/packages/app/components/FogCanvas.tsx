'use client';

import { useRef } from 'react';
import { useFog } from '../hooks/useFog';
import Loader from './Loader';
import Slider from './Slider';
import SaveImageButton from './SaveImageButton';

interface FogCanvasProps {
  depthMapUrl: string;
  textureImageUrl: string;
}

const FogCanvas: React.FC<FogCanvasProps> = ({ depthMapUrl, textureImageUrl }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const {
    fogDensity,
    fogNear,
    fogFar,
    aspectRatio,
    loading,
    setFogDensity,
    setFogNear,
    setFogFar,
  } = useFog(canvasRef, depthMapUrl, textureImageUrl);

  return (
    <>
      {loading && <Loader />}
      <div className="flex flex-col lg:flex-row gap-8 h-full">
        {/* Canvas Section */}
        <div className="w-full lg:w-2/3 bg-gray-50 rounded-lg border border-gray-200 flex items-center justify-center overflow-hidden min-h-[400px]">
          <canvas 
            ref={canvasRef} 
            style={{ aspectRatio: `${aspectRatio}` }}
            className="max-w-full max-h-[70vh] object-contain block"
          ></canvas>
        </div>

        {/* Controls Section */}
        <div className="w-full lg:w-1/3">
          <div className="bg-gray-50 rounded-lg p-6 border border-gray-200">
            <h3 className="text-lg font-semibold text-gray-900 mb-4 border-b border-gray-200 pb-2">
              Fog Controls
            </h3>
            
            <div className="mb-6">
              <div className="flex justify-between items-center mb-2">
                <label className="text-sm font-medium text-gray-700">Fog Density</label>
                <span className="text-sm font-mono text-gray-500">{fogDensity.toFixed(2)}</span>
              </div>
              <Slider
                id="densitySlider"
                label=""
                min={0}
                max={1}
                step={0.01}
                value={fogDensity}
                onChange={(e) => setFogDensity(parseFloat(e.target.value))}
              />
            </div>

            <div className="mb-6">
              <div className="flex justify-between items-center mb-2">
                <label className="text-sm font-medium text-gray-700">Start Distance</label>
                <span className="text-sm font-mono text-gray-500">{fogNear.toFixed(2)}</span>
              </div>
              <Slider
                id="nearSlider"
                label=""
                min={0}
                max={1}
                step={0.01}
                value={fogNear}
                onChange={(e) => setFogNear(parseFloat(e.target.value))}
              />
            </div>

            <div className="mb-6">
              <div className="flex justify-between items-center mb-2">
                <label className="text-sm font-medium text-gray-700">End Distance</label>
                <span className="text-sm font-mono text-gray-500">{fogFar.toFixed(2)}</span>
              </div>
              <Slider
                id="farSlider"
                label=""
                min={0}
                max={1}
                step={0.01}
                value={fogFar}
                onChange={(e) => setFogFar(parseFloat(e.target.value))}
              />
            </div>

            <SaveImageButton 
              canvasRef={canvasRef} 
              filenamePrefix="fog-image" 
              disabled={loading} 
            />
          </div>
        </div>
      </div>
    </>
  );
};

export default FogCanvas;
