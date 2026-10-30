package ir.negahjavan.newscard.studio

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import android.text.style.LineHeightSpan
import android.text.style.ReplacementSpan
import kotlin.math.ceil

/** Display-only paragraph styling: source characters and pagination offsets stay intact. */
object ParagraphText {
 private val marker=Regex("^[ \\t]*(?:🔹|🔸|🔺|🔻|♦\\uFE0F?|▪\\uFE0F?|▫\\uFE0F?|[•●◦‣]|[-–—](?=[ \\t]))[ \\t]*")
 private val tail=Regex("(?m)(\\S+)([ \\t]+)(\\S+)(?=[ \\t]*\\r?$)")
 fun hasBullets(text:String)=text.lineSequence().any{marker.containsMatchIn(it)}
 fun gutter(size:Float)=ceil(size*.9f).toInt()
 fun styled(text:String,size:Float,color:Int,listGutter:Boolean):SpannableString {
  // Short function words may wrap normally in prose. Keep only numbers and units
  // together so full-width lines do not acquire artificial gaps.
  val prose=tail.replace(StudioText.keepUnits(text)){match->
   val last=match.groupValues[3].trimEnd('.','!','؟','?','؛','،','»','”','"')
   if(last.length in 1..6&&match.groupValues[1].length<=14)match.groupValues[1]+match.groupValues[2].replace(' ','\u00a0').replace('\t','\u00a0')+match.groupValues[3]else match.value
  }
  val result=SpannableString(prose)
  var start=0
  while(start<text.length){
   val next=text.indexOf('\n',start).let{if(it<0)text.length else it+1}
   val paragraph=text.substring(start,next)
   val prefix=marker.find(paragraph)
   val hidden=prefix?.value?.length?:paragraph.takeWhile{it==' '||it=='\t'}.length
   if(hidden>0)result.setSpan(HiddenPrefix(),start,start+hidden,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
   if(listGutter)result.setSpan(MarkerMargin(gutter(size),if(prefix!=null)start else -1,size*.095f,color),start,next,Spanned.SPAN_PARAGRAPH)
   if(start>0&&paragraph.isNotBlank())result.setSpan(ParagraphSpace(start,ceil(size*.24f).toInt()),start,next,Spanned.SPAN_PARAGRAPH)
   start=next
  }
  return result
 }
 private class HiddenPrefix:ReplacementSpan(){
  override fun getSize(paint:Paint,text:CharSequence,start:Int,end:Int,fm:Paint.FontMetricsInt?):Int{
   if(fm!=null)paint.getFontMetricsInt(fm)
   return 0
  }
  override fun draw(canvas:Canvas,text:CharSequence,start:Int,end:Int,x:Float,top:Int,y:Int,bottom:Int,paint:Paint){}
 }
 private class MarkerMargin(val width:Int,val markerStart:Int,val radius:Float,val color:Int):LeadingMarginSpan {
  override fun getLeadingMargin(first:Boolean)=width
  override fun drawLeadingMargin(c:Canvas,p:Paint,x:Int,dir:Int,top:Int,baseline:Int,bottom:Int,text:CharSequence,start:Int,end:Int,first:Boolean,layout:Layout){
   if(!first||start!=markerStart)return
   val dot=Paint(p).apply{this.color=this@MarkerMargin.color;style=Paint.Style.FILL;isAntiAlias=true}
   val centerY=baseline+(p.fontMetrics.ascent+p.fontMetrics.descent)/2
   c.drawCircle(x+dir*width*.28f,centerY,radius,dot)
  }
 }
 private class ParagraphSpace(val paragraphStart:Int,val gap:Int):LineHeightSpan {
  override fun chooseHeight(text:CharSequence,start:Int,end:Int,spanstartv:Int,v:Int,fm:Paint.FontMetricsInt){
   if(start==paragraphStart){fm.ascent-=gap;fm.top-=gap}
  }
 }
}
