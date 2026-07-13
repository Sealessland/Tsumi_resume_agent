# AGENTS.md

This repository is a Vue 3 + Vite resume builder application using TDesign UI and TailwindCSS.

## Build & Development Commands

```bash
# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

**Note:** No linting or test commands are configured. The project uses manual testing only.

## Code Style Guidelines

### File Formatting
- **Indentation:** 2 spaces (enforced by .editorconfig)
- **Line endings:** LF (enforced by .editorconfig)
- **Charset:** UTF-8
- **Final newline:** Required

### JavaScript/Vue Conventions

#### Vue Components
- Use **Vue 3 Composition API** with `<script setup>` syntax
- Import Vue composition functions from `'vue'` (reactive, ref, computed, watch, etc.)
- Components named with **PascalCase** (e.g., `ResumeEditor.vue`, `App.vue`)
- Props defined with `defineProps({ ... })`
- Events defined with `defineEmits([...])`
- Use TDesign Vue Next components via named imports (e.g., `import { ColorPicker } from 'tdesign-vue-next'`)

#### JavaScript (JS files)
- Use **camelCase** for variables and functions
- Constants use **UPPER_SNAKE_CASE** (e.g., `EDUCATION_LOGO_MAX_BYTES`, `AUTO_SAVE_DELAY`)
- Export functions with `export function` or `export const`
- Use ES modules syntax (this is a `"type": "module"` project)
- Import dependencies without file extensions (e.g., `import { ref } from 'vue'`)

#### Code Organization
- **Components:** `src/components/` - organized by feature (e.g., `resume/ResumeEditor.vue`)
- **Composables:** `src/composables/` - reusable logic (e.g., `useResumeBuilder.js`)
- **Modules:** `src/modules/` - business logic organized by domain (e.g., `resume/constants.js`, `resume/factories.js`)
- **Styles:** TailwindCSS with custom component classes in `src/style.css`

### Error Handling
- Use try/catch blocks for async operations
- Log errors with `console.error()` for debugging
- Provide user-facing feedback via reactive refs (e.g., `actionErrorMessage.value = '...'`)
- Use fallback patterns (e.g., IndexedDB fallback to localStorage)

### CSS & Styling
- Primary framework: **TailwindCSS v4** with PostCSS
- Use utility classes via `@apply` for reusable component styles
- Custom component classes defined in `@layer components` in `style.css`
- Print styles in `@media print` block for PDF export
- CSS custom properties for theming: `--brand`, `--brand-deep`

### Imports
- Vue composition functions: `import { computed, reactive, ref, watch } from 'vue'`
- TDesign components: `import { ColorPicker } from 'tdesign-vue-next'`
- TDesign icons: `import { BrowseIcon } from 'tdesign-icons-vue-next'`
- Local modules: use relative paths without extensions (e.g., `import { createDemoResume } from '../modules/resume/templates'`)

### Naming Patterns
- **Variables/Functions:** `camelCase` (e.g., `photoUploadMessage`, `exportJson`)
- **Constants:** `UPPER_SNAKE_CASE` (e.g., `PHOTO_RULES`, `STORAGE_KEY`)
- **Components:** `PascalCase` (e.g., `ResumeEditor`, `ResumeToolbar`)
- **CSS Classes:** `kebab-case` with BEM-like modifiers (e.g., `panel-card`, `field-input`, `field-input:focus`)

### File Upload Handling
- Use `FileReader` API for reading files as data URLs
- Validate file type with `Set` of allowed MIME types
- Validate file size in bytes
- Reset input value after processing: `input.value = ''`

### Storage Patterns
- Primary: IndexedDB for drafts
- Fallback: localStorage for lightweight storage
- Migration logic handles legacy data format changes
- Storage keys defined in constants file

### Vue Reactivity
- Use `reactive()` for complex objects (e.g., `resume`, `panels`)
- Use `ref()` for primitive values and DOM references
- Use `computed()` for derived values
- Use `watch()` with `{ deep: true }` for object reactivity

## Project Stack
- **Framework:** Vue 3.5+ (Composition API, script setup)
- **Build Tool:** Vite 8.0+
- **UI Library:** TDesign Vue Next 1.18+
- **Styling:** TailwindCSS 4.2+
- **Drag & Drop:** vuedraggable 4.1+
- **Language:** JavaScript (ES modules, no TypeScript)
