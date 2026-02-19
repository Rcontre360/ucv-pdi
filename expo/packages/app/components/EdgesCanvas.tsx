'use client';

import { useRef } from 'react';
import { useDepthEdges } from '../hooks/useDepthEdges';
import Loader from './Loader';
import Slider from './Slider';

interface EdgesCanvasProps {
  depthMapUrl: string;
  textureImageUrl: string;
}

const EdgesCanvas: React.FC<EdgesCanvasProps> = ({ depthMapUrl, textureImageUrl }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const {
    threshold,
    thickness,
    setThreshold,
    setThickness,
    loading
  } = useDepthEdges(canvasRef, depthMapUrl, textureImageUrl);

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
              Depth Edge Controls
            </h3>
            
            <div className="mb-6">
              <div className="flex justify-between items-center mb-2">
                <label className="text-sm font-medium text-gray-700">Sensitivity Threshold</label>
                <span className="text-sm font-mono text-gray-500">{threshold.toFixed(2)}</span>
              </div>
              <Slider
                id="thresholdSlider"
                label=""
                min={0.01}
                max={5.0}
                step={0.01}
                value={threshold}
                onChange={(e) => setThreshold(parseFloat(e.target.value))}
              />
              <p className="text-xs text-gray-500 mt-1">Lower values detect more subtle depth changes.</p>
            </div>

            <div className="mb-6">
              <div className="flex justify-between items-center mb-2">
                <label className="text-sm font-medium text-gray-700">Line Thickness</label>
                <span className="text-sm font-mono text-gray-500">{thickness.toFixed(1)} px</span>
              </div>
              <Slider
                id="thicknessSlider"
                label=""
                min={1}
                max={10}
                step={0.5}
                value={thickness}
                onChange={(e) => setThickness(parseFloat(e.target.value))}
              />
            </div>
            
            <div className="mt-4 p-4 bg-gray-100 rounded text-sm text-gray-700">
              <p>This view highlights <strong>geometric edges</strong> based on depth discontinuities, ignoring texture patterns and shadows.</p>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default EdgesCanvas;
