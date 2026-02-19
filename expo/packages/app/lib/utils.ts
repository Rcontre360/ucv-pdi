"use client"

export const dataUriToImage = (dataUri: string): Promise<HTMLImageElement> => {
  console.log('DATA TO URI', dataUri)
  return new Promise((resolve, reject) => {
    if (!dataUri || dataUri.length < 10) {
      return reject(new Error("Data URI is empty or too short"));
    }

    const img = new Image();

    img.onload = () => resolve(img);
    img.onerror = (e) => {
      console.error("Image Decode Error Details:", e);
      reject(new Error(`Failed to load image. Data length: ${dataUri.length}`));
    };

    // SANITIZATION: Remove whitespace/newlines that often come from server responses
    const cleanUri = dataUri.replace(/\s/g, '');

    // Ensure it starts with the correct header
    img.src = cleanUri.startsWith('data:') ? cleanUri : `data:image/png;base64,${cleanUri}`;
  });
};
