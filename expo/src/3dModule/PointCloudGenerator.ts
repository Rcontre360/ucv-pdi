import {intToRGBA, Jimp} from "jimp";
import fs from "fs";

interface Point {
  x: number;
  y: number;
  z: number;
  r: number;
  g: number;
  b: number;
}

export class PointCloudGenerator {
  private depthScale: number;
  private depthTrunc: number;

  constructor(depthScale: number = 1.0, depthTrunc: number = 15.0) {
    this.depthScale = depthScale;
    this.depthTrunc = depthTrunc;
  }

  async generatePointCloud(
    rgbImagePath: string,
    depthMapBuffer: Buffer,
    outputPlyPath: string
  ): Promise<void> {
    // 1. Read RGB image using Jimp
    const rgbImage = await Jimp.read(rgbImagePath);

    // 2. Read depth map buffer using Jimp
    // Assuming the depth map buffer is a grayscale image
    const depthImage = await Jimp.read(depthMapBuffer);

    // Ensure RGB and depth images have the same dimensions
    if (
      rgbImage.bitmap.width !== depthImage.bitmap.width ||
      rgbImage.bitmap.height !== depthImage.bitmap.height
    ) {
      throw new Error(
        "RGB image and depth map must have the same dimensions."
      );
    }

    const width = rgbImage.bitmap.width;
    const height = rgbImage.bitmap.height;

    // Define intrinsic camera parameters (example values, adjust as needed)
    // These values are often obtained from camera calibration
    const fx = 1000; // Focal length x
    const fy = 1000; // Focal length y
    const cx = width / 2; // Principal point x
    const cy = height / 2; // Principal point y

    const points: Point[] = [];

    for (let yPx = 0; yPx < height; yPx++) {
      for (let xPx = 0; xPx < width; xPx++) {
        const depthPixel = depthImage.getPixelColor(xPx, yPx);
        // Assuming grayscale depth map, so R, G, B should be the same
        const depthValue = intToRGBA(depthPixel).r; // Extract red channel as depth

        // Convert depth to meters (or scene units)
        let z = depthValue / this.depthScale;

        // Apply depth truncation
        if (z > this.depthTrunc || z === 0) {
          continue; // Skip points that are too far or have no depth information
        }

        // 3D projection formulas
        let x = (xPx - cx) * z / fx;
        let y = (yPx - cy) * z / fy;

        // Flip the point cloud to avoid upside down (similar to Open3D's pcd.transform)
        y = -y;
        z = -z;

        // Get color for the point
        const rgbPixel = rgbImage.getPixelColor(xPx, yPx);
        const {r, g, b} = intToRGBA(rgbPixel);

        points.push({x, y, z, r, g, b});
      }
    }

    this.savePLY(points, outputPlyPath);
  }

  private savePLY(points: Point[], filePath: string): void {
    let plyContent = `ply\nformat ascii 1.0\nelement vertex ${points.length}\nproperty float x\nproperty float y\nproperty float z\nproperty uchar red\nproperty uchar green\nproperty uchar blue\nend_header\n`;

    points.forEach((p) => {
      plyContent += `${p.x} ${p.y} ${p.z} ${p.r} ${p.g} ${p.b}\n`;
    });

    fs.writeFileSync(filePath, plyContent);
  }
}
