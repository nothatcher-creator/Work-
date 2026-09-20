# BlendWeb

BlendWeb is a Blender-inspired 3D editor that runs in a modern browser using Three.js.

This build lives on the `blendweb-web` branch so it stays separate from the other projects in this repository.

## Features

- WebGL viewport with orbit, pan and zoom
- Move, rotate and scale gizmos
- Basic mesh Edit Mode with movable vertices
- Scene outliner and transform/material properties
- Mesh primitives, lights, cameras and empties
- Solid, material and wireframe views
- Snapping, duplicate, delete, undo and redo
- Browser autosave and `.blendweb` project files
- GLB/GLTF, OBJ and STL import
- GLB, OBJ and STL export
- PNG viewport screenshots
- Responsive mobile portrait controls
- PWA service worker

## GitHub Pages

A Pages workflow is included at `.github/workflows/pages.yml`. It deliberately publishes only the BlendWeb site files, not the other project files inherited from `main`.

In **Settings → Pages**, set **Source** to **GitHub Actions**. Then run the workflow or push another change to `blendweb-web`.

Because this repository is private, GitHub Pages availability depends on the GitHub plan for the repository.

## Local run

```bash
python3 -m http.server 8080
```

Open `http://localhost:8080`.
