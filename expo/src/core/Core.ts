import {
  DepthService,
  DepthMap,
} from "../depthService/DepthService";

export class Core {
  private depthService: DepthService;

  constructor(depthService: DepthService) {
    this.depthService = depthService;
  }

  async processImage(imagePath: string): Promise<DepthMap> {
    console.log(`Processing image: ${imagePath}`);
    const depthMap = await this.depthService.getDepthMap(imagePath);
    console.log("Depth map generated.");
    return depthMap;
  }
}
