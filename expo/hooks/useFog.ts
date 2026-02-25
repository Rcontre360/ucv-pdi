import { useEffect, useState } from 'react';
import { useWebGLSetup } from './useWebGLSetup';
import {basicVertexShader} from '../utils/shaders/common';
import { fogFragmentShader } from '../utils/shaders/fog';
import { createBuffer } from '../utils/webgl_helpers';

interface FogHook {
  fogDensity: number;
  fogNear: number;
  fogFar: number;
  aspectRatio: number;
  setFogDensity: (v: number) => void;
  setFogNear: (v: number) => void;
  setFogFar: (v: number) => void;
  loading: boolean;
}

export const useFog = (
  canvasRef: React.RefObject<HTMLCanvasElement | null>,
  depthMapUrl: string,
  textureImageUrl: string
): FogHook => {
  const [fogDensity, setFogDensity] = useState(0.8);
  const [fogNear, setFogNear] = useState(0.0);
  const [fogFar, setFogFar] = useState(1.0);

  const { loading, gl, program, textures, aspectRatio } = useWebGLSetup(
    canvasRef, depthMapUrl, textureImageUrl, basicVertexShader, fogFragmentShader
  );

  useEffect(() => {
    if (!gl || !program || !textures || loading) return;

    // Quad Buffer
    createBuffer(gl, new Float32Array([-1, -1, 1, -1, -1, 1, -1, 1, 1, -1, 1, 1]));
    const posLoc = gl.getAttribLocation(program, 'position');
    gl.enableVertexAttribArray(posLoc);
    gl.vertexAttribPointer(posLoc, 2, gl.FLOAT, false, 0, 0);

    gl.useProgram(program);
    gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, textures.image);
    gl.activeTexture(gl.TEXTURE1); gl.bindTexture(gl.TEXTURE_2D, textures.depth);
    
    gl.uniform1i(gl.getUniformLocation(program, 'u_image'), 0);
    gl.uniform1i(gl.getUniformLocation(program, 'u_depth'), 1);
    gl.uniform1f(gl.getUniformLocation(program, 'u_fogDensity'), fogDensity);
    gl.uniform1f(gl.getUniformLocation(program, 'u_fogNear'), fogNear);
    gl.uniform1f(gl.getUniformLocation(program, 'u_fogFar'), fogFar);
    gl.uniform3f(gl.getUniformLocation(program, 'u_fogColor'), 0.8, 0.8, 0.9);

    gl.drawArrays(gl.TRIANGLES, 0, 6);
  }, [fogDensity, fogNear, fogFar, loading, gl, program, textures]);

  return { fogDensity, fogNear, fogFar, aspectRatio, setFogDensity, setFogNear, setFogFar, loading };
};