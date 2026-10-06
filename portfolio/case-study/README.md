# Case study kit

Everything you need to put BuildFlow in a portfolio: the story, short copy to paste, a designed page, and a tool that takes the screenshots.

## What is here

| File | What it is |
| --- | --- |
| `CASE_STUDY.md` | The full write-up. Copy it into your portfolio, or use it as the outline for your own page. |
| `SHORT_COPY.md` | One line, a two-sentence blurb, resume bullets, LinkedIn text, GitHub about text, alt text and captions for every screenshot, and likely interview questions. |
| `index.html` | The same case study as a finished page. Open it in a browser, or host it as it is. |
| `capture.sh` | Takes the screenshots from the running demo. It only reads: it signs in with the demo accounts and looks at pages, it never sends a form. |
| `tools/capture.mjs` | The script that `capture.sh` runs. |
| `screenshots/` | Created by `capture.sh`. The page and the write-up point at these files. |

## Take the screenshots

1. Start the project with the demo turned on (`BUILDFLOW_DEMO_ENABLED=true`) so the API answers on http://localhost:8080.
2. From the repo root, run:

   ```
   bash portfolio/case-study/capture.sh
   ```

   If nothing is running on http://localhost:3000 it builds the frontend, serves it for the run and stops it afterwards. The first run also downloads Playwright into a temporary folder, not into your project.
3. Open `portfolio/case-study/screenshots/_contact-sheet.jpg` to see everything at once. If one looks wrong, run the script again; it overwrites the files.
4. Open `portfolio/case-study/index.html` in your browser. A figure whose image is missing hides itself, so the page always looks finished.

The shots are taken at 1440 by 900 (desktop) and 390 by 844 (phone), at 1.5x and 2x pixel density, with animations switched off so nothing is caught half-way.

## Before you publish

- Fill in the brackets: `[live demo URL]`, `[how long it took]` and the "What I learned" section of `CASE_STUDY.md`. For the page, set `DEMO_URL` in the script at the bottom of `index.html`; until you do, the demo links stay hidden.
- Read every sentence and ask whether it is true and whether you can show it in the code. Remove or reword what is not. The page says "solo project, backend to frontend"; change that if it does not describe how you worked, and be ready to say plainly which tools helped you.
- Check the numbers against the repo:
  - Flyway migrations: `ls backend/src/main/resources/db/migration | wc -l` (the text says 13)
  - Backend tests: run them and read the "Tests run" total (the text says more than 100)
- Look at the screenshots for anything you would not want public: real names, emails, keys. The demo data is made up, but check anyway.
- The page uses fonts that ship with macOS and Windows (Charter, Avenir Next, Segoe UI). It needs no internet and no build step.
