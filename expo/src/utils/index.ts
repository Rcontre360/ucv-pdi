import {Jimp} from "jimp";

export async function savePNG(inputBuffer: Buffer, outputPath: `${string}.${string}`) {
  const image = await Jimp.read(inputBuffer);
  await image.write(outputPath);
}
