package ir.negahjavan.newscard.studio

import android.content.Context
import android.graphics.*
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StudioRegressionTest{
 private val c:Context=RuntimeEnvironment.getApplication()
 private val title="بسته‌های اینترنت از شب گذشته ۴۰ درصد گران شد"
 private val summary="کیفیت خدمات و سرعت اینترنت همچنان محل پرسش کاربران است."
 private fun normalized(s:String)=s.replace('\u00a0',' ').replace(Regex("\\s+")," ").trim()
 private fun save(name:String,b:Bitmap){val f=File("build/reports/studio-158/$name.png");requireNotNull(f.parentFile).mkdirs();f.outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}}
 private fun photo()=Bitmap.createBitmap(700,1100,Bitmap.Config.ARGB_8888).also{b->val ca=Canvas(b);ca.drawColor(Color.rgb(43,79,70));val p=Paint(Paint.ANTI_ALIAS_FLAG);p.color=Color.rgb(193,157,92);ca.drawCircle(350f,320f,190f,p);p.color=Color.rgb(20,39,34);ca.drawRoundRect(110f,530f,590f,1100f,160f,160f,p)}
 @Test fun photoAndTextNeverOverlapInCoverLayouts(){
  val b=photo()
  for(style in Style.entries)for(ratio in Ratio.entries){
   val plan=StudioRenderer.plan(c,Project(title=title,summary=summary,style=style,ratio=ratio),b)
   assertFalse("$style $ratio",RectF.intersects(plan.text,requireNotNull(plan.photo)))
  }
  b.recycle()
 }
 @Test fun storyTemplatesHaveDistinctGeometry(){
  val b=photo()
  val plans=listOf(Style.PHOTO,Style.EDITORIAL,Style.BREAKING).map{StudioRenderer.plan(c,Project(title=title,style=it),b)}
  assertEquals(3,plans.map{it.text.toString()}.distinct().size)
  assertTrue(plans[2].text.bottom<requireNotNull(plans[2].photo).top)
  b.recycle()
 }
 @Test fun manualAndAutomaticSize(){for(r in Ratio.entries){val p=Project(title=title,summary=summary,ratio=r,auto=false,titleSize=22f);assertTrue(StudioRenderer.plan(c,p).pages.all{it.titleSize==22f});assertTrue(StudioRenderer.plan(c,p.copy(auto=true)).pages.all{it.titleSize<=22f})}}
 @Test fun preservesLongInputWithoutEllipsis(){val t=(1..30).joinToString(" "){"تیتر شماره $it بدون حذف"};val b=(1..80).joinToString(" "){"جزئیات خبر $it برای خواندن کامل"};for(r in Ratio.entries){val plan=StudioRenderer.plan(c,Project(title=t,summary=b,ratio=r));assertEquals(normalized(t),normalized(plan.pages.joinToString(" "){it.title}));assertEquals(normalized(b),normalized(plan.pages.joinToString(" "){it.body}));assertTrue(plan.pages.size>1)}}
 @Test fun whitespaceDoesNotLoseSummaryWords(){val b="  "+summary.repeat(20)+"   ";val plan=StudioRenderer.plan(c,Project(title=title,summary=b,ratio=Ratio.SQUARE));assertEquals(normalized(b),normalized(plan.pages.joinToString(" "){it.body}))}
 @Test fun numberUnitsRemainTogetherAndIndicesStable(){val t="۴۰ درصد و ۱۲ میلیون تومان افزایش";val got=StudioText.keepUnits(t);assertEquals(t.length,got.length);assertTrue(got.contains("۴۰\u00a0درصد"));assertTrue(got.contains("۱۲\u00a0میلیون\u00a0تومان"))}
 @Test fun captionUsesEditedTitleWithoutOldHeadline(){val(t,b)=StudioText.prepare("تیتر قبلی\nمتن اصلی این خبر برای انتشار کامل است.");val p=Project(raw=t+"\n"+b,title="تیتر تازه",article=b);assertFalse(p.caption().contains("تیتر قبلی"));assertTrue(p.caption().contains("تیتر تازه"));assertTrue(p.caption().contains(b))}
 @Test fun suggestedSummaryIsAnExactSourceSentence(){val body="گزارش عمومی درباره موضوع دیگری منتشر شده است. قیمت اینترنت ۴۰ درصد افزایش یافته است.";assertEquals("قیمت اینترنت ۴۰ درصد افزایش یافته است.",StudioText.summary(title,body))}
 @Test fun portraitFitKeepsSubjectLargeAndTextReadable(){
  val b=photo()
  for(ratio in listOf(Ratio.SQUARE,Ratio.POST)){
   val plan=StudioRenderer.plan(c,Project(title=title,summary=summary,ratio=ratio),b)
   val frame=requireNotNull(plan.photo)
   val subjectWidth=minOf(frame.width(),frame.height()*b.width/b.height)
   assertTrue("Portrait subject too small in $ratio",subjectWidth>=350)
   assertTrue("Headline column too narrow in $ratio",plan.text.width()>=450)
  }
  b.recycle()
 }

 @Test fun cropStaysInsideSourceAndAllowsPanZoom(){val a=StudioRenderer.cropRect(1920,1080,1080f,1920f,0f,0f,1f);val b=StudioRenderer.cropRect(1920,1080,1080f,1920f,1f,1f,2f);assertEquals(0f,a.left,.01f);assertTrue(b.width()<a.width());assertTrue(b.right<=1920&&b.bottom<=1080);assertFalse(Project().videoCrop)}
 @Test fun carouselPreservesArticleAndHasClosing(){val article=(1..60).joinToString(" "){"متن کامل خبر شماره $it"};for(style in Style.entries){val plan=StudioRenderer.plan(c,Project(title=title,summary=summary,article=article,carousel=true,closingText="برای خبرهای بیشتر همراه ما باشید",ratio=Ratio.POST,style=style));assertEquals("cover",plan.pages.first().kind);assertEquals("closing",plan.pages.last().kind);assertEquals(article,normalized(plan.pages.filter{it.kind=="article"}.joinToString(" "){it.body}));for(i in listOf(0,1,plan.pages.lastIndex)){val b=StudioRenderer.draw(c,plan,i);save("carousel-$style-$i",b);b.recycle()}}}
 @Test fun projectRestoresAllFields(){val p=Project(title=title,summary=summary,article="محتوا",font=StudioFont.AMIRI,bodyFont=StudioFont.AREF,useLogo=false,fullLogo=true,titleSize=21f,ratio=Ratio.SQUARE,crop=true,x=.2f,y=.7f,zoom=2f,video="content://video",image="file:///image",videoCrop=true,vy=.1f,showBrand=false,showSite=false,fullHd=false,closingText="متن پایان قابل ویرایش",exports=listOf("content://saved"));ProjectStore.save(c,p);assertEquals(p,ProjectStore.load(c));ProjectStore.save(c,p,true);assertTrue(ProjectStore.list(c).contains(p))}
 @Test fun reopeningAnOldProjectRequiresFreshDesignedExports(){
  val p=Project(title=title,summary=summary,font=StudioFont.AMIRI,titleSize=29f,exports=listOf("content://old-card"))
  val legacy=p.json().apply{remove("exportRendererVersion")}
  assertEquals(p.copy(exports=emptyList()),Project.read(legacy))
  assertEquals(p.copy(exports=emptyList()),Project.read(p.json().put("exportRendererVersion",27)))
  assertEquals(p,Project.read(p.json()))
 }
 @Test fun videoPaginationAndTooShortDuration(){val pages=List(3){Page("خبر","",24f)};assertEquals(0,StudioVideo.pageAt(0,9000,pages));assertEquals(1,StudioVideo.pageAt(3000000,9000,pages));assertEquals(2,StudioVideo.pageAt(9000000,9000,pages));val plan=StudioRenderer.plan(c,Project(title=(title+" ").repeat(20)),overlay=true);assertThrows(IllegalArgumentException::class.java){StudioVideo.validate(plan,StudioVideo.Info(1920,1080,1000))}}
 @Test fun videoGivesLongerPagesMoreReadingTime(){val pages=listOf(Page("الف".repeat(300),"",24f),Page("پایان","",24f));assertEquals(0,StudioVideo.pageAt(15_000_000,24000,pages));assertEquals(1,StudioVideo.pageAt(23_900_000,24000,pages))}
 @Test fun videoTopReallyStartsNearTop(){for(pos in Position.entries){val plan=StudioRenderer.plan(c,Project(title=title,summary=summary,position=pos),overlay=true);val b=StudioRenderer.draw(c,plan,0);save("video-$pos",b);if(pos==Position.TOP)assertTrue(Color.alpha(b.getPixel(540,420))>150);if(pos==Position.BOTTOM)assertEquals(0,Color.alpha(b.getPixel(540,420)));b.recycle()}}
 @Test fun continuationsReleasePhotoSpaceAndPreserveText(){
  val b=photo();val body=(1..120).joinToString(" "){"جزئیات شماره $it"}
  val plan=StudioRenderer.plan(c,Project(title=title,summary=body,ratio=Ratio.POST),b)
  assertEquals(normalized(body),normalized(plan.pages.joinToString(" "){it.body}))
  for(page in plan.pages.drop(1)){
   assertEquals("continued",page.kind)
   val full=StudioRenderer.pageBox(plan,page)
   assertTrue(full.width()*full.height()>plan.text.width()*plan.text.height())
   assertTrue(StudioRenderer.blockHeight(c,plan,page)<=StudioRenderer.pageBox(plan,page).height()+1f)
  }
  b.recycle()
 }
 @Test fun longCategoryAndManualTypefaceCannotOverflow(){
  for(style in Style.entries)for(ratio in Ratio.entries){
   val plan=StudioRenderer.plan(c,Project(title=title.repeat(6),summary=summary.repeat(7),category="گزارش ویژه اقتصاد و فناوری",style=style,ratio=ratio,auto=false,titleSize=40f,font=StudioFont.KUFI),overlay=true)
   assertTrue(plan.pages.all{it.titleSize==40f})
   for(page in plan.pages)assertTrue("$style $ratio",StudioRenderer.blockHeight(c,plan,page)<=StudioRenderer.pageBox(plan,page).height()+1f)
  }
 }
 @Test fun closingIsOptionalAndLongClosingIsNotTruncated(){
  val p=Project(title=title,article=summary,carousel=true)
  assertFalse(StudioRenderer.plan(c,p).pages.any{it.kind=="closing"})
  val ending=(1..100).joinToString(" "){"پایان شماره $it"}
  val plan=StudioRenderer.plan(c,p.copy(closingText=ending))
  assertEquals(ending,normalized(plan.pages.filter{it.kind=="closing"}.joinToString(" "){it.body}))
 }
 @Test fun designContactSheet(){
  val board=Bitmap.createBitmap(1128,1480,Bitmap.Config.ARGB_8888);val canvas=Canvas(board)
  canvas.drawColor(Color.rgb(235,237,240));val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
  val landscape=Bitmap.createBitmap(1200,800,Bitmap.Config.ARGB_8888)
  val scene=Canvas(landscape);scene.drawColor(Color.rgb(199,211,201))
  paint.color=Color.rgb(67,99,89);scene.drawRect(0f,470f,1200f,800f,paint)
  paint.color=Color.rgb(25,43,45);scene.drawRoundRect(430f,80f,770f,710f,30f,30f,paint)
  paint.color=Color.rgb(225,207,156);scene.drawRoundRect(450f,120f,750f,650f,12f,12f,paint)
  for(i in 0..3){paint.color=Color.rgb(53+i*8,86+i*6,77+i*5);scene.drawRect(480f+i*62,560f-i*82,520f+i*62,600f,paint)}
  val samples=listOf(Style.SIGNATURE,Style.EDITORIAL,Style.BREAKING,Style.PHOTO,Style.QUOTE,Style.STAT)
  for((i,style) in samples.withIndex()){
   val project=Project(title=title,summary=summary,style=style,category="خبر نمونه",ratio=Ratio.STORY)
   val plan=StudioRenderer.plan(c,project,landscape);val bitmap=StudioRenderer.draw(c,plan,0,landscape)
   if(i==0)save("brand-story",bitmap)
   val left=12f+(i%3)*372f;val top=12f+(i/3)*734f
   paint.alpha=255;canvas.drawBitmap(bitmap,null,RectF(left,top,left+360,top+640),paint);bitmap.recycle()
   val label=StudioRenderer.layout(style.fa,360,19f,StudioFont.VAZIR.face(c),Color.rgb(26,28,33))
   canvas.save();canvas.translate(left,top+652);label.draw(canvas);canvas.restore()
  }
  save("design-contact-sheet",board)
  val reference=File("../docs/design/previews/7a1c74761f3c5d2ceca38f63ba07f0339331c9a2/design-contact-sheet.png")
  if(reference.isFile){
   val before=BitmapFactory.decodeFile(reference.path)
   val comparison=Bitmap.createBitmap(760,724,Bitmap.Config.ARGB_8888);val compare=Canvas(comparison)
   compare.drawColor(Color.rgb(235,237,240))
   compare.drawBitmap(before,Rect(12,12,372,652),RectF(12f,12f,372f,652f),paint)
   compare.drawBitmap(board,Rect(12,12,372,652),RectF(388f,12f,748f,652f),paint)
   for((i,labelText)in listOf("نسخهٔ قبل · ۱.۷.۱","چیدمان تازه · ۱.۸.۲").withIndex()){
    val caption=StudioRenderer.layout(labelText,360,19f,StudioFont.VAZIR.face(c),Color.rgb(26,28,33))
    compare.save();compare.translate(12f+i*376,664f);caption.draw(compare);compare.restore()
   }
   save("design-before-after",comparison);comparison.recycle();before.recycle()
  }
  board.recycle();landscape.recycle()
 }
 @Test fun decoratedSurfacesStayInsideOutputAndClearFooter(){
  val photo=photo()
  for(style in Style.entries)for(ratio in Ratio.entries)for(position in Position.entries)for(hasPhoto in listOf(false,true)){
   val project=Project(title=title,summary=summary,ratio=ratio,style=style,position=position)
   val plan=StudioRenderer.plan(c,project,if(hasPhoto)photo else null)
   for(page in plan.pages){
    val rect=StudioRenderer.surfaceBox(c,plan,page)
    assertTrue("Surface outside output: $style $ratio $position",rect.left>=0&&rect.top>=0&&rect.right<=ratio.w&&rect.bottom<=ratio.h)
    val footerRule=ratio.h-(if(ratio==Ratio.STORY)72 else 41)*(minOf(ratio.w,ratio.h)/360f)
    assertTrue("Surface crosses footer: $style $ratio $position",rect.bottom<footerRule)
   }
  }
  photo.recycle()
 }
 @Test fun realHeadlineWeightAndLegacyDefaults(){
  assertEquals(900,StudioFont.ESTEDAD.face(c).weight)
  assertEquals(500,StudioFont.CAIRO.bodyFace(c).weight)
  val old=Project(title=title,font=StudioFont.VAZIR,titleSize=21f,bodySize=12f).json().apply{remove("typographyVersion");remove("bodyFont");remove("useLogo");remove("fullLogo")}
  val updated=Project.read(old)
  assertEquals(StudioFont.ESTEDAD,updated.font);assertEquals(StudioFont.CAIRO,updated.bodyFont)
  assertEquals(26f,updated.titleSize,0f);assertEquals(13f,updated.bodySize,0f)
  assertTrue(updated.useLogo);assertFalse(updated.fullLogo)
  val manual=Project.read(old.put("auto",false))
  assertEquals(StudioFont.VAZIR,manual.font);assertEquals(21f,manual.titleSize,0f);assertEquals(12f,manual.bodySize,0f)
 }
 @Test fun independentBodyFontsPreserveLongCopy(){
  val body=(1..28).joinToString(" "){"جزئیات خبر شماره $it برای مطالعهٔ کامل"}
  for(font in StudioFont.entries)for(ratio in Ratio.entries){
   val plan=StudioRenderer.plan(c,Project(title=title,summary=body,bodyFont=font,ratio=ratio))
   assertEquals(normalized(body),normalized(plan.pages.joinToString(" "){it.body}))
   for(page in plan.pages)assertTrue("Body overflow $font $ratio",StudioRenderer.blockHeight(c,plan,page)<=StudioRenderer.pageBox(plan,page).height()+1)
   StudioRenderer.draw(c,plan,0).recycle()
  }
 }
 @Test fun integratedLogoModesStayClearOfContent(){
  for(ratio in Ratio.entries)for(full in listOf(false,true))for(size in listOf(8f,12f,18f)){
   val p=Project(title=title,summary=summary,ratio=ratio,fullLogo=full,brandSize=size)
   val plan=StudioRenderer.plan(c,p);val rect=requireNotNull(StudioRenderer.logoBox(plan))
   val s=minOf(ratio.w,ratio.h)/360f
   assertTrue(rect.left>=0&&rect.top>=0&&rect.right<=ratio.w)
   assertTrue(rect.bottom+5*s<plan.fullText.top-12*s)
   val bitmap=StudioRenderer.draw(c,plan,0)
   if(ratio==Ratio.STORY&&full&&size==12f)save("brand-with-tagline",bitmap)
   bitmap.recycle()
  }
  assertNull(StudioRenderer.logoBox(StudioRenderer.plan(c,Project(title=title,showBrand=false))))
  assertNull(StudioRenderer.logoBox(StudioRenderer.plan(c,Project(title=title,useLogo=false))))
 }
 @Test fun wordmarkHasTransparentCountersAndUsesCardColors(){
  for(style in Style.entries)for(tagline in listOf(false,true)){
   val p=Project(title=title,style=style,fullLogo=tagline)
   val bitmap=Bitmap.createBitmap(600,280,Bitmap.Config.ARGB_8888)
   val(w,h)=StudioBrandMark.size(p,4f);val rect=RectF(20f,20f,20+w,20+h)
   StudioBrandMark.draw(c,Canvas(bitmap),rect,p,4f)
   val top=rect.bottom-rect.width()*StudioBrandMark.ASPECT
   fun pixel(x:Int,y:Int)=bitmap.getPixel((rect.left+x*rect.width()/1264).toInt(),(top+y*rect.width()/1264).toInt())
   // Both enclosed letter counters and the open space above the lettering
   // must remain transparent, including in the reversed (white) variant.
   for((x,y)in listOf(425 to 215,811 to 226,660 to 44,10 to 10))assertEquals("Opaque logo background: $style $tagline",0,Color.alpha(pixel(x,y)))
   val colors=StudioBrandMark.colors(style)
   assertEquals("Wrong wordmark ink: $style",colors.ink,pixel(40,244))
   assertEquals("Wrong accent ink: $style",colors.accent,pixel(1110,44))
   bitmap.recycle()
  }
 }
 @Test fun logoInHeaderHasNoStickerBacking(){
  val backgrounds=mapOf(Style.SIGNATURE to "#111419",Style.EDITORIAL to "#FFFFFF",Style.PHOTO to "#090D11",Style.BREAKING to "#C70B28",Style.QUOTE to "#F0EFEC",Style.STAT to "#101923")
  val board=Bitmap.createBitmap(1080,720,Bitmap.Config.ARGB_8888);val canvas=Canvas(board);canvas.drawColor(Color.rgb(30,34,40))
  val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
  for((i,style)in Style.entries.withIndex()){
   val p=Project(title=title,summary=summary,style=style)
   val plan=StudioRenderer.plan(c,p);val rect=requireNotNull(StudioRenderer.logoBox(plan))
   val bitmap=StudioRenderer.draw(c,plan,0)
   val expected=Color.parseColor(requireNotNull(backgrounds[style]))
   // This exact pixel was the white support rectangle in version 1.7.0.
   assertEquals("White backing returned: $style",expected,bitmap.getPixel((rect.left-5).toInt(),(rect.top+10).toInt()))
   assertEquals("Counter must reveal header: $style",expected,bitmap.getPixel((rect.left+425*rect.width()/1264).toInt(),(rect.top+215*rect.width()/1264).toInt()))
   canvas.drawBitmap(bitmap,Rect(0,90,1080,360),RectF((i%2)*540f,(i/2)*240f,(i%2)*540f+540,(i/2)*240f+135),paint)
   val caption=StudioRenderer.layout(style.fa,480,25f,StudioFont.ESTEDAD.bodyFace(c),Color.rgb(240,240,240))
   canvas.save();canvas.translate((i%2)*540f+30,(i/2)*240f+160);caption.draw(canvas);canvas.restore()
   bitmap.recycle()
  }
  save("brand-header-variants",board);board.recycle()
 }
 @Test fun typographyChoicesPreview(){
  val board=Bitmap.createBitmap(1128,724,Bitmap.Config.ARGB_8888);val canvas=Canvas(board)
  canvas.drawColor(Color.rgb(235,237,240));val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
  val variants=listOf(
   "هویت برند · استعداد / قاهره" to Project(title=title,summary=summary),
   "نمایشی · لاله‌زار / استعداد" to Project(title=title,summary=summary,font=StudioFont.LALEZAR,bodyFont=StudioFont.ESTEDAD,titleSize=28f),
   "کلاسیک · امیری / استعداد" to Project(title=title,summary=summary,font=StudioFont.AMIRI,bodyFont=StudioFont.ESTEDAD,titleSize=28f)
  )
  for((i,entry)in variants.withIndex()){
   val plan=StudioRenderer.plan(c,entry.second);val bitmap=StudioRenderer.draw(c,plan,0)
   val x=12f+i*372f;canvas.drawBitmap(bitmap,null,RectF(x,12f,x+360,652f),paint);bitmap.recycle()
   val caption=StudioRenderer.layout(entry.first,360,16f,StudioFont.ESTEDAD.bodyFace(c),Color.rgb(26,28,33))
   canvas.save();canvas.translate(x,666f);caption.draw(canvas);canvas.restore()
  }
  save("typography-options",board);board.recycle()
 }
 @Test fun shortSquareSummaryStaysCompleteOnTheCover(){
  val image=photo();val plan=StudioRenderer.plan(c,Project(title=title,summary=summary,ratio=Ratio.SQUARE),image)
  assertEquals("A short summary should not need a second card",1,plan.pages.size)
  assertEquals(summary,plan.pages.single().body)
  assertEquals(title,plan.pages.single().title)
  image.recycle()
 }
 @Test fun textOnlyCoverBalancesSpaceAndSingleCardsHaveNoFolio(){
  for(style in Style.entries){
   val plan=StudioRenderer.plan(c,Project(title=title,summary=summary,style=style))
   val page=plan.pages.first();val top=StudioRenderer.contentTop(c,plan,page)
   val above=top-plan.text.top;val below=plan.text.bottom-top-StudioRenderer.blockHeight(c,plan,page)
   assertEquals("Unbalanced text-only cover: $style",above,below,1f)
   assertFalse(StudioRenderer.showsPageNumber(plan))
  }
  val long=StudioRenderer.plan(c,Project(title=title,article=summary.repeat(80),carousel=true))
  assertTrue(StudioRenderer.showsPageNumber(long))
 }
 @Test fun statisticalFeatureKeepsEveryWordAndDoesNotRepeatTheNumber(){
  for(ratio in Ratio.entries){
   val plan=StudioRenderer.plan(c,Project(title=title,summary=summary,style=Style.STAT,ratio=ratio))
   assertEquals("۴۰ درصد",plan.pages.first().feature)
   val visible=normalized(plan.pages.joinToString(" "){it.feature+" "+it.visualTitle})
   assertEquals(normalized(title).split(" ").sorted(),visible.split(" ").sorted())
   assertEquals(title,plan.pages.joinToString(" "){it.title})
  }
  val noNumber=StudioRenderer.plan(c,Project(title="گزارش بررسی کیفیت خدمات",style=Style.STAT))
  assertTrue(noNumber.pages.all{it.feature.isEmpty()})
  val decimal=StudioRenderer.plan(c,Project(title="تغییر شاخص به −۱۲٫۵ درصد رسید",style=Style.STAT))
  assertEquals("−۱۲٫۵ درصد",decimal.pages.first().feature)
  val word=StudioRenderer.plan(c,Project(title="۲۰ مترجم در برنامه شرکت کردند",style=Style.STAT))
  assertEquals("۲۰",word.pages.first().feature)
  assertTrue(word.pages.first().visualTitle.contains("مترجم"))
 }
 @Test fun quotationAttributionIsSeparateAndNeverTruncated(){
  for(byline in listOf("گویندهٔ نمونه",("شرح گوینده و سمت سازمانی ").repeat(100))){
   val plan=StudioRenderer.plan(c,Project(title=title,summary=summary,style=Style.QUOTE,byline=byline,ratio=Ratio.SQUARE))
   assertEquals(normalized(byline),normalized(plan.pages.joinToString(" "){it.byline}))
   assertEquals(summary,normalized(plan.pages.joinToString(" "){it.body}))
   for(page in plan.pages)assertTrue(StudioRenderer.blockHeight(c,plan,page)<=StudioRenderer.pageBox(plan,page).height()+1)
  }
  assertTrue(Project(title=title,style=Style.QUOTE,byline="گویندهٔ نمونه").caption().contains("گویندهٔ نمونه"))
  val pages=listOf(Page("خبر","",26f,byline="الف".repeat(300)),Page("پایان","",26f))
  assertEquals(0,StudioVideo.pageAt(15_000_000,24000,pages))
 }
 @Test fun sentenceBoundariesAndPersianPhrasesPreserveSource(){
  val text="از شب گذشته و برای مردم ۴۰ درصد افزایش گزارش شد."
  val kept=StudioText.keepPhrases(text)
  assertEquals(text.length,kept.length);assertEquals(text,normalized(kept))
  assertTrue(kept.contains("از\u00a0شب"));assertTrue(kept.contains("۴۰\u00a0درصد"))
  val first="این یک جملهٔ کامل است."
  val rest=" این جملهٔ دوم بسیار طولانی است و برای بررسی حفظ جمله هنگام صفحه‌بندی چندین بار ادامه پیدا می‌کند."
  val font=StudioFont.CAIRO.bodyFace(c)
  val height=StudioRenderer.layout(first+" این جمله",420,30f,font,spacing=1.06f).height.toFloat()
  val parts=StudioRenderer.split(first+rest,420,height,30f,font,1.06f,sentences=true)
  assertEquals(first,parts.first());assertEquals(first+rest,normalized(parts.joinToString(" ")))
  for(part in parts)assertTrue(StudioRenderer.layout(part,420,30f,font,spacing=1.06f).height<=height)
 }
 @Test fun specializedTemplatesHaveTheirOwnVisualFocus(){
  val samples=listOf(
   Project(title="نمونهٔ خبر فوری برای بررسی طراحی کارت",summary="این محتوا آزمایشی است و خبر واقعی محسوب نمی‌شود.",style=Style.BREAKING),
   Project(title="کیفیت خدمات باید با هزینهٔ پرداختی مردم متناسب باشد",byline="گویندهٔ فرضی · متن آزمایشی",style=Style.QUOTE),
   Project(title="در نمونهٔ آزمایشی رضایت کاربران ۴۰ درصد افزایش یافت",summary="این عدد صرفاً برای نمایش قالب آماری است.",style=Style.STAT)
  )
  val board=Bitmap.createBitmap(1128,724,Bitmap.Config.ARGB_8888);val canvas=Canvas(board);canvas.drawColor(Color.rgb(235,237,240))
  val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
  for((i,p)in samples.withIndex()){
   val plan=StudioRenderer.plan(c,p.copy(category="نمونهٔ طراحی"));val bitmap=StudioRenderer.draw(c,plan,0)
   save("focus-${p.style}",bitmap)
   val x=12f+i*372f;canvas.drawBitmap(bitmap,null,RectF(x,12f,x+360,652f),paint)
   val caption=StudioRenderer.layout(p.style.fa,360,20f,StudioFont.ESTEDAD.bodyFace(c),Color.BLACK)
   canvas.save();canvas.translate(x,665f);caption.draw(canvas);canvas.restore();bitmap.recycle()
  }
  save("specialized-templates",board);board.recycle()
 }
 @Test fun realPhotoCardsPreserveTheWholePhotoAndClearAllText(){
  val board=Bitmap.createBitmap(1128,1480,Bitmap.Config.ARGB_8888);val canvas=Canvas(board);canvas.drawColor(Color.rgb(235,237,240))
  val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
  for((row,name)in listOf("webb-portrait.jpg","webb-landscape.png").withIndex()){
   val file=File("src/test/resources/photos/$name");assertTrue("Missing verified photo fixture $name",file.isFile)
   val b=BitmapFactory.decodeFile(file.path,BitmapFactory.Options().apply{inSampleSize=2})
   for((col,style)in listOf(Style.SIGNATURE,Style.EDITORIAL,Style.PHOTO).withIndex()){
    val p=Project(title="آینهٔ تلسکوپ جیمز وب در اتاق تمیز",summary="تصویر آرشیوی برای بررسی قاب و خوانایی کارت.",category="نمونهٔ طراحی",site=if(row==0)"NASA / Chris Gunn"else"NASA",style=style)
    for(ratio in Ratio.entries){
     val plan=StudioRenderer.plan(c,p.copy(ratio=ratio),b);val frame=requireNotNull(plan.photo)
     val fit=StudioRenderer.containedPhotoRect(b.width,b.height,frame)
     assertEquals(b.width.toFloat()/b.height,fit.width()/fit.height(),.001f)
     assertFalse(RectF.intersects(fit,plan.text))
     if(ratio==Ratio.STORY&&plan.pages.size==1){
      val top=StudioRenderer.contentTop(c,plan,plan.pages.first())
      val groupTop=minOf(frame.top,top)
      val groupBottom=maxOf(frame.bottom,top+StudioRenderer.blockHeight(c,plan,plan.pages.first()))
      assertEquals("Photo and copy should balance together",groupTop-plan.fullText.top,plan.fullText.bottom-groupBottom,2f)
     }
     val output=StudioRenderer.draw(c,plan,0,b)
     save("real-${row}-${style}-${ratio}",output)
     if(ratio==Ratio.STORY){
      val x=12f+col*372;val y=12f+row*734
      canvas.drawBitmap(output,null,RectF(x,y,x+360,y+640),paint)
      val label=StudioRenderer.layout(style.fa,360,19f,StudioFont.ESTEDAD.bodyFace(c),Color.BLACK)
      canvas.save();canvas.translate(x,y+650);label.draw(canvas);canvas.restore()
     }
     output.recycle()
    }
   }
   b.recycle()
  }
  save("real-photo-contact-sheet",board);board.recycle()
 }
 @Test fun allFontsStylesRatiosRender(){val photo=photo();for(font in StudioFont.entries)for(style in Style.entries)for(ratio in Ratio.entries)for(hasPhoto in listOf(false,true)){
  val p=Project(title=title,summary=summary,font=font,style=style,ratio=ratio);val plan=try{StudioRenderer.plan(c,p,if(hasPhoto)photo else null)}catch(e:Exception){throw AssertionError("Render failed: $font $style $ratio photo=$hasPhoto",e)}
  assertEquals(normalized(title),normalized(plan.pages.joinToString(" "){it.title}));assertEquals(normalized(summary),normalized(plan.pages.joinToString(" "){it.body}))
  for(i in plan.pages.indices){assertTrue("Text overflow: $font $style $ratio page $i",StudioRenderer.blockHeight(c,plan,plan.pages[i])<=StudioRenderer.pageBox(plan,plan.pages[i]).height()+1f);val b=StudioRenderer.draw(c,plan,i,if(hasPhoto)photo else null);assertEquals(ratio.w,b.width);assertEquals(ratio.h,b.height);if(font==StudioFont.ESTEDAD&&i==0)save("${style}-${ratio}-${hasPhoto}",b);b.recycle()}
 };photo.recycle()}
}
