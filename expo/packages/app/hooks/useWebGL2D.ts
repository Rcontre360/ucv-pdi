import { useEffect, useRef, useState } from 'react';
import { dataUriToImage } from '../utils/images';

export interface WebGL2DHook {
  loading: boolean;
  gl: WebGLRenderingContext | null;
  program: WebGLProgram | null;
  textures: { image: WebGLTexture; depth: WebGLTexture } | null;
}

export const useWebGL2D = (
  canvasRef: React.RefObject<HTMLCanvasElement>,
  depthMapUrl: string,
  textureImageUrl: string,
  vsSource: string,
  fsSource: string
): WebGL2DHook => {
  const [loading, setLoading] = useState(true);
  const glRef = useRef<WebGLRenderingContext | null>(null);
  const programRef = useRef<WebGLProgram | null>(null);
  const texturesRef = useRef<{ image: WebGLTexture; depth: WebGLTexture } | null>(null);

  useEffect(() => {
    if (!canvasRef.current || !depthMapUrl || !textureImageUrl) return;

    const canvas = canvasRef.current;
    const gl = canvas.getContext('webgl');
    if (!gl) return;
    glRef.current = gl;

    setLoading(true);

    Promise.all([dataUriToImage(textureImageUrl), dataUriToImage(depthMapUrl)])
      .then(([img, depthImg]) => {
        canvas.width = img.naturalWidth;
        canvas.height = img.naturalHeight;
        gl.viewport(0, 0, canvas.width, canvas.height);

        // Shaders
        const createShader = (type: number, source: string) => {
          const shader = gl.createShader(type)!;
          gl.shaderSource(shader, source);
          gl.compileShader(shader);
          if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
            console.error('Shader Compile Error:', gl.getShaderInfoLog(shader));
            return null;
          }
          return shader;
        };

        const vs = createShader(gl.VERTEX_SHADER, vsSource);
        const fs = createShader(gl.FRAGMENT_SHADER, fsSource);
        
        if (!vs || !fs) return;

        const program = gl.createProgram()!;
        gl.attachShader(program, vs);
        gl.attachShader(program, fs);
        gl.linkProgram(program);
        
        if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
             console.error('Program Link Error:', gl.getProgramInfoLog(program));
             return;
        }
        
        gl.useProgram(program);
        programRef.current = program;

        // Quad Buffer
        const buffer = gl.createBuffer();
        gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
        gl.bufferData(
          gl.ARRAY_BUFFER,
          new Float32Array([-1, -1, 1, -1, -1, 1, -1, 1, 1, -1, 1, 1]),
          gl.STATIC_DRAW
        );

        const positionLocation = gl.getAttribLocation(program, 'position');
        gl.enableVertexAttribArray(positionLocation);
        gl.vertexAttribPointer(positionLocation, 2, gl.FLOAT, false, 0, 0);

        // Textures
        const createTexture = (source: HTMLImageElement) => {
          const tex = gl.createTexture()!;
          gl.bindTexture(gl.TEXTURE_2D, tex);
          gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, source);
          gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
          gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
          gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
          gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
          return tex;
        };

        const texImage = createTexture(img);
        const texDepth = createTexture(depthImg);
        texturesRef.current = { image: texImage, depth: texDepth };

        setLoading(false);
      })
      .catch(err => console.error("Error loading images", err));
      
      return () => {
          // Cleanup could go here
      };
  }, [depthMapUrl, textureImageUrl, vsSource, fsSource]);

  return {
    loading,
    gl: glRef.current,
    program: programRef.current,
    textures: texturesRef.current
  };
};
