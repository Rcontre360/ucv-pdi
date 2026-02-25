export const edgesFragmentShader = `
  precision mediump float;
  uniform sampler2D u_depth;
  uniform float u_threshold;
  uniform float u_thickness;
  uniform vec2 u_resolution;
  varying vec2 texCoords;

  void main() {
    float x = u_thickness / u_resolution.x;
    float y = u_thickness / u_resolution.y;

    float d_tl = texture2D(u_depth, texCoords + vec2(-x, -y)).r;
    float d_t  = texture2D(u_depth, texCoords + vec2( 0, -y)).r;
    float d_tr = texture2D(u_depth, texCoords + vec2( x, -y)).r;
    
    float d_l  = texture2D(u_depth, texCoords + vec2(-x,  0)).r;
    float d_r  = texture2D(u_depth, texCoords + vec2( x,  0)).r;
    
    float d_bl = texture2D(u_depth, texCoords + vec2(-x,  y)).r;
    float d_b  = texture2D(u_depth, texCoords + vec2( 0,  y)).r;
    float d_br = texture2D(u_depth, texCoords + vec2( x,  y)).r;

    float gx = (d_tl + 2.0*d_l + d_bl) - (d_tr + 2.0*d_r + d_br);
    float gy = (d_tl + 2.0*d_t + d_tr) - (d_bl + 2.0*d_b + d_br);
    
    float g = sqrt(gx*gx + gy*gy);
    float edge = g > u_threshold ? 0.0 : 1.0;
    
    gl_FragColor = vec4(vec3(edge), 1.0);
  }
`;
