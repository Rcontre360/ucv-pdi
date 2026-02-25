import React from 'react';

const Loader = () => {
  return (
    <div id="loader-overlay-div">
      <div className="overlay-content-container">
        <div className="spinner"></div>
        <br />
        Creating Mesh. Please Wait!
      </div>
    </div>
  );
};

export default Loader;