'use client';

import { useRef } from 'react';
import { useWebGL } from '../hooks/useWebGL';
import Loader from './Loader';
import Slider from './Slider';

interface WebGLCanvasProps {
  depthMapUrl: string;
  textureImageUrl: string;
  processing: boolean;
}

const WebGLCanvas: React.FC<WebGLCanvasProps> = ({ depthMapUrl, textureImageUrl, processing }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
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
    let newTextureLighting = textureLighting;
    if (id === 'lighting-checkbox') newTextureLighting = checked ? newTextureLighting + 2 : newTextureLighting - 2;
    else if (id === 'texture-checkbox') newTextureLighting = checked ? newTextureLighting + 1 : newTextureLighting - 1;
    setTextureLighting(newTextureLighting);
  };

  return (
    <>
      {loading && <Loader />}
      <div className="row">
        <div className="col-12 col-lg-8">
          <div className="canvas-wrapper" style={{ width: '100%', height: 'auto', overflow: 'hidden' }}>
            <canvas 
              ref={canvasRef} 
              style={{ width: '100%', height: 'auto', display: 'block' }}
            ></canvas>
          </div>
        </div>
        <div className="col-12 col-lg-4">
          <div className="p-3 bg-light rounded shadow-sm">
            <h6 className="mb-3 border-bottom pb-2">
              <u>Light Controls</u>
            </h6>
            
            <div className="mb-3">
              <div className="d-flex justify-content-between small text-muted">
                <span>Light Position:</span>
                <span id="light-pos-span">
                  [{lightPos[0].toFixed(2)}, {lightPos[1].toFixed(2)}, {lightPos[2].toFixed(2)}]
                </span>
              </div>
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

            <hr />

            <div className="mb-3">
              <div className="d-flex justify-content-between small text-muted">
                <label htmlFor="lightIntensitySlider">Light Intensity:</label>
                <span id="light-intensity-span">{lightIntensity}</span>
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

            <hr />

            <div className="row">
              <div className="col">
                <div className="form-check">
                  <input
                    className="form-check-input"
                    type="checkbox"
                    id="lighting-checkbox"
                    checked={textureLighting > 1}
                    onChange={handleCheckboxChange}
                    disabled={processing}
                  />
                  <label className="form-check-label" htmlFor="lighting-checkbox">
                    Enable Lighting
                  </label>
                </div>
                <div className="form-check">
                  <input
                    className="form-check-input"
                    type="checkbox"
                    id="texture-checkbox"
                    checked={textureLighting % 2 === 1}
                    onChange={handleCheckboxChange}
                    disabled={processing}
                  />
                  <label className="form-check-label" htmlFor="texture-checkbox">
                    Enable Texture
                  </label>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default WebGLCanvas;