import {useEffect, useRef, useState} from 'react';
import {dataUriToImage, imageHelper} from '../utils/images';
import {createProgram, createTexture} from '../utils/webgl_helpers';

export interface WebGLSetupResult {
  loading: boolean;
  gl: WebGLRenderingContext | null;
  program: WebGLProgram | null;
  textures: {image: WebGLTexture; depth: WebGLTexture} | null;
  images: {image: HTMLImageElement; depth: HTMLImageElement} | null;
}

export const useWebGLSetup = (
  canvasRef: React.RefObject<HTMLCanvasElement>,
  depthMapUri: string,
  textureImageUrl: string,
  vsSource: string,
  fsSource: string
): WebGLSetupResult => {
  const [loading, setLoading] = useState(true);
  const glRef = useRef<WebGLRenderingContext | null>(null);
  const programRef = useRef<WebGLProgram | null>(null);
  const texturesRef = useRef<{image: WebGLTexture; depth: WebGLTexture} | null>(null);
  const imagesRef = useRef<{image: HTMLImageElement; depth: HTMLImageElement} | null>(null);

  useEffect(() => {
    if (!canvasRef.current || !depthMapUri || !textureImageUrl) return;

    const canvas = canvasRef.current;
    const gl = canvas.getContext('webgl');
    if (!gl) return;
    glRef.current = gl;

    setLoading(true);

    Promise.all([dataUriToImage(textureImageUrl), dataUriToImage(depthMapUri)])
      .then(([img, depthImg]) => {
        // Sync canvas size to image
        const [w, h] = imageHelper.getImageSize(img);
        canvas.width = w;
        canvas.height = h;
        gl.viewport(0, 0, w, h);

        // Compile and Link
        const program = createProgram(gl, vsSource, fsSource);
        if (!program) return;
        programRef.current = program;

        // Create textures
        const texImage = createTexture(gl, img);
        const texDepth = createTexture(gl, depthImg);
        if (!texImage || !texDepth) return;

        texturesRef.current = {image: texImage, depth: texDepth};
        imagesRef.current = {image: img, depth: depthImg};

        setLoading(false);
      })
      .catch(err => console.error("WebGL Setup Error:", err));

  }, [depthMapUri, textureImageUrl, vsSource, fsSource]);

  return {
    loading,
    gl: glRef.current,
    program: programRef.current,
    textures: texturesRef.current,
    images: imagesRef.current
  };
};
