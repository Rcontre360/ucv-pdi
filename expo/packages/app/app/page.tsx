'use client';
import { useMemo, useState, useRef } from 'react';
import Loader from '../components/Loader';
import WebGLCanvas from '../components/WebGLCanvas';

export default function Home() {
  const [userImage, setUserImage] = useState<string | null>(null);
  const [userDepthMap, setUserDepthMap] = useState<string | null>(null);
  const [processingUserImage, setProcessingUserImage] = useState(false);

  const webGLContent = useMemo(() => {
    if (!userImage || !userDepthMap) {
      return null;
    }

    return (
      <WebGLCanvas
        depthMapUrl={userDepthMap}
        textureImageUrl={userImage}
        processing={processingUserImage}
      />
    );
  }, [userImage, userDepthMap, processingUserImage]);

  const handleImageUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setProcessingUserImage(true);
    setUserImage(null);
    setUserDepthMap(null);

    const reader = new FileReader();
    reader.onload = async (e) => {
      const imageUrl = e.target?.result as string;

      try {
        const response = await fetch('http://localhost:8080/depthmap', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ imageData: imageUrl }),
        });

        if (!response.ok) {
          const errorData = await response.json();
          throw new Error(errorData.error || 'Failed to fetch depth map from API.');
        }

        const data = await response.json();
        setUserImage(imageUrl);
        setUserDepthMap(data.depthMap);

      } catch (error) {
        console.error("Error processing image with SDK:", error);
        alert(`Error generating depth map: ${error instanceof Error ? error.message : 'Unknown error'}`);
      } finally {
        setProcessingUserImage(false);
      }
    };
    reader.readAsDataURL(file);
  };

  return (
    <div>
      {(processingUserImage) && <Loader />}
      <nav className="navbar navbar-dark bg-dark">
        <a className="navbar-brand disabled text-light">Image Relighting</a>
      </nav>
      <div className="container">
        <br />
        <div className="row">
          <div className="col">
            <div className="form-group row">
              <label htmlFor="imageUpload" className="col-form-label-md">Upload Custom Image:</label>
              <input
                type="file"
                id="imageUpload"
                accept="image/*"
                onChange={handleImageUpload}
                className="form-control-file"
                disabled={processingUserImage}
              />
            </div>
            <hr />
            {webGLContent}
          </div>
        </div>
      </div>
    </div>
  );
}
