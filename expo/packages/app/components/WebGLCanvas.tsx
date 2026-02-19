'use client';

import { useRef } from 'react';
import { useWebGL } from '../hooks/useWebGL';
import Loader from './Loader';

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
        <div className="col">
          <canvas ref={canvasRef}></canvas>
        </div>
        <div className="col">
          <div className="row">
            <h6 className="">
              <u>Light Controls</u>
            </h6>
          </div>
          <div className="row">
            <span className="col">Light Position:</span>
            <span className="col" id="light-pos-span">
              [{lightPos[0].toFixed(2)}, {lightPos[1].toFixed(2)}, {lightPos[2].toFixed(2)}]
            </span>
          </div>
          <br />
          <div className="row">
            <label className="col-form-label-md" htmlFor="xlightSlider">x :</label>
            <input
              className="form-control-range"
              type="range"
              id="xlightSlider"
              min="-1"
              max="1"
              step="0.01"
              value={lightPos[0]}
              onChange={handleSliderChange}
              disabled={processing}
            />
          </div>
          <div className="row">
            <label className="col-form-label-md" htmlFor="ylightSlider">y :</label>
            <input
              className="form-control-range"
              type="range"
              id="ylightSlider"
              min="-1"
              max="1"
              step="0.01"
              value={lightPos[1]}
              onChange={handleSliderChange}
              disabled={processing}
            />
          </div>
          <div className="row">
            <label className="col-form-label-md" htmlFor="zlightSlider">z :</label>
            <input
              className="form-control-range"
              type="range"
              id="zlightSlider"
              min="-1"
              max="1"
              step="0.01"
              value={lightPos[2]}
              onChange={handleSliderChange}
              disabled={processing}
            />
          </div>
          <hr />
          <div className="form-group row">
            <label className="form-control-md" htmlFor="lightIntensitySlider">
              Light Intensity: &nbsp;&nbsp;
            </label>
            <span className="form-control-md" id="light-intensity-span">
              {lightIntensity}
            </span>
            <input
              className="form-control-range"
              type="range"
              id="lightIntensitySlider"
              min="0"
              max="2"
              step="0.01"
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
                  className="form-control-md"
                  type="checkbox"
                  id="lighting-checkbox"
                  checked={textureLighting > 1}
                  onChange={handleCheckboxChange}
                  disabled={processing}
                />
                <label className="form-check-label col-form-label-md" htmlFor="lighting-checkbox">
                  Light
                </label>
              </div>
              <div className="form-check">
                <input
                  className="form-control-md"
                  type="checkbox"
                  id="texture-checkbox"
                  checked={textureLighting % 2 === 1}
                  onChange={handleCheckboxChange}
                  disabled={processing}
                />
                <label className="form-check-label col-form-label-md" htmlFor="texture-checkbox">
                  Texture
                </label>
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default WebGLCanvas;
