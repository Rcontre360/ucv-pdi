import dotenv from "dotenv";
import {Core} from "src/core/Core";
import {DepthAnythingV2} from "src/core/depth"; // Using existing DepthAnythingV2, fixed typo
import {PointCloudGenerator} from "src/core/points"; // Using existing PointCloudGenerator
import {env} from "src/config";
import fs from "fs"; // Import fs

dotenv.config();

const rgbImagePath = "../../images/kitchen_stuff.jpg";

async function main() {
  if (!fs.existsSync("output")) {
    fs.mkdirSync("output");
  }

  const depthService = new DepthAnythingV2(env.REPLICATE_API_TOKEN); // Fixed typo
  const pointCloudGenerator = new PointCloudGenerator(1.0, 15.0, 0.1); // depthScale, depthTrunc, minDepth
  const core = new Core(depthService, pointCloudGenerator);

  try {
    await core.processImage(
      rgbImagePath,
    );

  } catch (error) {
    console.error("Error in main application:", error);
  }
}

main();
