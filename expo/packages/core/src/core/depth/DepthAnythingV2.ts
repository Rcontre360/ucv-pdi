import {DepthService, DepthMap} from "src/core/depth";
import fs from "fs";
import Replicate from "replicate";

export class DepthAnythingV2 implements DepthService {
  private replicate: Replicate;

  constructor(apiToken: string) {
    this.replicate = new Replicate({
      auth: apiToken,
    });
  }

  async getDepthMap(imagePath: string): Promise<DepthMap> {
    const image = fs.readFileSync(imagePath, "base64");
    const dataUri = `data:image/jpeg;base64,${image}`;

    const output = (await this.replicate.run(
      "chenxwh/depth-anything-v2:b239ea33cff32bb7abb5db39ffe9a09c14cbc2894331d1ef66fe096eed88ebd4",
      {
        input: {
          image: dataUri,
          model_size: "Large",
        },
      }
    )) as {color_depth: {url: () => Promise<string>; bytes: () => Promise<Buffer>}};

    return await output.color_depth.bytes();
  }
}
