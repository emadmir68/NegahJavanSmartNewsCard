package ir.negahjavan.newscard.studio

import android.content.Context
import android.graphics.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SmartTextFitTest {
 private val c:Context get()=RuntimeEnvironment.getApplication()
 // User-supplied, unverified claims used solely to reproduce a layout defect.
 private val raw="""
  هک گسترده صرافی‌های ارز دیجیتال؛ میلیاردها دلار دارایی در خطر!

  🔹سه صرافی بزرگ ارز دیجیتال، بایننس، بای‌بیت و بیت‌گت، هدف حملات سایبری قرار گرفته‌اند و بخشی از دارایی‌های دیجیتال به سرقت رفته است.
  🔹هک هم‌زمان هر سه صرافی هنوز به‌طور مستقل تأیید نشده است.
  🔹صرافی بایننس رسماً اعلام کرده که در یک حمله سایبری، حدود ۶.۳ میلیارد دلار از دارایی‌های مرتبط با این صرافی تحت تأثیر قرار گرفته است.
  🔹تاکنون حدود سه میلیارد دلار از دارایی‌های مرتبط با حمله مسدود شده و بایننس وعده داده تمام خسارت‌ها را جبران کند.
 """.trimIndent()
 private fun prepared()=StudioText.prepareProject(Project(raw=raw,style=Style.QUOTE))
 private fun normalized(s:String)=s.trim().replace(Regex("[\\s\\u00a0]+")," ")
 private fun save(name:String,b:Bitmap){
  val f=File("build/reports/studio-158/$name.png");f.parentFile.mkdirs()
  f.outputStream().use{assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,it))}
 }
 private fun assertFits(plan:Plan){
  for(page in plan.pages){
   val height=StudioRenderer.blockHeight(c,plan,page)
   assertTrue("Overflow: ${plan.project.style} ${plan.project.ratio} ${page.kind}",height<=StudioRenderer.pageBox(plan,page).height()+1)
   assertTrue(StudioRenderer.contentTop(c,plan,page)+height<=StudioRenderer.pageBox(plan,page).bottom+1)
  }
 }
 @Test fun pastedNewsKeepsAllFourParagraphsInOneStory(){
  val p=prepared()
  assertEquals(raw.substringAfter('\n').trim(),p.summary)
  assertEquals(p.article,p.summary)
  assertEquals(4,p.summary.lines().count{it.startsWith("🔹")})
  for(style in Style.entries){
   val plan=StudioRenderer.plan(c,p.copy(style=style))
   assertEquals("The complete pasted news should fit: $style",1,plan.pages.size)
   assertEquals(p.summary,plan.pages.single().body)
   assertEquals(p.title,plan.pages.single().title)
   assertFalse(StudioRenderer.showsPageNumber(plan));assertFits(plan)
  }
  val current=StudioRenderer.plan(c,p)
  val output=StudioRenderer.draw(c,current,0);save("smart-fit-user-text",output)
  // Reproduce the old prepare button's one-sentence output for comparison.
  val previous=StudioRenderer.plan(c,p.copy(summary=StudioText.summary(p.title,p.article),auto=false))
  val before=StudioRenderer.draw(c,previous,0)
  val board=Bitmap.createBitmap(760,724,Bitmap.Config.ARGB_8888)
  val canvas=Canvas(board);canvas.drawColor(Color.rgb(235,237,240))
  val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
  for((i,b) in listOf(before,output).withIndex()){
   val x=12f+i*376
   canvas.drawBitmap(b,null,RectF(x,12f,x+360,652f),paint)
   val label=if(i==0)"پیشین · فقط یک بند"else"۱.۸.۱ · تمام چهار بند"
   val caption=StudioRenderer.layout(label,360,19f,StudioFont.ESTEDAD.bodyFace(c),Color.BLACK)
   canvas.save();canvas.translate(x,665f);caption.draw(canvas);canvas.restore()
  }
  save("smart-fit-before-after",board)
  File("build/reports/studio-158/smart-fit-measurements.txt").writeText("pages=${current.pages.size}\ntitleSize=${current.pages.single().titleSize}\nbodySize=${current.typography.bodySize}\nbodySpacing=${current.typography.bodySpacing}\ntextGap=${current.typography.textGap}\nbodyParagraphs=${current.pages.single().body.lines().size}\n")
  board.recycle();before.recycle();output.recycle()
 }
 @Test fun automaticFitAdjustsBothFontsAndGapsWithoutChangingSavedSettings(){
  val p=prepared().copy(titleSize=40f,bodySize=13f)
  val plan=StudioRenderer.plan(c,p)
  assertEquals(1,plan.pages.size)
  assertTrue(plan.pages.single().titleSize<p.titleSize)
  assertTrue(plan.typography.bodySize<p.bodySize)
  assertTrue(plan.typography.bodySize>=11f)
  assertTrue(plan.typography.bodySpacing>=1f)
  assertTrue(plan.typography.textGap>=6f)
  assertEquals(p,plan.project);assertFits(plan)
  val manual=StudioRenderer.plan(c,p.copy(auto=false))
  assertTrue(manual.pages.size>1)
  assertTrue(manual.pages.all{it.titleSize==40f})
  assertEquals(13f,manual.typography.bodySize,0f);assertFits(manual)
 }
 @Test fun shortCopyKeepsItsOriginalTypography(){
  val p=Project(title="تیتر کوتاه",summary="یک بند کوتاه برای بررسی خوانایی.")
  val plan=StudioRenderer.plan(c,p)
  assertEquals(p.titleSize,plan.pages.single().titleSize,0f)
  assertEquals(CardTypography(p.bodySize),plan.typography)
 }
 @Test fun allFontsAndRatiosPreserveEveryWordAndStayInBounds(){
  val p=prepared()
  for(font in StudioFont.entries)for(ratio in Ratio.entries)for(overlay in listOf(false,true)){
   val plan=StudioRenderer.plan(c,p.copy(bodyFont=font,ratio=ratio),overlay=overlay)
   assertEquals(normalized(p.title),normalized(plan.pages.joinToString(" "){it.title}))
   assertEquals(normalized(p.summary),normalized(plan.pages.joinToString(" "){it.body}))
   assertTrue(plan.typography.bodySize>=11f);assertFits(plan)
   StudioRenderer.draw(c,plan,0).recycle()
  }
 }
 @Test fun veryLongCopyPaginatesAtReadableSizeAndNeverDropsParagraphs(){
  val p=prepared().copy(summary=(1..18).joinToString("\n\n"){"بخش $it\n${prepared().summary}"})
  for(ratio in Ratio.entries){
   val plan=StudioRenderer.plan(c,p.copy(ratio=ratio))
   assertTrue(plan.pages.size>1)
   assertEquals(p.bodySize,plan.typography.bodySize,0f)
   assertEquals(normalized(p.summary),normalized(plan.pages.joinToString(" "){it.body}))
   assertFits(plan)
  }
 }
 @Test fun onlyUntouchedLegacySuggestionsAreRestoredToFullCopy(){
  val p=prepared();val suggestion=StudioText.summary(p.title,p.article)
  assertTrue(suggestion.isNotBlank());assertNotEquals(p.article,suggestion)
  val legacy=p.copy(summary=suggestion).json().apply{remove("cardTextVersion")}
  val restored=Project.read(legacy)
  assertEquals(p.article,restored.summary)
  assertEquals(p.title,restored.title);assertEquals(p.raw,restored.raw)
  assertEquals("متن ویرایش‌شده",Project.read(legacy.put("summary","متن ویرایش‌شده")).summary)
  assertEquals(suggestion,Project.read(p.copy(summary=suggestion).json()).summary)
  assertEquals(suggestion,Project.read(p.copy(title="تیتر دستی",summary=suggestion).json().apply{remove("cardTextVersion")}).summary)
 }
 @Test fun preparedCarouselDoesNotRepeatTheCompleteArticle(){
  val p=prepared().copy(carousel=true,closingText="همراه نگاه جوان")
  val plan=StudioRenderer.plan(c,p)
  assertFalse(plan.pages.any{it.kind=="article"})
  assertEquals(normalized(p.article),normalized(plan.pages.filter{it.kind!="closing"}.joinToString(" "){it.body}))
  assertEquals("closing",plan.pages.last().kind)
  assertTrue(p.caption().contains(p.article))
  val summary=p.copy(summary=StudioText.summary(p.title,p.article))
  assertEquals(normalized(p.article),normalized(StudioRenderer.plan(c,summary).pages.filter{it.kind=="article"}.joinToString(" "){it.body}))
 }
}
