import { useEffect, useState } from 'react';
import { useWebGLSetup } from './useWebGLSetup';
import { ssaoVertexShader, ssaoFragmentShader } from '../utils/shaders/ssao';
import { createBuffer } from '../utils/webgl_helpers';

interface SSAOHook {
  radius: number;
  bias: number;
  intensity: number;
  aspectRatio: number;
  setRadius: (v: number) => void;
  setBias: (v: number) => void;
  setIntensity: (v: number) => void;
  loading: boolean;
}

export const useSSAO = (
  canvasRef: React.RefObject<HTMLCanvasElement>,
  depthMapUrl: string,
  textureImageUrl: string
): SSAOHook => {
  const [radius, setRadius] = useState(0.05);
  const [bias, setBias] = useState(0.01);
  const [intensity, setIntensity] = useState(2.0);

  const { loading, gl, program, textures, aspectRatio } = useWebGLSetup(
    canvasRef, depthMapUrl, textureImageUrl, ssaoVertexShader, ssaoFragmentShader
  );

  useEffect(() => {
    if (!gl || !program || !textures || loading) return;

    createBuffer(gl, new Float32Array([-1, -1, 1, -1, -1, 1, -1, 1, 1, -1, 1, 1]));
    const posLoc = gl.getAttribLocation(program, 'position');
    gl.enableVertexAttribArray(posLoc);
    gl.vertexAttribPointer(posLoc, 2, gl.FLOAT, false, 0, 0);

    gl.useProgram(program);
    gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, textures.image);
    gl.activeTexture(gl.TEXTURE1); gl.bindTexture(gl.TEXTURE_2D, textures.depth);
    
    gl.uniform1i(gl.getUniformLocation(program, 'u_image'), 0);
    gl.uniform1i(gl.getUniformLocation(program, 'u_depth'), 1);
    gl.uniform1f(gl.getUniformLocation(program, 'u_radius'), radius);
    gl.uniform1f(gl.getUniformLocation(program, 'u_bias'), bias);
    gl.uniform1f(gl.getUniformLocation(program, 'u_intensity'), intensity);
    gl.uniform2f(gl.getUniformLocation(program, 'u_resolution'), gl.canvas.width, gl.canvas.height);

    gl.drawArrays(gl.TRIANGLES, 0, 6);
  }, [radius, bias, intensity, loading, gl, program, textures]);

  return { radius, bias, intensity, aspectRatio, setRadius, setBias, setIntensity, loading };
};
