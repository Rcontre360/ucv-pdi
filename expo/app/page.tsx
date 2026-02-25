'use client';
import {useMemo, useState, useRef, useEffect} from 'react';
import Loader from '../components/Loader';
import RelightingCanvas from '../components/RelightingCanvas';
import BokehCanvas from '../components/BokehCanvas';
import FogCanvas from '../components/FogCanvas';
import EdgesCanvas from '../components/EdgesCanvas';
import UnderwaterCanvas from '../components/UnderwaterCanvas';
import DepthMapOverlay from '../components/DepthMapOverlay';

type Mode = 'relighting' | 'bokeh' | 'fog' | 'edges' | 'underwater';

export default function Home() {
  const [mounted, setMounted] = useState(false);
  const [userImage, setUserImage] = useState<string | null>(null);
  const [userDepthMap, setUserDepthMap] = useState<string | null>(null);
  const [processingUserImage, setProcessingUserImage] = useState(false);
  const [activeMode, setActiveMode] = useState<Mode>('relighting');
  const [showDepthMap, setShowDepthMap] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    setMounted(true);
  }, []);

  const content = useMemo(() => {
    if (!userImage || !userDepthMap) {
      return (
        <div className="flex flex-col items-center justify-center h-96 border-2 border-dashed border-gray-300 rounded-lg bg-gray-50 text-gray-500">
          <svg className="w-16 h-16 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          <p className="text-xl font-medium">Upload image</p>
        </div>
      );
    }

    switch (activeMode) {
      case 'relighting':
        return <RelightingCanvas depthMapUrl={userDepthMap} textureImageUrl={userImage} processing={processingUserImage} />;
      case 'bokeh':
        return <BokehCanvas depthMapUrl={userDepthMap} textureImageUrl={userImage} />;
      case 'fog':
        return <FogCanvas depthMapUrl={userDepthMap} textureImageUrl={userImage} />;
      case 'edges':
        return <EdgesCanvas depthMapUrl={userDepthMap} textureImageUrl={userImage} />;
      case 'underwater':
        return <UnderwaterCanvas depthMapUrl={userDepthMap} textureImageUrl={userImage} />;
      default:
        return null;
    }
  }, [userImage, userDepthMap, processingUserImage, activeMode]);

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
        const response = await fetch('/api/depthmap', {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify({imageData: imageUrl}),
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

  const triggerFileUpload = () => {
    fileInputRef.current?.click();
  };

  if (!mounted) return null;

  return (
    <div className="min-h-screen bg-gray-100 text-gray-900 font-sans">
      {processingUserImage && <Loader />}

      <header className="bg-white shadow-sm sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center">
            <h1 className="text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-500 to-indigo-600">
              Depth Maps App - PDI
            </h1>
          </div>

          <div className="flex items-center space-x-4">
            <input
              type="file"
              ref={fileInputRef}
              className="hidden"
              accept="image/*"
              onChange={handleImageUpload}
              disabled={processingUserImage}
            />
            <button
              onClick={triggerFileUpload}
              disabled={processingUserImage}
              className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-md shadow-sm transition-colors duration-200 flex items-center focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
              </svg>
              Upload Image
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

        {userImage && (
          <div className="flex justify-center mb-8">
            <div className="bg-white p-1 rounded-lg shadow-sm inline-flex items-center flex-wrap gap-y-2">
              <button
                onClick={() => setActiveMode('relighting')}
                className={`px-4 py-2 text-sm font-medium rounded-md transition-all duration-200 focus:outline-none ${activeMode === 'relighting'
                  ? 'bg-blue-100 text-blue-700 shadow-sm ring-1 ring-blue-200'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                  }`}
              >
                3D Relighting
              </button>
              <button
                onClick={() => setActiveMode('bokeh')}
                className={`px-4 py-2 text-sm font-medium rounded-md transition-all duration-200 focus:outline-none ml-2 ${activeMode === 'bokeh'
                  ? 'bg-blue-100 text-blue-700 shadow-sm ring-1 ring-blue-200'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                  }`}
              >
                Bokeh Effect
              </button>
              <button
                onClick={() => setActiveMode('fog')}
                className={`px-4 py-2 text-sm font-medium rounded-md transition-all duration-200 focus:outline-none ml-2 ${activeMode === 'fog'
                  ? 'bg-blue-100 text-blue-700 shadow-sm ring-1 ring-blue-200'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                  }`}
              >
                Virtual Fog
              </button>
              <button
                onClick={() => setActiveMode('edges')}
                className={`px-4 py-2 text-sm font-medium rounded-md transition-all duration-200 focus:outline-none ml-2 ${activeMode === 'edges'
                  ? 'bg-blue-100 text-blue-700 shadow-sm ring-1 ring-blue-200'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                  }`}
              >
                Depth Edges
              </button>
              <button
                onClick={() => setActiveMode('underwater')}
                className={`px-4 py-2 text-sm font-medium rounded-md transition-all duration-200 focus:outline-none ml-2 ${activeMode === 'underwater'
                  ? 'bg-blue-100 text-blue-700 shadow-sm ring-1 ring-blue-200'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                  }`}
              >
                Underwater
              </button>

              <div className="h-6 w-px bg-gray-300 mx-4 hidden sm:block"></div>

              <button
                onClick={() => setShowDepthMap(true)}
                className="px-4 py-2 text-sm font-medium text-gray-600 hover:text-gray-900 hover:bg-gray-50 rounded-md transition-all duration-200 focus:outline-none flex items-center ml-auto sm:ml-0 mt-2 sm:mt-0"
                title="View Depth Map"
              >
                <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
                Depth Map
              </button>
            </div>
          </div>
        )}

        <div className="bg-white rounded-xl shadow-lg overflow-hidden min-h-[600px] p-6">
          {content}
        </div>
      </main>

      {userDepthMap && (
        <DepthMapOverlay
          depthMapUrl={userDepthMap}
          isOpen={showDepthMap}
          onClose={() => setShowDepthMap(false)}
        />
      )}
    </div>
  );
}
