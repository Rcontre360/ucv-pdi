import { useEffect, useRef, useState } from 'react';
import { ImgHelper } from '../lib/imageHelper';
import { vs_src } from '../lib/vs';
import { fs_src } from '../lib/fs';
import * as twgl from '../lib/twgl-full.min.js';

interface WebGLHook {
  lightPos: number[];
  lightIntensity: number;
  textureLighting: number;
  loading: boolean;
  setLightPos: (pos: number[]) => void;
  setLightIntensity: (intensity: number) => void;
  setTextureLighting: (lighting: number) => void;
}

export const useWebGL = (
  canvasRef: React.RefObject<HTMLCanvasElement>,
  depthImageUrl: string,
  textureImageUrl: string
): WebGLHook => {
  const [lightPos, setLightPos] = useState([0, 0, -1]);
  const [lightIntensity, setLightIntensity] = useState(0.4);
  const [textureLighting, setTextureLighting] = useState(3);
  const [loading, setLoading] = useState(true);

  const glRef = useRef<WebGLRenderingContext | null>(null);
  const shaderProgramRef = useRef<WebGLProgram | null>(null);
  const imgBufferRef = useRef<any>({});
  const depthImageRef = useRef<HTMLImageElement | null>(null);
  const textureImageRef = useRef<HTMLImageElement | null>(null);

  // Effect to load images
  useEffect(() => {
    if (!depthImageUrl || !textureImageUrl) {
      setLoading(false);
      depthImageRef.current = null;
      textureImageRef.current = null;
      return;
    }

    setLoading(true);
    const newDepthImg = new Image();
    newDepthImg.onload = () => {
      depthImageRef.current = newDepthImg;
      const newTextureImg = new Image();
      newTextureImg.onload = () => {
        textureImageRef.current = newTextureImg;
        setLoading(false);
      };
      newTextureImg.src = textureImageUrl;
    };
    newDepthImg.src = depthImageUrl;
  }, [depthImageUrl, textureImageUrl]);

  // Effect to setup WebGL and draw
  useEffect(() => {
    if (loading || !canvasRef.current || !depthImageRef.current || !textureImageRef.current) {
      return;
    }

    const canvas = canvasRef.current;
    const gl = canvas.getContext('webgl');
    if (!gl) {
      console.error('WebGL not supported');
      return;
    }
    glRef.current = gl;

    const m4 = twgl.m4;
    const v3 = twgl.v3;

    const setupShaders = () => {
        var vertexShader = gl.createShader(gl.VERTEX_SHADER);
        gl.shaderSource(vertexShader, vs_src)
        gl.compileShader(vertexShader);
        if (!gl.getShaderParameter(vertexShader, gl.COMPILE_STATUS)) {
            alert("Compile Error : Vertex Shader\n" +
                  "-----------------------------\n" +
                  gl.getShaderInfoLog(vertexShader) + 
                  "\n-----------------------------");
            return false;
        }
    
        var fragmentShader = gl.createShader(gl.FRAGMENT_SHADER);
        gl.shaderSource(fragmentShader, fs_src);
        gl.compileShader(fragmentShader);
        if (!gl.getShaderParameter(fragmentShader, gl.COMPILE_STATUS)) {
            alert("Compile Error : Fragment Shader\n" +
                  "-------------------------------\n" +
                  gl.getShaderInfoLog(fragmentShader) +
                  "\n-----------------------------");
            return false;
        }
    
        shaderProgramRef.current = gl.createProgram();
        gl.attachShader(shaderProgramRef.current, vertexShader);
        gl.attachShader(shaderProgramRef.current, fragmentShader);
        gl.linkProgram(shaderProgramRef.current);
        if (!gl.getProgramParameter(shaderProgramRef.current, gl.LINK_STATUS)) {
            alert("Failed to Link Shaders");
            return false;
        }
        gl.useProgram(shaderProgramRef.current);
        return true;
    };
    if (!setupShaders()) {
        return;
      }

    const mesh = new Float32Array(ImgHelper.getMesh(5, depthImageRef.current));
    const normals = new Float32Array(ImgHelper.getNormals(depthImageRef.current));

    const setupShaderAttributes = () => {
        const shaderProgram = shaderProgramRef.current;
        if (!shaderProgram) return;
        shaderProgram.positionAttr = gl.getAttribLocation(shaderProgram, 'vPos');
        gl.enableVertexAttribArray(shaderProgram.positionAttr);
    
        shaderProgram.normalAttr = gl.getAttribLocation(shaderProgram, 'normal');
        gl.enableVertexAttribArray(shaderProgram.normalAttr);
    
        imgBufferRef.current.positionBuffer = gl.createBuffer();
        gl.bindBuffer(gl.ARRAY_BUFFER, imgBufferRef.current.positionBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, mesh, gl.STATIC_DRAW);
        imgBufferRef.current.positionBuffer.itemSize = 3;
        imgBufferRef.current.positionBuffer.numItems  = mesh.length / imgBufferRef.current.positionBuffer.itemSize;
    
        imgBufferRef.current.normalBuffer = gl.createBuffer();
        gl.bindBuffer(gl.ARRAY_BUFFER, imgBufferRef.current.normalBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, normals, gl.STATIC_DRAW);
        imgBufferRef.current.normalBuffer.itemSize = 3;
        imgBufferRef.current.normalBuffer.numItems = normals.length / imgBufferRef.current.normalBuffer.itemSize;
    
        imgBufferRef.current.texture = gl.createTexture();
        gl.bindTexture(gl.TEXTURE_2D, imgBufferRef.current.texture);
        gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, textureImageRef.current);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
    
        shaderProgram.imgSizeUnif = gl.getUniformLocation(shaderProgram, 'imgSize');
        shaderProgram.minMaxZUnif = gl.getUniformLocation(shaderProgram, 'minMaxZ');
        shaderProgram.lightPos = gl.getUniformLocation(shaderProgram, 'lightPos');
        shaderProgram.texSampler = gl.getUniformLocation(shaderProgram, 'texSampler');
        shaderProgram.textureLighting = gl.getUniformLocation(shaderProgram, 'textureLighting');
        shaderProgram.lightIntensity = gl.getUniformLocation(shaderProgram, 'lightIntensity');
    
        gl.bindBuffer(gl.ARRAY_BUFFER, null);
    };

    setupShaderAttributes();

    const draw = () => {
        if (!gl || !shaderProgramRef.current || !depthImageRef.current || !textureImageRef.current) return;
        const shaderProgram = shaderProgramRef.current;

        gl.clearColor(0, 0, 0, 1);
        gl.enable(gl.DEPTH_TEST);
        gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
    
        gl.bindBuffer(gl.ARRAY_BUFFER, imgBufferRef.current.positionBuffer);
        gl.vertexAttribPointer(shaderProgram.positionAttr, imgBufferRef.current.positionBuffer.itemSize, 
            gl.FLOAT, false, 0, 0);
    
        gl.bindBuffer(gl.ARRAY_BUFFER, imgBufferRef.current.normalBuffer);
        gl.vertexAttribPointer(shaderProgram.normalAttr, imgBufferRef.current.normalBuffer.itemSize, 
            gl.FLOAT, false, 0, 0);
    
        gl.bindTexture(gl.TEXTURE_2D, imgBufferRef.current.texture);
        gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, textureImageRef.current);
    
        gl.uniform2fv(shaderProgram.imgSizeUnif, new Float32Array(ImgHelper.getImageSize(depthImageRef.current)));
        gl.uniform2fv(shaderProgram.minMaxZUnif, new Float32Array([ImgHelper.minZ, ImgHelper.maxZ]));
        gl.uniform3fv(shaderProgram.lightPos, new Float32Array(lightPos));
        gl.uniform1i(shaderProgram.texSampler, 0);
        gl.uniform1i(shaderProgram.textureLighting, textureLighting);
        gl.uniform1f(shaderProgram.lightIntensity, lightIntensity);
    
        gl.drawArrays(gl.TRIANGLES, 0, imgBufferRef.current.positionBuffer.numItems);
    };

    draw();

  }, [loading, canvasRef, depthImageRef.current, textureImageRef.current, lightPos, lightIntensity, textureLighting]);

  return {
    lightPos,
    lightIntensity,
    textureLighting,
    loading,
    setLightPos,
    setLightIntensity,
    setTextureLighting,
  };
};
