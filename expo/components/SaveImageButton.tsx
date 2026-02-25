'use client';

import React from 'react';
import Button from './Button';

interface SaveImageButtonProps {
  canvasRef: React.RefObject<HTMLCanvasElement | null>;
  filenamePrefix: string;
  disabled?: boolean;
}

const SaveImageButton: React.FC<SaveImageButtonProps> = ({ canvasRef, filenamePrefix, disabled }) => {
  const handleSaveImage = () => {
    if (!canvasRef.current) return;
    
    try {
      const link = document.createElement('a');
      link.download = `${filenamePrefix}-${Date.now()}.png`;
      link.href = canvasRef.current.toDataURL('image/png');
      link.click();
    } catch (error) {
      console.error("Error saving image:", error);
      alert("Failed to save image. This might be due to cross-origin restrictions if the image was loaded from an external domain.");
    }
  };

  return (
    <div className="pt-6 border-t border-gray-200">
      <Button
        onClick={handleSaveImage}
        disabled={disabled}
        className="w-full"
      >
        <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
        </svg>
        Save Resulting Image
      </Button>
    </div>
  );
};

export default SaveImageButton;
