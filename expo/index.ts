import dotenv from "dotenv";
import {Core} from "src/core/Core";
import {DephtAnythingV2} from "src/core/depth";
import {PointCloudGenerator} from "src/core/points";
import {env} from "./src/config";

dotenv.config();

const imgPath = "images/example.png";

async function main() {
  const depthService = new DephtAnythingV2(env.REPLICATE_API_TOKEN);
  const pointCloud = new PointCloudGenerator(1.0, 15.0, 0.1);
  const core = new Core(depthService, pointCloud);

  try {
    await core.processImage(imgPath);

  } catch (error) {
    console.error("Error in main application:", error);
  }
}

main();
