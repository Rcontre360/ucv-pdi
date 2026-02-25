export const fogVertexShader = `
  attribute vec2 position;
  varying vec2 texCoords;
  void main() {
    texCoords = (position + 1.0) / 2.0;
    texCoords.y = 1.0 - texCoords.y; // Flip Y
    gl_Position = vec4(position, 0.0, 1.0);
  }
`;

export const fogFragmentShader = `
  precision mediump float;
  uniform sampler2D u_image;
  uniform sampler2D u_depth;
  uniform float u_fogDensity;
  uniform float u_fogNear;
  uniform float u_fogFar;
  uniform vec3 u_fogColor;
  
  varying vec2 texCoords;

  void main() {
    vec4 color = texture2D(u_image, texCoords);
    vec4 depthVal = texture2D(u_depth, texCoords);
    float depth = depthVal.r;
    
    // Linear Fog
    float fogFactor = smoothstep(u_fogNear, u_fogFar, depth);
    
    // Mix with density
    fogFactor *= u_fogDensity;
    fogFactor = clamp(fogFactor, 0.0, 1.0);
    
    vec3 finalColor = mix(color.rgb, u_fogColor, fogFactor);
    
    gl_FragColor = vec4(finalColor, color.a);
  }
`;
