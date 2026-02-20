# WebGL Shaders: Technical Deep Dive

This document explains the mathematical pipeline and per-pixel logic for every shader in the application.

---

## 1. 3D Relighting Pipeline
This is a true 3D pipeline that generates geometry from a heightmap.

### A. Vertex Shader: Displacement Mapping
**Input:** 2D pixel coordinates and a raw depth value.
1.  **Normalization:** Pixel $(x, y)$ is divided by half the image size and shifted by -1 to fit the WebGL NDC range $[-1, 1]$.
2.  **Y-Flip:** The Y coordinate is inverted: `pos.y = -((vPos.y / yDiv) - 1.0)`.
3.  **Depth Scaling:** Raw depth (0-255) is normalized to a 0.0-1.0 range: `(z - minZ) / (maxZ - minZ)`.
4.  **Normal Correction:** The CPU-calculated normal is adjusted to match the 3D visual. Since we flipped Y and Z in the geometry, we must flip them in the normal vector to keep lighting consistent.

### B. Fragment Shader (`relightingFragmentShader`)
**Logic for each pixel:**
1.  **Diffuse (Lambert):** Calculates `dot(Normal, LightDir)`.
2.  **Specular (Blinn-Phong):** Calculates the "Halfway Vector" between Light and Camera. The dot product of this vector and the Normal, raised to a power, creates the shiny hotspot.
3.  **Combining:** `Final = (Ambient + Diffuse + Specular) * Texture`.

---

## 2. 2D Post-Processing Pipeline
All these shaders run on a flat quad (two triangles) covering the screen. The "3D" feeling comes from sampling the depth texture.

### A. Bokeh (Depth of Field)
**Logic for each pixel:**
1.  **CoC (Circle of Confusion):** Calculate `blurRadius = abs(currentPixelDepth - focusDepth) * aperture`.
2.  **Weighted Sampling:** If `blurRadius > 0`, the shader enters a nested loop (box blur). It samples neighboring pixels in a grid.
3.  **Averaging:** It adds up the colors of all samples and divides by the total count.
4.  **Optimization:** If the pixel is exactly at the focal plane, the loop is skipped for performance.

### B. Virtual Fog
**Logic for each pixel:**
1.  **Fog Factor:** Uses `smoothstep(near, far, currentPixelDepth)`.
    *   Depth < Near: Fog is 0.
    *   Depth > Far: Fog is 1.0.
    *   In between: Smooth linear ramp.
2.  **Scattering:** Blends the original pixel color with a constant light-blue `fogColor` using the Fog Factor. DISTANT pixels are replaced by the fog color.

### C. Screen Space Ambient Occlusion (SSAO)
**Logic for each pixel:**
1.  **Random Sampling:** The shader picks 16 points in a circle around the current pixel. It uses a `rand()` function to "jitter" the points to avoid banding artifacts.
2.  **Occlusion Check:** For each neighbor, it checks: *"Is the neighbor's depth significantly higher than mine?"*
3.  **Range Check:** If the neighbor is too far away in depth (e.g., background vs foreground), it is ignored to prevent "halos" around objects.
4.  **Darkening:** The more "occluders" found, the darker the pixel becomes. This creates shadows in cracks and corners.

### D. Depth Edges (Sobel Filter)
**Logic for each pixel:**
1.  **Gradient Analysis:** Samples the depth of the 8 surrounding neighbors.
2.  **Horizontal Change ($G_x$):** Subtracts left neighbors from right neighbors.
3.  **Vertical Change ($G_y$):** Subtracts top neighbors from bottom neighbors.
4.  **Magnitude:** Calculates the "hypotenuse" of the change: `sqrt(Gx² + Gy²)`.
5.  **Threshold:** If the magnitude is greater than the user's setting, the pixel is painted black (Edge). Otherwise, it's white.

---

## 3. Light Visualizer
**Logic for each pixel:**
1.  **Distance Check:** Inside the `gl.POINTS` square, calculate the distance from the center.
2.  **Discard:** If distance > 0.5 (outside circle), kill the pixel.
3.  **Outline:** If distance > 0.4, paint White. Otherwise, paint Yellow.
4.  **Result:** A sharp, vector-like 2D circle layered on top of the 3D scene.