# Image Processing & Depth Map Techniques

This document details the mathematical and logical foundations of the visual effects implemented in this application. All features are driven by a **Depth Map**, a grayscale image where pixel intensity represents the distance of the object from the camera.

---

## 1. 3D Relighting

**Goal:** To simulate how a light source would interact with the 2D image as if it were a 3D surface.

### The Logic
We treat the image as a topographic map. Brighter pixels (Near) represent "peaks," and darker pixels (Far) represent "valleys." By knowing the height of every pixel, we can calculate which direction the surface is facing at any point.

### The Math (Blinn-Phong Reflection Model)
The light intensity $I$ at any pixel is calculated using three components:

1.  **Surface Normal ($\vec{N}$):**
    We calculate the slope of the depth map using the difference between neighboring pixels.
    $$ \vec{N} = \text{normalize}(\vec{V}_{right} \times \vec{V}_{top}) $$
    Where $\vec{V}$ are vectors pointing to the pixel's neighbors.

2.  **Diffuse Reflection (Lambertian):**
    Light hits surfaces directly facing it more intensely.
    $$ I_{diffuse} = \max(\vec{N} \cdot \vec{L}, 0) $$
    Where $\vec{L}$ is the direction vector from the surface to the light source.

3.  **Specular Reflection (Highlights):**
    This simulates the shiny "hotspot" on a surface. It is strongest when the light reflects directly into the viewer's eye.
    $$ I_{specular} = (\vec{N} \cdot \vec{H})^s $$
    Where $\vec{H}$ is the "halfway vector" between the light direction and the view direction, and $s$ is the shininess factor.

**Final Color:**
$$ \text{Color} = (\text{Ambient} + I_{diffuse} + I_{specular}) \times \text{TextureColor} $$

---

## 2. Depth-of-Field (Bokeh Effect)

**Goal:** To mimic a camera lens where objects outside the focal plane appear blurred.

### The Logic
A physical lens focuses light rays from a specific distance onto the film/sensor perfectly. Rays from objects closer or farther away do not converge, creating a "Circle of Confusion." The size of this circle determines the blur amount.

### The Math
1.  **Focal Distance ($d_{focus}$):** The specific depth value (0.0 to 1.0) chosen by the user to be sharp.
2.  **Blur Radius ($R$):**
    Calculated for every pixel based on its depth ($d_{pixel}$):
    $$ R = |d_{pixel} - d_{focus}| \times \text{Aperture} $$
    *   If $d_{pixel} \approx d_{focus}$, $R \approx 0$ (Sharp).
    *   As distance increases, $R$ grows linearly (Blurred).

3.  **Convolution:**
    We average the colors of neighboring pixels within the radius $R$.
    $$ \text{Color}(x,y) = \frac{1}{N} \sum_{i,j \in R} \text{Texture}(x+i, y+j) $$

---

## 3. Virtual Fog

**Goal:** To simulate atmospheric scattering, where distant objects are obscured by haze.

### The Logic
Light traveling through the atmosphere hits particles (water, dust). The farther an object is, the more particles the light must pass through, causing the object's color to be replaced by the color of the fog (scattering).

### The Math (Linear Mixing)
We mix the original pixel color ($\vec{C}_{orig}$) with the fog color ($\vec{C}_{fog}$) based on a "Fog Factor" derived from the depth.

1.  **Fog Factor ($f$):**
    $$ f = \text{smoothstep}(\text{Start}, \text{End}, \text{Depth}) \times \text{Density} $$
    *   This creates a smooth transition from 0 (No Fog) to 1 (Full Fog) between the defined start and end distances.

2.  **Linear Interpolation (Lerp):**
    $$ \vec{C}_{final} = (1 - f) \cdot \vec{C}_{orig} + f \cdot \vec{C}_{fog} $$

---

## 4. 3D Parallax Effect

**Goal:** To create the illusion of 3D volume by moving foreground and background layers at different speeds.

### The Logic
When an observer moves, objects closer to them appear to move across their field of view faster than objects far away. We simulate this by shifting the image texture based on the mouse position.

### The Math (Texture Shifting)
We calculate a displacement vector ($\Delta \vec{UV}$) for the texture coordinates:

$$ \Delta \vec{UV} = \vec{MousePosition} \times \text{Depth} \times \text{Intensity} $$

*   **Near Pixels (Depth $\approx$ 1.0):** Large shift.
*   **Far Pixels (Depth $\approx$ 0.0):** Small or zero shift.

This effectively "slides" the foreground pixels over the background pixels, creating a strong sensation of depth.

---

## 5. Depth Edge Detection

**Goal:** To identify geometric boundaries (outlines) of objects, ignoring surface textures and shadows.

### The Logic
Standard edge detection looks for sharp changes in *color*. Depth edge detection looks for sharp changes in *distance*. This separates physical objects from their surroundings.

### The Math (Sobel Filter)
We measure the "gradient" (rate of change) of the depth map in both X and Y directions.

For a pixel $P$, we sample its neighbors:
$$ G_x = (Z_{topright} + 2Z_{right} + Z_{bottomright}) - (Z_{topleft} + 2Z_{left} + Z_{bottomleft}) $$
$$ G_y = (Z_{topleft} + 2Z_{top} + Z_{topright}) - (Z_{bottomleft} + 2Z_{bottom} + Z_{bottomright}) $$

**Gradient Magnitude:**
$$ G = \sqrt{G_x^2 + G_y^2} $$

*   If $G > \text{Threshold}$, it is an **Edge** (Deep drop-off).
*   If $G < \text{Threshold}$, it is **Flat Surface** (Continuous).
