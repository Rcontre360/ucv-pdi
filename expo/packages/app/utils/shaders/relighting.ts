export const relightingVertexShader = `
    precision highp float;

    uniform vec2 imgSize;
    uniform vec2 minMaxZ;
    
    attribute vec3 vPos;
    attribute vec3 normal;
    
    varying vec3 fPos;
    varying vec3 fNormal;
    varying vec2 texCoords;

    void main() {
        // Map Pixel Coordinates to NDC [-1, 1]
        // Image: (0,0) Top-Left -> NDC: (-1, 1) Top-Left
        // Image: (W,H) Bot-Right -> NDC: (1, -1) Bot-Right
        
        float xDiv = imgSize.x / 2.0;
        float yDiv = imgSize.y / 2.0;

        vec3 pos;
        pos.x = (vPos.x / xDiv) - 1.0;
        pos.y = -((vPos.y / yDiv) - 1.0); // Flip Y: 0->1, H->-1.
        
        // Z mapping: 0..255 -> 0..1
        pos.z = (vPos.z - minMaxZ.x) / (minMaxZ.y - minMaxZ.x + 1.0);

        fPos = pos;
        
        // Texture Coordinates
        texCoords = vec2((pos.x + 1.0) / 2.0, (1.0 - pos.y) / 2.0);

        // Normal Transformation
        vec3 correctedNormal = normalize(normal);
        
        // Fix Orientation
        // 1. Invert Z because cross-product calc likely produced -Z (into screen) normals, 
        //    but we want +Z (towards camera) for lighting.
        // 2. Invert Y because we flipped the Y-axis position above.
        // 3. Keep X (Image X+ is Right, NDC X+ is Right).
        
        fNormal = vec3(correctedNormal.x, -correctedNormal.y, -correctedNormal.z);

        gl_Position = vec4(pos, 1.0);
    }
`;

export const relightingFragmentShader = `
    precision highp float;

    // Material properties
    const vec3 diffuseColor = vec3(1.0, 1.0, 1.0);
    const vec3 specColor = vec3(1.0, 1.0, 1.0);
    const float shininess = 32.0;
    const float ambientStrength = 0.3;

    uniform vec3 lightPos;
    uniform sampler2D texSampler;
    uniform int textureLighting;
    uniform float lightIntensity;

    varying vec3 fPos;
    varying vec3 fNormal;
    varying vec2 texCoords;

    void main() {
        // Normalize interpolated normal
        vec3 normal = normalize(fNormal);
        vec4 texColor = texture2D(texSampler, texCoords);

        // Vector from fragment to light
        vec3 lightVector = lightPos - fPos;
        float distance = length(lightVector);
        vec3 lightDir = normalize(lightVector);

        // --- Attenuation ---
        // Inverse square law with a tweak for the scene scale (NDC -1 to 1)
        float attenuation = 1.0 / (1.0 + 0.1 * distance + 0.2 * distance * distance);

        // --- Ambient ---
        vec3 ambient = ambientStrength * texColor.rgb;

        // --- Diffuse (Lambert) ---
        float diff = max(dot(normal, lightDir), 0.0);
        vec3 diffuse = diff * texColor.rgb * lightIntensity;

        // --- Specular (Blinn-Phong) ---
        vec3 viewDir = vec3(0.0, 0.0, 1.0); 
        vec3 halfDir = normalize(lightDir + viewDir);
        float spec = pow(max(dot(normal, halfDir), 0.0), shininess);
        
        vec3 specular = specColor * spec * lightIntensity * 0.8;

        // Apply Attenuation
        diffuse *= attenuation;
        specular *= attenuation;

        vec3 finalColor;

        if (textureLighting == 1) {
            // Mode 1: Texture Only (Unlit)
            finalColor = texColor.rgb;
        } 
        else {
            // Mode 3: Texture + Lighting (Default)
            vec3 lighting = ambient + diffuse; 
            finalColor = lighting + specular;
        }

        gl_FragColor = vec4(finalColor, texColor.a);
    }
`;
