const lightingFunctions = `
    void BlinnPhong(
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

export const relightingFragmentShader = `
    precision highp float;
    const vec3 diffuseColor = vec3(1.0, 1.0, 1.0);
    const vec3 specColor = vec3(1.0, 1.0, 1.0);
    const float shininess = 64.0;
    const float ambientStrength = 1.0; 

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
        vec4 depthColor = texture2D(depthSampler, texCoords);
        vec3 lightVector = lightPos - fPos;
        float distance = length(lightVector);
        vec3 lightDir = normalize(lightVector);
        vec3 viewDir = vec3(0.0, 0.0, 1.0);

        float attenuation = 1.0 / (1.0 + 0.05 * distance + 0.01 * distance * distance);

        vec3 baseColor = (textureLighting == 4) ? depthColor.rgb : texColor.rgb;
        vec3 ambient = ambientStrength * baseColor;
        vec3 diffuse;
        vec3 specular;
        BlinnPhong(normal, lightDir, viewDir, shininess, baseColor, specColor, lightIntensity, diffuse, specular);

        diffuse *= attenuation;
        specular *= attenuation;

        vec3 finalColor;
        if (textureLighting == 1) {
            finalColor = texColor.rgb;
        } else if (textureLighting == 2) {
            finalColor = depthColor.rgb;
        } else if (textureLighting == 4) {
            finalColor = ambient + diffuse + specular;
        } else {
            finalColor = ambient + diffuse + specular;
        }
        gl_FragColor = vec4(finalColor, 1.0);
    }
`;

// light ball
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
        float r = distance(gl_PointCoord, vec2(0.5));
        if (r > 0.5) { discard; }
        if (r > 0.4) {
            gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);
        } else {
            gl_FragColor = vec4(1.0, 1.0, 0.0, 1.0);
        }
    }
`;
