import { useEffect, useRef, useState } from 'react';
import { dataUriToImage } from '../utils/images';

const vs_src = `
  attribute vec2 position;
  varying vec2 texCoords;
  void main() {
    texCoords = (position + 1.0) / 2.0;
    texCoords.y = 1.0 - texCoords.y; // Flip Y for texture
    gl_Position = vec4(position, 0.0, 1.0);
  }
`;

const fs_src = `
  precision mediump float;
  uniform sampler2D u_image;
  uniform sampler2D u_depth;
  uniform vec2 u_resolution;
  uniform float u_focusDepth;
  uniform float u_aperture;
  
  varying vec2 texCoords;

  void main() {
    vec4 depthColor = texture2D(u_depth, texCoords);
    float depth = depthColor.r;
    
    // Calculate blur radius based on distance from focal plane
    float blur = abs(depth - u_focusDepth) * u_aperture * 0.05; // Scaling factor
    
    vec4 col = vec4(0.0);
    float totalWeight = 0.0;
    
    // Simple box blur (inefficient but works for proof of concept)
    // For better Bokeh, we'd need a disc sampling pattern or multiple passes
    const float samples = 5.0; // Defines loop range -samples to +samples
    
    if (blur < 0.001) {
        gl_FragColor = texture2D(u_image, texCoords);
        return;
    }

    // Limit max blur to prevent performance kill
    float radius = clamp(blur, 0.0, 0.02); 
    
    // Adaptive sampling based on blur amount? 
    // WebGL 1.0 loop limits must be constant.
    // We'll iterate fixed steps and scale offset by blur radius.
    
    for (float x = -2.0; x <= 2.0; x++) {
      for (float y = -2.0; y <= 2.0; y++) {
        vec2 offset = vec2(x, y) * radius;
        vec4 c = texture2D(u_image, texCoords + offset);
        col += c;
        totalWeight += 1.0;
      }
    }
    
    gl_FragColor = col / totalWeight;
  }
`;

interface BokehHook {
  focusDepth: number;
  aperture: number;
  setFocusDepth: (v: number) => void;
  setAperture: (v: number) => void;
  loading: boolean;
}

export const useBokeh = (
  canvasRef: React.RefObject<HTMLCanvasElement>,
  depthMapUrl: string,
  textureImageUrl: string
): BokehHook => {
  const [focusDepth, setFocusDepth] = useState(0.5);
  const [aperture, setAperture] = useState(1.0);
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

        // Quad Buffer (Full screen)
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

    // Bind Textures
    gl.activeTexture(gl.TEXTURE0);
    gl.bindTexture(gl.TEXTURE_2D, textures.image);
    gl.uniform1i(gl.getUniformLocation(program, 'u_image'), 0);

    gl.activeTexture(gl.TEXTURE1);
    gl.bindTexture(gl.TEXTURE_2D, textures.depth);
    gl.uniform1i(gl.getUniformLocation(program, 'u_depth'), 1);

    // Uniforms
    gl.uniform1f(gl.getUniformLocation(program, 'u_focusDepth'), focusDepth);
    gl.uniform1f(gl.getUniformLocation(program, 'u_aperture'), aperture);
    gl.uniform2f(gl.getUniformLocation(program, 'u_resolution'), gl.canvas.width, gl.canvas.height);

    gl.drawArrays(gl.TRIANGLES, 0, 6);
  }, [focusDepth, aperture, loading]);

  return {
    focusDepth,
    aperture,
    setFocusDepth,
    setAperture,
    loading
  };
};
