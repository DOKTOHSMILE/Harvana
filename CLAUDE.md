# Notes for any assistant working in this repository

- **Server access (always):** Node deploy steps are documented securely in the node repository (`docs/DEPLOYMENT.md`, "How to reach the server"). Production SSH keys and node IP addresses must never be committed to repositories or chat.
- The build is CodeAssist from `app/module.toml`. The Gradle files (`settings.gradle`, `build.gradle`, `gradle.properties`, `gradle/wrapper/`, `app/build.gradle`) are stored so CodeAssist can import the project as a Gradle module (owner, 12 Oct 2026). `app/build.gradle` is generated: edit the toml, run `python3 tools/module_toml_to_gradle.py`, commit both. Do not commit `.platform` caches.
- The brand is written Harvana. The only old spelling kept is the registered domain `harvannaprotocol.com`.
- **No explanations in the app (owner, 12 Oct 2026):** build exactly what is asked and show values only — no sentences telling users how a feature works, who pays a fee, or why. Figures go in tables, sections, rows and columns (`utils/InfoTable`). Add explanatory text only when the owner asks for it.
- **Gas is called tFare (owner, 12 Oct 2026)** in everything a person sees on Harvana. Other chains keep their own fee names. Code and JSON keep `gas` and `transportFare*`.
