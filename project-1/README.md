# Visor y Procesador de Imágenes

Este proyecto es una aplicación JavaFX desarrollada en Kotlin para la visualización y procesamiento de imágenes. Proporciona una interfaz fácil de usar para realizar diversas manipulaciones de imágenes, desde ajustes básicos como brillo y contraste hasta operaciones avanzadas como la convolución con kernels personalizados y la detección de bordes.

**Nota sobre los comentarios:** Los comentarios dentro del código del proyecto están en inglés para mantener la consistencia con el lenguaje de programación y las convenciones de desarrollo.

## Estructura del Proyecto

El proyecto está organizado en módulos distintos, promoviendo una clara separación de responsabilidades y facilidad de mantenimiento:

*   **`org.pdi.core`**: Este módulo contiene los bloques de construcción fundamentales y la lógica para el procesamiento de imágenes.
    *   `Image.kt`: Representa una imagen inmutable y proporciona un rico conjunto de métodos para manipulaciones de imágenes como conversión a escala de grises, efectos negativos, ajustes de brillo y contraste, umbralización, rotación, zoom y aplicación de varios kernels para convolución y detección de bordes. También maneja los cálculos de histogramas y curvas tonales.
    *   `AppState.kt`: Actúa como el gestor de estado central para toda la aplicación. Contiene el `StateContext` (el estado actual de la imagen), procesa las acciones de `UpdateType` y notifica a los oyentes registrados (típicamente componentes de la interfaz de usuario) sobre los cambios de estado. Este control centralizado asegura la consistencia y simplifica los flujos de trabajo complejos de modificación de imágenes.
    *   `StateContext.kt`: Una clase de datos que encapsula el estado transitorio de la imagen actual, incluyendo brillo, contraste, rotación, nivel de zoom y estado negativo. Estos son los parámetros que se pueden ajustar sin alterar inmediatamente los datos de la imagen base.
    *   `UpdateType.kt`: Una clase sellada que define todas las posibles acciones o eventos que pueden modificar el estado de la aplicación. Este enfoque estructurado asegura que todos los cambios de estado sean explícitos y se manejen de manera predecible.
    *   `Kernel.kt` y `core/kernels/*`: Define una clase abstracta `Kernel` y sus implementaciones concretas (por ejemplo, `AverageKernel`, `MedianKernel`, `GaussianKernel`, `LaplacianKernel`, `LaplacianKernelProfiling`, `Sobel`, `Roberts`, `Prewitt`). Estos se utilizan para varios algoritmos de filtrado de imágenes y detección de bordes.
    *   `Histogram.kt`: Gestiona el cálculo y la manipulación (por ejemplo, estiramiento) de los histogramas de imagen.
    *   `Utils.kt`: Contiene funciones de utilidad generales, como el cálculo de la `luminosidad`, utilizadas en todo el módulo `core`.

*   **`org.pdi.ui` y `org.pdi.ui.panels`**: Este módulo es responsable de la interfaz de usuario de la aplicación, construida con JavaFX y FXML.
    *   `Main.kt`: El punto de entrada de la aplicación, responsable de inicializar la etapa de JavaFX y cargar el diseño FXML principal.
    *   `MainController.kt`: El controlador principal de la interfaz de usuario que orquesta la interacción entre la ventana principal de la aplicación y los diversos controladores de subpaneles. También configura un oyente para los cambios de `AppState` para actualizar la visualización de la imagen principal.
    *   `TopPanelController.kt`, `LeftPanelController.kt`, `RightPanelController.kt`, `BottomPanelController.kt`: Estos controladores gestionan las secciones principales de la interfaz de la aplicación, cada una manejando conjuntos específicos de funcionalidades e interactuando con `AppState` para activar operaciones de procesamiento de imágenes.
    *   `ui/panels/*Controller.kt`: Una colección de controladores para paneles emergentes especializados, como `HistogramPanelController`, `InfoPanelController`, `KernelMatrixPanelController`, `LineProfilePanelController`, `SaveImagePanelController`, `TonalCurvePanelController` y `UmbralizationPanelController`. Estos paneles proporcionan vistas o controles detallados para tareas específicas de procesamiento de imágenes.
    *   **Separación de Responsabilidades**: Un principio arquitectónico clave aquí es que los controladores de la interfaz de usuario no manipulan directamente los datos de la imagen. En su lugar, interactúan con `AppState` para solicitar cambios. `AppState` luego aplica estos cambios a los objetos `Image` y notifica a la interfaz de usuario para que actualice su visualización, asegurando una clara separación entre la presentación y la lógica de negocio.

