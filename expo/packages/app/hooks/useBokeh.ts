import { useEffect, useState } from 'react';
import { useWebGLSetup } from './useWebGLSetup';
import { bokehVertexShader, bokehFragmentShader } from '../utils/shaders/bokeh';
import { createBuffer } from '../utils/webgl_helpers';

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

  const { loading, gl, program, textures } = useWebGLSetup(
    canvasRef, depthMapUrl, textureImageUrl, bokehVertexShader, bokehFragmentShader
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
    gl.uniform1f(gl.getUniformLocation(program, 'u_focusDepth'), focusDepth);
    gl.uniform1f(gl.getUniformLocation(program, 'u_aperture'), aperture);
    gl.uniform2f(gl.getUniformLocation(program, 'u_resolution'), gl.canvas.width, gl.canvas.height);

    gl.drawArrays(gl.TRIANGLES, 0, 6);
  }, [focusDepth, aperture, loading, gl, program, textures]);

  return { focusDepth, aperture, setFocusDepth, setAperture, loading };
};
