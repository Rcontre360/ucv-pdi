'use client';
import { useRef, useState, useEffect } from 'react';
import { useWebGL } from '../hooks/useWebGL';
import Loader from '../components/Loader';

const images = {
  img: [
    'bird.jpg',
    'coke.jpg',
    'tunnel.jpg',
    'room.jpg',
    'shelf.jpg',
    'flower.jpg',
    'misc.jpg',
    'office.jpg',
    'kitchen.jpg',
    'human.jpg',
  ],
  texRoot: '/images/texture/',
  depthRoot: '/images/depth/',
};

export default function Home() {
  const [imgIdx, setImgIdx] = useState(0);
  const [loading, setLoading] = useState(true);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const depthImageRef = useRef<HTMLImageElement>(null);
  const textureImageRef = useRef<HTMLImageElement>(null);

  const {
    lightPos,
    lightIntensity,
    textureLighting,
    setLightPos,
    setLightIntensity,
    setTextureLighting,
  } = useWebGL(canvasRef, depthImageRef, textureImageRef);

  useEffect(() => {
    const depthImg = new Image();
    depthImg.src = images.depthRoot + images.img[imgIdx];
    depthImg.onload = () => {
      if (depthImageRef.current) {
        depthImageRef.current.src = depthImg.src;
      }
      const textureImg = new Image();
      textureImg.src = images.texRoot + images.img[imgIdx];
      textureImg.onload = () => {
        if (textureImageRef.current) {
          textureImageRef.current.src = textureImg.src;
        }
        setLoading(false);
      };
    };
  }, [imgIdx]);

  const handleSliderChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { id, value } = e.target;
    const newLightPos = [...lightPos];
    if (id === 'xlightSlider') {
      newLightPos[0] = parseFloat(value);
    } else if (id === 'ylightSlider') {
      newLightPos[1] = parseFloat(value);
    } else if (id === 'zlightSlider') {
      newLightPos[2] = parseFloat(value);
    }
    setLightPos(newLightPos);
  };

  const handleIntensityChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setLightIntensity(parseFloat(e.target.value));
  };

  const handleCheckboxChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { id, checked } = e.target;
    let newTextureLighting = textureLighting;
    if (id === 'lighting-checkbox') {
      newTextureLighting = checked ? newTextureLighting + 2 : newTextureLighting - 2;
    } else if (id === 'texture-checkbox') {
      newTextureLighting = checked ? newTextureLighting + 1 : newTextureLighting - 1;
    }
    setTextureLighting(newTextureLighting);
  };

  return (
    <div>
      {loading && <Loader />}
      <nav className="navbar navbar-dark bg-dark">
        <a className="navbar-brand disabled text-light">Image Relighting</a>
      </nav>
      <div className="container">
        <br />
        <div className="row">
          <div className="col">
            <canvas ref={canvasRef}></canvas>
          </div>
          <div className="col">
            <div className="form-group row d-flex justify-content-between">
              <div className="p-0 align-self-center">
                <span className="image-label"> Image: </span>
              </div>
              <div className="p-0 align-self-center">
                <select
                  className="col-form-label-md custom-select"
                  id="image-select"
                  value={imgIdx}
                  onChange={(e) => {
                    setLoading(true);
                    setImgIdx(parseInt(e.target.value));
                  }}
                >
                  {images.img.map((img, idx) => (
                    <option key={idx} value={idx}>
                      {img}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <hr />
            <div className="row">
              <h6 className="">
                <u>Light Controls</u>
              </h6>
            </div>
            <div className="row">
              <span className="col">Light Position:</span>
              <span className="col" id="light-pos-span">
                [{lightPos[0].toFixed(2)}, {lightPos[1].toFixed(2)},{' '}
                {lightPos[2].toFixed(2)}]
              </span>
            </div>
            <br />
            <div className="row">
              <label className="col-form-label-md" htmlFor="xlightSlider">
                x :
              </label>
              <input
                className="form-control-range"
                type="range"
                id="xlightSlider"
                min="-1"
                max="1"
                step="0.01"
                value={lightPos[0]}
                onChange={handleSliderChange}
              />
            </div>
            <div className="row">
              <label className="col-form-label-md" htmlFor="ylightSlider">
                y :
              </label>
              <input
                className="form-control-range"
                type="range"
                id="ylightSlider"
                min="-1"
                max="1"
                step="0.01"
                value={lightPos[1]}
                onChange={handleSliderChange}
              />
            </div>
            <div className="row">
              <label className="col-form-label-md" htmlFor="zlightSlider">
                z :
              </label>
              <input
                className="form-control-range"
                type="range"
                id="zlightSlider"
                min="-1"
                max="1"
                step="0.01"
                value={lightPos[2]}
                onChange={handleSliderChange}
              />
            </div>
            <hr />
            <div className="form-group row">
              <label
                className="form-control-md"
                htmlFor="lightIntensitySlider"
              >
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
                  />
                  <label
                    className="form-check-label col-form-label-md"
                    htmlFor="lighting-checkbox"
                  >
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
                  />
                  <label
                    className="form-check-label col-form-label-md"
                    htmlFor="texture-checkbox"
                  >
                    Texture
                  </label>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <img ref={depthImageRef} style={{ display: 'none' }} />
      <img ref={textureImageRef} style={{ display: 'none' }} />
    </div>
  );
}