export const underwaterVertexShader = `
  attribute vec2 position;
  varying vec2 texCoords;
  void main() {
    texCoords = (position + 1.0) / 2.0;
    texCoords.y = 1.0 - texCoords.y; // Flip Y
    gl_Position = vec4(position, 0.0, 1.0);
  }
`;

export const underwaterFragmentShader = `
  precision mediump float;
  uniform sampler2D u_image;
  uniform sampler2D u_depth;
  uniform float u_waterDensity;
  uniform float u_redAbsorb;
  
  varying vec2 texCoords;

  void main() {
    float depthRaw = texture2D(u_depth, texCoords).r; // 1.0 = Near, 0.0 = Far
    float dist = 1.0 - depthRaw; // 0.0 = Near, 1.0 = Far

    // --- 1. Distance-Based Refraction (Wobble) ---
    // Wobble effect disabled.
    vec2 distortedUV = texCoords;

    // --- 2. Color Sampling ---
    // No chromatic aberration.
    vec3 color = texture2D(u_image, distortedUV).rgb;

    // --- 3. Absorption (Beer-Lambert) ---
    // Red dies first, then Green.
    float redFactor = exp(-dist * u_redAbsorb * 8.0);
    float greenFactor = exp(-dist * u_redAbsorb * 2.5);
    float blueFactor = exp(-dist * u_redAbsorb * 0.8);
    
    vec3 absorbedColor = color;
    absorbedColor.r *= redFactor;
    absorbedColor.g *= greenFactor;
    absorbedColor.b *= blueFactor;

    // --- 4. Scattering (Murkiness) ---
    vec3 waterColor = vec3(0.0, 0.2, 0.3); // Darker Deep Teal
    float scatterFactor = smoothstep(0.0, 1.2, dist * u_waterDensity * 3.0);
    
    vec3 finalColor = mix(absorbedColor, waterColor, scatterFactor);

    // --- 5. Vignette & Global Wash ---
    float vignette = length(texCoords - 0.5);
    finalColor *= 1.0 - (vignette * 0.4);
    finalColor = mix(finalColor, waterColor, 0.1); 

    gl_FragColor = vec4(finalColor, 1.0);
  }
`;
