const lightingFunctions = `
    float TraceShadow(vec3 hitPos, vec3 lightDir, float lightDist, sampler2D depthSampler, vec3 lightPos) {
        float shadow = 1.0;
        float t = 0.05; 
        float maxt = length(lightPos.xy - hitPos.xy);
        maxt = min(maxt, 1.5); 
        float softShadowK = 8.0; 

        for(int i = 0; i < 48; ++i) { 
            if (t >= maxt) break;
            vec3 curPos = hitPos + lightDir * t;
            vec2 uv = vec2((curPos.x + 1.0) / 2.0, (1.0 - curPos.y) / 2.0);
            if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) break;
            float h = texture2D(depthSampler, uv).r;
            if (curPos.z < h - 0.02) {
                float y = h - curPos.z;
                float effectiveDist = max(t, 0.1); 
                shadow = min(shadow, softShadowK * y / effectiveDist);
                if (shadow < 0.01) { shadow = 0.0; break; }
            }
            t += 0.02; 
        }
        float skyFade = smoothstep(0.0, 0.2, hitPos.z);
        return mix(1.0, clamp(shadow, 0.0, 1.0), skyFade);
    }

    void CalculateBlinnPhong(
        vec3 normal, vec3 lightDir, vec3 viewDir, float shininess,
        vec3 diffColor, vec3 specColor, float lightIntensity,
        out vec3 diffuse, out vec3 specular
    ) {
        float diff = max(dot(normal, lightDir), 0.0);
        diffuse = diff * diffColor * lightIntensity;
        vec3 halfDir = normalize(lightDir + viewDir);
        float spec = pow(max(dot(normal, halfDir), 0.0), shininess);
        specular = specColor * spec * lightIntensity * 0.5;
    }
`;

// main relighting shaders 
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
        pos.y = -((vPos.y / yDiv) - 1.0);
        pos.z = (vPos.z - minMaxZ.x) / (minMaxZ.y - minMaxZ.x + 1.0);

        fPos = pos;
        texCoords = vec2((pos.x + 1.0) / 2.0, (1.0 - pos.y) / 2.0);
        vec3 correctedNormal = normalize(normal);
        fNormal = vec3(correctedNormal.x, -correctedNormal.y, -correctedNormal.z);
        gl_Position = vec4(pos, 1.0);
    }
`;

/**
 * RELIGHTING FRAGMENT SHADER
 * --------------------------
 * Purpose: Renders the actual image with 3D lighting effects.
 * Logic:
 * 1. Samples the original image texture.
 * 2. Samples the depth map to calculate raymarching shadows.
 * 3. Applies Blinn-Phong reflection (Ambient + Diffuse + Specular).
 * 4. Combines everything to produce the final lit pixel color.
 */
export const relightingFragmentShader = `
    precision highp float;
    const vec3 diffuseColor = vec3(1.0, 1.0, 1.0);
    const vec3 specColor = vec3(1.0, 1.0, 1.0);
    const float shininess = 64.0;
    const float ambientStrength = 0.7; 

    uniform vec3 lightPos;
    uniform sampler2D texSampler;
    uniform sampler2D depthSampler;
    uniform int textureLighting;
    uniform float lightIntensity;

    varying vec3 fPos;
    varying vec3 fNormal;
    varying vec2 texCoords;

    ${lightingFunctions}

    void main() {
        vec3 normal = normalize(fNormal);
        vec4 texColor = texture2D(texSampler, texCoords);
        vec3 lightVector = lightPos - fPos;
        float distance = length(lightVector);
        vec3 lightDir = normalize(lightVector);
        vec3 viewDir = vec3(0.0, 0.0, 1.0);

        float attenuation = 1.0 / (1.0 + 0.05 * distance + 0.01 * distance * distance);
        float shadow = 1.0;
        if (dot(normal, lightDir) > 0.0) {
             shadow = TraceShadow(fPos, lightDir, distance, depthSampler, lightPos);
        }

        vec3 ambient = ambientStrength * texColor.rgb;
        vec3 diffuse;
        vec3 specular;
        CalculateBlinnPhong(normal, lightDir, viewDir, shininess, texColor.rgb, specColor, lightIntensity, diffuse, specular);

        diffuse *= shadow * attenuation;
        specular *= shadow * attenuation;

        vec3 finalColor;
        if (textureLighting == 1) {
            finalColor = texColor.rgb;
        } else {
            finalColor = ambient + diffuse + specular;
        }
        gl_FragColor = vec4(finalColor, texColor.a);
    }
`;

// ball shaders to know where's the light point
export const lightVertexShader = `
    precision highp float;
    attribute vec3 position;
    void main() {
        gl_Position = vec4(position, 1.0);
        gl_PointSize = 10.0;
    }
`;

export const lightFragmentShader = `
    precision highp float;
    void main() {
        gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0); 
    }
`;

