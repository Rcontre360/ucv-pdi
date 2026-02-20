export type DepthMap = string;

export interface DepthService {
  getDepthMap(imagePath: string): Promise<DepthMap>;
}
