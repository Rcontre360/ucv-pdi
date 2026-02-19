export const lightVertexShader = `
    precision highp float;
    attribute vec3 position;
    void main() {
        gl_Position = vec4(position, 1.0);
        gl_PointSize = 15.0; // Size of the light "ball"
    }
`;

export const lightFragmentShader = `
    precision highp float;
    void main() {
        // Calculate distance from center of the point (0.5, 0.5)
        float r = distance(gl_PointCoord, vec2(0.5));
        
        // Discard pixels outside the circle radius
        if (r > 0.5) {
            discard;
        }
        
        // Soft edge
        float alpha = 1.0 - smoothstep(0.3, 0.5, r);
        
        // Glowing yellow/white color
        gl_FragColor = vec4(1.0, 1.0, 0.8, alpha);
    }
`;
