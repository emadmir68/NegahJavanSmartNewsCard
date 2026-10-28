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
 @Test fun projectRestoresAllFields(){val p=Project(title=title,summary=summary,article="محتوا",font=StudioFont.AMIRI,titleSize=21f,ratio=Ratio.SQUARE,crop=true,x=.2f,y=.7f,zoom=2f,video="content://video",image="file:///image",videoCrop=true,vy=.1f,showBrand=false,showSite=false,fullHd=false,closingText="متن پایان قابل ویرایش",exports=listOf("content://saved"));ProjectStore.save(c,p);assertEquals(p,ProjectStore.load(c));ProjectStore.save(c,p,true);assertTrue(ProjectStore.list(c).contains(p))}
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
  canvas.drawColor(Color.rgb(227,229,222));val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
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
   val left=12f+(i%3)*372f;val top=12f+(i/3)*734f
   paint.alpha=255;canvas.drawBitmap(bitmap,null,RectF(left,top,left+360,top+640),paint);bitmap.recycle()
   val label=StudioRenderer.layout(style.fa,360,19f,StudioFont.VAZIR.face(c),Color.rgb(38,52,44))
   canvas.save();canvas.translate(left,top+652);label.draw(canvas);canvas.restore()
  }
  save("design-contact-sheet",board)
  val reference=File("../docs/design/previews/726a578ca61bf9772d6c936989f6f749edae4c9a/design-contact-sheet.png")
  if(reference.isFile){
   val before=BitmapFactory.decodeFile(reference.path)
   val comparison=Bitmap.createBitmap(760,724,Bitmap.Config.ARGB_8888);val compare=Canvas(comparison)
   compare.drawColor(Color.rgb(227,229,222))
   compare.drawBitmap(before,Rect(12,12,372,652),RectF(12f,12f,372f,652f),paint)
   compare.drawBitmap(board,Rect(12,12,372,652),RectF(388f,12f,748f,652f),paint)
   for((i,labelText)in listOf("نسخهٔ قبل · ۱.۵.۹","طراحی تازه · ۱.۶.۰").withIndex()){
    val caption=StudioRenderer.layout(labelText,360,19f,StudioFont.VAZIR.face(c),Color.rgb(38,52,44))
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
 @Test fun allFontsStylesRatiosRender(){val photo=photo();for(font in StudioFont.entries)for(style in Style.entries)for(ratio in Ratio.entries)for(hasPhoto in listOf(false,true)){
  val p=Project(title=title,summary=summary,font=font,style=style,ratio=ratio);val plan=try{StudioRenderer.plan(c,p,if(hasPhoto)photo else null)}catch(e:Exception){throw AssertionError("Render failed: $font $style $ratio photo=$hasPhoto",e)}
  assertEquals(normalized(title),normalized(plan.pages.joinToString(" "){it.title}));assertEquals(normalized(summary),normalized(plan.pages.joinToString(" "){it.body}))
  for(i in plan.pages.indices){assertTrue("Text overflow: $font $style $ratio page $i",StudioRenderer.blockHeight(c,plan,plan.pages[i])<=StudioRenderer.pageBox(plan,plan.pages[i]).height()+1f);val b=StudioRenderer.draw(c,plan,i,if(hasPhoto)photo else null);assertEquals(ratio.w,b.width);assertEquals(ratio.h,b.height);if(font==StudioFont.VAZIR&&i==0)save("${style}-${ratio}-${hasPhoto}",b);b.recycle()}
 };photo.recycle()}
}
