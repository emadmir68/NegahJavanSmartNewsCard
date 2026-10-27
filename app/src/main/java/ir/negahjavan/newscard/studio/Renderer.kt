package ir.negahjavan.newscard.studio

import android.content.Context
import android.graphics.*
import android.graphics.text.LineBreaker
import android.text.*
import kotlin.math.*

data class Page(val title:String,val body:String,val titleSize:Float,val kind:String="cover")
data class Plan(val project:Project,val width:Int,val height:Int,val text:RectF,val photo:RectF?,val pages:List<Page>,val overlay:Boolean=false,val fullText:RectF=RectF(text))
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
   var count=(0 until l.lineCount).takeWhile{l.getLineBottom(it)<=height}.size
   // A fresh paragraph can have different final-line font padding from the source layout.
   while(count>0&&layout(rest.substring(0,l.getLineEnd(count-1)).trimEnd(),width,size,font).height>height)count--
   require(count>0){"فضای متن کافی نیست؛ اندازهٔ فونت را کاهش دهید."}
   val end=l.getLineEnd(count-1);require(end>0)
   result+=rest.substring(0,end).trimEnd();rest=rest.substring(end).trimStart()
  }
  return result
 }
 private fun label(p:Project,kind:String)=when(kind){
  "article","continued","title"->"ادامهٔ خبر"
  "closing"->"همراه ما"
  else->if(p.style==Style.BREAKING)"خبر فوری"else p.category
 }
 private fun reserve(c:Context,p:Project,w:Int,s:Float,kind:String):Float{
  val category=label(p,kind)
  val labelHeight=if(category.isBlank())0f else layout(category,w,9*s,p.font.face(c)).height+12*s
  val quote=if(p.style==Style.QUOTE&&kind=="cover")layout("“",w,26*s,p.font.face(c)).height+2*s else 0f
  return labelHeight+quote
 }
 fun pageBox(plan:Plan,page:Page)=RectF(if(page.kind=="cover")plan.text else plan.fullText)
 // These are the same measured bounds used by the bitmap renderer, including multiline labels.
 fun blockHeight(c:Context,plan:Plan,page:Page):Float{
  val p=plan.project;val s=min(plan.width,plan.height)/360f;val pad=if(plan.overlay)14*s else 0f
  val w=(pageBox(plan,page).width()-2*pad).toInt()
  return reserve(c,p,w,s,page.kind)+2*pad+
   (if(page.title.isBlank())0 else layout(page.title,w,page.titleSize*s,p.font.face(c)).height)+
   (if(page.body.isBlank())0 else layout(page.body,w,p.bodySize*s,p.font.bodyFace(c)).height)+
   (if(page.title.isNotBlank()&&page.body.isNotBlank())12*s else 0f)
 }
 fun plan(c:Context,p:Project,photo:Bitmap?=null,overlay:Boolean=false,width:Int=p.ratio.w,height:Int=p.ratio.h):Plan{
  require(p.title.isNotBlank()){ "تیتر را وارد کنید." }
  require(p.brand.length<=60&&p.site.length<=80&&p.category.length<=30){"نام رسانه، نشانی یا دسته‌بندی بیش از اندازه طولانی است."}
  val s=min(width,height)/360f;val margin=24*s;val story=height>width*1.5f
  val top=(if(story)90 else 66)*s
  val bottom=height-(if(story)84 else 48)*s
  val full=RectF(margin,top,width-margin,bottom)
  val box=RectF(full);var image:RectF?=null
  val font=p.font.face(c);val bodyFont=p.font.bodyFace(c)
  // A wide card has room for columns. Portrait and square cards keep a full-width headline.
  if(photo!=null&&!overlay){
   image=RectF(full)
   if(width>height*1.3f){
    image.right=width*.48f;box.left=image.right+22*s
   }else{
    val gap=(if(p.style==Style.EDITORIAL)20 else 16)*s
    val textWidth=box.width().toInt()
    val measured=reserve(c,p,textWidth,s,"cover")+layout(p.title,textWidth,p.titleSize*s,font).height+
     (if(p.summary.isBlank())0 else min(layout(p.summary,textWidth,p.bodySize*s,bodyFont).height,layout("آ",textWidth,p.bodySize*s,bodyFont).height*3))+12*s
    val minTitleSize=(if(p.auto)min(p.titleSize,16f)else p.titleSize)*s
    val firstLine=layout(p.title,textWidth,minTitleSize,font)
    val oneLine=layout(p.title.substring(0,firstLine.getLineEnd(0)).trimEnd(),textWidth,minTitleSize,font).height
    val minimum=reserve(c,p,textWidth,s,"cover")+oneLine+8*s
    val textHeight=measured.coerceIn(max(full.height()*.27f,minimum),max(full.height()*.59f,minimum))
    val natural=box.width()*photo.height/photo.width
    val preferred=when(p.style){Style.PHOTO->.57f;Style.EDITORIAL->.43f;Style.BREAKING->.55f;Style.QUOTE->.35f;else->.49f}
    val photoHeight=min(natural, min(full.height()*preferred,full.height()-textHeight-gap)).coerceAtLeast(24*s)
    if(p.style==Style.BREAKING){
     box.bottom=full.top+textHeight
     image.top=box.bottom+gap;image.bottom=min(full.bottom,image.top+photoHeight)
    }else{
     if(p.style==Style.PHOTO){image.left=0f;image.right=width.toFloat()}
     image.bottom=full.top+photoHeight;box.top=image.bottom+gap
    }
   }
  }
  val pad=if(overlay)14*s else 0f
  val w=(box.width()-pad*2).toInt();val fw=(full.width()-pad*2).toInt()
  val available=box.height()-2*pad-reserve(c,p,w,s,"cover")
  val fullAvailable=full.height()-2*pad-reserve(c,p,fw,s,"continued")
  require(available>35*s){"فضای قابل استفاده برای این چیدمان کافی نیست."}
  var size=p.titleSize*s
  val bodyReserve=if(p.summary.isBlank())0f else min(available*.40f,layout(p.summary,w,p.bodySize*s,bodyFont).height.toFloat())+12*s
  if(p.auto)while(size>16*s&&layout(p.title,w,size,font).height>available-bodyReserve)size=max(16*s,size-s)
  val first=split(p.title,w,available,size,font).first()
  val pages=mutableListOf(Page(first,"",size/s))
  val rest=p.title.trim().substring(first.length).trimStart()
  split(rest,fw,fullAvailable,size,font).forEach{pages+=Page(it,"",size/s,"title")}
  var summary=listOf(p.summary,if(p.style==Style.QUOTE)p.byline else "").map{it.trim()}.filter{it.isNotBlank()}.joinToString("\n")
  if(summary.isNotBlank()){
   val last=pages.last();val lw=if(last.kind=="cover")w else fw
   val remaining=(if(last.kind=="cover")available else fullAvailable)-layout(last.title,lw,size,font).height-12*s
   if(remaining>=layout("آ",lw,p.bodySize*s,bodyFont).height){
    val firstBody=split(summary,lw,remaining,p.bodySize*s,bodyFont).first()
    pages[pages.lastIndex]=last.copy(body=firstBody);summary=summary.substring(firstBody.length).trimStart()
   }
   split(summary,fw,fullAvailable,p.bodySize*s,bodyFont).forEach{pages+=Page("",it,size/s,"continued")}
  }
  if(p.carousel&&!overlay){
   split(p.article,fw,fullAvailable,p.bodySize*s,bodyFont).forEach{pages+=Page("",it,size/s,"article")}
   if(p.closingText.isNotBlank()){
    // An optional closing uses the same pagination rules; it can never silently truncate.
    split(p.closingText,fw,fullAvailable,p.bodySize*s,bodyFont).forEach{pages+=Page("",it,size/s,"closing")}
   }
  }
  return Plan(p,width,height,box,image,pages,overlay,full)
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
  val muted=if(light)Color.rgb(83,95,87)else Color.rgb(184,199,190)
  val accent=if(p.style==Style.BREAKING)Color.rgb(239,91,84)else if(p.style==Style.STAT)Color.rgb(53,174,156)else Color.rgb(184,151,92)
  val font=p.font.face(c);val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
  val bitmap=Bitmap.createBitmap(plan.width,plan.height,Bitmap.Config.ARGB_8888)
  try{
   val canvas=Canvas(bitmap)
   if(!plan.overlay){
    canvas.drawColor(bg)
    if(!light){val glow=when(p.style){Style.BREAKING->Color.rgb(62,26,30);Style.PHOTO->Color.rgb(30,48,60);else->Color.rgb(28,64,54)};paint.shader=RadialGradient(plan.width*.85f,plan.height*.15f,plan.height*.9f,glow,bg,Shader.TileMode.CLAMP);canvas.drawRect(0f,0f,plan.width.toFloat(),plan.height.toFloat(),paint);paint.shader=null}
   }
   val visiblePhoto=photo!=null&&plan.photo!=null&&page.kind=="cover"
   if(visiblePhoto)this.photo(canvas,requireNotNull(photo),requireNotNull(plan.photo),p,paint)
   val margin=24*s
   fun text(value:String,x:Float,y:Float,w:Int,size:Float,color:Int=ink):Int{
    if(value.isBlank())return 0
    val l=layout(value,w,size,font,color);canvas.save();canvas.translate(x,y);l.draw(canvas);canvas.restore();return l.height
   }
   val story=plan.height>plan.width*1.5f
   val headerY=(if(story)52 else 26)*s
   if(!plan.overlay){
    // A restrained masthead and a consistent green/gold signature leave the headline in charge.
    if(p.showBrand){
     paint.color=if(light)Color.rgb(212,210,199)else Color.rgb(63,82,72);paint.strokeWidth=.45f*s
     canvas.drawLine(margin,headerY+27*s,plan.width-margin,headerY+27*s,paint)
     paint.color=accent;canvas.drawRect(plan.width-margin-2*s,headerY+3*s,plan.width-margin,headerY+17*s,paint)
    }
    if(p.style==Style.SIGNATURE){
     paint.color=Color.argb(100,184,151,92);paint.style=Paint.Style.STROKE;paint.strokeWidth=.5f*s
     canvas.drawRect(12*s,(if(story)40 else 14)*s,plan.width-12*s,plan.height-(if(story)43 else 14)*s,paint)
     paint.style=Paint.Style.FILL
    }
   }
   if(p.showBrand){
    var size=p.brandSize*s;val brandWidth=(plan.width-margin*2-12*s).toInt()
    while(layout(p.brand,brandWidth,size,font).lineCount>1&&size>6*s)size-=.5f*s
    if(plan.overlay){paint.color=Color.argb(210,10,25,23);canvas.drawRoundRect(RectF(margin,headerY-2*s,plan.width-margin,headerY+24*s),3*s,3*s,paint)}
    text(p.brand,margin,headerY,brandWidth,size,if(plan.overlay)Color.WHITE else ink)
   }
   val box=pageBox(plan,page)
   val panel=plan.overlay
   val pad=if(panel)14*s else 0f;val w=(box.width()-pad*2).toInt()
   val title=if(page.title.isBlank())null else layout(page.title,w,page.titleSize*s,font,ink)
   val body=if(page.body.isBlank())null else layout(page.body,w,p.bodySize*s,p.font.bodyFace(c),muted)
   val gap=if(title!=null&&body!=null)12*s else 0f
   val actual=blockHeight(c,plan,page)
   val factor=when(p.position){Position.AUTO->if(plan.overlay)1f else if(visiblePhoto||page.kind=="article"||page.kind=="continued"||page.kind=="title")0f else when(p.style){Style.EDITORIAL,Style.BREAKING->.12f;Style.PHOTO->.28f;Style.QUOTE->.25f;Style.STAT->.20f;else->.20f};Position.TOP->0f;Position.CENTER->.5f;Position.BOTTOM->1f}
   var y=box.top+max(0f,box.height()-actual)*factor
   if(panel){paint.color=if(light)Color.argb(244,247,243,233)else Color.argb(233,8,25,25);canvas.drawRoundRect(RectF(box.left,y,box.right,y+actual),6*s,6*s,paint)}
   y+=pad
   val category=label(p,page.kind)
   if(category.isNotBlank()){
    val categoryLayout=layout(category,w,9*s,font,accent)
    if(!plan.overlay&&p.style==Style.BREAKING&&page.kind=="cover"){
     paint.color=accent
     val badgeWidth=min(w.toFloat(),categoryLayout.getLineWidth(0)+16*s)
     canvas.drawRect(box.right-pad-badgeWidth,y,box.right-pad,y+categoryLayout.height+4*s,paint)
     text(category,box.left+pad-8*s,y+2*s,w,9*s,Color.WHITE)
    }else text(category,box.left+pad,y,w,9*s,accent)
    y+=categoryLayout.height+12*s
   }
   if(p.style==Style.QUOTE&&page.kind=="cover")y+=text("“",box.left+pad,y,w,26*s,accent)+2*s

   fun drawLayout(l:StaticLayout){canvas.save();canvas.translate(box.left+pad,y);l.draw(canvas);canvas.restore();y+=l.height}
   title?.let{drawLayout(it);y+=gap};body?.let{drawLayout(it)}
   val footer=plan.height-(if(plan.height>plan.width*1.5f)63 else 24)*s
   if(plan.overlay&&(p.showSite||plan.pages.size>1)){paint.color=if(light)Color.argb(235,247,243,233)else Color.argb(210,8,25,25);canvas.drawRoundRect(RectF(margin-5*s,footer-12*s,plan.width-margin+5*s,footer+22*s),4*s,4*s,paint)}
   if(p.showSite||plan.pages.size>1){
    paint.color=if(light)Color.rgb(212,210,199)else Color.rgb(63,82,72);paint.strokeWidth=.45f*s
    canvas.drawLine(margin,footer-9*s,plan.width-margin,footer-9*s,paint)
   }
   if(p.showSite){
    var size=9*s;val fw=(plan.width-margin*2-55*s).toInt()
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
