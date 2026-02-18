import dotenv from "dotenv";
import {Core} from "src/core/Core";
import {ReplicateDepthService} from "src/core/depth";
import {PointCloudGenerator} from "src/core/points";
import {env} from "./src/config";

dotenv.config();

const imgPath = "images/kitchen_stuff.jpg";

async function main() {
  const depthService = new ReplicateDepthService(env.REPLICATE_API_TOKEN);
  const pointCloud = new PointCloudGenerator(0.1, 1);
  const core = new Core(depthService, pointCloud);

  try {
    await core.processImage(imgPath);

  } catch (error) {
    console.error("Error in main application:", error);
  }
}

main();
