import { useEffect, useState } from 'react';
import { useWebGLSetup } from './useWebGLSetup';
import { createBuffer } from '../utils/webgl_helpers';

const vertexShader = `
  attribute vec2 position;
  varying vec2 texCoords;
  void main() {
    texCoords = (position + 1.0) / 2.0;
    texCoords.y = 1.0 - texCoords.y;
    gl_Position = vec4(position, 0.0, 1.0);
  }
`;

const fragmentShader = `
  precision mediump float;
  uniform sampler2D u_depth;
  uniform float u_threshold;
  uniform float u_thickness;
  uniform vec2 u_resolution;
  varying vec2 texCoords;
  void main() {
    float x = u_thickness / u_resolution.x;
    float y = u_thickness / u_resolution.y;
    float d_tl = texture2D(u_depth, texCoords + vec2(-x, -y)).r;
    float d_t  = texture2D(u_depth, texCoords + vec2( 0, -y)).r;
    float d_tr = texture2D(u_depth, texCoords + vec2( x, -y)).r;
    float d_l  = texture2D(u_depth, texCoords + vec2(-x,  0)).r;
    float d_r  = texture2D(u_depth, texCoords + vec2( x,  0)).r;
    float d_bl = texture2D(u_depth, texCoords + vec2(-x,  y)).r;
    float d_b  = texture2D(u_depth, texCoords + vec2( 0,  y)).r;
    float d_br = texture2D(u_depth, texCoords + vec2( x,  y)).r;
    float gx = (d_tl + 2.0*d_l + d_bl) - (d_tr + 2.0*d_r + d_br);
    float gy = (d_tl + 2.0*d_t + d_tr) - (d_bl + 2.0*d_b + d_br);
    float g = sqrt(gx*gx + gy*gy);
    float edge = g > u_threshold ? 0.0 : 1.0;
    gl_FragColor = vec4(vec3(edge), 1.0);
  }
`;

interface DepthEdgeHook {
  threshold: number;
  thickness: number;
  setThreshold: (v: number) => void;
  setThickness: (v: number) => void;
  loading: boolean;
}

export const useDepthEdges = (
  canvasRef: React.RefObject<HTMLCanvasElement>,
  depthMapUrl: string,
  textureImageUrl: string
): DepthEdgeHook => {
  const [threshold, setThreshold] = useState(0.5);
  const [thickness, setThickness] = useState(1.0);

  const { loading, gl, program, textures } = useWebGLSetup(
    canvasRef, depthMapUrl, textureImageUrl, vertexShader, fragmentShader
  );

  useEffect(() => {
    if (!gl || !program || !textures || loading) return;

    createBuffer(gl, new Float32Array([-1, -1, 1, -1, -1, 1, -1, 1, 1, -1, 1, 1]));
    const posLoc = gl.getAttribLocation(program, 'position');
    gl.enableVertexAttribArray(posLoc);
    gl.vertexAttribPointer(posLoc, 2, gl.FLOAT, false, 0, 0);

    gl.useProgram(program);
    gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, textures.depth);
    gl.uniform1i(gl.getUniformLocation(program, 'u_depth'), 0);
    gl.uniform1f(gl.getUniformLocation(program, 'u_threshold'), threshold * 0.1);
    gl.uniform1f(gl.getUniformLocation(program, 'u_thickness'), thickness);
    gl.uniform2f(gl.getUniformLocation(program, 'u_resolution'), gl.canvas.width, gl.canvas.height);

    gl.drawArrays(gl.TRIANGLES, 0, 6);
  }, [threshold, thickness, loading, gl, program, textures]);

  return { threshold, thickness, setThreshold, setThickness, loading };
};