*   **`org.pdi.io`**: Este módulo maneja todas las operaciones de entrada/salida de archivos.
    *   `FileSystem.kt`: Proporciona funcionalidad para cargar y guardar imágenes. Admite formatos de imagen estándar (como PNG, BMP), así como formatos personalizados como `.pdi` (que incluye compresión y una suma de verificación para la integridad) y `.netpbm` (PBM, PGM, PPM).

## Guía de Usuario

Esta sección explica cómo utilizar las diversas características del Visor y Procesador de Imágenes, organizadas por sus respectivos paneles.

### Panel Superior

El Panel Superior proporciona acciones globales relacionadas con la gestión de archivos y el acceso a herramientas de análisis especializadas.

*   **Seleccionar Imagen**: Haga clic en este botón para abrir un cuadro de diálogo de archivo y cargar una imagen desde su sistema de archivos local en la aplicación.
*   **Guardar Imagen**: Esto abre una nueva ventana que le permite guardar la imagen actualmente mostrada. Puede especificar el nombre del archivo, elegir el formato de salida (PNG, BMP, Netpbm o PDI) y seleccionar un directorio para guardar la imagen. El formato PDI personalizado incluye compresión y una suma de verificación para la integridad de los datos.
*   **Mostrar Histograma**: Muestra una ventana emergente con el histograma de la imagen actual. Puede seleccionar ver el histograma para los canales Rojo, Verde, Azul o Gris.
*   **Mostrar Curva Tonal**: Abre una ventana que visualiza la curva tonal, que muestra cómo los valores de intensidad de píxeles de entrada se mapean a los valores de salida. Puede ver las curvas para los canales Rojo, Verde, Azul o Luminosidad.
*   **Mostrar Umbralización**: Esta función le permite aplicar umbralización binaria a la imagen.
    *   **Importante**: La imagen **debe estar en escala de grises** antes de aplicar la umbralización. Si no lo está, una alerta le pedirá que aplique primero un filtro de escala de grises.
    *   En el panel de umbralización, puede agregar nuevos umbrales de forma interactiva haciendo **doble clic** en la barra de degradado.
    *   Los umbrales existentes se pueden **arrastrar** para ajustar sus valores.
    *   Para eliminar un umbral, haga **clic derecho** sobre él.
*   **Mostrar Perfil de Línea**: Esta herramienta le permite analizar la intensidad de los píxeles a lo largo de una línea específica en la imagen.
    *   **Uso**: Haga clic directamente en la imagen en la vista principal para seleccionar una línea. Puede elegir entre perfiles del eje X (horizontal) o del eje Y (vertical) y seleccionar el canal (Rojo, Verde, Azul o Gris) para analizar.
*   **Limpiar Imagen**: Restablece la imagen a su estado original cargado, deshaciendo todas las modificaciones aplicadas.

### Panel Izquierdo

El Panel Izquierdo se centra en la información básica de la imagen y los ajustes fundamentales.

*   **Información de la Imagen**: Muestra metadatos esenciales sobre la imagen actualmente cargada, incluyendo su ancho, alto, bits por píxel (BPP), número de colores únicos y formato.
*   **Brillo**: Utilice el deslizador para ajustar el brillo de la imagen. El ajuste se aplica como un factor (entre 0 y 2), permitiendo cambios sutiles o significativos.
*   **Contraste**: Ajuste el contraste de la imagen utilizando el deslizador proporcionado. Esta operación se implementa estirando el histograma de la imagen.
*   **Escala de Grises**: Convierte la imagen a una representación en escala de grises. Hay un selector de color disponible para aplicar un "tinte" a la imagen en escala de grises; seleccionar blanco resultará en una imagen estándar en escala de grises.
*   **Negativo**: Aplica un efecto de negativo fotográfico a la imagen, invirtiendo sus colores.

### Panel Derecho

El Panel Derecho está dedicado a las operaciones avanzadas de filtrado de imágenes y detección de bordes.

*   **Filtros (Convolución)**:
    *   **Selección de Tipo de Kernel**: Elija entre una variedad de kernels predefinidos, incluyendo `Custom`, `Average`, `Median`, `Gaussian`, `Laplacian`, `Laplacian Profiling`, `Sobel X/Y`, `Roberts X/Y` y `Prewitt X/Y`.
    *   **Filas/Columnas**: Para kernels personalizables (como `Custom`, `Average`, `Median`, `Gaussian`, `Laplacian`), puede ajustar las dimensiones (filas y columnas) del kernel. Algunos kernels tienen dimensiones fijas y no se pueden editar.
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
