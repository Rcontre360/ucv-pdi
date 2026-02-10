# Visor y Procesador de Imágenes

**Nota sobre los comentarios:** Los comentarios dentro del código del proyecto están en inglés para mantener la consistencia con el lenguaje de programación y las convenciones de desarrollo.

## Estructura del Proyecto

El proyecto está organizado en módulos distintos, promoviendo una clara separación de responsabilidades y facilidad de mantenimiento:

*   **`org.pdi.core`**: Este módulo contiene los bloques de construcción fundamentales y la lógica para el procesamiento de imágenes.
    *   `Image.kt`: Representa una imagen inmutable y proporciona métodos para manipulaciones de imágenes como conversión a escala de grises, efectos negativos, ajustes de brillo y contraste, umbralización, rotación, zoom y aplicación de kernels para convolución y detección de bordes. También maneja los cálculos de histogramas y curvas tonales.
    *   `AppState.kt`:Gestor de estado central para toda la aplicación. Contiene el estado actual de la imagen, procesa las acciones de `UpdateType` y notifica a los oyentes registrados (componentes de la interfaz de usuario) sobre los cambios de estado. Este control centralizado asegura la consistencia y simplifica el manejo de la app.
    *   `UpdateType.kt`: Una clase sellada que define todas las posibles acciones o eventos que pueden modificar el estado de la aplicación. 
    *   `Kernel.kt` y `core/kernels/*`: Define una clase abstracta `Kernel` y sus implementaciones concretas (por ejemplo, `AverageKernel`, `MedianKernel`, `GaussianKernel`, `LaplacianKernel`, `LaplacianKernelProfiling`, `Sobel`, `Roberts`, `Prewitt`).
    *   `Histogram.kt`: Gestiona el cálculo y la manipulación de los histogramas de imagen.
    *   `Utils.kt`: Contiene funciones de utilidad generales, como el cálculo de la `luminosidad`, utilizadas en todo el módulo `core`.

*   **`org.pdi.ui` y `org.pdi.ui.panels`**: Este módulo es responsable de la interfaz de usuario de la aplicación, construida con JavaFX y FXML.
    *   `Main.kt`: El punto de entrada de la aplicación, responsable de inicializar la etapa de JavaFX y cargar el diseño FXML principal.
    *   `MainController.kt`: El controlador principal de la interfaz de usuario que orquesta la interacción entre la ventana principal de la aplicación y los diversos controladores de subpaneles. También configura un oyente para los cambios de `AppState` para actualizar la visualización de la imagen principal.
    *   `TopPanelController.kt`, `LeftPanelController.kt`, `RightPanelController.kt`, `BottomPanelController.kt`: Estos controladores gestionan las secciones principales de la interfaz de la aplicación, cada una manejando conjuntos específicos de funcionalidades e interactuando con `AppState`.
    *   `ui/panels/*Controller.kt`: Una colección de controladores para paneles emergentes, como `HistogramPanelController`, `InfoPanelController`, `KernelMatrixPanelController`, `LineProfilePanelController`, `SaveImagePanelController`, `TonalCurvePanelController` y `UmbralizationPanelController`. 

*   **`org.pdi.io`**: Este módulo maneja todas las operaciones de entrada/salida de archivos.
    *   `FileSystem.kt`: Proporciona funcionalidad para cargar y guardar imágenes. Admite formatos de imagen estándar (como PNG, BMP), ademas el formato `.pdi` que incluye compresión y un checksum de verificación para la integridad. Tambien tenemos `.netpbm` (PBM, PGM, PPM).

## Guía de Usuario

Esta sección explica cómo utilizar las diversas características del Visor y Procesador de Imágenes, organizadas por sus respectivos paneles.

### Panel Superior

El Panel Superior proporciona acciones globales relacionadas con la gestión de archivos y el acceso a herramientas de análisis especializadas.

*   **Seleccionar Imagen**: Haga clic en este botón para abrir un cuadro de diálogo de archivo y cargar una imagen desde su sistema de archivos local en la aplicación.
*   **Guardar Imagen**: Esto abre una nueva ventana que le permite guardar la imagen actualmente mostrada. Puede especificar el nombre del archivo, elegir el formato de salida (PNG, BMP, Netpbm o PDI) y seleccionar un directorio para guardar la imagen. El formato PDI personalizado incluye compresión, esta es mas evidente en imagenes con pocos colores (binarias).
*   **Mostrar Histograma**: Muestra una ventana emergente con el histograma de la imagen actual. 
*   **Mostrar Curva Tonal**: Abre una ventana que visualiza la curva tonal, que muestra cómo los valores de intensidad de píxeles de entrada se mapean a los valores de salida. 
*   **Mostrar Umbralización**: Esta función le permite aplicar umbralización binaria a la imagen.
    *   **Importante**: La imagen **debe estar en escala de grises** antes de aplicar la umbralización.
    *   En el panel de umbralización, puede agregar nuevos umbrales con un boton o haciendo **doble clic** en la barra de degradado.
    *   Los umbrales existentes se pueden **arrastrar** para ajustar sus valores.
    *   Para eliminar un umbral, haga **clic derecho** sobre él.
