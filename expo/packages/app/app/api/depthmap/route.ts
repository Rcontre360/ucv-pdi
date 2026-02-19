import {NextResponse} from 'next/server';
import {Core, DepthAnythingV2, PointCloudGenerator} from 'core';
import fs from 'fs';
import path from 'path';
import {v4 as uuidv4} from 'uuid';

// In a real application, ensure REPLICATE_API_TOKEN is stored securely (e.g., environment variables)
const REPLICATE_API_TOKEN = process.env.REPLICATE_API_TOKEN || '';

export async function POST(request: Request) {
  if (!REPLICATE_API_TOKEN) {
    return NextResponse.json({error: 'REPLICATE_API_TOKEN is not configured on the server.'}, {status: 500});
  }

  try {
    const {imageData} = await request.json();

    if (!imageData || typeof imageData !== 'string' || !imageData.startsWith('data:image')) {
      return NextResponse.json({error: 'Invalid image data provided.'}, {status: 400});
    }

    // Decode base64 image data and save to a temporary file
    const base64Image = imageData.split(';base64,').pop();
    if (!base64Image) {
      return NextResponse.json({error: 'Invalid base64 image data.'}, {status: 400});
    }

    const tempFileName = `${uuidv4()}.jpeg`; // Assuming JPEG, adjust if needed
    const tempFilePath = path.join(process.cwd(), 'tmp', tempFileName);

    // Ensure the temporary directory exists
    const tempDir = path.dirname(tempFilePath);
    if (!fs.existsSync(tempDir)) {
      fs.mkdirSync(tempDir, {recursive: true});
    }

    fs.writeFileSync(tempFilePath, base64Image, {encoding: 'base64'});

    const depthService = new DepthAnythingV2(REPLICATE_API_TOKEN);
    const pointCloudGenerator = new PointCloudGenerator(1.0, 15.0, 0.1); // Default values
    const core = new Core(depthService, pointCloudGenerator);

    console.log('RETURN DEPTH MAP')
    // Process the image to get the depth map
    const datauri = await core.processImage(tempFilePath);

    console.log('FINAL DEPTH MAP')
    // Convert the depth map Buffer to a base64 data URL
    // Clean up temporary file
    fs.unlinkSync(tempFilePath);

    return NextResponse.json({depthMap: datauri});
  } catch (error) {
    console.error('API Error:', error);
    return NextResponse.json({error: 'Failed to generate depth map.'}, {status: 500});
  }
}
