import { Vector3 } from 'three';

export const ImgHelper = {
  img_mesh: [] as number[],
  getMesh(blockSize: number = 2, srcImg: HTMLImageElement) {
    if (this.img_mesh.length !== 0) {
      return this.img_mesh;
    }

    console.log(blockSize);

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

    const createMeshFromImage = () => {
      let minZ = 255;
      let maxZ = 0;
      const stepSize = blockSize - 1;
      const xSteps = Math.floor(width / stepSize);
      const ySteps = Math.floor(height / stepSize);

      console.log(xSteps);
      console.log(ySteps);

      let idx = 0;
      const mesh: number[] = [];
      const normals: number[] = [];
      for (let i = 0; i < ySteps; i++) {
        for (let j = 0; j < xSteps; j++) {
          const startX = j * stepSize;
          const startY = i * stepSize;

          const endX = Math.min((j + 1) * stepSize, width - 1);
          const endY = Math.min((i + 1) * stepSize, height - 1);

          const v0Data = imgContext.getImageData(startX, startY, 1, 1).data;
          const v0 = (v0Data[0] + v0Data[1] + v0Data[2]) / 3;
          minZ = Math.min(v0, minZ);
          maxZ = Math.max(v0, maxZ);

          const v1Data = imgContext.getImageData(endX, startY, 1, 1).data;
          const v1 = (v1Data[0] + v1Data[1] + v1Data[2]) / 3;
          minZ = Math.min(v1, minZ);
          maxZ = Math.max(v1, maxZ);

          const v2Data = imgContext.getImageData(startX, endY, 1, 1).data;
          const v2 = (v2Data[0] + v2Data[1] + v2Data[2]) / 3;
          minZ = Math.min(v2, minZ);
          maxZ = Math.max(v2, maxZ);

          const v3Data = imgContext.getImageData(endX, endY, 1, 1).data;
          const v3 = (v3Data[0] + v3Data[1] + v3Data[2]) / 3;
          minZ = Math.min(v3, minZ);
          maxZ = Math.max(v3, maxZ);

          const points = [
            [startX, startY, v0],
            [endX, startY, v1],
            [startX, endY, v2],
            [endX, endY, v3],
          ];

          for (let m = 0; m < 2; m++) {
            for (let n = 0; n < 3; n++) {
              for (let o = 0; o < 3; o++) {
                mesh[idx] = points[m + n][o];
                idx++;
              }
            }
          }
        }
      }

      const scale = (maxZ - minZ) * 15;
      for (let nidx = 0; nidx < mesh.length; nidx += 3) {
        const pt = new Vector3(mesh[nidx], mesh[nidx + 1], mesh[nidx + 2]);
        const normal = getNormalsForPoints(pt, minZ, scale, width, height, imgContext);
        normals[nidx] = normal.x;
        normals[nidx + 1] = normal.y;
        normals[nidx + 2] = normal.z;
      }
      return [mesh, minZ, maxZ, normals];
    };

    const getNormalsForPoints = (
        point: Vector3,
        minZ: number,
        zScale: number,
        width: number,
        height: number,
        imgContext: CanvasRenderingContext2D
      ) => {
        const s = 3;
        const { x, y } = point;

        const lt = new Vector3(Math.max(0, x - s), Math.max(0, y - s), 0);
        const ltVal = imgContext.getImageData(lt.x, lt.y, 1, 1).data;
        lt.z = (ltVal[0] + ltVal[1] + ltVal[2]) / 3;

        const rt = new Vector3(Math.min(width - 1, x + s), Math.max(0, y - s), 0);
        const rtVal = imgContext.getImageData(rt.x, rt.y, 1, 1).data;
        rt.z = (rtVal[0] + rtVal[1] + rtVal[2]) / 3;

        const lb = new Vector3(Math.max(0, x - s), Math.min(height - 1, y + s), 0);
        const lbVal = imgContext.getImageData(lb.x, lb.y, 1, 1).data;
        lb.z = (lbVal[0] + lbVal[1] + lbVal[2]) / 3;

        const rb = new Vector3(Math.min(width - 1, x + s), Math.min(height - 1, y + s), 0);
        const rbVal = imgContext.getImageData(rb.x, rb.y, 1, 1).data;
        rb.z = (rbVal[0] + rbVal[1] + rbVal[2]) / 3;

        const rtv = new Vector3().subVectors(rt, point);
        const ltv = new Vector3().subVectors(lt, point);
        const lbv = new Vector3().subVectors(lb, point);
        const rbv = new Vector3().subVectors(rb, point);

        const n1 = new Vector3().crossVectors(rtv, ltv);
        const n2 = new Vector3().crossVectors(ltv, lbv);
        const n3 = new Vector3().crossVectors(lbv, rbv);
        const n4 = new Vector3().crossVectors(rbv, rtv);

        const normal = new Vector3().add(n1).add(n2).add(n3).add(n4);

        return normal;
      };

    const res = createMeshFromImage();
    this.img_mesh = res[0] as number[];
    this.minZ = res[1] as number;
    this.maxZ = res[2] as number;
    this.normals = res[3] as number[];

    return this.img_mesh;
  },
  img_size: [] as number[],
  getImageSize(srcImg: HTMLImageElement) {
    if (this.img_size.length !== 0) {
      return this.img_size;
    }
    this.img_size = [srcImg.naturalWidth, srcImg.naturalHeight];
    return this.img_size;
  },
  aspect_ratio: -1,
  getAspectRatio(srcImg: HTMLImageElement) {
    if (this.aspect_ratio !== -1) {
      return this.aspect_ratio;
    }
    const size = this.getImageSize(srcImg);
    this.aspect_ratio = size[0] / size[1];
    return this.aspect_ratio;
  },
  minZ: 255,
  maxZ: 0,

  normals: [] as number[],
  getNormals(srcImg: HTMLImageElement) {
    if (this.normals.length === 0) {
      this.getMesh(5, srcImg);
    }

    return this.normals;
  },
};