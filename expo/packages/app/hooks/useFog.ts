import { useEffect, useRef, useState } from 'react';
import { dataUriToImage } from '../utils/images';

const vs_src = `
  attribute vec2 position;
  varying vec2 texCoords;
  void main() {
    texCoords = (position + 1.0) / 2.0;
    texCoords.y = 1.0 - texCoords.y; // Flip Y
    gl_Position = vec4(position, 0.0, 1.0);
  }
`;

const fs_src = `
  precision mediump float;
  uniform sampler2D u_image;
  uniform sampler2D u_depth;
  uniform float u_fogDensity;
  uniform float u_fogNear;
  uniform float u_fogFar;
  uniform vec3 u_fogColor;
  
  varying vec2 texCoords;

  void main() {
    vec4 color = texture2D(u_image, texCoords);
    vec4 depthVal = texture2D(u_depth, texCoords);
    float depth = depthVal.r;
    
    // Linear Fog
    // Factor = (End - Dist) / (End - Start)
    // Here we want smooth transition based on depth.
    // Let's define fogFactor: 0 = No Fog, 1 = Full Fog
    
    float fogFactor = smoothstep(u_fogNear, u_fogFar, depth);
    
    // Exponential fog option:
    // float fogFactor = 1.0 - exp(-depth * u_fogDensity);
    
    // Mix with density
    fogFactor *= u_fogDensity;
    fogFactor = clamp(fogFactor, 0.0, 1.0);
    
    vec3 finalColor = mix(color.rgb, u_fogColor, fogFactor);
    
    gl_FragColor = vec4(finalColor, color.a);
  }
`;

interface FogHook {
  fogDensity: number;
  fogNear: number;
  fogFar: number;
  setFogDensity: (v: number) => void;
  setFogNear: (v: number) => void;
  setFogFar: (v: number) => void;
  loading: boolean;
}

export const useFog = (
  canvasRef: React.RefObject<HTMLCanvasElement>,
  depthMapUrl: string,
  textureImageUrl: string
): FogHook => {
  const [fogDensity, setFogDensity] = useState(0.8);
  const [fogNear, setFogNear] = useState(0.0);
  const [fogFar, setFogFar] = useState(1.0);
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
        const vs = gl.createShader(gl.VERTEX_SHADER)!;
        gl.shaderSource(vs, vs_src);
        gl.compileShader(vs);

        const fs = gl.createShader(gl.FRAGMENT_SHADER)!;
        gl.shaderSource(fs, fs_src);
        gl.compileShader(fs);
        
        if (!gl.getShaderParameter(fs, gl.COMPILE_STATUS)) {
            console.error(gl.getShaderInfoLog(fs));
        }

        const program = gl.createProgram()!;
        gl.attachShader(program, vs);
        gl.attachShader(program, fs);
        gl.linkProgram(program);
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
      });
  }, [depthMapUrl, textureImageUrl]);

  useEffect(() => {
    const gl = glRef.current;
    const program = programRef.current;
    const textures = texturesRef.current;

    if (!gl || !program || !textures || loading) return;

    gl.useProgram(program);

    gl.activeTexture(gl.TEXTURE0);
    gl.bindTexture(gl.TEXTURE_2D, textures.image);
    gl.uniform1i(gl.getUniformLocation(program, 'u_image'), 0);

    gl.activeTexture(gl.TEXTURE1);
    gl.bindTexture(gl.TEXTURE_2D, textures.depth);
    gl.uniform1i(gl.getUniformLocation(program, 'u_depth'), 1);

    gl.uniform1f(gl.getUniformLocation(program, 'u_fogDensity'), fogDensity);
    gl.uniform1f(gl.getUniformLocation(program, 'u_fogNear'), fogNear);
    gl.uniform1f(gl.getUniformLocation(program, 'u_fogFar'), fogFar);
    gl.uniform3f(gl.getUniformLocation(program, 'u_fogColor'), 0.8, 0.8, 0.9); // Light blueish gray fog

    gl.drawArrays(gl.TRIANGLES, 0, 6);
  }, [fogDensity, fogNear, fogFar, loading]);

  return {
    fogDensity,
    fogNear,
    fogFar,
    setFogDensity,
    setFogNear,
    setFogFar,
    loading
  };
};
