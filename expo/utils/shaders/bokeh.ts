export const bokehFragmentShader = `
  precision mediump float;
  uniform sampler2D u_image;
  uniform sampler2D u_depth;
  uniform float u_focusDepth;
  
  varying vec2 texCoords;

  void main() {
    vec4 depthColor = texture2D(u_depth, texCoords);
    float depth = depthColor.r;
    
    float blur = min(abs(depth - u_focusDepth) * 0.007, 0.002);
    
    vec4 col = vec4(0.0);
    float totalWeight = 0.0;
    
    if (blur < 0.0001) {
        gl_FragColor = texture2D(u_image, texCoords);
        return;
    }

    float radius = clamp(blur, 0.0, 0.05); 
    
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
