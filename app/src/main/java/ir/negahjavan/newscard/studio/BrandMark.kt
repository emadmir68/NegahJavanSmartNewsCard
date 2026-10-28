package ir.negahjavan.newscard.studio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import ir.negahjavan.newscard.R

/** Transparent vector lockup: colors belong to the card, not to a source bitmap. */
object StudioBrandMark {
 const val ASPECT = 394f / 1264f
 data class Colors(val ink:Int,val accent:Int)

 fun colors(style:Style):Colors=when(style){
  Style.SIGNATURE,Style.PHOTO->Colors(Color.rgb(249,249,246),Color.rgb(239,38,70))
  Style.BREAKING->Colors(Color.rgb(255,255,252),Color.rgb(24,27,33))
  else->Colors(Color.rgb(23,27,33),Color.rgb(217,8,45))
 }

 fun size(p:Project,s:Float):Pair<Float,Float>{
  val scale=s*p.brandSize/12f
  val width=132*scale
  return width to width*ASPECT+(if(p.fullLogo)16*scale else 0f)
 }

 fun draw(c:Context,canvas:Canvas,box:RectF,p:Project,s:Float){
  val colors=colors(p.style)
  val markHeight=box.width()*ASPECT
  val markTop=box.bottom-markHeight
  if(p.fullLogo){
   val tagline=StudioRenderer.layout("پایگاه خبری",box.width().toInt(),8f*s*p.brandSize/12f,StudioFont.ESTEDAD.bodyFace(c),colors.ink)
   canvas.save();canvas.translate(box.left,box.top);tagline.draw(canvas);canvas.restore()
  }
  // Drawing the two independent vector layers keeps counters transparent and allows
  // a readable reversed mark on dark cards and a two-tone white/ink mark on red.
  for((resource,color)in listOf(R.drawable.negahjavan_mark_ink to colors.ink,R.drawable.negahjavan_mark_accent to colors.accent)){
   val drawable=requireNotNull(c.getDrawable(resource)).mutate()
   drawable.setTint(color)
   canvas.save();canvas.translate(box.left,markTop)
   drawable.setBounds(0,0,box.width().toInt(),markHeight.toInt())
   drawable.draw(canvas);canvas.restore()
  }
 }
}
