import { useEffect, useState } from 'react';
import { useWebGLSetup } from './useWebGLSetup';
import { createBuffer } from '../utils/webgl_helpers';
import { edgesVertexShader, edgesFragmentShader } from '../utils/shaders/edges';

interface DepthEdgeHook {
  threshold: number;
  thickness: number;
  aspectRatio: number;
  setThreshold: (v: number) => void;
  setThickness: (v: number) => void;
  loading: boolean;
}

export const useDepthEdges = (
  canvasRef: React.RefObject<HTMLCanvasElement | null>,
  depthMapUrl: string,
  textureImageUrl: string
): DepthEdgeHook => {
  const [threshold, setThreshold] = useState(0.5);
  const [thickness, setThickness] = useState(1.0);

  const { loading, gl, program, textures, aspectRatio } = useWebGLSetup(
    canvasRef, depthMapUrl, textureImageUrl, edgesVertexShader, edgesFragmentShader
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

  return { threshold, thickness, aspectRatio, setThreshold, setThickness, loading };
};