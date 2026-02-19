import { useEffect, useRef, useState } from 'react';
import { imageHelper, dataUriToImage } from '../utils/images';
import { relightingVertexShader, relightingFragmentShader } from '../utils/shaders/relighting';
import { lightVertexShader, lightFragmentShader } from '../utils/shaders/light';

interface WebGLHook {
  lightPos: number[];
  lightIntensity: number;
  textureLighting: number;
  loading: boolean;
  setLightPos: (pos: number[] | ((prev: number[]) => number[])) => void;
  setLightIntensity: (intensity: number) => void;
  setTextureLighting: (lighting: number) => void;
}

export const useWebGL = (
  canvasRef: React.RefObject<HTMLCanvasElement>,
  depthImageUri: string,
  textureImageUri: string
): WebGLHook => {
  const [lightPos, setLightPos] = useState([0, 0, -1]);
  const [lightIntensity, setLightIntensity] = useState(0.4);
  const [textureLighting, setTextureLighting] = useState(3);
  const [isLoading, setIsLoading] = useState(true);

  const glRef = useRef<WebGLRenderingContext | null>(null);
  const shaderProgramRef = useRef<any>(null);
  const lightProgramRef = useRef<any>(null); // New ref for light shader
  const bufferRef = useRef<any>({});
  const vertexCountRef = useRef<number>(0);
  const imagesRef = useRef<{ depthImg: HTMLImageElement, textureImg: HTMLImageElement } | null>(null);

  const createShader = (gl: WebGLRenderingContext, type: number, src: string) => {
    const shader = gl.createShader(type);
    if (!shader) return null;
    gl.shaderSource(shader, src);
    gl.compileShader(shader);
    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
      console.error("Shader compilation error:", gl.getShaderInfoLog(shader));
      return null;
    }
    return shader;
  };

  const initShaderProgram = (gl: WebGLRenderingContext) => {
    // 1. Relighting Program
    const vs = createShader(gl, gl.VERTEX_SHADER, relightingVertexShader);
    const fs = createShader(gl, gl.FRAGMENT_SHADER, relightingFragmentShader);
    if (!vs || !fs) return null;

    const program = gl.createProgram();
    if (!program) return null;
    gl.attachShader(program, vs);
    gl.attachShader(program, fs);
    gl.linkProgram(program);
    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
      console.error("Shader linking error:", gl.getProgramInfoLog(program));
      return null;
    }
    
    // 2. Light Indicator Program
    const lVs = createShader(gl, gl.VERTEX_SHADER, lightVertexShader);
    const lFs = createShader(gl, gl.FRAGMENT_SHADER, lightFragmentShader);
    if (!lVs || !lFs) return null; // Should ideally handle error better

    const lightProgram = gl.createProgram();
    if (!lightProgram) return null;
    gl.attachShader(lightProgram, lVs);
    gl.attachShader(lightProgram, lFs);
    gl.linkProgram(lightProgram);
    
    return { program, lightProgram };
  };

  const setupBuffersAndAttributes = (gl: WebGLRenderingContext, program: any, lightProgram: any, depthImg: HTMLImageElement, textureImg: HTMLImageElement) => {
    imageHelper.reset();
    const mesh = new Float32Array(imageHelper.getMesh(5, depthImg));
    const normals = new Float32Array(imageHelper.getNormals(depthImg));
    vertexCountRef.current = mesh.length / 3;

    // --- Mesh Buffers ---
    const positionAttr = gl.getAttribLocation(program, 'vPos');
    // Note: We enable attributes per-draw-call usually, or keep them enabled if layouts match.
    const positionBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, mesh, gl.STATIC_DRAW);

    const normalAttr = gl.getAttribLocation(program, 'normal');
    const normalBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, normalBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, normals, gl.STATIC_DRAW);

    const texture = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, texture);
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, textureImg);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);

    const depthTexture = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, depthTexture);
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, depthImg);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);

    const lightPosBuffer = gl.createBuffer();

    bufferRef.current = { positionBuffer, normalBuffer, texture, depthTexture, lightPosBuffer };
    
    // Store attrib locations for later use
    program.positionAttr = positionAttr;
    program.normalAttr = normalAttr;
    
    lightProgram.positionAttr = gl.getAttribLocation(lightProgram, 'position');

    // Cache uniform locations
    program.imgSizeUnif = gl.getUniformLocation(program, 'imgSize');
    program.minMaxZUnif = gl.getUniformLocation(program, 'minMaxZ');
    program.lightPosUnif = gl.getUniformLocation(program, 'lightPos');
    program.texSamplerUnif = gl.getUniformLocation(program, 'texSampler');
    program.depthSamplerUnif = gl.getUniformLocation(program, 'depthSampler');
    program.textureLightingUnif = gl.getUniformLocation(program, 'textureLighting');
    program.lightIntensityUnif = gl.getUniformLocation(program, 'lightIntensity');
  };

  useEffect(() => {
    if (!depthImageUri || !textureImageUri || !canvasRef.current) return;

    let isMounted = true;
    const canvas = canvasRef.current;
    const gl = canvas.getContext('webgl');
    if (!gl) {
      console.error("WebGL not supported");
      return;
    }
    glRef.current = gl;

    setIsLoading(true);
    Promise.all([dataUriToImage(depthImageUri), dataUriToImage(textureImageUri)])
      .then(([depthImg, textureImg]) => {
        if (!isMounted) return;

        imagesRef.current = { depthImg, textureImg };
        
        // Update canvas size to match image aspect ratio
        const [width, height] = imageHelper.getImageSize(depthImg);
        canvas.width = width;
        canvas.height = height;

        const programs = initShaderProgram(gl);
        if (programs) {
          shaderProgramRef.current = programs.program;
          lightProgramRef.current = programs.lightProgram;
          
          setupBuffersAndAttributes(gl, programs.program, programs.lightProgram, depthImg, textureImg);
        }
        setIsLoading(false);
      })
      .catch(err => {
        console.error("Error loading WebGL images:", err);
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
      if (gl) {
        if (shaderProgramRef.current) gl.deleteProgram(shaderProgramRef.current);
        if (lightProgramRef.current) gl.deleteProgram(lightProgramRef.current);
        // ... cleanup buffers ...
      }
    };
  }, [depthImageUri, textureImageUri, canvasRef]);

  const drawFrame = () => {
    const gl = glRef.current;
    const prog = shaderProgramRef.current;
    const lightProg = lightProgramRef.current;
    const canvas = canvasRef.current;
    if (!gl || !prog || !lightProg || !canvas || !imagesRef.current) return;

    const { depthImg } = imagesRef.current;

    gl.viewport(0, 0, canvas.width, canvas.height);
    gl.clearColor(0, 0, 0, 1);
    gl.enable(gl.DEPTH_TEST);
    // Enable blending for the light glow
    gl.enable(gl.BLEND);
    gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
    
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);

    // --- PASS 1: Draw Mesh ---
    gl.useProgram(prog);

    gl.bindBuffer(gl.ARRAY_BUFFER, bufferRef.current.positionBuffer);
    gl.enableVertexAttribArray(prog.positionAttr);
    gl.vertexAttribPointer(prog.positionAttr, 3, gl.FLOAT, false, 0, 0);

    gl.bindBuffer(gl.ARRAY_BUFFER, bufferRef.current.normalBuffer);
    gl.enableVertexAttribArray(prog.normalAttr);
    gl.vertexAttribPointer(prog.normalAttr, 3, gl.FLOAT, false, 0, 0);

    gl.activeTexture(gl.TEXTURE0);
    gl.bindTexture(gl.TEXTURE_2D, bufferRef.current.texture);

    gl.activeTexture(gl.TEXTURE1);
    gl.bindTexture(gl.TEXTURE_2D, bufferRef.current.depthTexture);

    gl.uniform2fv(prog.imgSizeUnif, new Float32Array(imageHelper.getImageSize(depthImg)));
    gl.uniform2fv(prog.minMaxZUnif, new Float32Array([imageHelper.minZ, imageHelper.maxZ]));
    gl.uniform3fv(prog.lightPosUnif, new Float32Array(lightPos));
    gl.uniform1i(prog.texSamplerUnif, 0);
    gl.uniform1i(prog.depthSamplerUnif, 1);
    gl.uniform1i(prog.textureLightingUnif, textureLighting);
    gl.uniform1f(prog.lightIntensityUnif, lightIntensity);

    gl.drawArrays(gl.TRIANGLES, 0, vertexCountRef.current);
    
    // Disable normal attr as next shader doesn't use it
    gl.disableVertexAttribArray(prog.normalAttr);

    // --- PASS 2: Draw Light Indicator ---
    if (textureLighting !== 1) { // Only show light if lighting is enabled
        gl.useProgram(lightProg);
        
        gl.bindBuffer(gl.ARRAY_BUFFER, bufferRef.current.lightPosBuffer);
        // Update buffer with current light position
        gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(lightPos), gl.DYNAMIC_DRAW);
        
        gl.enableVertexAttribArray(lightProg.positionAttr);
        gl.vertexAttribPointer(lightProg.positionAttr, 3, gl.FLOAT, false, 0, 0);
        
        // Draw 1 point
        gl.drawArrays(gl.POINTS, 0, 1);
    }
  };

  useEffect(() => {
    if (!isLoading) {
      drawFrame();
    }
  }, [isLoading, lightPos, lightIntensity, textureLighting]);

  return {
    lightPos,
    lightIntensity,
    textureLighting,
    loading: isLoading,
    setLightPos,
    setLightIntensity,
    setTextureLighting,
  };
};
