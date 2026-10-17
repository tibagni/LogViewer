# LogViewer Documentation Website

This branch (`gh-pages`) hosts the source code and assets for the official LogViewer documentation website at [https://tibagni.github.io/LogViewer/](https://tibagni.github.io/LogViewer/).

The site is built with [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/).

---

## Local Development & Live Preview

### 1. Prerequisites
Install `mkdocs-material` (using `pipx` or within a Python virtual environment):
```bash
pipx install mkdocs-material
```

### 2. Run Local Preview Server
From the root of this branch, run:
```bash
mkdocs serve
```
Open [http://localhost:8000](http://localhost:8000) in your browser. Any edits made to files in `docs/` or `mkdocs.yml` will automatically hot-reload in real time.

### 3. Build & Validate
To test that all pages, internal links, and assets compile without errors:
```bash
mkdocs build --strict
```

---

## Directory Structure

* `mkdocs.yml`: Site configuration, theme styling, navigation tree, and Markdown extensions.
* `docs/`: Documentation Markdown source files, diagrams, custom stylesheets, and screenshots.
* `.github/workflows/docs.yml`: GitHub Actions CI/CD workflow that validates builds on Pull Requests and deploys the site to GitHub Pages on push to `gh-pages`.

---

## Making Updates & Pull Requests

1. Create a feature branch off `gh-pages`.
2. Preview your edits locally using `mkdocs serve`.
3. Open a Pull Request targeting `gh-pages`.