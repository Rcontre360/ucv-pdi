import { NextResponse } from 'next/server';
import { DepthAnythingV2 } from 'src/depth/DepthAnythingV2';
import { logger } from 'src/utils/Logger';

export async function POST(req: Request) {
  const REPLICATE_API_TOKEN = process.env.REPLICATE_API_TOKEN || '';
  
  if (!REPLICATE_API_TOKEN) {
    logger.error('REPLICATE_API_TOKEN is not configured.');
    return NextResponse.json(
      { error: 'REPLICATE_API_TOKEN is not configured on the server.' }, 
      { status: 500 }
    );
  }

  try {
    const body = await req.json();
    const { imageData } = body;
    
    if (!imageData || typeof imageData !== 'string' || !imageData.startsWith('data:image')) {
      return NextResponse.json(
        { error: 'Invalid image data provided.' }, 
        { status: 400 }
      );
    }

    const depthService = new DepthAnythingV2(REPLICATE_API_TOKEN);
    const depthMapDataUri = await depthService.getDepthMap(imageData);
    
    logger.info('Generated depth map URI successfully');

    return NextResponse.json({ depthMap: depthMapDataUri });
  } catch (error) {
    logger.error('API Route Error:', error);
    return NextResponse.json(
      { error: 'Failed to generate depth map.' }, 
      { status: 500 }
    );
  }
}
