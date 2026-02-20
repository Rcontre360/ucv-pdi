import {DepthService, DepthMap} from "src/core/depth";
import {PointCloudGenerator} from "src/core/points";

const plyOutput = "output/point_cloud.ply";

export class Core {
  private depthService: DepthService;
  private cloudgen: PointCloudGenerator;

  constructor(
    depthService: DepthService,
    cloudgen: PointCloudGenerator,
  ) {
    this.depthService = depthService;
    this.cloudgen = cloudgen;
  }

  async processImage(imageDataUri: string): Promise<DepthMap> {
    console.log('depth service')
    const depthMap = await this.depthService.getDepthMap(imageDataUri);
    console.log('END')

    //await this.cloudgen.generatePointCloud(imageDataUri, depthMap, plyOutput);

    return depthMap;
  }
}
