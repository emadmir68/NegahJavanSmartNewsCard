const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const fs=require('node:fs');const path=require('node:path');
const dir=__dirname,file=process.env.STUDIO_HTML||path.join(dir,'NegahJavan_Premium_Studio_3.html');
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.STUDIO_CHROMIUM?{executablePath:process.env.STUDIO_CHROMIUM}:{})});
 const context=await browser.newContext({viewport:{width:393,height:852},deviceScaleFactor:1});
 const page=await context.newPage(),errors=[],requests=[],cases=[];let downloads=0;
 page.on('pageerror',e=>errors.push(e.message));page.on('console',m=>{if(m.type()==='error')errors.push(m.text())});
 await page.route('**/*',r=>{if(/^https?:/.test(r.request().url())){requests.push(r.request().url());r.abort()}else r.continue()});
 const fixtures=await page.evaluate(()=>{
  const c=document.createElement('canvas');c.width=3000;c.height=1800;const g=c.getContext('2d');g.fillStyle='#195d60';g.fillRect(0,0,3000,1800);
  for(let x=0;x<3000;x+=150){g.fillStyle=`rgb(${35+x/25},85,110)`;g.fillRect(x,0,75,1800)}
  g.fillStyle='#c9a258';g.beginPath();g.ellipse(1450,790,650,650,0,0,Math.PI*2);g.fill();g.fillStyle='#263c2e';g.fillRect(0,1450,3000,350);
  const photo=c.toDataURL('image/jpeg',.9).split(',')[1];c.width=400;c.height=300;g.fillStyle='#fff';g.beginPath();g.ellipse(185,150,145,140,0,0,Math.PI*2);g.fill();g.fillStyle='#d4af63';g.fillRect(130,70,115,170);
  return {photo,logo:c.toDataURL('image/png').split(',')[1]};
 });
 for(const [name,data] of Object.entries(fixtures))fs.writeFileSync(path.join(dir,'fixture-'+name+(name==='photo'?'.jpg':'.png')),Buffer.from(data,'base64'));
 await page.goto('file://'+file);await page.waitForFunction(()=>!document.querySelector('#smartPrepare').disabled);
 assert.equal(await page.locator('#headlineInput').inputValue(),'');assert.equal(await page.locator('#exportBtn').isDisabled(),true);
 assert.equal(await page.locator('details[open]').count(),0);
 assert.equal(await page.evaluate(()=>document.fonts.check('750 64px Vazirmatn','خبر فارسی')),true);
 for(const width of [320,393,768,1440]){
  await page.setViewportSize({width,height:852});
  const m=await page.evaluate(()=>{const box=s=>document.querySelector(s).getBoundingClientRect();return {w:document.documentElement.scrollWidth,v:innerWidth,preview:box('.preview-area').bottom,editor:box('.panel').top,shell:box('#previewShell').height,foot:box('.publish-bar').top}});
  assert.ok(m.w<=m.v,'no horizontal page scrolling');
  if(width<=800){assert.ok(m.preview<m.editor,'mobile preview comes first');assert.ok(m.shell<=370,'mobile preview leaves room for controls')}
 }
 await page.setViewportSize({width:393,height:852});
 await page.screenshot({path:path.join(dir,'qa-empty-mobile.png')});
 const title='نگاه تازه به فردای جنوب کرمان',lead='این یک متن نمونه برای نمایش طراحی تازهٔ کارت خبری است.';
 await page.locator('#headlineInput').fill(title);await page.locator('#leadInput').fill(lead);
 await page.locator('#designOptions').evaluate(e=>e.open=true);
 async function exportImage(label,height){
  await page.waitForFunction(()=>!document.querySelector('#exportBtn').disabled);
  const [download]=await Promise.all([page.waitForEvent('download'),page.locator('#exportBtn').click()]);
  const target=path.join(dir,'out-'+label+'.png');await download.saveAs(target);
  await page.waitForFunction(()=>!document.querySelector('#exportBtn').disabled);
  const png=fs.readFileSync(target);assert.equal(png.readUInt32BE(16),1080);assert.equal(png.readUInt32BE(20),height);assert.ok(png.length>10000);downloads++;return target;
 }
 const measure=()=>page.evaluate(()=>{
  const box=s=>document.querySelector(s).getBoundingClientRect(),h=box('#headline'),lead=box('#lead'),photo=box('#cardPhoto'),header=box('.card-header'),footer=box('.card-footer');
  return {fits:document.querySelector('#news-card').dataset.fits,headerGap:h.top-header.bottom,photoTextGap:h.top-photo.bottom,photoHeaderGap:photo.top-header.bottom,footerGap:footer.top-lead.bottom,dir:getComputedStyle(document.querySelector('#news-card')).direction,font:parseFloat(getComputedStyle(document.querySelector('#headline')).fontSize)};
 });
 for(const hasPhoto of [false,true]){
  if(hasPhoto){await page.locator('#photoInput').setInputFiles(path.join(dir,'fixture-photo.jpg'));await page.waitForFunction(()=>document.querySelector('#news-card').classList.contains('has-photo'))}
  for(const layout of ['smart','breaking'])for(const theme of ['crimson','gold'])for(const format of ['story','post','square']){
   await page.locator('[data-layout='+layout+']').click();await page.locator('[data-theme='+theme+']').click();await page.locator('[data-format='+format+']').click();
   await page.waitForFunction(()=>!document.querySelector('#exportBtn').disabled);const m=await measure();
   assert.equal(m.fits,'true');assert.equal(m.dir,'rtl');assert.ok(m.headerGap>=0);assert.ok(m.footerGap>=0);
   if(layout==='smart'&&hasPhoto){assert.ok(m.photoTextGap>8);assert.ok(m.photoHeaderGap>=0)}
   const label=`${hasPhoto?'photo':'type'}-${layout}-${theme}-${format}`;cases.push({label,...m});await exportImage(label,{story:1920,post:1350,square:1080}[format]);
  }
 }
 await page.locator('[data-layout=smart]').click();await page.locator('#logoInput').setInputFiles(path.join(dir,'fixture-logo.png'));await page.waitForFunction(()=>document.querySelector('#brandLogo').style.display==='block');await exportImage('custom-logo',1080);
 await page.locator('#removeLogo').click();assert.equal(await page.locator('#brandLogo').isVisible(),false);assert.equal(await page.locator('#brandText').isVisible(),true);
 const padded=await page.locator('.card-body').evaluate(e=>parseFloat(getComputedStyle(e).paddingTop));await page.locator('#removePhoto').click();const unpadded=await page.locator('.card-body').evaluate(e=>parseFloat(getComputedStyle(e).paddingTop));assert.ok(unpadded<padded);
 const veryLong='این خبر بسیار طولانی است و نباید قسمتی از متن آن بدون هشدار حذف شود. '.repeat(80);
 await page.locator('#headlineInput').fill(veryLong);await page.waitForFunction(()=>document.querySelector('#news-card').dataset.fits==='false');assert.equal(await page.locator('#exportBtn').isDisabled(),true);assert.equal(await page.locator('#fitWarning').isVisible(),true);assert.equal(await page.locator('#headline').innerText(),veryLong);
 await page.locator('#headlineInput').fill(title);await page.locator('#leadInput').fill(lead.repeat(80));await page.waitForFunction(()=>document.querySelector('#news-card').dataset.fits==='false');assert.equal(await page.locator('#exportBtn').isDisabled(),true);
 await page.locator('#leadInput').fill(lead);await page.locator('#headlineInput').fill('تیتر فارسی ۱۲۳ / English 456\nخط دوم 👋');await page.waitForFunction(()=>document.querySelector('#news-card').dataset.fits==='true');assert.equal(await page.locator('#headline').innerText(),'تیتر فارسی ۱۲۳ / English 456\nخط دوم 👋');
 const selectAll=()=>page.evaluate(()=>{const r=document.createRange();r.selectNodeContents(document.querySelector('#headline'));const s=getSelection();s.removeAllRanges();s.addRange(r);document.dispatchEvent(new Event('selectionchange'))});
 await selectAll();await page.locator('#redSelection').click();assert.ok(await page.locator('#headline .hl-red').count());await selectAll();await page.locator('#goldSelection').click();assert.equal(await page.locator('#headline .hl-red').count(),0);assert.ok(await page.locator('#headline .hl-gold').count());await selectAll();await page.locator('#clearSelection').click();assert.equal(await page.locator('#headline span').count(),0);
 await page.locator('#fontRange').fill('44');await page.locator('#fontRange').dispatchEvent('input');await page.waitForFunction(()=>getComputedStyle(document.querySelector('#headline')).fontSize==='44px');await exportImage('manual-bilingual',1080);
 await page.locator('#headlineInput').fill(title);await page.locator('#autoFitBtn').click();await page.locator('#photoInput').setInputFiles(path.join(dir,'fixture-photo.jpg'));await page.waitForFunction(()=>document.querySelector('#news-card').classList.contains('has-photo'));
 await page.waitForFunction(()=>{const d=JSON.parse(localStorage.getItem('negahjavan.studio.draft.v1')||'null');return d&&d.photo&&d.theme==='gold'&&d.appVersion==='4.0'});
 await page.reload();await page.waitForFunction(()=>!document.querySelector('#exportBtn').disabled);assert.equal(await page.locator('#headlineInput').inputValue(),title);assert.ok((await page.locator('#news-card').getAttribute('class')).includes('has-photo'));assert.equal(await page.locator('details[open]').count(),0);
 await page.locator('#photoInput').setInputFiles({name:'broken.png',mimeType:'image/png',buffer:Buffer.from('not an image')});await page.waitForFunction(()=>document.querySelector('#status').textContent.includes('قابل خواندن نیست'));
 await page.locator('#photoInput').setInputFiles({name:'oversize.jpg',mimeType:'image/jpeg',buffer:Buffer.alloc(26*1024*1024)});await page.waitForFunction(()=>document.querySelector('#status').textContent.includes('۲۵ مگابایت'));
 await page.locator('#designOptions').evaluate(e=>e.open=true);await page.locator('[data-theme=crimson]').click();await page.locator('[data-format=post]').click();await exportImage('visual-reference',1350);
 await page.evaluate(()=>{document.querySelectorAll('details').forEach(e=>e.open=false);document.activeElement.blur();document.querySelector('#publishStatus').hidden=true;scrollTo({top:0,behavior:'instant'})});
 await page.screenshot({path:path.join(dir,'qa-filled-mobile.png')});await page.setViewportSize({width:1440,height:1000});await page.screenshot({path:path.join(dir,'qa-filled-desktop.png')});
 await page.setViewportSize({width:1300,height:2200});await page.evaluate(()=>{const c=document.querySelector('#news-card');document.body.append(c);c.style.position='absolute';c.style.left='0';c.style.top='0'});await page.locator('#news-card').screenshot({path:path.join(dir,'preview-reference.png')});
 const denied=await context.newPage();await denied.addInitScript(()=>{Storage.prototype.setItem=function(){throw new DOMException('blocked','QuotaExceededError')}});await denied.goto('file://'+file);await denied.waitForFunction(()=>!document.querySelector('#smartPrepare').disabled);await denied.locator('#headlineInput').fill('آزمایش ذخیره‌سازی');await denied.waitForFunction(()=>document.querySelector('#saveStatus').textContent.includes('ممکن نشد'));
 const migration=await browser.newPage();await migration.addInitScript(()=>localStorage.setItem('negahjavan.studio.draft.v1',JSON.stringify({version:1,appVersion:'3.2',headline:[{text:'تیتر ذخیره‌شدهٔ قبلی',color:'red'}],lead:'خلاصهٔ قبلی',article:'متن کامل قبلی',autoSummary:false,layout:'glass',theme:'gold',badge:'خبر'})));await migration.goto('file://'+file);await migration.waitForFunction(()=>!document.querySelector('#exportBtn').disabled);assert.equal(await migration.locator('#headlineInput').inputValue(),'تیتر ذخیره‌شدهٔ قبلی');assert.equal(await migration.locator('#leadInput').inputValue(),'خلاصهٔ قبلی');assert.equal(await migration.locator('#articleInput').inputValue(),'متن کامل قبلی');assert.ok(await migration.locator('#headline .hl-red').count());assert.equal(await migration.locator('[data-layout=smart]').getAttribute('aria-pressed'),'true');assert.equal(await migration.locator('[data-theme=crimson]').getAttribute('aria-pressed'),'true');
 assert.deepEqual(errors,[]);assert.deepEqual(requests,[]);const result={status:'PASS',browser:browser.version(),downloads,cases,offlineRequests:requests,errors,checks:['responsive 320/393/768/1440','empty draft and collapsed settings','24 format/layout/theme/photo exports','photo/text separation','overflow keeps source and blocks PNG','custom logo and removal','photo removal reclaims space','multiline bilingual text','recolor replaces previous color','manual typography','v4 draft restore','v3.2 draft migration preserves content','invalid/oversized images','storage-denied notice']};
 fs.writeFileSync(path.join(dir,'test-results.json'),JSON.stringify(result,null,2));console.log(JSON.stringify({...result,cases:cases.length}));await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
