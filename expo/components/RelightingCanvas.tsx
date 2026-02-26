'use client';

import {useRef, useEffect} from 'react';
import {useRelighting} from '../hooks/useRelighting';
import Loader from './Loader';
import Slider from './Slider';
import SaveImageButton from './SaveImageButton';

interface WebGLCanvasProps {
  depthMapUrl: string;
  textureImageUrl: string;
  processing: boolean;
}

const RelightingCanvas: React.FC<WebGLCanvasProps> = ({depthMapUrl, textureImageUrl, processing}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  const {
    lightPos,
    lightIntensity,
    textureLighting,
    loading,
    aspectRatio,
    setLightPos,
    setLightIntensity,
    setTextureLighting,
  } = useRelighting(
    canvasRef,
    depthMapUrl,
    textureImageUrl
  );

  const handleSliderChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const {id, value} = e.target;
    const val = parseFloat(value);

    setLightPos((prev) => {
      const next = [...prev];
      if (id === 'xlightSlider') next[0] = val;
      else if (id === 'ylightSlider') next[1] = val;
      else if (id === 'zlightSlider') next[2] = val;
      return next;
    });
  };

  const handleIntensityChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setLightIntensity(parseFloat(e.target.value));
  };

  const handleToggleLighting = (e: React.ChangeEvent<HTMLInputElement>) => {
    const {checked} = e.target;
    setTextureLighting(checked ? 3 : 1);
  };

  const handleToggleRelightDepth = (e: React.ChangeEvent<HTMLInputElement>) => {
    const {checked} = e.target;
    setTextureLighting(checked ? 4 : 1);
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

    canvas.addEventListener('wheel', handleWheel, {passive: false});

    return () => {
      canvas.removeEventListener('wheel', handleWheel);
    };
  }, [processing, setLightPos]);

  return (
    <>
      {loading && <Loader />}
      <div className="flex flex-col lg:flex-row gap-8 h-full flex-1 w-full">
        {/* Canvas Section */}
        <div className="w-full lg:w-2/3 bg-gray-50 rounded-lg border border-gray-200 flex items-center justify-center overflow-hidden min-h-[400px]">
          <div ref={containerRef} className="relative w-full h-full flex items-center justify-center">
            <canvas
              ref={canvasRef}
              style={{aspectRatio: `${aspectRatio}`}}
              className="max-w-full max-h-[70vh] object-contain cursor-crosshair block shadow-md"
              onClick={handleCanvasClick}
            ></canvas>
          </div>
        </div>

        {/* Controls Section */}
        <div className="w-full lg:w-1/3 space-y-6">
          <div className="bg-gray-50 rounded-lg p-6 border border-gray-200 shadow-sm">
            <h3 className="text-lg font-semibold text-gray-900 mb-4 border-b border-gray-200 pb-2 flex items-center">
              <svg className="w-5 h-5 mr-2 text-yellow-500" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
                <path fillRule="evenodd" d="M10 2a1 1 0 011 1v1a1 1 0 11-2 0V3a1 1 0 011-1zm4 8a4 4 0 11-8 0 4 4 0 018 0zm-.464 4.95l.707.707a1 1 0 001.414-1.414l-.707-.707a1 1 0 00-1.414 1.414zm2.12-10.607a1 1 0 010 1.414l-.706.707a1 1 0 11-1.414-1.414l.707-.707a1 1 0 011.414 0zM17 11a1 1 0 100-2h-1a1 1 0 100 2h1zm-7 4a1 1 0 011 1v1a1 1 0 11-2 0v-1a1 1 0 011-1zM5.05 6.464A1 1 0 106.465 5.05l-.708-.707a1 1 0 00-1.414 1.414l.707.707zm1.414 8.486l-.707.707a1 1 0 01-1.414-1.414l.707-.707a1 1 0 011.414 1.414zM4 11a1 1 0 100-2H3a1 1 0 000 2h1z" clipRule="evenodd" />
              </svg>
              Light Controls
            </h3>

            <div className="space-y-4">
              <div>
                <div className="flex justify-between text-xs font-mono text-gray-500 mb-2 bg-white p-2 rounded border border-gray-100">
                  <span>POS (X,Y,Z):</span>
                  <span>[{lightPos[0].toFixed(2)}, {lightPos[1].toFixed(2)}, {lightPos[2].toFixed(2)}]</span>
                </div>

                <Slider
                  id="xlightSlider"
                  label="Horizontal (X)"
                  min={-1}
                  max={1}
                  value={lightPos[0]}
                  onChange={handleSliderChange}
                  disabled={processing}
                />
                <Slider
                  id="ylightSlider"
                  label="Vertical (Y)"
                  min={-1}
                  max={1}
                  value={lightPos[1]}
                  onChange={handleSliderChange}
                  disabled={processing}
                />
                <Slider
                  id="zlightSlider"
                  label="Depth (Z)"
                  min={-1}
                  max={1}
                  value={lightPos[2]}
                  onChange={handleSliderChange}
                  disabled={processing}
                />
              </div>

              <div className="pt-2">
                <div className="flex justify-between items-center mb-1">
                  <label className="text-sm font-medium text-gray-700">Intensity</label>
                  <span className="text-xs font-mono text-gray-500">{lightIntensity.toFixed(2)}</span>
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

              <div className="pt-4 border-t border-gray-200 space-y-3">
                <label className="flex items-center cursor-pointer group">
                  <input
                    type="checkbox"
                    id="lighting-checkbox"
                    checked={textureLighting === 3}
                    onChange={handleToggleLighting}
                    disabled={processing}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded transition-colors"
                  />
                  <span className="ml-3 text-sm font-medium text-gray-700 group-hover:text-gray-900 transition-colors">
                    Enable 3D Relighting
                  </span>
                </label>

                <label className="flex items-center cursor-pointer group">
                  <input
                    type="checkbox"
                    id="relightdepth-checkbox"
                    checked={textureLighting === 4}
                    onChange={handleToggleRelightDepth}
                    disabled={processing}
                    className="h-4 w-4 text-green-600 focus:ring-green-500 border-gray-300 rounded transition-colors"
                  />
                  <span className="ml-3 text-sm font-medium text-gray-700 group-hover:text-gray-900 transition-colors">
                    Relight Depth Map
                  </span>
                </label>
              </div>
            </div>

            <SaveImageButton
              canvasRef={canvasRef}
              filenamePrefix="relighted-image"
              disabled={processing || loading}
            />
          </div>

          <div className="bg-blue-50 p-4 rounded-lg text-xs text-blue-700 border border-blue-100 leading-relaxed shadow-sm">
            <strong>Pro Tip:</strong> You can click/drag on the image to move the light horizontally and vertically, or use the mouse wheel to move it closer or further away.
          </div>
        </div>
      </div>
    </>
  );
};

export default RelightingCanvas;
