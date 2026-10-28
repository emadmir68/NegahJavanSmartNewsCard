package ir.negahjavan.newscard.studio

import android.content.Context
import android.graphics.*
import android.graphics.text.LineBreaker
import android.text.*
import kotlin.math.*

data class Page(val title:String,val body:String,val titleSize:Float,val kind:String="cover")
data class Plan(val project:Project,val width:Int,val height:Int,val text:RectF,val photo:RectF?,val pages:List<Page>,val overlay:Boolean=false,val fullText:RectF=RectF(text))
object StudioRenderer{
 fun layout(text:String,width:Int,size:Float,font:Typeface,color:Int=Color.WHITE,rtl:Boolean=true,emphasis:Int?=null):StaticLayout{
  val safe=SpannableString(StudioText.keepUnits(text))
  if(emphasis!=null)Regex("[0-9۰-۹٠-٩]+(?:[٫.,][0-9۰-۹٠-٩]+)?(?:\\u00a0(?:درصد|میلیون|میلیارد|تومان|ریال|دلار|٪))*").find(safe)?.let{
   safe.setSpan(android.text.style.ForegroundColorSpan(emphasis),it.range.first,it.range.last+1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
  }
  return StaticLayout.Builder.obtain(safe,0,safe.length,TextPaint(Paint.ANTI_ALIAS_FLAG).apply{textSize=size;typeface=font;this.color=color},width.coerceAtLeast(1))
   .setTextDirection(if(rtl)TextDirectionHeuristics.RTL else TextDirectionHeuristics.FIRSTSTRONG_LTR).setAlignment(Layout.Alignment.ALIGN_NORMAL)
   .setBreakStrategy(LineBreaker.BREAK_STRATEGY_BALANCED).setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE).setIncludePad(true).setLineSpacing(0f,1.08f).build()
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
  val labelHeight=if(category.isBlank())0f else layout(category,(w-22*s).toInt(),9*s,p.font.face(c)).height+16*s
  val quote=if(p.style==Style.QUOTE&&kind=="cover")38*s else 0f
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
   (if(page.title.isNotBlank()&&page.body.isNotBlank())16*s else 0f)
 }
 private fun brandLayout(c:Context,p:Project,width:Int,s:Float,color:Int=Color.WHITE):StaticLayout{
  val font=p.font.face(c);val w=(width-142*s).toInt();var size=p.brandSize*s
  while(layout(p.brand,w,size,font).lineCount>1&&size>6*s)size-=.5f*s
  return layout(p.brand,w,size,font,color)
 }
 fun plan(c:Context,p:Project,photo:Bitmap?=null,overlay:Boolean=false,width:Int=p.ratio.w,height:Int=p.ratio.h):Plan{
  require(p.title.isNotBlank()){ "تیتر را وارد کنید." }
  require(p.brand.length<=60&&p.site.length<=80&&p.category.length<=30){"نام رسانه، نشانی یا دسته‌بندی بیش از اندازه طولانی است."}
  val s=min(width,height)/360f;val margin=24*s;val story=height>width*1.5f
  val headerY=(if(story)52 else 26)*s
  val top=if(p.showBrand)headerY+max(28*s,brandLayout(c,p,width,s).height.toFloat())+28*s else (if(story)64 else 32)*s
  val bottom=height-(if(story)100 else 66)*s
  val full=RectF(margin,top,width-margin,bottom)
  val box=RectF(full);var image:RectF?=null
  val font=p.font.face(c);val bodyFont=p.font.bodyFace(c)
  // Use columns only when they preserve a large portrait or when the output itself is wide.
  if(photo!=null&&!overlay){
   image=RectF(full)
   val portraitColumn=!p.crop&&photo.height>photo.width*1.15f&&height<width*1.5f
   if(width>height*1.3f||portraitColumn){
    image.right=width*(if(portraitColumn).43f else .46f);box.left=image.right+18*s
   }else{
    val gap=(if(p.style==Style.EDITORIAL)26 else 24)*s
    box.inset(10*s,0f)
    val textWidth=box.width().toInt()
    val measured=reserve(c,p,textWidth,s,"cover")+layout(p.title,textWidth,p.titleSize*s,font).height+
     (if(p.summary.isBlank())0 else min(layout(p.summary,textWidth,p.bodySize*s,bodyFont).height,layout("آ",textWidth,p.bodySize*s,bodyFont).height*3))+16*s
    val minTitleSize=(if(p.auto)min(p.titleSize,16f)else p.titleSize)*s
    val firstLine=layout(p.title,textWidth,minTitleSize,font)
    val oneLine=layout(p.title.substring(0,firstLine.getLineEnd(0)).trimEnd(),textWidth,minTitleSize,font).height
    val minimum=reserve(c,p,textWidth,s,"cover")+oneLine+8*s
    val textHeight=measured.coerceIn(max(full.height()*.27f,minimum),max(full.height()*.59f,minimum))
    if(p.style==Style.PHOTO){image.left=0f;image.right=width.toFloat()}
    val natural=image.width()*photo.height/photo.width
    val preferred=when(p.style){Style.PHOTO->.62f;Style.EDITORIAL->.45f;Style.BREAKING->.54f;Style.QUOTE->.32f;Style.STAT->.42f;else->.49f}
    val photoHeight=min(natural, min(full.height()*preferred,full.height()-textHeight-gap)).coerceAtLeast(24*s)
    if(p.style==Style.BREAKING){
     box.bottom=full.top+textHeight
     image.top=box.bottom+gap;image.bottom=min(full.bottom,image.top+photoHeight)
    }else{
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
  val bodyReserve=if(p.summary.isBlank())0f else min(available*.40f,layout(p.summary,w,p.bodySize*s,bodyFont).height.toFloat())+16*s
  if(p.auto)while(size>16*s&&layout(p.title,w,size,font).height>available-bodyReserve)size=max(16*s,size-s)
  val first=split(p.title,w,available,size,font).first()
  val pages=mutableListOf(Page(first,"",size/s))
  val rest=p.title.trim().substring(first.length).trimStart()
  split(rest,fw,fullAvailable,size,font).forEach{pages+=Page(it,"",size/s,"title")}
  var summary=listOf(p.summary,if(p.style==Style.QUOTE)p.byline else "").map{it.trim()}.filter{it.isNotBlank()}.joinToString("\n")
  if(summary.isNotBlank()){
   val last=pages.last();val lw=if(last.kind=="cover")w else fw
   val remaining=(if(last.kind=="cover")available else fullAvailable)-layout(last.title,lw,size,font).height-16*s
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
 private data class Palette(val background:Int,val surface:Int,val ink:Int,val muted:Int,val accent:Int,val chrome:Int)
 private fun palette(style:Style):Palette{
  fun c(hex:String)=Color.parseColor(hex)
  return when(style){
   Style.SIGNATURE->Palette(c("#081F1C"),c("#F2ECDF"),c("#193A31"),c("#53665C"),c("#AC854D"),c("#F4ECDD"))
   Style.EDITORIAL->Palette(c("#F1EEE4"),c("#F1EEE4"),c("#203A31"),c("#5F695F"),c("#977344"),c("#203A31"))
   Style.PHOTO->Palette(c("#101A20"),c("#15282A"),c("#F5F0E4"),c("#B5C5BF"),c("#D2B782"),c("#F5F0E4"))
   Style.BREAKING->Palette(c("#171B1D"),c("#22282A"),c("#F6F0E7"),c("#BBC4BE"),c("#E37D69"),c("#F6F0E7"))
   Style.QUOTE->Palette(c("#EAE3D5"),c("#F8F3E9"),c("#303C32"),c("#626A5D"),c("#9B7544"),c("#303C32"))
   Style.STAT->Palette(c("#DAE5DD"),c("#123E34"),c("#F6F2E7"),c("#B9D3C3"),c("#E5C87F"),c("#173E33"))
  }
 }
 fun contentTop(c:Context,plan:Plan,page:Page):Float{
  val p=plan.project;val box=pageBox(plan,page)
  val factor=when(p.position){
   Position.TOP->0f;Position.CENTER->.5f;Position.BOTTOM->1f
   Position.AUTO->if(plan.overlay)1f else if(plan.photo!=null&&page.kind=="cover"||page.kind!="cover")0f else when(p.style){Style.EDITORIAL,Style.BREAKING->.12f;Style.QUOTE->.20f;else->.24f}
  }
  return box.top+max(0f,box.height()-blockHeight(c,plan,page))*factor
 }
 fun surfaceBox(c:Context,plan:Plan,page:Page):RectF{
  val s=min(plan.width,plan.height)/360f;val box=pageBox(plan,page)
  val y=contentTop(c,plan,page);val actual=blockHeight(c,plan,page)
  return if(plan.overlay)RectF(box.left,y,box.right,y+actual)else RectF(box.left-10*s,y-18*s,box.right+10*s,y+actual+20*s)
 }
 private fun diamond(canvas:Canvas,x:Float,y:Float,r:Float,paint:Paint){
  canvas.drawPath(Path().apply{moveTo(x,y-r);lineTo(x+r,y);lineTo(x,y+r);lineTo(x-r,y);close()},paint)
 }
 private fun emblem(canvas:Canvas,x:Float,y:Float,s:Float,color:Int,paint:Paint){
  paint.color=color;paint.style=Paint.Style.STROKE;paint.strokeWidth=.85f*s
  diamond(canvas,x,y,16*s,paint)
  val lens=Path().apply{moveTo(x-10*s,y);quadTo(x,y-11*s,x+10*s,y);quadTo(x,y+11*s,x-10*s,y)}
  canvas.drawPath(lens,paint);canvas.drawCircle(x,y,3.2f*s,paint)
  paint.style=Paint.Style.FILL;canvas.drawCircle(x,y,1.1f*s,paint)
 }
 private fun foil(paint:Paint,x:Float,y:Float,right:Float,bottom:Float){
  paint.alpha=255
  paint.shader=LinearGradient(x,y,right,bottom,intArrayOf(Color.rgb(112,81,43),Color.rgb(223,202,153),Color.rgb(153,113,58),Color.rgb(238,219,177)),floatArrayOf(0f,.38f,.7f,1f),Shader.TileMode.CLAMP)
 }
 private fun background(canvas:Canvas,plan:Plan,t:Palette,s:Float,paint:Paint){
  val w=plan.width.toFloat();val h=plan.height.toFloat();val style=plan.project.style
  canvas.drawColor(t.background)
  if(style==Style.SIGNATURE||style==Style.PHOTO||style==Style.BREAKING){
   val glow=when(style){Style.SIGNATURE->Color.rgb(37,78,60);Style.PHOTO->Color.rgb(39,60,65);else->Color.rgb(62,40,39)}
   paint.shader=RadialGradient(w*.9f,h*.15f,h*.85f,glow,t.background,Shader.TileMode.CLAMP)
   canvas.drawRect(0f,0f,w,h,paint);paint.shader=null
  }
  // Engraved arcs are decoration, never invented data or extra headline copy.
  paint.color=when(style){Style.SIGNATURE->Color.argb(34,211,183,121);Style.PHOTO->Color.argb(24,209,190,149);Style.BREAKING->Color.argb(25,232,120,98);else->Color.argb(22,79,96,73)}
  paint.style=Paint.Style.STROKE;paint.strokeWidth=.6f*s
  for(i in 0..4)canvas.drawCircle(20*s,h-70*s,(76+i*13)*s,paint)
  paint.style=Paint.Style.FILL
  if(style==Style.EDITORIAL){
   paint.color=Color.argb(25,139,116,73)
   canvas.drawRect(12*s,14*s,14*s,h-14*s,paint)
  }
  if(style==Style.SIGNATURE){
   foil(paint,0f,0f,w,h);paint.style=Paint.Style.STROKE;paint.strokeWidth=.7f*s
   canvas.drawRect(12*s,(if(h>w*1.5f)38 else 12)*s,w-12*s,h-(if(h>w*1.5f)38 else 12)*s,paint)
   paint.shader=null;paint.style=Paint.Style.FILL
  }
  if(style==Style.BREAKING){paint.color=t.accent;canvas.drawRect(w-8*s,0f,w,h,paint)}
 }
 private fun photoFinish(canvas:Canvas,frame:RectF,style:Style,t:Palette,s:Float,paint:Paint){
  if(style==Style.PHOTO)return
  paint.style=Paint.Style.STROKE;paint.strokeWidth=.8f*s
  paint.color=if(style==Style.STAT)Color.rgb(78,116,93)else t.accent
  if(style==Style.SIGNATURE||style==Style.QUOTE)foil(paint,frame.left,frame.top,frame.right,frame.bottom)
  canvas.drawRect(RectF(frame).apply{inset(-3*s,-3*s)},paint)
  paint.shader=null;paint.style=Paint.Style.FILL
  // Corner bars survive thumbnail size, unlike a hairline by itself.
  val length=26*s;paint.color=if(style==Style.STAT)Color.rgb(36,82,63)else t.accent
  canvas.drawRect(frame.right-length,frame.top-3*s,frame.right+3*s,frame.top,paint)
  canvas.drawRect(frame.right,frame.top-3*s,frame.right+3*s,frame.top+length,paint)
  canvas.drawRect(frame.left-3*s,frame.bottom,frame.left+length,frame.bottom+3*s,paint)
 }
 fun draw(c:Context,plan:Plan,index:Int,photo:Bitmap?=null):Bitmap{
  val p=plan.project;val page=plan.pages[index];val s=min(plan.width,plan.height)/360f;val t=palette(p.style)
  val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG);val font=p.font.face(c)
  val bitmap=Bitmap.createBitmap(plan.width,plan.height,Bitmap.Config.ARGB_8888)
  try{
   val canvas=Canvas(bitmap);val margin=24*s;val story=plan.height>plan.width*1.5f
   if(!plan.overlay)background(canvas,plan,t,s,paint)
   val visiblePhoto=photo!=null&&plan.photo!=null&&page.kind=="cover"
   if(visiblePhoto){
    val frame=requireNotNull(plan.photo)
    this.photo(canvas,requireNotNull(photo),frame,p,paint)
    photoFinish(canvas,frame,p.style,t,s,paint)
   }
   fun drawLayout(l:StaticLayout,x:Float,y:Float){canvas.save();canvas.translate(x,y);l.draw(canvas);canvas.restore()}
   fun persian(n:Int)=n.toString().padStart(2,'0').map{if(it in '0'..'9')'۰'+(it-'0')else it}.joinToString("")
   val headerY=(if(story)52 else 26)*s
   val masthead=brandLayout(c,p,plan.width,s,t.chrome)
   if(p.showBrand){
    val headerH=max(28*s,masthead.height.toFloat())
    if(plan.overlay){
     paint.color=t.background;paint.alpha=242
     canvas.drawRoundRect(RectF(margin-8*s,headerY-7*s,plan.width-margin+8*s,headerY+headerH+6*s),3*s,3*s,paint);paint.alpha=255
    }
    emblem(canvas,plan.width-margin-16*s,headerY+14*s,s,if(p.style==Style.STAT)t.chrome else t.accent,paint)
    drawLayout(masthead,margin+52*s,headerY+(headerH-masthead.height)/2)
    paint.typeface=Typeface.create("serif",Typeface.NORMAL);paint.textSize=28*s;paint.color=t.chrome;paint.alpha=160
    canvas.drawText(persian(index+1),margin,headerY+24*s,paint);paint.alpha=255
    if(!plan.overlay){
     paint.color=t.chrome;paint.alpha=60;paint.strokeWidth=.65f*s
     canvas.drawLine(margin,headerY+headerH+10*s,plan.width-margin,headerY+headerH+10*s,paint);paint.alpha=255
     paint.color=if(p.style==Style.STAT)t.chrome else t.accent
     canvas.drawRect(plan.width-margin-40*s,headerY+headerH+9*s,plan.width-margin,headerY+headerH+11*s,paint)
    }
   }
   val box=pageBox(plan,page);val pad=if(plan.overlay)14*s else 0f;val w=(box.width()-2*pad).toInt()
   val plate=surfaceBox(c,plan,page)
   val plated=p.style!=Style.EDITORIAL||plan.overlay
   if(plated){
    paint.color=t.surface
    if(!plan.overlay&&(p.style==Style.SIGNATURE||p.style==Style.QUOTE||p.style==Style.PHOTO))paint.setShadowLayer(9*s,0f,5*s,Color.argb(65,0,0,0))
    canvas.drawRoundRect(plate,2*s,2*s,paint);paint.clearShadowLayer()
    if(p.style==Style.SIGNATURE){
     foil(paint,plate.left,plate.top,plate.right,plate.bottom);paint.style=Paint.Style.STROKE;paint.strokeWidth=.65f*s
     canvas.drawRect(RectF(plate).apply{inset(4*s,4*s)},paint);paint.shader=null;paint.style=Paint.Style.FILL
    }else if(p.style==Style.QUOTE){
     paint.color=t.accent;canvas.drawRect(plate.right-3*s,plate.top,plate.right,plate.bottom,paint)
    }else if(p.style==Style.STAT||p.style==Style.PHOTO){
     paint.color=t.accent;canvas.drawRect(plate.left,plate.top,plate.left+44*s,plate.top+3*s,paint)
    }else if(p.style==Style.BREAKING){
     paint.color=t.accent;canvas.drawRect(plate.right-3*s,plate.top,plate.right,plate.bottom,paint)
    }
   }
   var y=contentTop(c,plan,page)+pad
   val category=label(p,page.kind)
   if(category.isNotBlank()){
    val labelWidth=(w-22*s).toInt()
    val categoryInk=if(p.style==Style.SIGNATURE||p.style==Style.EDITORIAL||p.style==Style.QUOTE)t.ink else t.accent
    val cat=layout(category,labelWidth,9*s,font,categoryInk)
    val lineWidth=(0 until cat.lineCount).maxOf{cat.getLineWidth(it)}
    val badgeWidth=min(w.toFloat(),lineWidth+20*s)
    if(p.style==Style.BREAKING&&page.kind=="cover"){
     paint.color=t.accent;canvas.drawRect(box.right-pad-badgeWidth,y-3*s,box.right-pad,y+cat.height+3*s,paint)
     drawLayout(layout(category,labelWidth,9*s,font,Color.rgb(28,30,30)),box.left+pad+11*s,y)
    }else{
     drawLayout(cat,box.left+pad+22*s,y)
     paint.color=t.accent
     diamond(canvas,box.right-pad-lineWidth-10*s,y+cat.height/2,2.2f*s,paint)
     if(w-badgeWidth>32*s){
      paint.alpha=80;paint.strokeWidth=.65f*s
      canvas.drawLine(box.left+pad,y+cat.height/2,box.right-pad-badgeWidth-8*s,y+cat.height/2,paint);paint.alpha=255
     }
    }
    y+=cat.height+16*s
   }
   if(p.style==Style.QUOTE&&page.kind=="cover"){
    paint.typeface=Typeface.create("serif",Typeface.NORMAL);paint.textSize=72*s;paint.color=t.accent
    canvas.drawText("“",box.right-pad-32*s,y+48*s,paint)
    y+=38*s
   }
   val title=if(page.title.isBlank())null else layout(page.title,w,page.titleSize*s,font,t.ink,emphasis=if(p.style==Style.STAT)t.accent else null)
   val body=if(page.body.isBlank())null else layout(page.body,w,p.bodySize*s,p.font.bodyFace(c),t.muted)
   if(title!=null){
    drawLayout(title,box.left+pad,y);y+=title.height
    if(body!=null){
     paint.color=t.accent;paint.strokeWidth=1f*s
     canvas.drawLine(box.right-pad-24*s,y+8*s,box.right-pad,y+8*s,paint);y+=16*s
    }
   }
   if(body!=null)drawLayout(body,box.left+pad,y)
   val footer=plan.height-(if(story)63 else 32)*s
   if(p.showSite||plan.pages.size>1){
    if(plan.overlay){
     paint.color=t.background;paint.alpha=242;canvas.drawRoundRect(RectF(margin-8*s,footer-13*s,plan.width-margin+8*s,footer+23*s),3*s,3*s,paint);paint.alpha=255
    }
    paint.color=t.chrome;paint.alpha=65;paint.strokeWidth=.7f*s
    canvas.drawLine(margin,footer-9*s,plan.width-margin,footer-9*s,paint);paint.alpha=255
    paint.color=if(p.style==Style.STAT)t.chrome else t.accent
    diamond(canvas,plan.width-margin-3*s,footer-9*s,2.4f*s,paint)
    if(p.showSite){
     var size=9*s;val fw=(plan.width-margin*2-70*s).toInt()
     while(layout(p.site,fw,size,p.font.bodyFace(c),rtl=false).lineCount>1&&size>6*s)size-=.5f*s
     val site=layout(p.site,fw,size,p.font.bodyFace(c),t.chrome,rtl=false)
     drawLayout(site,margin,footer)
    }
    if(plan.pages.size>1){
     val counter=layout("${persian(index+1)} / ${persian(plan.pages.size)}",(60*s).toInt(),9*s,font,t.chrome,rtl=false)
     drawLayout(counter,plan.width-margin-60*s,footer)
    }
   }
   return bitmap
  }catch(t:Throwable){bitmap.recycle();throw t}
 }
}
