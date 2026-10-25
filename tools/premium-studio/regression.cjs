const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const fs=require('node:fs');const path=require('node:path');
const dir=__dirname, file=process.env.STUDIO_HTML||path.join(dir,'NegahJavan_Premium_Studio_3.html');
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.STUDIO_CHROMIUM?{executablePath:process.env.STUDIO_CHROMIUM}:{})});
 const browserVersion=browser.version();
 const context=await browser.newContext({viewport:{width:393,height:852},deviceScaleFactor:1});
 const page=await context.newPage();const errors=[],requests=[];let downloads=0;
 page.on('pageerror',e=>errors.push(e.message));page.on('console',m=>{if(m.type()==='error')errors.push(m.text())});
 page.on('request',r=>{if(/^https?:/.test(r.url()))requests.push(r.url())});
 await page.route('**/*',r=>/^https?:/.test(r.request().url())?r.abort():r.continue());
 // Generate deterministic photo/logo fixtures locally; no network assets are needed.
 const fixtures=await page.evaluate(()=>{
  const c=document.createElement('canvas');c.width=3000;c.height=1800;const g=c.getContext('2d');g.fillStyle='#195d60';g.fillRect(0,0,3000,1800);
  for(let x=0;x<3000;x+=150){g.fillStyle=`rgb(${35+x/25},85,110)`;g.fillRect(x,0,75,1800);}
  g.fillStyle='#c9a258';g.beginPath();g.ellipse(1450,790,650,650,0,0,Math.PI*2);g.fill();g.fillStyle='#263c2e';g.fillRect(0,1450,3000,350);
  const photo=c.toDataURL('image/jpeg',.9).split(',')[1];c.width=400;c.height=300;g.fillStyle='#fff';g.beginPath();g.ellipse(185,150,145,140,0,0,Math.PI*2);g.fill();g.fillStyle='#d4af63';g.fillRect(130,70,115,170);
  return {photo,logo:c.toDataURL('image/png').split(',')[1]};
 });
 fs.writeFileSync(path.join(dir,'fixture-photo.jpg'),Buffer.from(fixtures.photo,'base64'));fs.writeFileSync(path.join(dir,'fixture-logo.png'),Buffer.from(fixtures.logo,'base64'));
 await page.goto('file://'+file);await page.waitForFunction(()=>!document.querySelector('#smartPrepare').disabled);
 assert.equal(await page.evaluate(()=>document.fonts.check('850 66px Vazirmatn','خبر فارسی')),true);
 assert.equal(await page.locator('.signature .handle').evaluate(e=>getComputedStyle(e).direction),'ltr');
 const draftTitle='افتتاح مسیر تازه برای توسعهٔ گردشگری جنوب کرمان';
 const draftLead='این یک متن نمونه برای آزمایش چیدمان کارت خبری است.';
 await page.locator('#headlineInput').fill(draftTitle);await page.locator('#leadInput').fill(draftLead);
 const measure=()=>page.evaluate(()=>{
   const box=s=>document.querySelector(s).getBoundingClientRect();
   const b=box('.badge'),h=box('.card-header'),l=box('#lead'),f=box('.card-footer');
   return {fits:document.querySelector('#news-card').dataset.fits,headerGap:b.top-h.bottom,footerGap:f.top-l.bottom,dir:getComputedStyle(document.querySelector('#news-card')).direction,font:parseFloat(getComputedStyle(document.querySelector('#headline')).fontSize)};
 });
 const cases=[];
 async function exportImage(label,expectedH){
   await page.waitForFunction(()=>!document.querySelector('#exportBtn').disabled);
   const [download]=await Promise.all([page.waitForEvent('download'),page.locator('#exportBtn').click()]);
   await page.waitForFunction(()=>!document.querySelector('#exportBtn').disabled);
   const output=path.join(dir,'out-'+label+'.png');await download.saveAs(output);
   const png=fs.readFileSync(output);assert.equal(png.readUInt32BE(16),1080);assert.equal(png.readUInt32BE(20),expectedH);
   assert.ok(png.length>10000);downloads++;return output;
 }
 for(const hasPhoto of [false,true]){
   if(hasPhoto){await page.locator('#photoInput').setInputFiles(path.join(dir,'fixture-photo.jpg'));await page.waitForFunction(()=>document.querySelector('#news-card').classList.contains('has-photo'));}
   for(const layout of ['smart','editorial','breaking','glass'])for(const format of ['story','post','square']){
     await page.locator('[data-layout='+layout+']').click();await page.locator('[data-format='+format+']').click();
     await page.waitForFunction(()=>!document.querySelector('#exportBtn').disabled);
     const m=await measure();if(layout==='smart'&&hasPhoto){const gap=await page.evaluate(()=>document.querySelector('.badge').getBoundingClientRect().top-document.querySelector('#cardPhoto').getBoundingClientRect().bottom);assert.ok(gap>10,'photo must not overlap text');}assert.equal(m.fits,'true');assert.equal(m.dir,'rtl');assert.ok(m.headerGap>=-1);assert.ok(m.footerGap>=-1);
     const label=`${hasPhoto?'photo':'type'}-${layout}-${format}`;cases.push({label,...m});
     await exportImage(label,{story:1920,post:1350,square:1080}[format]);
   }
 }
 await page.locator('#logoInput').setInputFiles(path.join(dir,'fixture-logo.png'));await page.waitForFunction(()=>document.querySelector('#brandLogo').style.display==='block');
 await exportImage('logo',1080);
 await page.locator('#removeLogo').click();assert.equal(await page.locator('#brandLogo').isVisible(),false);assert.equal(await page.locator('#brandText').isVisible(),true);
 await page.locator('[data-layout=editorial]').click();const padded=await page.locator('.card-body').evaluate(e=>parseFloat(getComputedStyle(e).paddingTop));
 await page.locator('#removePhoto').click();const unpadded=await page.locator('.card-body').evaluate(e=>parseFloat(getComputedStyle(e).paddingTop));assert.ok(unpadded<padded);
 await page.locator('#headlineInput').fill(('این خبر بسیار طولانی است و نباید قسمتی از متن آن بدون هشدار حذف شود. ').repeat(80));
 await page.waitForFunction(()=>document.querySelector('#news-card').dataset.fits==='false');
 assert.equal(await page.locator('#exportBtn').isDisabled(),true);assert.equal(await page.locator('#fitWarning').isVisible(),true);
 assert.ok((await page.locator('#headline').innerText()).length>1000);
 await page.locator('#headlineInput').fill(draftTitle);await page.locator('#leadInput').fill(draftLead.repeat(80));
 await page.waitForFunction(()=>document.querySelector('#news-card').dataset.fits==='false');assert.equal(await page.locator('#exportBtn').isDisabled(),true);
 await page.locator('#leadInput').fill(draftLead);await page.locator('#headlineInput').fill('تیتر فارسی ۱۲۳ / English 456\nخط دوم 👋');
 await page.waitForFunction(()=>document.querySelector('#news-card').dataset.fits==='true');
 assert.equal(await page.locator('#headline').innerText(),'تیتر فارسی ۱۲۳ / English 456\nخط دوم 👋');
 const selectAll=()=>page.evaluate(()=>{const range=document.createRange();range.selectNodeContents(document.querySelector('#headline'));const sel=getSelection();sel.removeAllRanges();sel.addRange(range);document.dispatchEvent(new Event('selectionchange'));});
 await selectAll();await page.locator('#redSelection').click();assert.ok((await page.locator('#headline').innerHTML()).includes('hl-red'));
 await selectAll();await page.locator('#goldSelection').click();assert.equal(await page.locator('#headline .hl-red').count(),0);assert.ok(await page.locator('#headline .hl-gold').count()>0);
 await selectAll();await page.locator('#clearSelection').click();assert.equal(await page.locator('#headline span').count(),0);
 await page.locator('#fontRange').fill('44');await page.locator('#fontRange').dispatchEvent('input');
 await page.waitForFunction(()=>getComputedStyle(document.querySelector('#headline')).fontSize==='44px');
 await page.locator('[data-theme=crimson]').click();await exportImage('crimson-bilingual',1080);
 await page.locator('[data-theme=platinum]').click();await exportImage('platinum-bilingual',1080);
 await page.locator('#headlineInput').fill(draftTitle);await page.locator('#autoFitBtn').click();
 await page.locator('#photoInput').setInputFiles(path.join(dir,'fixture-photo.jpg'));await page.waitForFunction(()=>document.querySelector('#news-card').classList.contains('has-photo'));
 await page.waitForFunction(()=>{const d=JSON.parse(localStorage.getItem('negahjavan.studio.draft.v1')||'null');return d&&d.photo&&d.theme==='platinum'});
 await page.reload();await page.waitForFunction(()=>!document.querySelector('#exportBtn').disabled);
 assert.equal(await page.locator('#headlineInput').inputValue(),draftTitle);assert.ok((await page.locator('#news-card').getAttribute('class')).includes('theme-platinum'));assert.ok((await page.locator('#news-card').getAttribute('class')).includes('has-photo'));
 await page.locator('#photoInput').setInputFiles({name:'broken.png',mimeType:'image/png',buffer:Buffer.from('not an image')});
 await page.waitForFunction(()=>document.querySelector('#status').textContent.includes('قابل خواندن نیست'));
 await page.locator('#photoInput').setInputFiles({name:'oversize.jpg',mimeType:'image/jpeg',buffer:Buffer.alloc(26*1024*1024)});
 await page.waitForFunction(()=>document.querySelector('#status').textContent.includes('۲۵ مگابایت'));
 // Store a full-size preview of the same DOM used for the final export.
 await page.locator('[data-theme=gold]').click();await page.locator('[data-format=post]').click();await page.locator('[data-layout=smart]').click();
 await exportImage('visual-reference',1350);
 await page.setViewportSize({width:1300,height:2200});
 await page.evaluate(()=>{const c=document.querySelector('#news-card');document.body.append(c);c.style.position='absolute';c.style.left='0';c.style.top='0';});
 await page.locator('#news-card').screenshot({path:path.join(dir,'preview-reference.png')});
 assert.deepEqual(errors,[]);assert.deepEqual(requests,[]);
 const denied=await context.newPage();await denied.addInitScript(()=>{Storage.prototype.setItem=function(){throw new DOMException('blocked','QuotaExceededError')}});
 await denied.goto('file://'+file);await denied.waitForFunction(()=>!document.querySelector('#exportBtn').disabled);await denied.locator('#headlineInput').fill('آزمایش ذخیره‌سازی');
 await denied.waitForFunction(()=>document.querySelector('#saveStatus').textContent.includes('ممکن نشد'));
 await browser.close();
 fs.writeFileSync(path.join(dir,'test-results.json'),JSON.stringify({browser:browserVersion,downloads,cases,offlineRequests:requests,errors,checks:['overflow blocks PNG without truncating source text','recolor replaces previous color','logo removal','photo removal reclaims space','manual font size','bilingual multiline text','draft with photo restores','invalid and oversized images','storage-denied notice']},null,2));
 console.log(JSON.stringify({status:'PASS',downloads,layoutCases:cases.length,offlineRequests:requests.length,errors}));
})().catch(e=>{console.error(e);process.exit(1)});
