import { Core } from "./src/core/Core";
import { ReplicateDepthService } from "./src/depthService/ReplicateDepthService";
import fs from "fs";
import dotenv from "dotenv";

dotenv.config();

const imagePath = "images/sample.jpg";
const outputPath = "output/depth_map.png";

async function main() {
  const apiKey = process.env.REPLICATE_API_TOKEN;

  if (!apiKey) {
    console.error(
      "Please provide your Replicate API token in the .env file (REPLICATE_API_TOKEN)."
    );
    return;
  }

  if (!fs.existsSync(imagePath)) {
    console.error(`Image not found at: ${imagePath}`);
    return;
  }

  const depthService = new ReplicateDepthService(apiKey);
  const core = new Core(depthService);

  try {
    const depthMap = await core.processImage(imagePath);

    if (!fs.existsSync("output")) {
      fs.mkdirSync("output");
    }

    fs.writeFileSync(outputPath, depthMap);
    console.log(`Depth map saved to: ${outputPath}`);
  } catch (error) {
    console.error("Error generating depth map:", error);
  }
}

main();
