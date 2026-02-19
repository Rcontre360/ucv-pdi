import { useEffect, useState } from 'react';
import { useWebGL2D } from './useWebGL2D';
import { fogVertexShader, fogFragmentShader } from '../utils/shaders/fog';

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

  const { loading, gl, program, textures } = useWebGL2D(
    canvasRef,
    depthMapUrl,
    textureImageUrl,
    fogVertexShader,
    fogFragmentShader
  );

  useEffect(() => {
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
  }, [fogDensity, fogNear, fogFar, loading, gl, program, textures]);

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