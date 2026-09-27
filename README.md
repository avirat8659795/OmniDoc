# OmniDoc Viewer - Offline Universal Document Opener for Android

A native Android mobile application built with **Kotlin** and **Jetpack Compose** designed to open, render, and manage all major document formats **100% offline**, blazingly fast, and with zero external cloud dependencies.

---

## 🚀 Key Features

1. **100% Offline & Private**: All document parsing and creation is handled on-device with zero network requests or third-party telemetry.
2. **Offline PDF Creator & Converter ("Make PDF")**:
   - 📸 **Pictures/Photos to PDF**: Select single or multiple images (JPG, PNG, WEBP), rearrange pages, choose orientation (Portrait, Landscape, Auto-fit), customize margins, and compile into a multi-page PDF document offline.
   - ✍️ **Text/Notes to PDF**: Format and paginate raw text or notes into a clean, paginated PDF document.
   - ⚡ **Instant Preview & Sharing**: Created PDFs immediately open in the built-in PDF viewer with one-tap system sharing.
3. **Comprehensive Document Format Support**:
   - 📕 **PDF (.pdf)**: Rendered via native `android.graphics.pdf.PdfRenderer` with pinch-to-zoom, page jump slider, page count indicator, and dark mode reading filter.
   - 📘 **Word (.docx, .doc)**: OpenXML XML pull parsing with styles (bold, italic, underline), headings, bullet lists, and embedded data tables.
   - 📗 **Excel & CSV (.xlsx, .xls, .csv)**: Shared string decoding, dynamic 2D scrollable data grid, row/column sticky headers (A-Z, 1-N), and multi-sheet tab switching.
   - 📙 **PowerPoint (.pptx, .ppt)**: OpenXML slide extractor with fullscreen swipeable presentation carousel, bullet cards, and slide counter.
   - 📓 **Plain Text, Markdown & Code (.txt, .md, .json, .xml, .py, .java, .kt, etc.)**: High-speed streaming reader with line numbers, monospace toggle, and one-tap clipboard copying.
   - 📚 **eBooks (.epub)**: ZIP container & OPF parsing, chapter navigation drawer, pagination, and font scaling.
   - 📄 **Rich Text (.rtf)**: Native RTF tokenizer & formatter.
3. **Deep System Integration ("Open With")**:
   - Configured `<intent-filter>` declarations in `AndroidManifest.xml` for `ACTION_VIEW` and `ACTION_SEND`.
   - Opens documents shared directly from **WhatsApp, Telegram, Gmail, Chrome Downloads, and File Manager**.
4. **Smart Offline Document Dashboard**:
   - Real-time device storage scanning with `MediaStore` and direct folder querying.
   - Segmented storage visualizer showing document distribution.
   - Categorized filter pills (All, PDF, Word, Excel, PPT, Text, eBooks).
   - Instant search by document name or extension.
   - Persistent **Recent Documents** and **Bookmarks/Favorites** history.

---

## 🏗️ Architecture & Engines

```
com.docopener.universal/
├── domain/
│   └── DocumentModels.kt          # Domain models for all document types and parsed structures
├── engine/
│   ├── DocTypeDetector.kt         # MIME type, extension & magic-byte sniffer
│   ├── PdfEngine.kt               # Memory-safe native PdfRenderer wrapper
│   ├── DocxEngine.kt              # Lightweight OpenXML parser for Word documents
│   ├── XlsxEngine.kt              # OpenXML & CSV spreadsheet parser
│   ├── PptxEngine.kt              # OpenXML presentation slide parser
│   ├── TextEngine.kt              # Fast buffered text/code stream reader
│   ├── EpubEngine.kt              # EPUB container and chapter extractor
│   └── RtfEngine.kt               # RTF format stripper and reader
├── data/
│   ├── StorageScanner.kt          # MediaStore & directory document scanner
│   └── RecentDocsRepository.kt    # SharedPreferences & DataStore recent/bookmark storage
├── ui/
│   ├── theme/                     # Material 3 colors, dark/light theme, typography
│   ├── components/                # DocCard, DocBadge, CategoryPills, StorageSummaryBar
│   └── screens/                   # HomeScreen, PdfViewer, DocxViewer, ExcelViewer,
│                                  # PptxViewer, TextViewer, EpubViewer, UniversalViewerContainer
└── MainActivity.kt                # Intent router, runtime permissions, and app lifecycle
```

---

## 📱 Building & Running

### Prerequisites
- Android Studio Iguana / Jellyfish or newer
- Android SDK 34 (Android 14)
- JDK 17

### Gradle Build
To build debug APK:
```bash
./gradlew assembleDebug
```

### Install to connected device or emulator:
```bash
./gradlew installDebug
```
