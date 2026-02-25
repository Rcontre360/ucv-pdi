'use client';

interface DepthMapOverlayProps {
  depthMapUrl: string;
  isOpen: boolean;
  onClose: () => void;
}

const DepthMapOverlay: React.FC<DepthMapOverlayProps> = ({ depthMapUrl, isOpen, onClose }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-75" onClick={onClose}>
      <div className="relative max-w-4xl w-full max-h-[90vh] p-4 bg-white rounded-lg shadow-2xl flex flex-col" onClick={(e) => e.stopPropagation()}>
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-xl font-semibold text-gray-900">Depth Map Preview</h3>
          <button 
            onClick={onClose}
            className="text-gray-500 hover:text-gray-700 focus:outline-none p-2 rounded-full hover:bg-gray-100"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div className="flex-1 overflow-auto flex items-center justify-center bg-gray-100 rounded border border-gray-300">
          <img 
            src={depthMapUrl} 
            alt="Depth Map" 
            className="max-w-full max-h-[70vh] object-contain"
          />
        </div>
        <div className="mt-4 text-sm text-gray-500">
          <p>This grayscale map represents depth: White indicates near objects, while Black indicates far objects.</p>
        </div>
      </div>
    </div>
  );
};

export default DepthMapOverlay;
