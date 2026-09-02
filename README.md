# Drawing Pro (Android Hand Drawing)

A high-performance, modern sketching application for Android, built from the ground up with **Jetpack Compose** and **Kotlin**. This project showcases a transition from legacy Android practices to the cutting edge of mobile development.

## ✨ Modernized Features

- **Material You Design**: Fully implements Material 3 with Dynamic Color support. The app's theme adapts to your system wallpaper (Android 12+).
- **Infinite Canvas Feel**: Support for **Zoom and Pan** using two-finger gestures, allowing for detailed work and easy navigation.
- **Pro Drawing Engine**: 
    - **Smoothing**: Uses Quadratic Bézier curves for butter-smooth lines.
    - **Hybrid Rendering**: Combines a persistent bitmap cache for finished strokes with a lightweight vector overlay for active drawing, ensuring zero lag even in complex sketches.
- **Tool Customization**: 
    - Adjustable brush size (1-100dp).
    - Curated professional color palette.
    - Dedicated Eraser tool with adaptive sizing.
- **Paper Styles**: Choose between **Plain**, **Grid**, and **Dots** backgrounds, which are correctly preserved in your exported images.
- **Workflow Tools**: Full **Undo/Redo** support and a quick-reset feature.
- **Media Integration**:
    - Import images from **Gallery** or **Camera** as drawing backgrounds.
    - Save high-resolution PNGs directly to the system **Pictures** folder using modern MediaStore APIs (Scoped Storage compliant).
    - Instant sharing via the standard Android share sheet.

## 🛠 Tech Stack

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose
- **Design System**: Material 3 (Material You)
- **Architecture**: Modern Android State Management (State holders & `SnapshotStateList`)
- **Build System**: Gradle Kotlin DSL (`.kts`) with Version Catalog (`libs.versions.toml`)
- **Compatibility**: Target SDK 35 (Android 15), Min SDK 24 (Android 7.0)

## 🚀 Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/swapnull/Android-Hand-Drawing.git
   ```
2. **Open in Android Studio**: Use Android Studio Ladybug or newer.
3. **Sync & Run**: Gradle will automatically download dependencies using the Version Catalog.

## 📱 Screenshots

| Modern Material You UI | Brush Settings | Grid Style Drawing |
|:---:|:---:|:---:|
| *(Screenshots Coming Soon)* | *(Screenshots Coming Soon)* | *(Screenshots Coming Soon)* |

---
*Modernized with ❤️ using AI-assisted engineering.*
