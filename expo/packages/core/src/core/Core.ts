import {DepthService, DepthMap} from "src/core/depth";

export class Core {
  private depthService: DepthService;

  constructor(
    depthService: DepthService,
  ) {
    this.depthService = depthService;
  }

  async processImage(imageData: string): Promise<DepthMap> {
    const depthMap = await this.depthService.getDepthMap(imageData);
    return depthMap;
  }
}