*   **Mostrar Perfil de Línea**: Esta herramienta le permite analizar la intensidad de los píxeles a lo largo de una línea específica en la imagen.
    *   **Uso**: Haga clic directamente en la imagen en la vista principal para seleccionar una línea. Puede elegir entre perfiles del eje X (horizontal) o del eje Y (vertical) y seleccionar el canal (Rojo, Verde, Azul o Gris) para analizar.
*   **Limpiar Imagen**: Restablece la imagen a su estado original cargado, deshaciendo todas las modificaciones aplicadas.

### Panel Izquierdo

El Panel Izquierdo se centra en la información básica de la imagen y los ajustes fundamentales.

*   **Información de la Imagen**: Muestra metadatos esenciales sobre la imagen actualmente cargada, incluyendo su ancho, alto, bits por píxel (BPP), número de colores únicos y formato. Tenga en cuenta que el ancho y el alto reflejan las dimensiones actuales de la imagen, estas pueden cambiar al aplicar el zoom...
*   **Brillo**: Utilice el deslizador para ajustar el brillo de la imagen. El ajuste se aplica como un factor (entre 0 y 2).
*   **Contraste**: Ajuste el contraste de la imagen utilizando el deslizador proporcionado. Esta operación se implementa estirando el histograma de la imagen.
*   **Escala de Grises**: Convierte la imagen a una representación en escala de grises. Hay un selector de color disponible para aplicar un "tinte" a la imagen en escala de grises; seleccionar blanco resultará en una imagen estándar en escala de grises. Para colorear seleccione un color y luego el boton de escala de grises/coloreado
*   **Negativo**: Negativo..

### Panel Derecho

El Panel Derecho está dedicado a las operaciones avanzadas de filtrado de imágenes y detección de bordes.

**Nota Importante:** Al aplicar filtros (convolución) u operaciones de detección de bordes, los ajustes de brillo, contraste, rotación, zoom y el estado negativo de la imagen se restablecerán a sus valores predeterminados.

*   **Filtros (Convolución)**:
    *   **Nota sobre el tamaño del Kernel**: No se han impuesto límites explícitos (7x7) al tamaño de los kernels para ofrecer mayor flexibilidad. Esto debido a que queria observar el laplaciano y sobel de 13x13, etc.
    *   **Selección de Tipo de Kernel**: Elija entre una variedad de kernels predefinidos, incluyendo `Custom`, `Average`, `Median`, `Gaussian`, `Laplacian`, `Laplacian Profiling`, `Sobel X/Y`, `Roberts X/Y` y `Prewitt X/Y`.
    *   **Filas/Columnas**: La capacidad de ajustar las dimensiones (filas y columnas) del kernel varía según el tipo:
        *   **Personalizables (Filas y Columnas)**: `Custom`, `Average`, `Median`, `Gaussian`. Para estos, puede especificar libremente el número de filas y columnas.
        *   **Personalizables (Solo Filas/Tamaño)**: `Laplacian`, `Sobel X/Y`. Para estos kernels, solo se puede ajustar el número de filas (que define el tamaño del kernel, ya que son cuadrados o tienen una relación fija).
        *   **Estáticos (No modificables)**: `Laplacian Profiling`, `Roberts X/Y`, `Prewitt X/Y`. Estos kernels tienen dimensiones fijas y no se pueden modificar.
    *   **Mostrar Kernel**: Abre una ventana emergente que muestra la matriz del kernel actualmente seleccionado. Para kernels personalizables, puede editar manualmente los valores en esta matriz.
    *   **Aplicar Filtro**: Aplica el kernel seleccionado a la imagen utilizando la convolución.
*   **Operaciones (Detección de Bordes)**:
    *   **Selección de Tipo de Operación**: Elija entre los operadores `Sobel`, `Roberts` o `Prewitt` para la detección de bordes.
    *   **Aplicar Operación**: Ejecuta el algoritmo de detección de bordes elegido en la imagen.

### Panel Inferior

El Panel Inferior maneja las transformaciones geométricas y los controles de zoom.

*   **Nivel de Zoom**: Muestra el factor de zoom actual aplicado a la imagen (por ejemplo, "x1.0" para 100%).
*   **Acercar / Alejar**: Botones para aumentar o disminuir incrementalmente el nivel de zoom de la imagen.
*   **Algoritmo de Zoom**: Seleccione el algoritmo utilizado para el zoom: `Vecino Más Cercano` (resultados más simples y pixelados) o `Interpolación Lineal` (resultados más suaves).
*   **Rotar 90° / Rotar -90°**: Rota la imagen en sentido horario o antihorario 90 grados, respectivamente.

