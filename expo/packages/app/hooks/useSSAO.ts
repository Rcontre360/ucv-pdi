import { useEffect, useState } from 'react';
import { useWebGL2D } from './useWebGL2D';
import { ssaoVertexShader, ssaoFragmentShader } from '../utils/shaders/ssao';

interface SSAOHook {
  radius: number;
  bias: number;
  intensity: number;
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
  const [radius, setRadius] = useState(0.05); // Search radius
  const [bias, setBias] = useState(0.01);     // Depth threshold
  const [intensity, setIntensity] = useState(2.0); // Shadow strength

  const { loading, gl, program, textures } = useWebGL2D(
    canvasRef,
    depthMapUrl,
    textureImageUrl,
    ssaoVertexShader,
    ssaoFragmentShader
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

    gl.uniform1f(gl.getUniformLocation(program, 'u_radius'), radius);
    gl.uniform1f(gl.getUniformLocation(program, 'u_bias'), bias);
    gl.uniform1f(gl.getUniformLocation(program, 'u_intensity'), intensity);
    gl.uniform2f(gl.getUniformLocation(program, 'u_resolution'), gl.canvas.width, gl.canvas.height);

    gl.drawArrays(gl.TRIANGLES, 0, 6);
  }, [radius, bias, intensity, loading, gl, program, textures]);

  return {
    radius,
    bias,
    intensity,
    setRadius,
    setBias,
    setIntensity,
    loading
  };
};
