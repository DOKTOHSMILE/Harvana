# Third-party notices

## Fluent Emoji (handshake reaction icon and milestone badges)

`app/src/main/res/drawable-nodpi/ic_learn_handshake.png` is the 3D "Handshake" emoji from Microsoft Fluent Emoji (https://github.com/microsoft/fluentui-emoji), cropped to its edges, colours unchanged. The milestone badges (`res/drawable-nodpi/ms3d_*.png`) are 3D Fluent Emoji from the same repository, trimmed and resized to 192 px. Licensed under the MIT License:

```
MIT License

Copyright (c) Microsoft Corporation.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE
```

## Fluent UI System Icons (every app icon)

The app's icons (`res/drawable*/ic_*.xml` and `icon_*.xml`, marked "Harvana duotone icon" in each file) are drawn from the shapes of Microsoft Fluent UI System Icons (https://github.com/microsoft/fluentui-system-icons), regular and filled 24px versions combined into a two-tone line-over-fill style. Licensed under the MIT License:

```
MIT License

Copyright (c) 2020 Microsoft Corporation

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Nigerian bank directory and logos

`app/src/main/assets/banks/ng_banks.json` and `app/src/main/assets/banks/logos/` come from the free public
directory at https://nigerianbanks.xyz (project: supermx1/nigerian-banks-api), fetched 7 Oct 2026. The logos were
scaled down to 128 px. They are the trademarks of their banks and are shown only to identify the bank a user
chooses. The app refreshes the list from the same address at most once a week.

## International bank list and logos

`app/src/main/assets/banks/world_banks.json` lists the main banks and payment apps of 43 countries with their
public websites, compiled for Harvana on 11 Oct 2026. The logos in `app/src/main/assets/banks/world/` are each
bank's own website icon, fetched once through Google's public favicon service and scaled to 96 px. They are the
trademarks of their owners and are shown only to identify the bank a user chooses. They ship inside the app, so no
request is made to Google or the bank when the list is opened.

Account lookups use two free public services, sending only the code being checked:
- openiban.com (IBAN → bank name and BIC, for DE, AT, CH, NL, BE, LU, LI);
- ifsc.razorpay.com (Razorpay's open IFSC directory, IFSC → bank and branch, for India).
