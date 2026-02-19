export const lightingFunctions = `
    // Raymarching for Soft Shadows
    // hitPos: The 3D position of the surface pixel
    // lightDir: Normalized vector towards the light
    // lightDist: Distance to the light source
    // depthSampler: The depth map texture
    float TraceShadow(vec3 hitPos, vec3 lightDir, float lightDist, sampler2D depthSampler, vec3 lightPos) {
        float shadow = 1.0;
        
        // Start further away to avoid self-shadowing acne from noisy depth map
        float t = 0.05; 
        
        float maxt = length(lightPos.xy - hitPos.xy);
        maxt = min(maxt, 1.5); 

        // Reduced sharpness factor to make shadows softer and less sensitive to noise
        float softShadowK = 8.0; 

        for(int i = 0; i < 48; ++i) { 
            if (t >= maxt) break;
            
            vec3 curPos = hitPos + lightDir * t;
            vec2 uv = vec2((curPos.x + 1.0) / 2.0, (1.0 - curPos.y) / 2.0);
            
            if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) break;
            
            float h = texture2D(depthSampler, uv).r;
            
            // Bias: We only consider it an occlusion if it's significantly higher 
            // than our ray. 0.02 bias helps ignore micro-bumps.
            if (curPos.z < h - 0.02) {
                // height difference
                float y = h - curPos.z;
                
                // Soft shadow formula: min(shadow, k * y / t)
                // We clamp t to a minimum value to prevent division by near-zero 
                // which causes artifacts right at the base of objects.
                float effectiveDist = max(t, 0.1); 
                
                shadow = min(shadow, softShadowK * y / effectiveDist);
                
                if (shadow < 0.01) {
                    shadow = 0.0;
                    break;
                }
            }
            
            t += 0.02; 
        }
        
        // Fade out shadow on background (Sky should not receive shadows)
        // hitPos.z is 0.0 at far plane, 1.0 at near plane.
        // smoothstep(0.0, 0.2, hitPos.z) returns 0.0 for deep background, 1.0 for foreground.
        float skyFade = smoothstep(0.0, 0.2, hitPos.z);
        
        return mix(1.0, clamp(shadow, 0.0, 1.0), skyFade);
    }

    // Standard Blinn-Phong Lighting Calculation
    // normal: Surface normal
    // lightDir: Vector to light
    // viewDir: Vector to viewer (camera)
    // shininess: Specular power
    // diffColor: Material diffuse color
    // specColor: Material specular color
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
