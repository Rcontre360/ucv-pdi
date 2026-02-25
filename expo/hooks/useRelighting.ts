import { useEffect, useRef, useState } from 'react';
import { useWebGLSetup } from './useWebGLSetup';
import { imageHelper } from '../utils/images';
import { 
  relightingVertexShader, 
  relightingFragmentShader, 
  lightVertexShader, 
  lightFragmentShader 
} from '../utils/shaders/relighting';
import { createProgram, createBuffer } from '../utils/webgl_helpers';

interface RelightingHook {
  lightPos: number[];
  lightIntensity: number;
  textureLighting: number;
  loading: boolean;
  aspectRatio: number;
  setLightPos: (pos: number[] | ((prev: number[]) => number[])) => void;
  setLightIntensity: (intensity: number) => void;
  setTextureLighting: (lighting: number) => void;
}

export const useRelighting = (
  canvasRef: React.RefObject<HTMLCanvasElement | null>,
  depthImageUri: string,
  textureImageUri: string
): RelightingHook => {
  const [lightPos, setLightPos] = useState([0, 0, -1]);
  const [lightIntensity, setLightIntensity] = useState(0.4);
  const [textureLighting, setTextureLighting] = useState(3);

  const lightProgramRef = useRef<WebGLProgram | null>(null);
  const bufferRef = useRef<any>({});
  const vertexCountRef = useRef<number>(0);

  const { loading, gl, program, textures, images, aspectRatio } = useWebGLSetup(
    canvasRef, depthImageUri, textureImageUri, relightingVertexShader, relightingFragmentShader
  );

  // Effect 1: Setup
  useEffect(() => {
    if (!gl || !program || !textures || !images || loading) return;

    // Clean up old buffers if they exist
    if (bufferRef.current.positionBuffer) gl.deleteBuffer(bufferRef.current.positionBuffer);
    if (bufferRef.current.normalBuffer) gl.deleteBuffer(bufferRef.current.normalBuffer);
    if (bufferRef.current.lightPosBuffer) gl.deleteBuffer(bufferRef.current.lightPosBuffer);
    bufferRef.current = {};

    lightProgramRef.current = createProgram(gl, lightVertexShader, lightFragmentShader);
    
    // Explicitly reset before calculating to be 100% sure
    imageHelper.reset();
    const mesh = new Float32Array(imageHelper.getMesh(5, images.depth));
    const normals = new Float32Array(imageHelper.getNormals(images.depth));
    vertexCountRef.current = mesh.length / 3;

    bufferRef.current.positionBuffer = createBuffer(gl, mesh);
    bufferRef.current.normalBuffer = createBuffer(gl, normals);
    bufferRef.current.lightPosBuffer = gl.createBuffer(); 

    bufferRef.current.positionAttr = gl.getAttribLocation(program, 'vPos');
    bufferRef.current.normalAttr = gl.getAttribLocation(program, 'normal');
    bufferRef.current.imgSizeUnif = gl.getUniformLocation(program, 'imgSize');
    bufferRef.current.minMaxZUnif = gl.getUniformLocation(program, 'minMaxZ');
    bufferRef.current.lightPosUnif = gl.getUniformLocation(program, 'lightPos');
    bufferRef.current.texSamplerUnif = gl.getUniformLocation(program, 'texSampler');
    bufferRef.current.depthSamplerUnif = gl.getUniformLocation(program, 'depthSampler');
    bufferRef.current.textureLightingUnif = gl.getUniformLocation(program, 'textureLighting');
    bufferRef.current.lightIntensityUnif = gl.getUniformLocation(program, 'lightIntensity');

  }, [loading, gl, program, textures, images]);

  // Effect 2: Draw Loop
  useEffect(() => {
    const lightProg = lightProgramRef.current;
    if (!gl || !program || !lightProg || loading || !images) return;

    gl.viewport(0, 0, gl.canvas.width, gl.canvas.height);
    gl.clearColor(0, 0, 0, 1);
    gl.enable(gl.DEPTH_TEST);
    gl.enable(gl.BLEND);
    gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);

    // Pass 1: Mesh
    gl.useProgram(program);
    gl.bindBuffer(gl.ARRAY_BUFFER, bufferRef.current.positionBuffer);
    gl.enableVertexAttribArray(bufferRef.current.positionAttr);
    gl.vertexAttribPointer(bufferRef.current.positionAttr, 3, gl.FLOAT, false, 0, 0);

    gl.bindBuffer(gl.ARRAY_BUFFER, bufferRef.current.normalBuffer);
    gl.enableVertexAttribArray(bufferRef.current.normalAttr);
    gl.vertexAttribPointer(bufferRef.current.normalAttr, 3, gl.FLOAT, false, 0, 0);

    gl.activeTexture(gl.TEXTURE0);
    gl.bindTexture(gl.TEXTURE_2D, textures!.image);
    gl.activeTexture(gl.TEXTURE1);
    gl.bindTexture(gl.TEXTURE_2D, textures!.depth);

    gl.uniform2fv(bufferRef.current.imgSizeUnif, new Float32Array(imageHelper.getImageSize(images.depth)));
    gl.uniform2fv(bufferRef.current.minMaxZUnif, new Float32Array([imageHelper.minZ, imageHelper.maxZ]));
    gl.uniform3fv(bufferRef.current.lightPosUnif, new Float32Array(lightPos));
    gl.uniform1i(bufferRef.current.texSamplerUnif, 0);
    gl.uniform1i(bufferRef.current.depthSamplerUnif, 1);
    gl.uniform1i(bufferRef.current.textureLightingUnif, textureLighting);
    gl.uniform1f(bufferRef.current.lightIntensityUnif, lightIntensity);

    gl.drawArrays(gl.TRIANGLES, 0, vertexCountRef.current);
    gl.disableVertexAttribArray(bufferRef.current.normalAttr);

    // Pass 2: Light Ball
    if (textureLighting !== 1) {
        gl.useProgram(lightProg);
        gl.bindBuffer(gl.ARRAY_BUFFER, bufferRef.current.lightPosBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(lightPos), gl.DYNAMIC_DRAW);
        const lPosAttr = gl.getAttribLocation(lightProg, 'position');
        gl.enableVertexAttribArray(lPosAttr);
        gl.vertexAttribPointer(lPosAttr, 3, gl.FLOAT, false, 0, 0);
        gl.drawArrays(gl.POINTS, 0, 1);
    }
  }, [loading, gl, program, textures, images, lightPos, lightIntensity, textureLighting]);

    return { lightPos, lightIntensity, textureLighting, loading, aspectRatio, setLightPos, setLightIntensity, setTextureLighting };

  };

  