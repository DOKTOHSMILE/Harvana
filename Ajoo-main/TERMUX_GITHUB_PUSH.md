# Push from Termux to GitHub

GitHub no longer accepts your account password for `git push`.
Create a Personal Access Token (PAT) and use that as the password.

## 1. Create the empty repo

On your phone browser open:
https://github.com/new

- Repository name: `Ajoo`
- Owner: `DOKTOHSMILE`
- Private (recommended)
- Do NOT add a README, .gitignore, or license
- Create repository

## 2. Create a PAT

https://github.com/settings/tokens

Classic token is enough. Enable `repo`. Copy it once.

## 3. Install git in Termux and unpack this zip

```bash
pkg update -y
pkg install git unzip -y
cd ~
unzip -o Ajoo-fixed.zip
cd Ajoo-main
```

## 4. First push

```bash
git init
git branch -M main
git add .
git status
git config user.name "DOKTOHSMILE"
git config user.email "YOUR_GITHUB_EMAIL@example.com"
git commit -m "Fix compile errors and GitHub Actions Android SDK setup"
git remote add origin https://github.com/DOKTOHSMILE/Ajoo.git
git push -u origin main
```

When Git asks for username: `DOKTOHSMILE`
When Git asks for password: paste the PAT, not your GitHub password.

## 5. Confirm Actions

Open https://github.com/DOKTOHSMILE/Ajoo/actions
The CI workflow should install JDK 17 + Android SDK 34 and assemble the APK.
