import { lightingFunctions } from './lighting_functions.glsl';

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
        float xDiv = imgSize.x / 2.0;
        float yDiv = imgSize.y / 2.0;

        vec3 pos;
        pos.x = (vPos.x / xDiv) - 1.0;
        pos.y = -((vPos.y / yDiv) - 1.0); // Flip Y
        pos.z = (vPos.z - minMaxZ.x) / (minMaxZ.y - minMaxZ.x + 1.0);

        fPos = pos;
        texCoords = vec2((pos.x + 1.0) / 2.0, (1.0 - pos.y) / 2.0);

        vec3 correctedNormal = normalize(normal);
        fNormal = vec3(correctedNormal.x, -correctedNormal.y, -correctedNormal.z);

        gl_Position = vec4(pos, 1.0);
    }
`;

export const relightingFragmentShader = `
    precision highp float;

    // --- Configuration ---
    const vec3 diffuseColor = vec3(1.0, 1.0, 1.0);
    const vec3 specColor = vec3(1.0, 1.0, 1.0);
    const float shininess = 64.0;
    const float ambientStrength = 0.7; 

    // --- Uniforms ---
    uniform vec3 lightPos;
    uniform sampler2D texSampler;
    uniform sampler2D depthSampler;
    uniform int textureLighting;
    uniform float lightIntensity;

    // --- Varyings ---
    varying vec3 fPos;
    varying vec3 fNormal;
    varying vec2 texCoords;

    // --- Helper Functions ---
    ${lightingFunctions}

    void main() {
        vec3 normal = normalize(fNormal);
        vec4 texColor = texture2D(texSampler, texCoords);

        // 1. Light Vectors
        vec3 lightVector = lightPos - fPos;
        float distance = length(lightVector);
        vec3 lightDir = normalize(lightVector);
        vec3 viewDir = vec3(0.0, 0.0, 1.0); // Viewer is always "in front"

        // 2. Attenuation (Quadratic Falloff)
        float attenuation = 1.0 / (1.0 + 0.05 * distance + 0.01 * distance * distance);

        // 3. Shadow (Raymarching)
        float shadow = 1.0;
        if (dot(normal, lightDir) > 0.0) {
             shadow = TraceShadow(fPos, lightDir, distance, depthSampler, lightPos);
        }

        // 4. Lighting Components
        vec3 ambient = ambientStrength * texColor.rgb;
        
        vec3 diffuse;
        vec3 specular;
        CalculateBlinnPhong(normal, lightDir, viewDir, shininess, texColor.rgb, specColor, lightIntensity, diffuse, specular);

        // 5. Combine (Apply Shadow & Attenuation)
        diffuse *= shadow * attenuation;
        specular *= shadow * attenuation;

        vec3 finalColor;

        if (textureLighting == 1) {
            finalColor = texColor.rgb; // Unlit
        } 
        else {
            finalColor = ambient + diffuse + specular; // Lit
        }

        gl_FragColor = vec4(finalColor, texColor.a);
    }
`;