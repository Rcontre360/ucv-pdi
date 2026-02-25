export const underwaterFragmentShader = `
  precision mediump float;
  uniform sampler2D u_image;
  uniform sampler2D u_depth;
  uniform float u_waterDensity;
  uniform float u_redAbsorb;
  
  varying vec2 texCoords;

  void main() {
    float depthRaw = texture2D(u_depth, texCoords).r; 
    float dist = 1.0 - depthRaw; // 0.0 = Near, 1.0 = Far

    vec2 distortedUV = texCoords;
    vec3 color = texture2D(u_image, distortedUV).rgb;

    float redFactor = exp(-dist * u_redAbsorb * 8.0);
    float greenFactor = exp(-dist * u_redAbsorb * 2.5);
    float blueFactor = exp(-dist * u_redAbsorb * 0.8);
    
    vec3 waterColor = vec3(0.0, 0.2, 0.3);
    vec3 absorbed = color;
    absorbed.r *= redFactor;
    absorbed.g *= greenFactor;
    absorbed.b *= blueFactor;

    float scatterFactor = smoothstep(0.0, 1.2, dist * u_waterDensity * 3.0);
    vec3 finalColor = mix(absorbed, waterColor, scatterFactor);

    float vignette = length(texCoords - 0.5);
    finalColor *= 1.0 - (vignette * 0.4);
    finalColor = mix(finalColor, waterColor, 0.1); 

    gl_FragColor = vec4(finalColor, 1.0);
  }
`;
