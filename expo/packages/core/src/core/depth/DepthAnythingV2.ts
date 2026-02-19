import {DepthService, DepthMap} from "src/core/depth";
import Replicate from "replicate";

export class DepthAnythingV2 implements DepthService {
  private replicate: Replicate;

  constructor(apiToken: string) {
    this.replicate = new Replicate({
      auth: apiToken,
    });
  }

  async getDepthMap(imageDataUri: string): Promise<DepthMap> {
    const output = (await this.replicate.run(
      "chenxwh/depth-anything-v2:b239ea33cff32bb7abb5db39ffe9a09c14cbc2894331d1ef66fe096eed88ebd4",
      {
        input: {
          image: imageDataUri,
          model_size: "Large",
        },
      }
    )) as {grey_depth: string};

    console.log('Replicate API output:', output);

    const response = await fetch(output.grey_depth);
    const arrayBuffer = await response.arrayBuffer();
    const buffer = Buffer.from(arrayBuffer);

    return `data:image/png;base64,${buffer.toString('base64')}`;
  }
}
