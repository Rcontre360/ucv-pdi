import {DepthService, DepthMap} from "./DepthService";
import fs from "fs";

const API_URL =
  "https://api-inference.huggingface.co/models/depth-anything/Depth-Anything-V2-Base-hf";

export class HuggingFaceDepthService implements DepthService {
  private apiKey: string;

  constructor(apiKey: string) {
    this.apiKey = apiKey;
  }

  async getDepthMap(imagePath: string): Promise<DepthMap> {
    const image = fs.readFileSync(imagePath);

    const response = await fetch(API_URL, {
      headers: {
        Authorization: `Bearer ${this.apiKey}`,
        "Content-Type": "image/jpeg",
      },
      method: "POST",
      body: image,
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const arrayBuffer = await response.arrayBuffer();
    return Buffer.from(arrayBuffer);
  }
}