---

## Funcionalidades Implementadas y su Ubicación

A continuación, se detalla la ubicación de cada funcionalidad implementada en la aplicación:

1.  **Conversión a escala de grises y a escala de algún color escogido por el usuario.**
    *   **Ubicación**: Panel Izquierdo -> "Escala de Grises"
2.  **Mostrar información sobre la imagen: dimensiones, bits por pixel y número de colores únicos.**
    *   **Ubicación**: Panel Izquierdo -> "Información de la Imagen"
3.  **Negativo de la imagen.**
    *   **Ubicación**: Panel Izquierdo -> "Negativo"
4.  **Despliegue de la curva tonal**
    *   **Ubicación**: Panel Superior -> "Mostrar Curva Tonal"
5.  **Cálculo y despliegue del histograma de la imagen.**
    *   **Ubicación**: Panel Superior -> "Mostrar Histograma"
6.  **Modificación del brillo y contraste.**
    *   **Ubicación**: Panel Izquierdo -> "Brillo" y "Contraste"
7.  **Umbralización de la imagen (sencilla y múltiple).**
    *   **Ubicación**: Panel Superior -> "Mostrar Umbralización"
8.  **Acercamiento y alejamiento (Zoom) con opción para aplicar Vecino Más Próximo e Interpolación Bilineal.**
    *   **Ubicación**: Panel Inferior -> "Nivel de Zoom", "Acercar / Alejar", "Algoritmo de Zoom"
9.  **Rotación en múltiplos de ángulo recto hacia cualquier dirección (incluyendo rotaciones en espejo verticales y horizontales).**
    *   **Ubicación**: Panel Inferior -> "Rotar 90° / Rotar -90°"
10. **Filtro del promedio, mediana y Laplaciano del Gaussiano.**
    *   **Ubicación**: Panel Derecho -> "Filtros (Convolución)" -> "Selección de Tipo de Kernel" (Opciones: `Average`, `Median`, `Gaussian`, `Laplacian`)
11. **Operadores de bordes Sobel, Roberts y Prewitt.**
    *   **Ubicación**: Panel Derecho -> "Operaciones (Detección de Bordes)" -> "Selección de Tipo de Operación" (Opciones: `Sobel`, `Roberts`, `Prewitt`)
12. **Cálculo del gradiente.**
    *   **Ubicación**: Panel Derecho -> "Operaciones (Detección de Bordes)" (Implícito al aplicar operadores como Sobel, Roberts o Prewitt)
13. **Operador de perfilado.**
    *   **Ubicación**: Panel Derecho -> "Filtros (Convolución)" -> "Selección de Tipo de Kernel" (Opción: `Laplacian Profiling`)
14. **Se debe poder especificar en la interfaz el tamaño del kernel a utilizar para cada uno de los filtros. Cualquier combinación de filas y/o columnas es válida partiendo desde un mínimo de 2x1 (vertical) ó 1x2 (horizontal) hasta un máximo de 7x7.**
    *   **Aclaración**: La flexibilidad para especificar tamaños de kernel arbitrarios (hasta 7x7 o más, si el kernel lo permite) se aplica principalmente a los filtros de convolución del punto 10. Tambien se aplico a varios otros kernels pero no todos.
15. **Mostrar la curva de perfil de una línea de la imagen.**
    *   **Ubicación**: Panel Superior -> "Mostrar Perfil de Línea"
16. **Salvar la imagen modificada con el menor desperdicio posible de información por pixel tanto para los formatos Bmp/Png como para el formato Netpbm versión ASCII (PPM para color, PGM para escala de grises y PBM para binario). Por ejemplo, si una imagen a color es convertida a escala de grises, sólo se requerirán 8 bits por píxel para el almacenamiento.**
    *   **Ubicación**: Panel Superior -> "Guardar Imagen"
17. **Compresión con RLE de las imágenes en formato Netpbm. El archivo resultante debe tener extensión .rle y debe poder ser cargado de vuelta por su propia aplicación.**
    *   **Ubicación**: Panel Superior -> "Guardar Imagen" (Formato: `PDI`). 
18. **Aplicar un kernel de tamaño y valores arbitrarios a la imagen (considerando que el objetivo sea suavizado, perfilado o detección de bordes). Debe proveer una forma simple y práctica de asignar los valores a cada posición del kernel.**
    *   **Ubicación**: Panel Derecho -> "Filtros (Convolución)" -> "Selección de Tipo de Kernel" (Opción: `Custom`), y "Mostrar Kernel" (para editar los valores de la matriz).




