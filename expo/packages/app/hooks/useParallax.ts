import { useEffect, useRef, useState } from 'react';
import { useWebGL2D } from './useWebGL2D';

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
  uniform sampler2D u_image;
  uniform sampler2D u_depth;
  uniform vec2 u_mouse;
  uniform float u_intensity;

  varying vec2 texCoords;

  void main() {
    vec4 depthVal = texture2D(u_depth, texCoords);
    float depth = depthVal.r; // 0 (Far) to 1 (Near) in our current inverted logic? 
    // Wait, usually white (1.0) is near. Let's assume 1.0 is near.
    
    // Parallax Offset
    // Near pixels should move MORE than far pixels.
    // Offset = MousePos * Depth * Intensity
    
    vec2 offset = u_mouse * depth * u_intensity;
    
    // We subtract offset because if we move camera RIGHT, objects move LEFT.
    vec2 parallaxCoords = texCoords - offset;
    
    // Clamp to prevent texture wrapping artifacts at edges
    // parallaxCoords = clamp(parallaxCoords, 0.001, 0.999);
    
    // Better: Check bounds and render black if out of bounds
    if (parallaxCoords.x < 0.0 || parallaxCoords.x > 1.0 || parallaxCoords.y < 0.0 || parallaxCoords.y > 1.0) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0); // Black borders
    } else {
        gl_FragColor = texture2D(u_image, parallaxCoords);
    }
  }
`;

interface ParallaxHook {
  intensity: number;
  setIntensity: (v: number) => void;
  loading: boolean;
}

export const useParallax = (
  canvasRef: React.RefObject<HTMLCanvasElement>,
  depthMapUrl: string,
  textureImageUrl: string
): ParallaxHook => {
  const [intensity, setIntensity] = useState(0.02); // subtle default
  const mouseRef = useRef<{ x: number, y: number }>({ x: 0, y: 0 });

  const { loading, gl, program, textures } = useWebGL2D(
    canvasRef,
    depthMapUrl,
    textureImageUrl,
    vertexShader,
    fragmentShader
  );

  useEffect(() => {
    if (!canvasRef.current) return;
    
    const handleMouseMove = (e: MouseEvent) => {
        const rect = canvasRef.current!.getBoundingClientRect();
        // Calculate normalized mouse position from center (-1 to 1)
        const x = ((e.clientX - rect.left) / rect.width) * 2 - 1;
        const y = ((e.clientY - rect.top) / rect.height) * 2 - 1;
        
        // Invert Y because WebGL/Screen coord diff
        mouseRef.current = { x, y: -y };
    };
    
    // Attach to window so effect works even if mouse leaves canvas slightly
    window.addEventListener('mousemove', handleMouseMove);
    return () => window.removeEventListener('mousemove', handleMouseMove);
  }, []);

  useEffect(() => {
    if (!gl || !program || !textures || loading) return;

    let animationFrameId: number;

    const render = () => {
      gl.useProgram(program);

      gl.activeTexture(gl.TEXTURE0);
      gl.bindTexture(gl.TEXTURE_2D, textures.image);
      gl.uniform1i(gl.getUniformLocation(program, 'u_image'), 0);

      gl.activeTexture(gl.TEXTURE1);
      gl.bindTexture(gl.TEXTURE_2D, textures.depth);
      gl.uniform1i(gl.getUniformLocation(program, 'u_depth'), 1);

      gl.uniform2f(gl.getUniformLocation(program, 'u_mouse'), mouseRef.current.x, mouseRef.current.y);
      gl.uniform1f(gl.getUniformLocation(program, 'u_intensity'), intensity);

      gl.drawArrays(gl.TRIANGLES, 0, 6);
      
      animationFrameId = requestAnimationFrame(render);
    };

    render();
    
    return () => cancelAnimationFrame(animationFrameId);
  }, [intensity, loading, gl, program, textures]);

  return {
    intensity,
    setIntensity,
    loading
  };
};
