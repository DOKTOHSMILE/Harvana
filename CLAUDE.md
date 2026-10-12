# Notes for any assistant working in this repository

- **Server access (always):** the node server is reached with `ssh -i ~/.ssh/ajc-node.key root@5.75.163.65` from Termux. Node deploy steps are in the node repository (`docs/DEPLOYMENT.md`, "How to reach the server") and are typed on the server after that login. Never write the private key or any secret into a repository or into chat.
- The build is CodeAssist from `app/module.toml`. The Gradle files (`settings.gradle`, `build.gradle`, `gradle.properties`, `gradle/wrapper/`, `app/build.gradle`) are stored so CodeAssist can import the project as a Gradle module (owner, 12 Oct 2026). `app/build.gradle` is generated: edit the toml, run `python3 tools/module_toml_to_gradle.py`, commit both. Do not commit `.platform` caches.
- The brand is written Harvana. The only old spelling kept is the registered domain `harvannaprotocol.com`.
