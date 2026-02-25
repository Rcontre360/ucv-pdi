import { useEffect, useState } from 'react';
import { useWebGLSetup } from './useWebGLSetup';
import { underwaterVertexShader, underwaterFragmentShader } from '../utils/shaders/underwater';
import { createBuffer } from '../utils/webgl_helpers';

interface UnderwaterHook {
  waterDensity: number;
  redAbsorb: number;
  aspectRatio: number;
  setWaterDensity: (v: number) => void;
  setRedAbsorb: (v: number) => void;
  loading: boolean;
}

export const useUnderwater = (
  canvasRef: React.RefObject<HTMLCanvasElement | null>,
  depthMapUrl: string,
  textureImageUrl: string
): UnderwaterHook => {
  const [waterDensity, setWaterDensity] = useState(0.5);
  const [redAbsorb, setRedAbsorb] = useState(0.8);
  
  const { loading, gl, program, textures, aspectRatio } = useWebGLSetup(
    canvasRef, depthMapUrl, textureImageUrl, underwaterVertexShader, underwaterFragmentShader
  );

  useEffect(() => {
    if (!gl || !program || !textures || loading) return;

    // Quad Buffer
    createBuffer(gl, new Float32Array([-1, -1, 1, -1, -1, 1, -1, 1, 1, -1, 1, 1]));
    const posLoc = gl.getAttribLocation(program, 'position');
    gl.enableVertexAttribArray(posLoc);
    gl.vertexAttribPointer(posLoc, 2, gl.FLOAT, false, 0, 0);

    const u_image = gl.getUniformLocation(program, 'u_image');
    const u_depth = gl.getUniformLocation(program, 'u_depth');
    const u_waterDensity = gl.getUniformLocation(program, 'u_waterDensity');
    const u_redAbsorb = gl.getUniformLocation(program, 'u_redAbsorb');

    const render = () => {
      gl.useProgram(program);
      gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, textures.image);
      gl.activeTexture(gl.TEXTURE1); gl.bindTexture(gl.TEXTURE_2D, textures.depth);
      
      gl.uniform1i(u_image, 0);
      gl.uniform1i(u_depth, 1);
      gl.uniform1f(u_waterDensity, waterDensity);
      gl.uniform1f(u_redAbsorb, redAbsorb);

      gl.drawArrays(gl.TRIANGLES, 0, 6);
    };

    render();
  }, [waterDensity, redAbsorb, loading, gl, program, textures]);

  return { waterDensity, redAbsorb, aspectRatio, setWaterDensity, setRedAbsorb, loading };
};
