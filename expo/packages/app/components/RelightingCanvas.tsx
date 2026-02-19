'use client';

import { useRef, useEffect } from 'react';
import { useWebGL } from '../hooks/useWebGL';
import Loader from './Loader';
import Slider from './Slider';

interface WebGLCanvasProps {
  depthMapUrl: string;
  textureImageUrl: string;
  processing: boolean;
}

const RelightingCanvas: React.FC<WebGLCanvasProps> = ({ depthMapUrl, textureImageUrl, processing }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const {
    lightPos,
    lightIntensity,
    textureLighting,
    loading,
    setLightPos,
    setLightIntensity,
    setTextureLighting,
  } = useWebGL(
    canvasRef,
    depthMapUrl,
    textureImageUrl
  );

  const handleSliderChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { id, value } = e.target;
    const newLightPos = [...lightPos];
    if (id === 'xlightSlider') newLightPos[0] = parseFloat(value);
    else if (id === 'ylightSlider') newLightPos[1] = parseFloat(value);
    else if (id === 'zlightSlider') newLightPos[2] = parseFloat(value);
    setLightPos(newLightPos);
  };

  const handleIntensityChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setLightIntensity(parseFloat(e.target.value));
  };

  const handleCheckboxChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { id, checked } = e.target;
    // We only control lighting toggle now (Mode 3 vs Mode 1)
    // Mode 3: Texture + Light (Default)
    // Mode 1: Texture Only (Unlit)
    // Mode 2: Light Only (Removed from UI)
    
    // If Checked: Enable Lighting (Mode 3)
    // If Unchecked: Disable Lighting (Mode 1)
    
    setTextureLighting(checked ? 3 : 1);
  };

  const handleCanvasClick = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (processing || !canvasRef.current) return;
    
    const rect = canvasRef.current.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    
    const normalizedX = (x / rect.width) * 2 - 1;
    const normalizedY = -((y / rect.height) * 2 - 1); 
    
    setLightPos((prev) => [normalizedX, normalizedY, prev[2]]);
  };

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const handleWheel = (e: WheelEvent) => {
      if (processing) return;
      e.preventDefault();
      
      const zoomSpeed = 0.1;
      const delta = e.deltaY > 0 ? -zoomSpeed : zoomSpeed;
      
      setLightPos((prev) => {
        const newZ = Math.max(-1, Math.min(1, prev[2] + delta));
        return [prev[0], prev[1], newZ];
      });
    };

    canvas.addEventListener('wheel', handleWheel, { passive: false });
    
    return () => {
      canvas.removeEventListener('wheel', handleWheel);
    };
  }, [processing, setLightPos]);

  return (
    <>
      {loading && <Loader />}
      <div className="flex flex-col lg:flex-row gap-8 h-full">
        {/* Canvas Section */}
        <div className="w-full lg:w-2/3 bg-gray-50 rounded-lg border border-gray-200 flex items-center justify-center overflow-hidden min-h-[400px]">
          <div ref={containerRef} className="relative max-w-full max-h-full">
             <canvas 
              ref={canvasRef} 
              className="max-w-full max-h-[70vh] object-contain cursor-crosshair block"
              onClick={handleCanvasClick}
            ></canvas>
          </div>
        </div>

        {/* Controls Section */}
        <div className="w-full lg:w-1/3">
          <div className="bg-gray-50 rounded-lg p-6 border border-gray-200">
            <h3 className="text-lg font-semibold text-gray-900 mb-4 border-b border-gray-200 pb-2">
              Light Controls
            </h3>
            
            <div className="mb-6">
              <div className="flex justify-between text-sm text-gray-500 mb-2">
                <span>Position (X, Y, Z):</span>
                <span className="font-mono text-gray-700">
                  [{lightPos[0].toFixed(2)}, {lightPos[1].toFixed(2)}, {lightPos[2].toFixed(2)}]
                </span>
              </div>
              
              <Slider
                id="xlightSlider"
                label="X Position"
                min={-1}
                max={1}
                value={lightPos[0]}
                onChange={handleSliderChange}
                disabled={processing}
              />
              <Slider
                id="ylightSlider"
                label="Y Position"
                min={-1}
                max={1}
                value={lightPos[1]}
                onChange={handleSliderChange}
                disabled={processing}
              />
              <Slider
                id="zlightSlider"
                label="Z Position"
                min={-1}
                max={1}
                value={lightPos[2]}
                onChange={handleSliderChange}
                disabled={processing}
              />
            </div>

            <div className="mb-6">
              <div className="flex justify-between items-center mb-2">
                <label className="text-sm font-medium text-gray-700">Intensity</label>
                <span className="text-sm font-mono text-gray-500">{lightIntensity.toFixed(2)}</span>
              </div>
              <Slider
                id="lightIntensitySlider"
                label=""
                min={0}
                max={2}
                value={lightIntensity}
                onChange={handleIntensityChange}
                disabled={processing}
              />
            </div>

            <div className="space-y-3">
              <div className="flex items-center">
                <input
                  type="checkbox"
                  id="lighting-checkbox"
                  checked={textureLighting === 3}
                  onChange={handleCheckboxChange}
                  disabled={processing}
                  className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                />
                <label htmlFor="lighting-checkbox" className="ml-2 block text-sm text-gray-700">
                  Enable Lighting
                </label>
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default RelightingCanvas;
