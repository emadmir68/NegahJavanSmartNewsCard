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
 @Test fun manualAndAutomaticSize(){for(r in Ratio.entries){val p=Project(title=title,summary=summary,ratio=r,auto=false,titleSize=22f);assertTrue(StudioRenderer.plan(c,p).pages.all{it.titleSize==22f});assertTrue(StudioRenderer.plan(c,p.copy(auto=true)).pages.all{it.titleSize<=22f})}}
 @Test fun preservesLongInputWithoutEllipsis(){val t=(1..30).joinToString(" "){"تیتر شماره $it بدون حذف"};val b=(1..80).joinToString(" "){"جزئیات خبر $it برای خواندن کامل"};for(r in Ratio.entries){val plan=StudioRenderer.plan(c,Project(title=t,summary=b,ratio=r));assertEquals(normalized(t),normalized(plan.pages.joinToString(" "){it.title}));assertEquals(normalized(b),normalized(plan.pages.joinToString(" "){it.body}));assertTrue(plan.pages.size>1)}}
 @Test fun whitespaceDoesNotLoseSummaryWords(){val b="  "+summary.repeat(20)+"   ";val plan=StudioRenderer.plan(c,Project(title=title,summary=b,ratio=Ratio.SQUARE));assertEquals(normalized(b),normalized(plan.pages.joinToString(" "){it.body}))}
 @Test fun numberUnitsRemainTogetherAndIndicesStable(){val t="۴۰ درصد و ۱۲ میلیون تومان افزایش";val got=StudioText.keepUnits(t);assertEquals(t.length,got.length);assertTrue(got.contains("۴۰\u00a0درصد"));assertTrue(got.contains("۱۲\u00a0میلیون\u00a0تومان"))}
 @Test fun captionUsesEditedTitleWithoutOldHeadline(){val(t,b)=StudioText.prepare("تیتر قبلی\nمتن اصلی این خبر برای انتشار کامل است.");val p=Project(raw=t+"\n"+b,title="تیتر تازه",article=b);assertFalse(p.caption().contains("تیتر قبلی"));assertTrue(p.caption().contains("تیتر تازه"));assertTrue(p.caption().contains(b))}
 @Test fun suggestedSummaryIsAnExactSourceSentence(){val body="گزارش عمومی درباره موضوع دیگری منتشر شده است. قیمت اینترنت ۴۰ درصد افزایش یافته است.";assertEquals("قیمت اینترنت ۴۰ درصد افزایش یافته است.",StudioText.summary(title,body))}
 @Test fun portraitIsWideEnoughInSquare(){val b=photo();val plan=StudioRenderer.plan(c,Project(title=title,summary=summary,ratio=Ratio.SQUARE),b);val box=requireNotNull(plan.photo);assertTrue(box.width()>400);assertTrue(plan.text.left>box.right);b.recycle()}
 @Test fun cropStaysInsideSourceAndAllowsPanZoom(){val a=StudioRenderer.cropRect(1920,1080,1080f,1920f,0f,0f,1f);val b=StudioRenderer.cropRect(1920,1080,1080f,1920f,1f,1f,2f);assertEquals(0f,a.left,.01f);assertTrue(b.width()<a.width());assertTrue(b.right<=1920&&b.bottom<=1080);assertFalse(Project().videoCrop)}
 @Test fun carouselPreservesArticleAndHasClosing(){val article=(1..60).joinToString(" "){"متن کامل خبر شماره $it"};for(style in Style.entries){val plan=StudioRenderer.plan(c,Project(title=title,summary=summary,article=article,carousel=true,ratio=Ratio.POST,style=style));assertEquals("cover",plan.pages.first().kind);assertEquals("closing",plan.pages.last().kind);assertEquals(article,normalized(plan.pages.filter{it.kind=="article"}.joinToString(" "){it.body}));for(i in listOf(0,1,plan.pages.lastIndex)){val b=StudioRenderer.draw(c,plan,i);save("carousel-$style-$i",b);b.recycle()}}}
 @Test fun projectRestoresAllFields(){val p=Project(title=title,summary=summary,article="محتوا",font=StudioFont.AMIRI,titleSize=21f,ratio=Ratio.SQUARE,crop=true,x=.2f,y=.7f,zoom=2f,video="content://video",image="file:///image",videoCrop=true,vy=.1f,showBrand=false,showSite=false,fullHd=false,exports=listOf("content://saved"));ProjectStore.save(c,p);assertEquals(p,ProjectStore.load(c));ProjectStore.save(c,p,true);assertTrue(ProjectStore.list(c).contains(p))}
 @Test fun videoPaginationAndTooShortDuration(){val pages=List(3){Page("خبر","",24f)};assertEquals(0,StudioVideo.pageAt(0,9000,pages));assertEquals(1,StudioVideo.pageAt(3000000,9000,pages));assertEquals(2,StudioVideo.pageAt(9000000,9000,pages));val plan=StudioRenderer.plan(c,Project(title=(title+" ").repeat(20)),overlay=true);assertThrows(IllegalArgumentException::class.java){StudioVideo.validate(plan,StudioVideo.Info(1920,1080,1000))}}
 @Test fun videoGivesLongerPagesMoreReadingTime(){val pages=listOf(Page("الف".repeat(300),"",24f),Page("پایان","",24f));assertEquals(0,StudioVideo.pageAt(15_000_000,24000,pages));assertEquals(1,StudioVideo.pageAt(23_900_000,24000,pages))}
 @Test fun videoTopReallyStartsNearTop(){for(pos in Position.entries){val plan=StudioRenderer.plan(c,Project(title=title,summary=summary,position=pos),overlay=true);val b=StudioRenderer.draw(c,plan,0);save("video-$pos",b);if(pos==Position.TOP)assertTrue(Color.alpha(b.getPixel(540,300))>150);if(pos==Position.BOTTOM)assertEquals(0,Color.alpha(b.getPixel(540,300)));b.recycle()}}
 @Test fun allFontsStylesRatiosRender(){val photo=photo();for(font in StudioFont.entries)for(style in Style.entries)for(ratio in Ratio.entries)for(hasPhoto in listOf(false,true)){
  val p=Project(title=title,summary=summary,font=font,style=style,ratio=ratio);val plan=StudioRenderer.plan(c,p,if(hasPhoto)photo else null)
  assertEquals(normalized(title),normalized(plan.pages.joinToString(" "){it.title}));assertEquals(normalized(summary),normalized(plan.pages.joinToString(" "){it.body}))
  for(i in plan.pages.indices){val b=StudioRenderer.draw(c,plan,i,if(hasPhoto)photo else null);assertEquals(ratio.w,b.width);assertEquals(ratio.h,b.height);if(font==StudioFont.VAZIR&&i==0)save("${style}-${ratio}-${hasPhoto}",b);b.recycle()}
 };photo.recycle()}
}
