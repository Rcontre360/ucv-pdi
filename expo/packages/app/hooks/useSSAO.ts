import { useEffect } from 'react';
import { useWebGLSetup } from './useWebGLSetup';
import { ssaoVertexShader, ssaoFragmentShader } from '../utils/shaders/ssao';
import { createBuffer } from '../utils/webgl_helpers';

export const useSSAO = (canvasRef: React.RefObject<HTMLCanvasElement>, depthMapUrl: string, textureImageUrl: string) => {
  const { loading, gl, program, textures } = useWebGLSetup(
    canvasRef, depthMapUrl, textureImageUrl, ssaoVertexShader, ssaoFragmentShader
  );

  useEffect(() => {
    if (!gl || !program || !textures || loading) return;

    // 1. Setup Quad Buffer (Done once when program is ready)
    createBuffer(gl, new Float32Array([-1,-1, 1,-1, -1,1, -1,1, 1,-1, 1,1]));
    const posLoc = gl.getAttribLocation(program, 'position');
    gl.enableVertexAttribArray(posLoc);
    gl.vertexAttribPointer(posLoc, 2, gl.FLOAT, false, 0, 0);

    // 2. Draw Call
    gl.useProgram(program);
    gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, textures.image);
    gl.activeTexture(gl.TEXTURE1); gl.bindTexture(gl.TEXTURE_2D, textures.depth);
    
    gl.uniform1i(gl.getUniformLocation(program, 'u_image'), 0);
    gl.uniform1i(gl.getUniformLocation(program, 'u_depth'), 1);
    gl.uniform1f(gl.getUniformLocation(program, 'u_radius'), 0.05);
    gl.uniform1f(gl.getUniformLocation(program, 'u_intensity'), 2.0);
    gl.uniform2f(gl.getUniformLocation(program, 'u_resolution'), gl.canvas.width, gl.canvas.height);

    gl.drawArrays(gl.TRIANGLES, 0, 6);
  }, [loading, gl, program, textures]);

  return { loading, radius: 0.05, setRadius: () => {}, bias: 0.01, setBias: () => {}, intensity: 2.0, setIntensity: () => {} };
};