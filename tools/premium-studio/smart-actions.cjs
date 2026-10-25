const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const fs=require('node:fs');const path=require('node:path');
const dir=__dirname,html=process.env.STUDIO_HTML||path.join(dir,'NegahJavan_Premium_Studio_3.html');
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.STUDIO_CHROMIUM?{executablePath:process.env.STUDIO_CHROMIUM}:{})});
 const context=await browser.newContext({viewport:{width:393,height:852}});const page=await context.newPage();
 const errors=[],network=[];page.on('pageerror',e=>errors.push(e.message));
 await page.route('**/*',r=>{if(/^https?:/.test(r.request().url())){network.push(r.request().url());r.abort();}else r.continue();});
 await page.goto('file://'+html);await page.waitForFunction(()=>!document.querySelector('#smartPrepare').disabled);
 assert.equal(await page.locator('#headlineInput').inputValue(),'');assert.equal(await page.locator('[data-send=post]').isDisabled(),true);
 const article='بازگشایی کتابخانهٔ مرکزی شهر\nکتابخانهٔ مرکزی پس از پایان عملیات بازسازی دوباره به روی علاقه‌مندان باز شد. سالن مطالعه و بخش کودک این مجموعه نوسازی شده است. ساعات فعالیت کتابخانه در اطلاعیهٔ مدیریت اعلام می‌شود.';
 await page.locator('#articleInput').fill(article);await page.waitForFunction(()=>!document.querySelector('[data-send=post]').disabled);
 assert.equal(await page.locator('#headlineInput').inputValue(),'بازگشایی کتابخانهٔ مرکزی شهر');
 for(const [format,max] of [['post',135],['story',175],['square',95]]){
  await page.locator('[data-format='+format+']').click();const lead=await page.locator('#leadInput').inputValue();assert.ok(lead.length<=max);assert.ok(article.includes(lead.replace(/…$/,'')));assert.ok((await page.locator('#captionInput').inputValue()).includes(article));
 }
 await page.locator('#leadInput').fill('خلاصهٔ انتخابی سردبیر');await page.locator('[data-format=post]').click();assert.equal(await page.locator('#leadInput').inputValue(),'خلاصهٔ انتخابی سردبیر');
 await page.locator('#autoSummary').check();assert.notEqual(await page.locator('#leadInput').inputValue(),'خلاصهٔ انتخابی سردبیر');
 await page.locator('#headlineInput').fill('تیتر انتخابی سردبیر');await page.locator('#articleInput').fill(article+'\nاین متن برای بررسی حفظ تیتر دستی اضافه شده است.');assert.equal(await page.locator('#headlineInput').inputValue(),'تیتر انتخابی سردبیر');
 const portrait=await page.evaluate(()=>{
  const c=document.createElement('canvas');c.width=800;c.height=1200;const g=c.getContext('2d');
  g.fillStyle='#e2e9ec';g.fillRect(0,0,800,1200);g.fillStyle='#304f5c';g.fillRect(120,700,560,500);
  g.fillStyle='#d1a176';g.beginPath();g.ellipse(400,460,210,280,0,0,Math.PI*2);g.fill();
  g.fillStyle='#263641';for(const x of [325,475]){g.beginPath();g.arc(x,430,18,0,Math.PI*2);g.fill();}
  for(const [x,y,color] of [[0,0,'red'],[750,0,'lime'],[0,1150,'blue'],[750,1150,'yellow']]){g.fillStyle=color;g.fillRect(x,y,50,50);}
  return c.toDataURL('image/png').split(',')[1];
 });
 await page.locator('#photoInput').setInputFiles({name:'portrait.png',mimeType:'image/png',buffer:Buffer.from(portrait,'base64')});
 await page.waitForFunction(()=>document.querySelector('#news-card').classList.contains('has-photo'));
 await page.locator('#headlineInput').fill('صابرین نیوز در ساعات گذشته ایران به تمام کشورهای منطقه هشدار داده است');
 await page.locator('#leadInput').fill('کشورهای منطقه یک هشدار بی‌سابقه در مورد جنگ دریافت کرده‌اند.');
 async function assertSmartLayout(){
  const data=await page.evaluate(()=>{
   const r=s=>document.querySelector(s).getBoundingClientRect(),photo=r('#cardPhoto'),badge=r('.badge'),head=r('.card-header');
   const node=document.querySelector('#headline'),rows=[],walker=document.createTreeWalker(node,NodeFilter.SHOW_TEXT);
   for(let t;t=walker.nextNode();)for(const m of t.textContent.matchAll(/\S+/gu)){const range=document.createRange();range.setStart(t,m.index);range.setEnd(t,m.index+m[0].length);const b=range.getBoundingClientRect();let row=rows.find(v=>Math.abs(v.top-b.top)<2);if(!row){row={top:b.top,count:0};rows.push(row);}row.count++;}
   rows.sort((a,b)=>a.top-b.top);
   return {fit:document.querySelector('#news-card').dataset.fits,photoTextGap:badge.top-photo.bottom,headerGap:photo.top-head.bottom,objectFit:getComputedStyle(document.querySelector('#photoImage')).objectFit,photoFilter:getComputedStyle(document.querySelector('#cardPhoto')).filter,lastLineWords:rows[rows.length-1].count};
  });
  assert.equal(data.fit,'true');assert.ok(data.photoTextGap>8);assert.ok(data.headerGap>=0);assert.equal(data.objectFit,'contain');assert.equal(data.photoFilter,'none');assert.ok(data.lastLineWords>1);return data;
 }
 const measurements=[];
 // Without Web Share, the two buttons still produce the correct downloadable formats.
 for(const [format,height] of [['post',1350],['story',1920]]){
  await page.evaluate(()=>{Object.defineProperty(navigator,'share',{configurable:true,value:undefined});Object.defineProperty(navigator,'canShare',{configurable:true,value:undefined});});
  const [download]=await Promise.all([page.waitForEvent('download'),page.locator('[data-send='+format+']').click()]);
  const target=path.join(dir,'out-smart-action-'+format+'.png');await download.saveAs(target);const png=fs.readFileSync(target);assert.equal(png.readUInt32BE(16),1080);assert.equal(png.readUInt32BE(20),height);
  await page.waitForFunction(()=>!document.querySelector('[data-send=post]').disabled);measurements.push({format,...await assertSmartLayout()});
 }
 await page.evaluate(()=>{
  window.shared=[];window.shareMode='ok';
  Object.defineProperty(navigator,'canShare',{configurable:true,value:d=>d.files&&d.files[0] instanceof File});
  Object.defineProperty(navigator,'share',{configurable:true,value:async data=>{
   if(window.shareMode==='deny'){window.shareMode='ok';throw new DOMException('activation required','NotAllowedError');}
   if(window.shareMode==='abort')throw new DOMException('cancelled','AbortError');
   const f=data.files[0],view=new DataView(await f.arrayBuffer());window.shared.push({name:f.name,width:view.getUint32(16),height:view.getUint32(20),count:data.files.length,text:data.text});
  }});
 });
 // These are API stubs only; the test never opens an app or sends to a person.
 await page.locator('[data-send=post]').click();await page.waitForFunction(()=>window.shared.length===1);
 assert.deepEqual(await page.evaluate(()=>[window.shared[0].width,window.shared[0].height]),[1080,1350]);
 await page.locator('[data-send=story]').click();await page.waitForFunction(()=>window.shared.length===2);
 assert.deepEqual(await page.evaluate(()=>[window.shared[1].width,window.shared[1].height]),[1080,1920]);
 const beforeName=await page.evaluate(()=>window.shared[1].name);
 await page.evaluate(()=>window.shareMode='deny');await page.locator('[data-send=story]').click();await page.waitForFunction(()=>document.querySelector('#status').textContent.includes('یک بار دیگر'));
 await page.locator('[data-send=story]').click();await page.waitForFunction(()=>window.shared.length===3);assert.equal(await page.evaluate(()=>window.shared[2].name),beforeName);
 let unwantedDownloads=0;page.on('download',()=>unwantedDownloads++);await page.evaluate(()=>window.shareMode='abort');await page.locator('[data-send=story]').click();await page.waitForFunction(()=>document.querySelector('#status').textContent.includes('لغو شد'));assert.equal(unwantedDownloads,0);
 await page.locator('#leadInput').fill('این متن بلند برای آزمایش خلاصه‌سازی خودکار نوشته شده است. '.repeat(15));
 await page.locator('#articleInput').fill('');const fullLead=(await page.locator('#leadInput').inputValue()).trim();await page.locator('#smartPrepare').click();assert.ok((await page.locator('#articleInput').inputValue()).includes(fullLead));assert.ok((await page.locator('#leadInput').inputValue()).length<=175);
 await page.waitForFunction(text=>{const d=JSON.parse(localStorage.getItem('negahjavan.studio.draft.v1')||'null');return d&&d.article.includes(text)&&d.appVersion==='3.2'},fullLead);
 await page.reload();await page.waitForFunction(()=>!document.querySelector('#smartPrepare').disabled);assert.equal(await page.locator('[data-layout=smart]').getAttribute('aria-pressed'),'true');assert.ok((await page.locator('#articleInput').inputValue()).includes(fullLead));
 assert.deepEqual(errors,[]);assert.deepEqual(network,[]);
 fs.writeFileSync(path.join(dir,'smart-results.json'),JSON.stringify({status:'PASS',measurements,checks:['fresh draft has no sample news','source-derived title and summary','source retained in caption','manual text respected','portrait not cropped or covered','balanced headline','post/story dimensions','native-share API stubs','activation retry reuses file','cancel does not download','automatic long-lead extraction','draft migration and restore']},null,2));
 console.log(JSON.stringify({status:'PASS',measurements,nativeShares:'stubbed; no external send',errors}));await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
