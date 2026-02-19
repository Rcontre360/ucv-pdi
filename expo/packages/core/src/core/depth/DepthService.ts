export type DepthMap = string;

export interface DepthService {
  getDepthMap(imageData: string): Promise<DepthMap>;
}
