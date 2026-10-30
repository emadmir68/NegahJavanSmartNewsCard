package ir.negahjavan.newscard.studio

import android.graphics.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ParagraphAlignmentTest {
 private val c get()=RuntimeEnvironment.getApplication()
 private val body="""
  🔹 این یک بند بلند فارسی است که باید در چند سطر خوانا و مرتب نوشته شود و ادامهٔ آن در همان ستون متن قرار بگیرد.
  🔸 بند دوم هم باید با بند قبلی هم‌راستا باشد و کلمات آن بدون تغییر در یک ستون منظم نمایش داده شوند.
  • جملهٔ پایانی کوتاه است.
 """.trimIndent()
 @Test fun wrappedLinesShareOneRightEdgeAndFullLinesAreJustified(){
  val p=Project(title="آزمون",summary=body)
  val l=StudioRenderer.bodyLayout(c,p,body,800,3f)
  val right=(800-ParagraphText.gutter(39f)).toFloat()
  var wrapped=0
  for(i in 0 until l.lineCount){
   assertEquals("Changing text column on line $i",right,l.getLineRight(i),1.5f)
   val end=l.getLineEnd(i)
   if(i<l.lineCount-1&&body.getOrNull(end-1)!='\n'){
    assertEquals("Ragged full line $i",0f,l.getLineLeft(i),1.5f);wrapped++
   }
  }
  assertTrue(wrapped>=3)
  assertTrue("Short final line must not be stretched",l.getLineLeft(l.lineCount-1)>100f)
 }
 @Test fun markersOccupyOneGutterAndOnlyAppearOncePerParagraph(){
  val p=Project(title="آزمون",summary=body)
  val l=StudioRenderer.bodyLayout(c,p,body,800,3f,Color.BLACK,markerColor=Color.RED)
  val b=Bitmap.createBitmap(800,l.height,Bitmap.Config.ARGB_8888)
  l.draw(Canvas(b))
  val markers=mutableListOf<Rect>()
  var active:Rect?=null
  for(y in 0 until b.height){
   val xs=(0 until b.width).filter{x->val color=b.getPixel(x,y);Color.alpha(color)>200&&Color.red(color)>200&&Color.green(color)<40&&Color.blue(color)<40}
   if(xs.isEmpty()){active?.let{markers+=it};active=null}
   else if(active==null)active=Rect(xs.first(),y,xs.last()+1,y+1)
   else{active.left=minOf(active.left,xs.first());active.right=maxOf(active.right,xs.last()+1);active.bottom=y+1}
  }
  active?.let{markers+=it}
  assertEquals(3,markers.size)
  assertEquals(1,markers.map{it.centerX()}.distinct().size)
  assertTrue(markers.all{it.left>800-ParagraphText.gutter(39f)})
  b.recycle()
 }
 @Test fun continuationPagesKeepTheSameColumnAndOriginalWords(){
  val p=Project(title="آزمون",summary=body,ratio=Ratio.SQUARE)
  val repeated=(1..15).joinToString("\n"){body}
  val plan=StudioRenderer.plan(c,p.copy(summary=repeated,auto=false))
  fun words(t:String)=t.trim().replace(Regex("[\\s\\u00a0]+")," ")
  assertEquals(words(repeated),words(plan.pages.joinToString(" "){it.body}))
  assertTrue(plan.pages.size>1)
  for(page in plan.pages){
   val box=StudioRenderer.pageBox(plan,page)
   val l=StudioRenderer.bodyLayout(c,plan.project,page.body,box.width().toInt(),3f,fit=plan.typography)
   for(line in 0 until l.lineCount)if(l.getLineEnd(line)>l.getLineStart(line))assertEquals(box.width()-ParagraphText.gutter(plan.typography.bodySize*3),l.getLineRight(line),1.5f)
   assertTrue(StudioRenderer.blockHeight(c,plan,page)<=box.height()+1)
  }
 }
 @Test fun markerStylingKeepsSourceIndicesAndDoesNotConsumeOrdinaryText(){
  val text="  🔹 قیمت ۶.۳ میلیارد دلار گزارش شد.\n-2 عدد منفی است.\nاین علامت 🔹 در میان متن باقی می‌ماند."
  val styled=ParagraphText.styled(text,39f,Color.RED,true)
  assertEquals(text.length,styled.length)
  assertEquals(StudioText.keepUnits(text),styled.toString())
  assertFalse(ParagraphText.hasBullets("-2 عدد منفی است."))
  assertFalse(ParagraphText.hasBullets("این علامت 🔹 در میان متن است."))
  val plain=StudioRenderer.bodyLayout(c,Project(title="آزمون",summary="یک متن ساده"),"یک متن ساده",800,3f)
  assertEquals(800f,plain.getLineRight(0),1f)
 }
}
