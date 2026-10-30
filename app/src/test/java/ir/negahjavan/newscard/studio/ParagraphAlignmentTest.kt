package ir.negahjavan.newscard.studio

import android.graphics.*
import android.text.StaticLayout
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
 private fun inkBounds(bitmap:Bitmap,layout:StaticLayout,line:Int):Rect?{
  var bounds:Rect?=null
  for(y in layout.getLineTop(line).coerceAtLeast(0) until layout.getLineBottom(line).coerceAtMost(bitmap.height))for(x in 0 until bitmap.width){
   val color=bitmap.getPixel(x,y)
   if(Color.alpha(color)>160&&Color.red(color)<80&&Color.green(color)<80&&Color.blue(color)<80){
    if(bounds==null)bounds=Rect(x,y,x+1,y+1)else bounds.union(x,y,x+1,y+1)
   }
  }
  return bounds
 }
 @Test fun wrappedLinesShareOneRightEdgeAndFullLinesAreJustified(){
  val p=Project(title="آزمون",summary=body)
  val l=StudioRenderer.bodyLayout(c,p,body,800,3f,Color.BLACK,markerColor=Color.RED)
  val bitmap=Bitmap.createBitmap(800,l.height,Bitmap.Config.ARGB_8888);l.draw(Canvas(bitmap))
  val right=(800-ParagraphText.gutter(39f)).toFloat()
  var wrapped=0
  for(i in 0 until l.lineCount){
   val ink=requireNotNull(inkBounds(bitmap,l,i))
   // Layout.getLineRight includes the leading marker margin. Inspect the actual
   // letter pixels so a passing test demonstrates the visible text column.
   assertTrue("Changing text column on line $i: ${ink.right} vs $right",ink.right in (right-10).toInt()..(right+2).toInt())
   val end=l.getLineEnd(i)
   if(i<l.lineCount-1&&body.getOrNull(end-1)!='\n'){
    assertTrue("Ragged full line $i: ${ink.left}",ink.left<=10);wrapped++
   }
  }
  assertTrue(wrapped>=3)
  assertTrue("Short final line must not be stretched",requireNotNull(inkBounds(bitmap,l,l.lineCount-1)).left>100)
  bitmap.recycle()
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
   val l=StudioRenderer.bodyLayout(c,plan.project,page.body,box.width().toInt(),3f,Color.BLACK,fit=plan.typography,markerColor=Color.RED)
   val bitmap=Bitmap.createBitmap(l.width,l.height,Bitmap.Config.ARGB_8888);l.draw(Canvas(bitmap))
   val right=box.width()-ParagraphText.gutter(plan.typography.bodySize*3)
   for(line in 0 until l.lineCount)inkBounds(bitmap,l,line)?.let{ink->assertTrue("Continuation moved right edge: ${ink.right} vs $right",ink.right in (right-10).toInt()..(right+2).toInt())}
   bitmap.recycle()
   assertTrue(StudioRenderer.blockHeight(c,plan,page)<=box.height()+1)
  }
 }
 @Test fun markerStylingKeepsSourceIndicesAndDoesNotConsumeOrdinaryText(){
  val text="  🔹 قیمت ۶.۳ میلیارد دلار گزارش شد.\n-2 عدد منفی است.\nاین علامت 🔹 در میان متن باقی می‌ماند."
  val styled=ParagraphText.styled(text,39f,Color.RED,true)
  assertEquals(text.length,styled.length)
  assertEquals(text,styled.toString().replace('\u00a0',' '))
  assertTrue(ParagraphText.styled("این خبر تأیید نشده است.",39f,Color.RED,false).toString().contains("نشده\u00a0است."))
  assertFalse(ParagraphText.hasBullets("-2 عدد منفی است."))
  assertFalse(ParagraphText.hasBullets("این علامت 🔹 در میان متن است."))
  val plain=StudioRenderer.bodyLayout(c,Project(title="آزمون",summary="یک متن ساده"),"یک متن ساده",800,3f)
  assertEquals(800f,plain.getLineRight(0),1f)
 }
}
