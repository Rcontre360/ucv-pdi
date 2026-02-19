'use client';
import {useRef, useState, useCallback, useEffect} from 'react';
import {useWebGL} from '../hooks/useWebGL';
import Loader from '../components/Loader';
import {Core, DepthAnythingV2} from 'core';

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
  const [userImage, setUserImage] = useState<string | null>(null);
  const [userDepthMap, setUserDepthMap] = useState<string | null>(null);
  const [processingUserImage, setProcessingUserImage] = useState(false);

  const canvasRef = useRef<HTMLCanvasElement>(null);

  const currentDepthMapUrl = userDepthMap || (images.depthRoot + images.img[imgIdx]);
  const currentTextureImageUrl = userImage || (images.texRoot + images.img[imgIdx]);

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
    currentDepthMapUrl,
    currentTextureImageUrl
  );

  const handleImageUpload = useCallback(async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setProcessingUserImage(true);
    setUserImage(null);
    setUserDepthMap(null);

    const reader = new FileReader();
    reader.onload = async (e) => {
      const imageUrl = e.target?.result as string;
      setUserImage(imageUrl);

      try {
        // IMPORTANT: The REPLICATE_API_TOKEN should NOT be exposed on the client-side in a production environment.
        // It should be handled via a secure backend API. For local development/demonstration,
        // you might temporarily use a token here, but be aware of the security implications.
        // Replace 'YOUR_REPLICATE_API_TOKEN' with an actual token if testing locally.
        const REPLICATE_API_TOKEN_PLACEHOLDER = process.env.NEXT_PUBLIC_REPLICATE_API_TOKEN || 'YOUR_REPLICATE_API_TOKEN';

        const depthService = new DepthAnythingV2(REPLICATE_API_TOKEN_PLACEHOLDER);
        const core = new Core(depthService);

        const depthMap = await core.processImage(imageUrl);
        setUserDepthMap(depthMap);

      } catch (error) {
        console.error("Error processing image with SDK:", error);
        setUserImage(null);
        setUserDepthMap(null);
      } finally {
        setProcessingUserImage(false);
      }
    };
    reader.readAsDataURL(file);
  }, []);

  return (
    <div>
      {(loading || processingUserImage) && <Loader />}
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
                    setUserImage(null); // Clear user image when selecting default
                    setUserDepthMap(null);
                    setImgIdx(parseInt(e.target.value));
                  }}
                  disabled={processingUserImage}
                >
                  {images.img.map((img, idx) => (
                    <option key={idx} value={idx}>
                      {img}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <div className="form-group row">
              <label htmlFor="imageUpload" className="col-form-label-md">Upload Custom Image:</label>
              <input
                type="file"
                id="imageUpload"
                accept="image/*"
                onChange={handleImageUpload}
                className="form-control-file"
                disabled={processingUserImage}
              />
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
                disabled={processingUserImage}
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
                disabled={processingUserImage}
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
                disabled={processingUserImage}
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
                disabled={processingUserImage}
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
                    disabled={processingUserImage}
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
                    disabled={processingUserImage}
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
    </div>
  );
}
