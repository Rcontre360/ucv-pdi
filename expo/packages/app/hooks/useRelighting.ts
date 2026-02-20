import { useEffect, useRef, useState } from 'react';
import { useWebGLSetup } from './useWebGLSetup';
import { imageHelper } from '../utils/images';
import { relightingVertexShader, relightingFragmentShader } from '../utils/shaders/relighting';
import { lightVertexShader, lightFragmentShader } from '../utils/shaders/light';
import { createProgram, createBuffer } from '../utils/webgl_helpers';

interface RelightingHook {
  lightPos: number[];
  lightIntensity: number;
  textureLighting: number;
  loading: boolean;
  setLightPos: (pos: number[] | ((prev: number[]) => number[])) => void;
  setLightIntensity: (intensity: number) => void;
  setTextureLighting: (lighting: number) => void;
}

export const useRelighting = (
  canvasRef: React.RefObject<HTMLCanvasElement>,
  depthImageUri: string,
  textureImageUri: string
): RelightingHook => {
  const [lightPos, setLightPos] = useState([0, 0, -1]);
  const [lightIntensity, setLightIntensity] = useState(0.4);
  const [textureLighting, setTextureLighting] = useState(3);

  const lightProgramRef = useRef<WebGLProgram | null>(null);
  const bufferRef = useRef<any>({});
  const vertexCountRef = useRef<number>(0);

  const { loading, gl, program, textures, images } = useWebGLSetup(
    canvasRef, depthImageUri, textureImageUri, relightingVertexShader, relightingFragmentShader
  );

  useEffect(() => {
    // This effect now handles both initialization (if needed) and drawing.
    // The setup logic will only run once when textures/images are ready.
    
    if (!gl || !program || !textures || !images || loading) return;

    // --- 1. Initialization (Run only once when program changes) ---
    if (!lightProgramRef.current) {
        lightProgramRef.current = createProgram(gl, lightVertexShader, lightFragmentShader);
        
        const mesh = new Float32Array(imageHelper.getMesh(5, images.depth));
        const normals = new Float32Array(imageHelper.getNormals(images.depth));
        vertexCountRef.current = mesh.length / 3;

        bufferRef.current.positionBuffer = createBuffer(gl, mesh);
        bufferRef.current.normalBuffer = createBuffer(gl, normals);
        bufferRef.current.lightPosBuffer = gl.createBuffer(); 

        program.positionAttr = gl.getAttribLocation(program, 'vPos');
        program.normalAttr = gl.getAttribLocation(program, 'normal');
        program.imgSizeUnif = gl.getUniformLocation(program, 'imgSize');
        program.minMaxZUnif = gl.getUniformLocation(program, 'minMaxZ');
        program.lightPosUnif = gl.getUniformLocation(program, 'lightPos');
        program.texSamplerUnif = gl.getUniformLocation(program, 'texSampler');
        program.depthSamplerUnif = gl.getUniformLocation(program, 'depthSampler');
        program.textureLightingUnif = gl.getUniformLocation(program, 'textureLighting');
        program.lightIntensityUnif = gl.getUniformLocation(program, 'lightIntensity');
    }

    // --- 2. Draw Frame ---
    const lightProg = lightProgramRef.current;
    if (!lightProg) return;

    gl.viewport(0, 0, gl.canvas.width, gl.canvas.height);
    gl.clearColor(0, 0, 0, 1);
    gl.enable(gl.DEPTH_TEST);
    gl.enable(gl.BLEND);
    gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);

    // Pass 1: Mesh
    gl.useProgram(program);
    gl.bindBuffer(gl.ARRAY_BUFFER, bufferRef.current.positionBuffer);
    gl.enableVertexAttribArray(program.positionAttr);
    gl.vertexAttribPointer(program.positionAttr, 3, gl.FLOAT, false, 0, 0);

    gl.bindBuffer(gl.ARRAY_BUFFER, bufferRef.current.normalBuffer);
    gl.enableVertexAttribArray(program.normalAttr);
    gl.vertexAttribPointer(program.normalAttr, 3, gl.FLOAT, false, 0, 0);

    gl.activeTexture(gl.TEXTURE0);
    gl.bindTexture(gl.TEXTURE_2D, textures.image);
    gl.activeTexture(gl.TEXTURE1);
    gl.bindTexture(gl.TEXTURE_2D, textures.depth);

    gl.uniform2fv(program.imgSizeUnif, new Float32Array(imageHelper.getImageSize(images.depth)));
    gl.uniform2fv(program.minMaxZUnif, new Float32Array([imageHelper.minZ, imageHelper.maxZ]));
    gl.uniform3fv(program.lightPosUnif, new Float32Array(lightPos));
    gl.uniform1i(program.texSamplerUnif, 0);
    gl.uniform1i(program.depthSamplerUnif, 1);
    gl.uniform1i(program.textureLightingUnif, textureLighting);
    gl.uniform1f(program.lightIntensityUnif, lightIntensity);

    gl.drawArrays(gl.TRIANGLES, 0, vertexCountRef.current);
    gl.disableVertexAttribArray(program.normalAttr);

    // Pass 2: Light
    if (textureLighting !== 1) {
        gl.useProgram(lightProg);
        gl.bindBuffer(gl.ARRAY_BUFFER, bufferRef.current.lightPosBuffer);
        gl.bufferSubData(gl.ARRAY_BUFFER, 0, new Float32Array(lightPos)); 
        // Wait, bufferData is safer for dynamic if size changes but here it's always 3 floats
        gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(lightPos), gl.DYNAMIC_DRAW);
        
        const lPosAttr = gl.getAttribLocation(lightProg, 'position');
        gl.enableVertexAttribArray(lPosAttr);
        gl.vertexAttribPointer(lPosAttr, 3, gl.FLOAT, false, 0, 0);
        gl.drawArrays(gl.POINTS, 0, 1);
    }

  }, [loading, gl, program, textures, images, lightPos, lightIntensity, textureLighting]);

  return { lightPos, lightIntensity, textureLighting, loading, setLightPos, setLightIntensity, setTextureLighting };
};