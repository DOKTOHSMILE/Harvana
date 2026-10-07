const { chromium } = require('playwright'); const fs=require('fs');
(async()=>{const b=await chromium.launch();const p=await b.newPage({viewport:{width:720,height:480}});
for (const f of fs.readdirSync('.').filter(f=>f.endsWith('.svg'))){await p.goto('file://'+process.cwd()+'/'+f);await p.screenshot({path:f.replace('.svg','.png'),omitBackground:true});}
await b.close();})();
