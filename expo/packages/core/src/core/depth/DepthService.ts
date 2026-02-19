export type DepthMap = Buffer;

export interface DepthService {
  getDepthMap(imagePath: string): Promise<DepthMap>;
}
