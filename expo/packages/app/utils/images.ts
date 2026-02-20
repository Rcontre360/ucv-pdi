"use client"

import { vec3 } from 'gl-matrix';

class ImageHelper {
  private mesh: number[] = [];
  private normals: number[] = [];
  private size: [number, number] = [0, 0];
  private aspectRatio = -1;
  private minZValue = 255;
  private maxZValue = 0;

  get minZ() { return this.minZValue; }
  get maxZ() { return this.maxZValue; }

  getMesh(blockSize: number = 5, srcImg: HTMLImageElement): number[] {
    if (this.mesh.length !== 0) {
      return this.mesh;
    }

    const imgCanvas = document.createElement('canvas');
    const imgContext = imgCanvas.getContext('2d');
    if (!imgContext) return [];

    const height = srcImg.naturalHeight;
    const width = srcImg.naturalWidth;
    imgCanvas.width = width;
    imgCanvas.height = height;
    imgContext.drawImage(srcImg, 0, 0);

    const stepSize = Math.max(1, blockSize - 1);
    const xSteps = Math.ceil(width / stepSize);
    const ySteps = Math.ceil(height / stepSize);

    // 1. Create Vertex Matrix
    const vertexMatrix: vec3[][] = [];
    this.minZValue = 255;
    this.maxZValue = 0;

    for (let i = 0; i < ySteps; i++) {
      const row: vec3[] = [];
      for (let j = 0; j < xSteps; j++) {
        const x = Math.min(j * stepSize, width - 1);
        const y = Math.min(i * stepSize, height - 1);
        
        // Sample depth with a small 3x3 average for smoothing
        const data = imgContext.getImageData(Math.max(0, x - 1), Math.max(0, y - 1), 3, 3).data;
        let sum = 0;
        let count = 0;
        for (let k = 0; k < data.length; k += 4) {
          sum += data[k];
          count++;
        }
        const z = count > 0 ? sum / count : 0;

        this.minZValue = Math.min(this.minZValue, z);
        this.maxZValue = Math.max(this.maxZValue, z);

        row.push(vec3.fromValues(x, y, z));
      }
      vertexMatrix.push(row);
    }

    // 2. Create Normal Matrix
    const normalMatrix: vec3[][] = [];
    for (let i = 0; i < ySteps; i++) {
      const row: vec3[] = [];
      for (let j = 0; j < xSteps; j++) {
        // Get neighbors in matrix
        const p = vertexMatrix[i][j];
        const p_right = vertexMatrix[i][Math.min(j + 1, xSteps - 1)];
        const p_down = vertexMatrix[Math.min(i + 1, ySteps - 1)][j];
        const p_left = vertexMatrix[i][Math.max(0, j - 1)];
        const p_up = vertexMatrix[Math.max(0, i - 1)][j];

        // Vectors
        const v1 = vec3.create();
        const v2 = vec3.create();
        const v3 = vec3.create();
        const v4 = vec3.create();
        
        vec3.subtract(v1, p_right, p);
        vec3.subtract(v2, p_down, p);
        vec3.subtract(v3, p_left, p);
        vec3.subtract(v4, p_up, p);

        // Cross products for quadrants
        const n1 = vec3.create();
        const n2 = vec3.create();
        const n3 = vec3.create();
        const n4 = vec3.create();

        vec3.cross(n1, v1, v2);
        vec3.cross(n2, v2, v3);
        vec3.cross(n3, v3, v4);
        vec3.cross(n4, v4, v1);

        const normal = vec3.create();
        vec3.add(normal, n1, n2);
        vec3.add(normal, normal, n3);
        vec3.add(normal, normal, n4);
        vec3.normalize(normal, normal);

        row.push(normal);
      }
      normalMatrix.push(row);
    }

    // 3. Triangulate and Flatten
    const mesh: number[] = [];
    const normals: number[] = [];

    for (let i = 0; i < ySteps - 1; i++) {
      for (let j = 0; j < xSteps - 1; j++) {
        const v0 = vertexMatrix[i][j];
        const v1 = vertexMatrix[i][j + 1];
        const v2 = vertexMatrix[i + 1][j];
        const v3 = vertexMatrix[i + 1][j + 1];

        const n0 = normalMatrix[i][j];
        const n1 = normalMatrix[i][j + 1];
        const n2 = normalMatrix[i + 1][j];
        const n3 = normalMatrix[i + 1][j + 1];

        // Triangle 1: v0, v1, v2
        mesh.push(...v0, ...v1, ...v2);
        normals.push(...n0, ...n1, ...n2);

        // Triangle 2: v1, v3, v2
        mesh.push(...v1, ...v3, ...v2);
        normals.push(...n1, ...n3, ...n2);
      }
    }

    this.mesh = mesh;
    this.normals = normals;

    return this.mesh;
  }

  getImageSize(srcImg: HTMLImageElement): [number, number] {
    if (this.size[0] !== 0) return this.size;
    this.size = [srcImg.naturalWidth, srcImg.naturalHeight];
    return this.size;
  }

  getAspectRatio(srcImg: HTMLImageElement): number {
    if (this.aspectRatio !== -1) return this.aspectRatio;
    const size = this.getImageSize(srcImg);
    this.aspectRatio = size[0] / size[1];
    return this.aspectRatio;
  }

  getNormals(srcImg: HTMLImageElement): number[] {
    if (this.normals.length === 0) {
      this.getMesh(5, srcImg);
    }
    return this.normals;
  }

  reset() {
    this.mesh = [];
    this.normals = [];
    this.size = [0, 0];
    this.aspectRatio = -1;
  }
}

export const imageHelper = new ImageHelper();

export const dataUriToImage = (dataUri: string): Promise<HTMLImageElement> => {
  return new Promise((resolve, reject) => {
    if (!dataUri || dataUri.length < 10) return reject(new Error("Data URI is empty"));
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = (e) => reject(new Error("Failed to load image"));
    const cleanUri = dataUri.replace(/\s/g, '');
    img.src = cleanUri.startsWith('data:') ? cleanUri : `data:image/png;base64,${cleanUri}`;
  });
};