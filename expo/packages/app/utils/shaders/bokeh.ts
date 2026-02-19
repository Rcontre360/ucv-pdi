export const bokehVertexShader = `
  attribute vec2 position;
  varying vec2 texCoords;
  void main() {
    texCoords = (position + 1.0) / 2.0;
    texCoords.y = 1.0 - texCoords.y; // Flip Y for texture
    gl_Position = vec4(position, 0.0, 1.0);
  }
`;

export const bokehFragmentShader = `
  precision mediump float;
  uniform sampler2D u_image;
  uniform sampler2D u_depth;
  uniform vec2 u_resolution;
  uniform float u_focusDepth;
  uniform float u_aperture;
  
  varying vec2 texCoords;

  void main() {
    vec4 depthColor = texture2D(u_depth, texCoords);
    float depth = depthColor.r;
    
    // Calculate blur radius based on distance from focal plane
    float blur = abs(depth - u_focusDepth) * u_aperture * 0.05; // Scaling factor
    
    vec4 col = vec4(0.0);
    float totalWeight = 0.0;
    
    if (blur < 0.001) {
        gl_FragColor = texture2D(u_image, texCoords);
        return;
    }

    // Limit max blur to prevent performance kill
    float radius = clamp(blur, 0.0, 0.02); 
    
    // Box blur kernel
    for (float x = -2.0; x <= 2.0; x++) {
      for (float y = -2.0; y <= 2.0; y++) {
        vec2 offset = vec2(x, y) * radius;
        vec4 c = texture2D(u_image, texCoords + offset);
        col += c;
        totalWeight += 1.0;
      }
    }
    
    gl_FragColor = col / totalWeight;
  }
`;
