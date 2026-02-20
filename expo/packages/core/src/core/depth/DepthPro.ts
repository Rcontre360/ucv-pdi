import {DepthService, DepthMap} from "src/core/depth";
import fs from "fs";
import Replicate from "replicate";

export class DepthPro implements DepthService {
  private replicate: Replicate;

  constructor(apiToken: string) {
    this.replicate = new Replicate({
      auth: apiToken,
    });
  }

  async getDepthMap(imagePath: string): Promise<DepthMap> {
    const image = Bun.file(imagePath);
    const dataUri = `data:image/jpeg;base64,${image}`;

    const output = (await this.replicate.run(
      "chenxwh/ml-depth-pro:a6645b33f4e36eda0d8d52ab3da6ef37b82d198e2b70c72e680cc75f0baf1623",
      {
        input: {
          image_path: dataUri
        },
      }
    )) as {color_depth: {url: () => Promise<string>; bytes: () => Promise<Buffer>}};

    return await output.color_depth.bytes();
  }
}
