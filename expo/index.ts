import { Core } from "./src/core/Core";
import { ReplicateDepthService } from "./src/depthService/ReplicateDepthService";
import { PointCloudGenerator } from "./src/3dModule/PointCloudGenerator";
import fs from "fs";
import dotenv from "dotenv";

dotenv.config();

const rgbImagePath = "images/sample.jpg";
const pointCloudOutputPath = "output/point_cloud.ply";

async function main() {
  const apiKey = process.env.REPLICATE_API_TOKEN;

  if (!apiKey) {
    console.error(
      "Please provide your Replicate API token in the .env file (REPLICATE_API_TOKEN)."
    );
    return;
  }

  if (!fs.existsSync(rgbImagePath)) {
    console.error(`RGB Image not found at: ${rgbImagePath}`);
    return;
  }

  const depthService = new ReplicateDepthService(apiKey);
  const core = new Core(depthService);
  const pointCloudGenerator = new PointCloudGenerator();

  try {
    // Generate Depth Map
    const depthMap = await core.processImage(rgbImagePath);

    if (!fs.existsSync("output")) {
      fs.mkdirSync("output");
    }

    // Generate Point Cloud
    console.log("Generating point cloud...");
    await pointCloudGenerator.generatePointCloud(
      rgbImagePath,
      depthMap,
      pointCloudOutputPath
    );
    console.log(`Point cloud saved to: ${pointCloudOutputPath}`);
  } catch (error) {
    console.error("Error in main application:", error);
  }
}

main();
