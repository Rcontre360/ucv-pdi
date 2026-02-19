import express from 'express';
import cors from 'cors';
import {json} from 'body-parser';
import {Core} from './src/core/Core';
import {DepthAnythingV2} from './src/core/depth/DepthAnythingV2';
import {PointCloudGenerator} from './src/core/points';
import dotenv from 'dotenv';
import path from 'path';
dotenv.config({ path: path.resolve(__dirname, '../../.env') });

const app = express();
const port = process.env.CORE_SERVER_PORT || 8080;

app.use(cors());
app.use(json({ limit: '50mb' }));

const REPLICATE_API_TOKEN = process.env.REPLICATE_API_TOKEN || '';

app.post('/depthmap', async (req, res) => {
  if (!REPLICATE_API_TOKEN) {
    return res.status(500).json({ error: 'REPLICATE_API_TOKEN is not configured on the server.' });
  }

  try {
    const { imageData } = req.body;
    if (!imageData || typeof imageData !== 'string' || !imageData.startsWith('data:image')) {
      return res.status(400).json({ error: 'Invalid image data provided.' });
    }

    const depthService = new DepthAnythingV2(REPLICATE_API_TOKEN);
    const pointCloudGenerator = new PointCloudGenerator(1.0, 15.0, 0.1);
    const core = new Core(depthService, pointCloudGenerator);

    const depthMapDataUri = await core.processImage(imageData);
    console.log('Generated depth map URI:', depthMapDataUri);

    res.json({ depthMap: depthMapDataUri });
  } catch (error) {
    console.error('Core Server Error:', error);
    res.status(500).json({ error: 'Failed to generate depth map.' });
  }
});

app.listen(port, () => {
  console.log(`Core server listening on port ${port}`);
});
