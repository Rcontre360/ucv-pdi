"use client"

class Vector3 {
  x: number;
  y: number;
  z: number;

  constructor(x = 0, y = 0, z = 0) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  subVectors(a: Vector3, b: Vector3) {
    this.x = a.x - b.x;
    this.y = a.y - b.y;
    this.z = a.z - b.z;
    return this;
  }

  crossVectors(a: Vector3, b: Vector3) {
    const ax = a.x, ay = a.y, az = a.z;
    const bx = b.x, by = b.y, bz = b.z;

    this.x = ay * bz - az * by;
    this.y = az * bx - ax * bz;
    this.z = ax * by - ay * bx;

    return this;
  }

  add(v: Vector3) {
    this.x += v.x;
    this.y += v.y;
    this.z += v.z;
    return this;
  }

  normalize() {
    const length = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    if (length > 0) {
      this.x /= length;
      this.y /= length;
      this.z /= length;
    }
    return this;
  }
}

class ImageHelper {
  private mesh: number[] = [];
  private normals: number[] = [];
  private size: [number, number] = [0, 0];
  private aspectRatio = -1;
  private minZValue = 255;
  private maxZValue = 0;

  get minZ() { return this.minZValue; }
  get maxZ() { return this.maxZValue; }

  getMesh(blockSize: number = 2, srcImg: HTMLImageElement): number[] {
    if (this.mesh.length !== 0) {
      return this.mesh;
    }

    const imgCanvas = document.createElement('canvas');
    const imgContext = imgCanvas.getContext('2d');

    if (!imgContext) {
      return [];
    }

    const height = srcImg.naturalHeight;
    const width = srcImg.naturalWidth;
    imgCanvas.width = width;
    imgCanvas.height = height;

    imgContext.drawImage(srcImg, 0, 0);

    const stepSize = blockSize - 1;
    const xSteps = Math.floor(width / stepSize);
    const ySteps = Math.floor(height / stepSize);

    let idx = 0;
    const mesh: number[] = [];
    const normals: number[] = [];
    this.minZValue = 255;
    this.maxZValue = 0;

    for (let i = 0; i < ySteps; i++) {
      for (let j = 0; j < xSteps; j++) {
        const startX = j * stepSize;
        const startY = i * stepSize;

        const endX = Math.min((j + 1) * stepSize, width - 1);
        const endY = Math.min((i + 1) * stepSize, height - 1);

        const getZ = (x: number, y: number) => {
          const data = imgContext.getImageData(x, y, 1, 1).data;
          return data[0];
        };

        const v0 = getZ(startX, startY);
        const v1 = getZ(endX, startY);
        const v2 = getZ(startX, endY);
        const v3 = getZ(endX, endY);

        this.minZValue = Math.min(this.minZValue, v0, v1, v2, v3);
        this.maxZValue = Math.max(this.maxZValue, v0, v1, v2, v3);

        const points = [
          [startX, startY, v0],
          [endX, startY, v1],
          [startX, endY, v2],
          [endX, endY, v3],
        ];

        // Triangle 1: v0, v1, v2
        mesh.push(...points[0], ...points[1], ...points[2]);
        // Triangle 2: v1, v3, v2 (correcting the order from original which seemed to be v1, v2, v3 but wait)
        // Original code:
        // for (let m = 0; m < 2; m++) {
        //   for (let n = 0; n < 3; n++) {
        //     for (let o = 0; o < 3; o++) {
        //       mesh[idx] = points[m + n][o];
        //       idx++;
        //     }
        //   }
        // }
        // m=0: points[0], points[1], points[2]
        // m=1: points[1], points[2], points[3]
        mesh.push(...points[1], ...points[2], ...points[3]);
      }
    }

    const scale = (this.maxZValue - this.minZValue) * 15;
    for (let nidx = 0; nidx < mesh.length; nidx += 3) {
      const pt = new Vector3(mesh[nidx], mesh[nidx + 1], mesh[nidx + 2]);
      const normal = this.getNormalsForPoint(pt, width, height, imgContext);
      normals[nidx] = normal.x;
      normals[nidx + 1] = normal.y;
      normals[nidx + 2] = normal.z;
    }

    this.mesh = mesh;
    this.normals = normals;

    return this.mesh;
  }

  private getNormalsForPoint(
    point: Vector3,
    width: number,
    height: number,
    imgContext: CanvasRenderingContext2D
  ): Vector3 {
    const s = 4; // Increased sampling radius for smoother normals
    const { x, y } = point;

    const getPoint = (px: number, py: number) => {
      const nx = Math.max(0, Math.min(width - 1, px));
      const ny = Math.max(0, Math.min(height - 1, py));
      
      // 3x3 Average for smoothing
      let sum = 0;
      let count = 0;
      // Define limits to stay within bounds
      const startI = Math.max(0, nx - 1);
      const endI = Math.min(width - 1, nx + 1);
      const startJ = Math.max(0, ny - 1);
      const endJ = Math.min(height - 1, ny + 1);

      const data = imgContext.getImageData(startI, startJ, endI - startI + 1, endJ - startJ + 1).data;
      
      // getImageData returns a flat array. We need to iterate it correctly.
      // Width of the block we grabbed:
      const blockWidth = endI - startI + 1;
      
      for (let i = 0; i < data.length; i += 4) {
          sum += data[i]; // Red channel
          count++;
      }
      
      const z = count > 0 ? sum / count : 0;
      return new Vector3(nx, ny, z);
    };

    const lt = getPoint(x - s, y - s);
    const rt = getPoint(x + s, y - s);
    const lb = getPoint(x - s, y + s);
    const rb = getPoint(x + s, y + s);

    const rtv = new Vector3().subVectors(rt, point);
    const ltv = new Vector3().subVectors(lt, point);
    const lbv = new Vector3().subVectors(lb, point);
    const rbv = new Vector3().subVectors(rb, point);

    const n1 = new Vector3().crossVectors(rtv, ltv);
    const n2 = new Vector3().crossVectors(ltv, lbv);
    const n3 = new Vector3().crossVectors(lbv, rbv);
    const n4 = new Vector3().crossVectors(rbv, rtv);

    return new Vector3().add(n1).add(n2).add(n3).add(n4).normalize();
  }

  getImageSize(srcImg: HTMLImageElement): [number, number] {
    if (this.size[0] !== 0) {
      return this.size;
    }
    this.size = [srcImg.naturalWidth, srcImg.naturalHeight];
    return this.size;
  }

  getAspectRatio(srcImg: HTMLImageElement): number {
    if (this.aspectRatio !== -1) {
      return this.aspectRatio;
    }
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
    if (!dataUri || dataUri.length < 10) {
      return reject(new Error("Data URI is empty or too short"));
    }

    const img = new Image();

    img.onload = () => resolve(img);
    img.onerror = (e) => {
      console.error("Image Decode Error Details:", e);
      reject(new Error(`Failed to load image. Data length: ${dataUri.length}`));
    };

    const cleanUri = dataUri.replace(/\s/g, '');
    img.src = cleanUri.startsWith('data:') ? cleanUri : `data:image/png;base64,${cleanUri}`;
  });
};