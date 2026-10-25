package ir.negahjavan.newscard.studio

import android.content.Context
import android.graphics.*
import android.graphics.text.LineBreaker
import android.text.*
import kotlin.math.*

data class Page(val title:String,val body:String,val titleSize:Float,val kind:String="cover")
data class Plan(val project:Project,val width:Int,val height:Int,val text:RectF,val photo:RectF?,val pages:List<Page>,val overlay:Boolean=false)
object StudioRenderer{
 fun layout(text:String,width:Int,size:Float,font:Typeface,color:Int=Color.WHITE,rtl:Boolean=true):StaticLayout{
  val safe=StudioText.keepUnits(text)
  return StaticLayout.Builder.obtain(safe,0,safe.length,TextPaint(Paint.ANTI_ALIAS_FLAG).apply{textSize=size;typeface=font;this.color=color},width.coerceAtLeast(1))
   .setTextDirection(if(rtl)TextDirectionHeuristics.RTL else TextDirectionHeuristics.FIRSTSTRONG_LTR).setAlignment(Layout.Alignment.ALIGN_NORMAL)
   .setBreakStrategy(LineBreaker.BREAK_STRATEGY_BALANCED).setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE).setIncludePad(true).setLineSpacing(0f,1.04f).build()
 }
 fun split(text:String,width:Int,height:Float,size:Float,font:Typeface):List<String>{
  if(text.isBlank())return emptyList()
  val result=mutableListOf<String>();var rest=text.trim()
  while(rest.isNotEmpty()){
   val l=layout(rest,width,size,font)
   if(l.height<=height){result+=rest;break}
   val count=(0 until l.lineCount).takeWhile{l.getLineBottom(it)<=height}.size
   require(count>0){"فضای متن کافی نیست؛ اندازهٔ فونت را کاهش دهید."}
   val end=l.getLineEnd(count-1);require(end>0)
   result+=rest.substring(0,end).trimEnd();rest=rest.substring(end).trimStart()
  }
  return result
 }
 fun plan(c:Context,p:Project,photo:Bitmap?=null,overlay:Boolean=false,width:Int=p.ratio.w,height:Int=p.ratio.h):Plan{
  require(p.title.isNotBlank()){ "تیتر را وارد کنید." }
  require(p.brand.length<=60&&p.site.length<=80&&p.category.length<=30){"نام رسانه، نشانی یا دسته‌بندی بیش از اندازه طولانی است."}
  val s=min(width,height)/360f;val margin=24*s
  val top=if(height>width*1.5f)76*s else 58*s
  val bottom=height-(if(height>width*1.5f)76 else 46)*s
  var box=RectF(margin,top,width-margin,bottom);var image:RectF?=null
  if(photo!=null&&!overlay){
   image=RectF(box)
   if(width>=height||(photo.height>photo.width&&height<width*1.5f)){
    // Preserve portraits in a generous, separate image column.
    image.right=width*.46f;box.left=image.right+18*s
   }else when(p.style){
    Style.PHOTO->{image.left=0f;image.right=width.toFloat();image.bottom=top+(bottom-top)*.52f;box.top=image.bottom+16*s}
    Style.BREAKING->{
     val tw=box.width().toInt()
     val measured=30*s+layout(p.title,tw,p.titleSize*s,p.font.face(c)).height+
      (if(p.summary.isBlank())0 else layout(p.summary,tw,p.bodySize*s,p.font.bodyFace(c)).height)+10*s
     box.bottom=top+measured.coerceIn((bottom-top)*.30f,(bottom-top)*.60f)
     image.top=box.bottom+18*s
    }
    Style.EDITORIAL->{image.bottom=top+(bottom-top)*.38f;box.top=image.bottom+22*s}
    else->{image.bottom=top+(bottom-top)*.46f;box.top=image.bottom+18*s}
   }
  }
  val font=p.font.face(c);val bodyFont=p.font.bodyFace(c);val pad=if(overlay)14*s else 0f
  val w=(box.width()-pad*2).toInt()
  val quote=if(p.style==Style.QUOTE)layout("“",w,32*s,font).height+4*s else 0f
  val ornament=quote
  val reserve=30*s+ornament
  val available=box.height()-2*pad-reserve
  require(available>40*s){"فضای قابل استفاده برای این چیدمان کافی نیست."}
  var size=p.titleSize*s;val target=available*(if(p.summary.isBlank())1f else .60f)
  if(p.auto)while(size>18*s&&layout(p.title,w,size,font).height>target)size=max(18*s,size-s)
  val pages=split(p.title,w,available,size,font).map{Page(it,"",size/s)}.toMutableList()
  var summary=listOf(p.summary,if(p.style==Style.QUOTE)p.byline else "").map{it.trim()}.filter{it.isNotBlank()}.joinToString("\n")
  if(summary.isNotBlank()){
   val last=pages.last();val remaining=available-layout(last.title,w,size,font).height-10*s;val bodySize=p.bodySize*s
   if(remaining>=layout("آ",w,bodySize,bodyFont).height){
    val first=split(summary,w,remaining,bodySize,bodyFont).first();pages[pages.lastIndex]=last.copy(body=first);summary=summary.substring(first.length).trimStart()
   }
   split(summary,w,available,bodySize,bodyFont).forEach{pages+=Page("",it,size/s,"continued")}
  }
  if(p.carousel&&!overlay){
   split(p.article,(width-2*margin).toInt(),bottom-top-30*s,p.bodySize*s,bodyFont).forEach{pages+=Page("",it,size/s,"article")}
   pages+=Page("پایان خبر","برای خبرهای بیشتر همراه ما باشید",min(22f,p.titleSize),"closing")
  }
  return Plan(p,width,height,box,image,pages,overlay)
 }
 fun cropRect(sw:Int,sh:Int,dw:Float,dh:Float,x:Float,y:Float,zoom:Float):RectF{
  val scale=max(dw/sw,dh/sh)*zoom.coerceIn(1f,3f);val w=dw/scale;val h=dh/scale
  val left=(sw-w)*x.coerceIn(0f,1f);val top=(sh-h)*y.coerceIn(0f,1f)
  return RectF(left,top,left+w,top+h)
 }
 private fun photo(canvas:Canvas,b:Bitmap,d:RectF,p:Project,paint:Paint){
  canvas.save();canvas.clipRect(d)
  if(p.crop){val r=cropRect(b.width,b.height,d.width(),d.height(),p.x,p.y,p.zoom);canvas.drawBitmap(b,Rect(r.left.toInt(),r.top.toInt(),r.right.toInt(),r.bottom.toInt()),d,paint)}
  else{
   // Low-resolution backdrop fills letterboxes while preserving the entire foreground photo.
   val longest=max(b.width,b.height)
   val tiny=Bitmap.createScaledBitmap(b,max(1,24*b.width/longest),max(1,24*b.height/longest),true)
   try{
    val back=cropRect(tiny.width,tiny.height,d.width(),d.height(),.5f,.5f,1f)
    canvas.drawBitmap(tiny,Rect(back.left.toInt(),back.top.toInt(),max(back.left.toInt()+1,back.right.toInt()),max(back.top.toInt()+1,back.bottom.toInt())),d,paint)
    paint.color=if(p.style==Style.EDITORIAL||p.style==Style.QUOTE)Color.argb(190,247,243,233)else Color.argb(150,8,24,24)
    canvas.drawRect(d,paint)
   }finally{if(tiny!==b)tiny.recycle()}
   paint.alpha=255
   val sc=min(d.width()/b.width,d.height()/b.height)
   canvas.drawBitmap(b,null,RectF(d.centerX()-b.width*sc/2,d.centerY()-b.height*sc/2,d.centerX()+b.width*sc/2,d.centerY()+b.height*sc/2),paint)
  }
  canvas.restore()
 }
 fun draw(c:Context,plan:Plan,index:Int,photo:Bitmap?=null):Bitmap{
  val p=plan.project;val page=plan.pages[index];val s=min(plan.width,plan.height)/360f
  val light=p.style==Style.EDITORIAL||p.style==Style.QUOTE
  val bg=if(light)Color.rgb(247,243,233)else if(p.style==Style.BREAKING)Color.rgb(23,17,21)else Color.rgb(10,30,29)
  val ink=if(light)Color.rgb(25,40,35)else Color.rgb(246,242,231)
  val accent=if(p.style==Style.BREAKING)Color.rgb(239,91,84)else if(p.style==Style.STAT)Color.rgb(53,174,156)else Color.rgb(184,151,92)
  val font=p.font.face(c);val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
  val bitmap=Bitmap.createBitmap(plan.width,plan.height,Bitmap.Config.ARGB_8888)
  try{
   val canvas=Canvas(bitmap)
   if(!plan.overlay){
    canvas.drawColor(bg)
    if(!light){val glow=when(p.style){Style.BREAKING->Color.rgb(62,26,30);Style.PHOTO->Color.rgb(30,48,60);else->Color.rgb(28,64,54)};paint.shader=RadialGradient(plan.width*.85f,plan.height*.15f,plan.height*.9f,glow,bg,Shader.TileMode.CLAMP);canvas.drawRect(0f,0f,plan.width.toFloat(),plan.height.toFloat(),paint);paint.shader=null}
   }
   val visiblePhoto=photo!=null&&plan.photo!=null&&page.kind!="article"&&page.kind!="closing"
   if(visiblePhoto)this.photo(canvas,requireNotNull(photo),requireNotNull(plan.photo),p,paint)
   val margin=24*s
   fun text(value:String,x:Float,y:Float,w:Int,size:Float,color:Int=ink):Int{
    if(value.isBlank())return 0
    val l=layout(value,w,size,font,color);canvas.save();canvas.translate(x,y);l.draw(canvas);canvas.restore();return l.height
   }
   if(!plan.overlay){
    // Masthead and section rules give the page a deliberate editorial grid.
    paint.color=accent;paint.strokeWidth=0.6f*s
    if(p.showBrand)canvas.drawLine(margin,54*s,plan.width-margin,54*s,paint)
    if(p.style==Style.BREAKING){paint.color=accent;canvas.drawRect(0f,0f,plan.width.toFloat(),4*s,paint)}
    if(p.style==Style.EDITORIAL){paint.color=Color.rgb(224,217,202);canvas.drawRect(0f,0f,8*s,plan.height.toFloat(),paint)}
   }
   if(p.showBrand){
    var size=min(p.brandSize,page.titleSize*.65f)*s
    while(layout(p.brand,(plan.width-margin*2).toInt(),size,font).lineCount>1&&size>7*s)size-=s
    if(plan.overlay){paint.color=Color.argb(195,10,25,23);canvas.drawRoundRect(RectF(margin,26*s,plan.width-margin,54*s),5*s,5*s,paint)}
    text(p.brand,margin,28*s,(plan.width-margin*2).toInt(),size,if(plan.overlay)Color.WHITE else ink)
   }
   var box=RectF(plan.text)
   if(page.kind=="article"||page.kind=="closing")box=RectF(margin,(if(plan.height>plan.width*1.5f)76 else 58)*s,plan.width-margin,plan.height-(if(plan.height>plan.width*1.5f)76 else 46)*s)
   val panel=plan.overlay
   val pad=if(panel)14*s else 0f;val w=(box.width()-pad*2).toInt()
   val title=if(page.title.isBlank())null else layout(page.title,w,page.titleSize*s,font,ink)
   val body=if(page.body.isBlank())null else layout(page.body,w,p.bodySize*s,p.font.bodyFace(c),ink)
   val ornament=if(p.style==Style.QUOTE&&page.kind!="article"&&page.kind!="closing")layout("“",w,32*s,font).height+4*s else 0f
   val gap=if(title!=null&&body!=null)10*s else 0f
   val actual=30*s+ornament+(title?.height?:0)+(body?.height?:0)+gap+2*pad
   val factor=when(p.position){Position.AUTO->if(plan.overlay)1f else if(visiblePhoto||page.kind=="article")0f else when(p.style){Style.EDITORIAL,Style.BREAKING->.12f;Style.PHOTO->.60f;Style.QUOTE->.30f;Style.STAT->.20f;else->.25f};Position.TOP->0f;Position.CENTER->.5f;Position.BOTTOM->1f}
   var y=box.top+max(0f,box.height()-actual)*factor
   if(panel){paint.color=if(light)Color.argb(244,247,243,233)else Color.argb(233,8,25,25);canvas.drawRoundRect(RectF(box.left,y,box.right,y+actual),6*s,6*s,paint)}
   y+=pad
   if(!plan.overlay&&p.style==Style.SIGNATURE){
    paint.color=accent;canvas.drawRect(box.left,y+7*s,box.left+28*s,y+8*s,paint)
   }
   if(!plan.overlay&&p.style==Style.BREAKING){
    paint.color=accent;canvas.drawRect(box.right-pad-66*s,y-2*s,box.right-pad+4*s,y+20*s,paint)
   }
   text(if(page.kind=="article")"ادامهٔ خبر"else if(page.kind=="closing")"پایان"else if(p.style==Style.BREAKING)"خبر فوری" else p.category,box.left+pad,y,w,10*s,if(p.style==Style.BREAKING&&!plan.overlay)Color.WHITE else accent);y+=30*s
   if(p.style==Style.QUOTE&&page.kind!="article"&&page.kind!="closing")y+=text("“",box.left+pad,y,w,32*s,accent)+4*s

   fun drawLayout(l:StaticLayout){canvas.save();canvas.translate(box.left+pad,y);l.draw(canvas);canvas.restore();y+=l.height}
   title?.let{drawLayout(it);y+=gap};body?.let{drawLayout(it)}
   val footer=plan.height-(if(plan.height>plan.width*1.5f)48 else 24)*s
   if(plan.overlay&&(p.showSite||plan.pages.size>1)){paint.color=if(light)Color.argb(235,247,243,233)else Color.argb(210,8,25,25);canvas.drawRoundRect(RectF(margin-5*s,footer-12*s,plan.width-margin+5*s,footer+22*s),4*s,4*s,paint)}
   if(p.showSite||plan.pages.size>1){
    paint.color=accent;paint.strokeWidth=.6f*s
    if(p.style==Style.EDITORIAL||p.style==Style.BREAKING)canvas.drawLine(margin,footer-8*s,plan.width-margin,footer-8*s,paint)
    else canvas.drawLine(plan.width-margin-28*s,footer-8*s,plan.width-margin,footer-8*s,paint)
   }
   if(p.showSite){
    var size=10*s;val fw=(plan.width-margin*2-55*s).toInt()
    while(layout(p.site,fw,size,font).lineCount>1&&size>6*s)size-=s
    val site=layout(p.site,fw,size,p.font.bodyFace(c),ink,rtl=false);canvas.save();canvas.translate(margin,footer);site.draw(canvas);canvas.restore()
   }
   if(plan.pages.size>1){
 val counter=layout("${index+1} / ${plan.pages.size}",(50*s).toInt(),10*s,font,accent,rtl=false)
 canvas.save();canvas.translate(plan.width-margin-50*s,footer);counter.draw(canvas);canvas.restore()
 }
   return bitmap
  }catch(t:Throwable){bitmap.recycle();throw t}
 }
}
