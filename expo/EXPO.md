# Exposition: Advanced Image Processing with Depth Maps

This document outlines the structure and key content for our presentation. It is designed to bridge the gap between modern AI-based depth estimation and classical image processing techniques (shaders/math).

## 1. Introduction: Motivation & Context
*   **Why this project?**
    *   [TO BE FILLED BY USER: Insert personal motivation here. e.g., "The desire to bring static 2D images to life," or "Exploring how 3D information can be extracted from 2D data."]
    *   **The Problem:** Traditional image processing operates on a 2D plane (X, Y). We lack information about the Z-axis (distance), limiting our ability to simulate physical phenomena like light interaction, focus, or atmosphere.
    *   **The Solution:** Using Depth Maps as a bridge. We transform a standard photo into a "2.5D" scene.

## 2. What are Depth Maps?
*   **Definition:** A grayscale image where the intensity of each pixel corresponds to its distance from the camera.
    *   White (1.0) = Near.
    *   Black (0.0) = Far.
*   **Traditional Acquisition (Stereo Vision):**
    *   **Concept:** Mimics human eyes. Two cameras separated by a known baseline.
    *   **The Math:** Triangulation using *Disparity* (the shift of an object's position between the left and right image).
    *   **Formula:** $Z = (f \cdot b) / d$ (where $f$=focal length, $b$=baseline, $d$=disparity).
    *   **Limitations:** Requires specialized hardware, precise calibration, and struggles with texture-less surfaces (like white walls).

## 3. The Modern Approach: Monocular Depth Estimation
*   **The Model:** `DepthAnythingV2` (via Replicate API).
*   **How it works (Simplified for IP Context):**
    *   It treats depth estimation as a dense regression problem.
    *   Instead of triangulation, it uses a massive Deep Neural Network (Transformer-based) trained on millions of labeled images.
    *   It learns "Semantic Cues":
        *   *Perspective:* Parallel lines converging.
        *   *Scale:* People in the back are smaller than people in the front.
        *   *Occlusion:* Objects in front block objects behind.
*   **Why we use it:** It works on *any* single image (Monocular), removing the need for stereo hardware. It provides the "Z-buffer" usually only available in 3D rendering engines.

## 4. Feature I: 3D Relighting
*   **General Idea:** Changing the lighting conditions of a photo *after* it has been taken.
*   **Why Depth is needed:** Light reacts to surface angles (Normal vectors). A flat 2D image has only one normal (pointing straight at the viewer). We need the surface topology to calculate shadows and highlights.
*   **Implementation (Math & Shaders):**
    1.  **Normal Generation:** We calculate the gradient (slope) of the depth map ($\nabla Z$) to generate a Normal Map.
        *   $\vec{N} = \text{normalize}(\partial z/\partial x, \partial z/\partial y, 1.0)$
    2.  **Blinn-Phong Reflection Model:**
        *   **Ambient:** Base light.
        *   **Diffuse:** $\vec{N} \cdot \vec{L}$ (Dot product of Normal and Light direction).
        *   **Specular:** $(\vec{N} \cdot \vec{H})^s$ (Shiny highlights based on viewer position).
*   **Alternatives:** Photometric Stereo (using multiple images of the same object under different lights to solve for shape).

## 5. Feature II: Bokeh (Depth of Field)
*   **General Idea:** Simulating a camera lens with a wide aperture, where only a specific slice of the world is in focus.
*   **Why Depth is needed:** We need to know "where" pixels are relative to the focal plane to determine how much to blur them. Standard Gaussian blur affects the whole image equally.
*   **Implementation (Math & Shaders):**
    1.  **Circle of Confusion (CoC):** We calculate a blur radius $R$ for every pixel.
    2.  **Formula:** $R = |Depth_{pixel} - Depth_{focus}| \cdot \text{ApertureStrength}$.
    3.  **Variable Blur:** We apply a convolution (averaging neighbors) where the kernel size depends on $R$.
*   **Alternatives:** Manual segmentation (masking the subject in Photoshop), which is slow and static.

## 6. Feature III: Virtual Fog (Atmospheric Scattering)
*   **General Idea:** Simulating light scattering through particles (air, dust, moisture). Distant objects lose contrast and take on the color of the atmosphere.
*   **Why Depth is needed:** Scattering is a function of distance. Without depth, we can't tell if a white pixel is a white shirt nearby or a cloud far away.
*   **Implementation (Math & Shaders):**
    1.  **Linear Interpolation (Lerp):** The fundamental blending operation.
    2.  **Fog Factor ($f$):** A value between 0 and 1, derived from depth.
    3.  **Formula:** $Color_{final} = \text{mix}(Color_{pixel}, Color_{fog}, f)$.
    4.  Often uses `smoothstep` to control where the fog starts and ends.
*   **Alternatives:** Darkening the image globally (looks like exposure reduction, not fog).

## 7. Feature IV: Depth Edges (Geometric Borders)
*   **General Idea:** Detecting the outlines of objects based on their physical separation, not their color. useful for "Toon Shading" or technical analysis.
*   **Why Depth is needed:** Standard edge detection (like Canny) on RGB images picks up *textures* (stripes on a shirt, shadows). Depth edge detection ignores textures and finds only the object's silhouette.
*   **Implementation (Math & Shaders):**
    1.  **Sobel Filter:** A convolution matrix used to find gradients.
        2. We run Sobel on the **Depth Map**.
        3. Areas with high rates of change in depth (discontinuities) are marked as edges.
    *   **Alternatives:** Canny Edge Detection on RGB (noisy), Semantic Segmentation (identifies "person" vs "car" but maybe not the exact outline).
    
