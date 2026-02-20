# 3D Relighting Implementation Guide

This document details the specific challenges, solutions, and parameters involved in the 2D-to-3D Relighting feature of this application.

## Core Concept
The goal is to take a flat 2D image and a grayscale depth map and simulate a dynamic light source interacting with the "surface" of the image.

## Key Challenges & Solutions

### 1. The "Dark Image" Bug
*   **Issue:** When enabling lighting, the image would instantly become dark and muddy, even if the light was bright.
*   **Cause:** The standard lighting equation `Ambient + Diffuse` often results in values $< 1.0$ if the ambient term is low (e.g., 0.3). This effectively multiplies the image brightness by 30%.
*   **Solution:** We increased the **Ambient Strength** to `0.7` and ensured that the ambient component is **not** affected by distance attenuation. This treats the original image brightness as the "base" (100% lit by environment) and adds directional light/shadows on top.

### 2. "Corrugated" Surfaces (Noise)
*   **Issue:** Smooth surfaces looked wrinkled or bumpy ("corrugado") under specular lighting.
*   **Cause:** The 8-bit depth map has discrete steps (0, 1, 2...). When calculating normals (slope) between adjacent pixels, these steps create sharp, jagged spikes.
*   **Solution:** 
    1.  **Increased Sampling Radius:** We essentially apply a "smoothing" filter by sampling depth values `4` pixels apart instead of `1`, averaging out the noise.
    2.  **Box Blur:** We average a 3x3 block of depth pixels before calculating the normal.

### 3. Light Bleeding (The "Wall" Problem)
*   **Issue:** Light placed in front of an object would illuminate the floor *behind* the object.
*   **Cause:** Simple local lighting only checks surface angle, not occlusion.
*   **Solution:** **Raymarched Shadows**. We trace a ray from the pixel towards the light in the depth map. If the ray hits a "taller" pixel, we darken the current pixel.

### 4. Shadow Acne & Artifacts
*   **Issue:** Shadows looked blocky or appeared on the lit surface itself.
*   **Cause:** Precision errors in the raymarching loop.
*   **Solution:**
    *   **Bias:** We only count a hit if the blocker is at least `0.02` units higher than the ray.
    *   **Soft Shadows:** We implemented a Penumbra calculation (`k * y / t`) to blur the shadow edges based on distance.

---

## Important Parameters

These values can be tweaked in `app/utils/shaders/relighting.ts` and `app/utils/shaders/lighting_functions.glsl.ts`.

### Lighting (`relighting.ts`)
| Parameter | Value | Description |
| :--- | :--- | :--- |
| `ambientStrength` | `0.7` | Base brightness of the image (0.0 - 1.0). Higher = brighter unlit areas. |
| `shininess` | `64.0` | Sharpness of the specular highlight. Higher = smaller, sharper hotspot. |
| `attenuation` | `0.05` | Linear falloff factor. Lower = light reaches further corners. |

### Shadows (`lighting_functions.glsl.ts`)
| Parameter | Value | Description |
| :--- | :--- | :--- |
| `t` (Start) | `0.05` | How far from the surface to start checking for shadows. Prevents self-shadowing. |
| `softShadowK` | `8.0` | Shadow sharpness. Lower = softer, wider shadows. |
| `iterations` | `48` | Raymarching steps. Higher = more accurate but slower. |
| `bias` | `0.02` | Height difference required to trigger a shadow. Higher = less noise, but might miss small details. |
| `skyFade` | `0.0 - 0.2` | Depth threshold below which shadows are faded out to prevent them from appearing on the background/sky. |
