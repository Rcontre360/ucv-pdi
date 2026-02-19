import {PointCloudGenerator} from "src/core/points";
import {DepthService, DepthMap} from "src/core/depth";
import {savePNG} from "src/utils";

const plyOutput = "output/point_cloud.ply";

export class Core {
  private depthService: DepthService;
  private cloudgen: PointCloudGenerator;

  constructor(depthService: DepthService, cloudgen: PointCloudGenerator) {
    this.depthService = depthService;
    this.cloudgen = cloudgen;
  }

  async processImage(imagePath: string): Promise<DepthMap> {
    const depthMap = await this.depthService.getDepthMap(imagePath);

    await savePNG(depthMap, "output/depth_map.png");

    await this.cloudgen.generatePointCloud(
      imagePath,
      depthMap,
      plyOutput
    );

    return depthMap;
  }
}
