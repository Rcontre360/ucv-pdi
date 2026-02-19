export const ssaoVertexShader = `
  attribute vec2 position;
  varying vec2 texCoords;
  void main() {
    texCoords = (position + 1.0) / 2.0;
    texCoords.y = 1.0 - texCoords.y;
    gl_Position = vec4(position, 0.0, 1.0);
  }
`;

export const ssaoFragmentShader = `
  precision mediump float;
  uniform sampler2D u_image;
  uniform sampler2D u_depth;
  uniform vec2 u_resolution;
  uniform float u_radius;     // How wide to look for occluders
  uniform float u_bias;       // Threshold to avoid self-shadowing
  uniform float u_intensity;  // Darkness strength

  varying vec2 texCoords;

  // Pseudo-random noise to jitter samples (prevents banding)
  float rand(vec2 co){
      return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
  }

  void main() {
    float depth = texture2D(u_depth, texCoords).r;
    
    float occlusion = 0.0;
    float samples = 16.0;
    
    // Iterate over a circle around the pixel
    for (float i = 0.0; i < 16.0; i++) {
        // Randomize radius and angle for cleaner look
        float r = u_radius * (0.5 + 0.5 * rand(texCoords + vec2(i, i)));
        float angle = 6.283 * (i / samples) + rand(texCoords) * 2.0;
        
        vec2 offset = vec2(cos(angle), sin(angle)) * r;
        // Aspect ratio correction roughly
        offset.x /= (u_resolution.x / u_resolution.y);
        
        vec2 sampleCoords = texCoords + offset;
        
        // Read neighbor depth
        float sampleDepth = texture2D(u_depth, sampleCoords).r;
        
        // Range Check: Only occlude if the neighbor is somewhat close in space
        // If neighbor is way in front (sampleDepth much larger), it's a separate object, not a crease.
        float rangeCheck = smoothstep(0.0, 1.0, u_radius / abs(depth - sampleDepth));
        
        // The Math:
        // If sampleDepth (neighbor) > depth (current) + bias, it means the neighbor 
        // is "sticking out" in front of the current pixel. Therefore, it blocks light.
        if (sampleDepth >= depth + u_bias) {
            occlusion += 1.0 * rangeCheck;
        }
    }
    
    occlusion = 1.0 - (occlusion / samples) * u_intensity;
    
    // Output options: 
    // 1. Pure AO (Clay mode) -> vec3(occlusion)
    // 2. Mixed -> texture * occlusion
    
    // Let's show Pure AO as it demonstrates the "Depth Only" feature best
    gl_FragColor = vec4(vec3(occlusion), 1.0);
  }
`;
