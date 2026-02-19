import {DepthService} from "src/core/depth";
import Replicate from "replicate";

export type DepthMap = string; // Define DepthMap as a base64 string

export class DepthAnythingV2 implements DepthService {
  private replicate: Replicate;

  constructor(apiToken: string) {
    this.replicate = new Replicate({
      auth: apiToken,
    });
  }

  async getDepthMap(imageData: string): Promise<DepthMap> { // Accepts base64 image data
    // Assuming imageData is already a base64 data URI
    const output = (await this.replicate.run(
      "chenxwh/depth-anything-v2:b239ea33cff32bb7abb5db39ffe9a09c14cbc2894331d1ef66fe096eed88ebd4",
      {
        input: {
          image: imageData, // Use the provided base64 image data directly
          model_size: "Large",
        },
      }
    )) as {color_depth: {url: string; bytes: () => Promise<Buffer>}}; // Adjust output type based on Replicate API

    // Replicate returns a URL for the color_depth image, we'll return that URL
    return output.color_depth.url; 
  }
}
