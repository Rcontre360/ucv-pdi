import { useEffect, useRef, useState } from 'react';
import { ImgHelper } from '../lib/imageHelper';
import { vs_src } from '../lib/vs';
import { fs_src } from '../lib/fs';
import { dataUriToImage } from '@/lib/utils';

interface WebGLHook {
  lightPos: number[];
  lightIntensity: number;
  textureLighting: number;
  setLightPos: (pos: number[]) => void;
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
  const bufferRef = useRef<any>({});
  const vertexCountRef = useRef<number>(0);
  const imagesRef = useRef<{ depthImg: HTMLImageElement, textureImg: HTMLImageElement } | null>(null);

  useEffect(() => {
    if (!depthImageUri || !textureImageUri || !canvasRef.current) {
      return;
    }

    let isMounted = true;
    const canvas = canvasRef.current;
    const gl = canvas.getContext('webgl');
    if (!gl) {
      console.error("WebGL not supported");
      return;
    }
    glRef.current = gl;

    const setupShaders = () => {
      const createShader = (type: number, src: string) => {
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

      const vs = createShader(gl.VERTEX_SHADER, vs_src);
      const fs = createShader(gl.FRAGMENT_SHADER, fs_src);
      if (!vs || !fs) return;

      const program = gl.createProgram();
      if (!program) return;
      gl.attachShader(program, vs);
      gl.attachShader(program, fs);
      gl.linkProgram(program);
      if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
        console.error("Shader linking error:", gl.getProgramInfoLog(program));
        return;
      }
      gl.useProgram(program);
      shaderProgramRef.current = program;
    };

    const setupBuffers = (depthImg: HTMLImageElement, textureImg: HTMLImageElement) => {
      const prog = shaderProgramRef.current;
      const mesh = new Float32Array(ImgHelper.getMesh(5, depthImg));
      const normals = new Float32Array(ImgHelper.getNormals(depthImg));
      vertexCountRef.current = mesh.length / 3;

      prog.positionAttr = gl.getAttribLocation(prog, 'vPos');
      gl.enableVertexAttribArray(prog.positionAttr);
      bufferRef.current.positionBuffer = gl.createBuffer();
      gl.bindBuffer(gl.ARRAY_BUFFER, bufferRef.current.positionBuffer);
      gl.bufferData(gl.ARRAY_BUFFER, mesh, gl.STATIC_DRAW);

      prog.normalAttr = gl.getAttribLocation(prog, 'normal');
      gl.enableVertexAttribArray(prog.normalAttr);
      bufferRef.current.normalBuffer = gl.createBuffer();
      gl.bindBuffer(gl.ARRAY_BUFFER, bufferRef.current.normalBuffer);
      gl.bufferData(gl.ARRAY_BUFFER, normals, gl.STATIC_DRAW);

      bufferRef.current.texture = gl.createTexture();
      gl.bindTexture(gl.TEXTURE_2D, bufferRef.current.texture);
      gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, textureImg);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);

      prog.imgSizeUnif = gl.getUniformLocation(prog, 'imgSize');
      prog.minMaxZUnif = gl.getUniformLocation(prog, 'minMaxZ');
      prog.lightPos = gl.getUniformLocation(prog, 'lightPos');
      prog.texSampler = gl.getUniformLocation(prog, 'texSampler');
      prog.textureLighting = gl.getUniformLocation(prog, 'textureLighting');
      prog.lightIntensity = gl.getUniformLocation(prog, 'lightIntensity');
    };

    setIsLoading(true);
    Promise.all([dataUriToImage(depthImageUri), dataUriToImage(textureImageUri)])
      .then(([depthImg, textureImg]) => {
        if (isMounted) {
          imagesRef.current = { depthImg, textureImg };
          setupShaders();
          setupBuffers(depthImg, textureImg);
          setIsLoading(false);
        }
      })
      .catch(err => {
        console.error("Error loading WebGL images:", err);
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
      if (gl) {
        gl.deleteProgram(shaderProgramRef.current);
        gl.deleteBuffer(bufferRef.current.positionBuffer);
        gl.deleteBuffer(bufferRef.current.normalBuffer);
        gl.deleteTexture(bufferRef.current.texture);
      }
    };
  }, [depthImageUri, textureImageUri, canvasRef]);

  useEffect(() => {
    if (isLoading || !glRef.current || !shaderProgramRef.current || !imagesRef.current) {
      return;
    }
    const gl = glRef.current;
    const prog = shaderProgramRef.current;
    const { depthImg } = imagesRef.current;
    const canvas = canvasRef.current;

    const draw = () => {
      gl.viewport(0, 0, canvas.width, canvas.height);
      gl.clearColor(0, 0, 0, 1);
      gl.enable(gl.DEPTH_TEST);
      gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);

      gl.bindBuffer(gl.ARRAY_BUFFER, bufferRef.current.positionBuffer);
      gl.vertexAttribPointer(prog.positionAttr, 3, gl.FLOAT, false, 0, 0);

      gl.bindBuffer(gl.ARRAY_BUFFER, bufferRef.current.normalBuffer);
      gl.vertexAttribPointer(prog.normalAttr, 3, gl.FLOAT, false, 0, 0);

      gl.activeTexture(gl.TEXTURE0);
      gl.bindTexture(gl.TEXTURE_2D, bufferRef.current.texture);

      gl.uniform2fv(prog.imgSizeUnif, new Float32Array(ImgHelper.getImageSize(depthImg)));
      gl.uniform2fv(prog.minMaxZUnif, new Float32Array([ImgHelper.minZ, ImgHelper.maxZ]));
      gl.uniform3fv(prog.lightPos, new Float32Array(lightPos));
      gl.uniform1i(prog.texSampler, 0);
      gl.uniform1i(prog.textureLighting, textureLighting);
      gl.uniform1f(prog.lightIntensity, lightIntensity);

      gl.drawArrays(gl.TRIANGLES, 0, vertexCountRef.current);
    };

    draw();
  }, [isLoading, lightPos, lightIntensity, textureLighting, canvasRef]);

  return {
    lightPos,
    lightIntensity,
    textureLighting,
    setLightPos,
    setLightIntensity,
    setTextureLighting,
    loading: isLoading,
  };
};

