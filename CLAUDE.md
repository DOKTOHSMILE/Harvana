# Notes for any assistant working in this repository

- **Server access (always):** the node server is reached with `ssh -i ~/.ssh/ajc-node.key root@5.75.163.65` from Termux. Node deploy steps are in the node repository (`docs/DEPLOYMENT.md`, "How to reach the server") and are typed on the server after that login. Never write the private key or any secret into a repository or into chat.
- The build is CodeAssist from `app/module.toml`; GitHub builds from the same toml and generates every Gradle file (none are stored in the repo). Do not commit CodeAssist's generated Gradle files or `.platform` caches.
- The brand is written Harvana. The only old spelling kept is the registered domain `harvannaprotocol.com`.
