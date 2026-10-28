package ir.negahjavan.newscard.studio

import android.content.Context
import android.graphics.*
import android.graphics.text.LineBreaker
import android.text.*
import ir.negahjavan.newscard.R
import kotlin.math.*

data class Page(val title:String,val body:String,val titleSize:Float,val kind:String="cover")
data class Plan(val project:Project,val width:Int,val height:Int,val text:RectF,val photo:RectF?,val pages:List<Page>,val overlay:Boolean=false,val fullText:RectF=RectF(text))
object StudioRenderer{
 fun layout(text:String,width:Int,size:Float,font:Typeface,color:Int=Color.WHITE,rtl:Boolean=true,emphasis:Int?=null,spacing:Float=1.03f):StaticLayout{
  val safe=SpannableString(StudioText.keepUnits(text))
  if(emphasis!=null)Regex("[0-9۰-۹٠-٩]+(?:[٫.,][0-9۰-۹٠-٩]+)?(?:\\u00a0(?:درصد|میلیون|میلیارد|تومان|ریال|دلار|٪))*").find(safe)?.let{
   safe.setSpan(android.text.style.ForegroundColorSpan(emphasis),it.range.first,it.range.last+1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
  }
  return StaticLayout.Builder.obtain(safe,0,safe.length,TextPaint(Paint.ANTI_ALIAS_FLAG).apply{textSize=size;typeface=font;this.color=color},width.coerceAtLeast(1))
   .setTextDirection(if(rtl)TextDirectionHeuristics.RTL else TextDirectionHeuristics.FIRSTSTRONG_LTR).setAlignment(Layout.Alignment.ALIGN_NORMAL)
   .setBreakStrategy(LineBreaker.BREAK_STRATEGY_BALANCED).setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE).setIncludePad(true).setLineSpacing(0f,spacing).build()
 }
 fun split(text:String,width:Int,height:Float,size:Float,font:Typeface,spacing:Float=1.03f):List<String>{
  if(text.isBlank())return emptyList()
  val result=mutableListOf<String>();var rest=text.trim()
  while(rest.isNotEmpty()){
   val l=layout(rest,width,size,font,spacing=spacing)
   if(l.height<=height){result+=rest;break}
   var count=(0 until l.lineCount).takeWhile{l.getLineBottom(it)<=height}.size
   // A fresh paragraph can have different final-line font padding from the source layout.
   while(count>0&&layout(rest.substring(0,l.getLineEnd(count-1)).trimEnd(),width,size,font,spacing=spacing).height>height)count--
   require(count>0){"فضای متن کافی نیست؛ اندازهٔ فونت را کاهش دهید."}
   val end=l.getLineEnd(count-1);require(end>0)
   result+=rest.substring(0,end).trimEnd();rest=rest.substring(end).trimStart()
  }
  return result
 }
 private const val BODY_SPACING=1.16f
 private fun bodyLayout(c:Context,p:Project,text:String,w:Int,s:Float,color:Int=Color.WHITE)=layout(text,w,p.bodySize*s,p.bodyFont.bodyFace(c),color,spacing=BODY_SPACING)
 private fun logoSize(p:Project,s:Float):Pair<Float,Float>{
  if(p.fullLogo){val d=64*s*p.brandSize/12f;return d to d}
  val w=126*s*p.brandSize/12f
  return w to w*540f/1264f
 }
 private fun brandHeight(c:Context,p:Project,width:Int,s:Float)=if(p.useLogo)logoSize(p,s).second else max(28*s,brandLayout(c,p,width,s).height.toFloat())
 fun logoBox(plan:Plan):RectF?{
  val p=plan.project;if(!p.showBrand||!p.useLogo)return null
  val s=min(plan.width,plan.height)/360f;val(w,h)=logoSize(p,s)
  val y=(if(plan.height>plan.width*1.5f)52 else 26)*s
  return RectF(plan.width-24*s-w,y,plan.width-24*s,y+h)
 }
 @Volatile private var cachedLogo:Bitmap?=null
 private fun logo(c:Context):Bitmap=cachedLogo?:synchronized(this){cachedLogo?:requireNotNull(BitmapFactory.decodeResource(c.resources,R.drawable.negahjavan_logo,BitmapFactory.Options().apply{inScaled=false})).also{cachedLogo=it}}
 private fun label(p:Project,kind:String)=when(kind){
  "article","continued","title"->"ادامهٔ خبر"
  "closing"->"همراه ما"
  else->if(p.style==Style.BREAKING)"خبر فوری"else p.category
 }
 private fun reserve(c:Context,p:Project,w:Int,s:Float,kind:String):Float{
  val category=label(p,kind)
  val labelHeight=if(category.isBlank())0f else layout(category,(w-22*s).toInt(),9.5f*s,StudioFont.ESTEDAD.bodyFace(c)).height+16*s
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
   (if(page.body.isBlank())0 else bodyLayout(c,p,page.body,w,s).height)+
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
  val top=if(p.showBrand)headerY+brandHeight(c,p,width,s)+26*s else (if(story)64 else 32)*s
  val bottom=height-(if(story)100 else 66)*s
  val full=RectF(margin,top,width-margin,bottom)
  val box=RectF(full);var image:RectF?=null
  val font=p.font.face(c);val bodyFont=p.bodyFont.bodyFace(c)
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
     (if(p.summary.isBlank())0 else min(bodyLayout(c,p,p.summary,textWidth,s).height,bodyLayout(c,p,"آ",textWidth,s).height*3))+16*s
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
  val bodyReserve=if(p.summary.isBlank())0f else min(available*.40f,bodyLayout(c,p,p.summary,w,s).height.toFloat())+16*s
  if(p.auto)while(size>16*s&&layout(p.title,w,size,font).height>available-bodyReserve)size=max(16*s,size-s)
  val first=split(p.title,w,available,size,font).first()
  val pages=mutableListOf(Page(first,"",size/s))
  val rest=p.title.trim().substring(first.length).trimStart()
  split(rest,fw,fullAvailable,size,font).forEach{pages+=Page(it,"",size/s,"title")}
  var summary=listOf(p.summary,if(p.style==Style.QUOTE)p.byline else "").map{it.trim()}.filter{it.isNotBlank()}.joinToString("\n")
  if(summary.isNotBlank()){
   val last=pages.last();val lw=if(last.kind=="cover")w else fw
   val remaining=(if(last.kind=="cover")available else fullAvailable)-layout(last.title,lw,size,font).height-16*s
   if(remaining>=bodyLayout(c,p,"آ",lw,s).height){
    val firstBody=split(summary,lw,remaining,p.bodySize*s,bodyFont,BODY_SPACING).first()
    pages[pages.lastIndex]=last.copy(body=firstBody);summary=summary.substring(firstBody.length).trimStart()
   }
   split(summary,fw,fullAvailable,p.bodySize*s,bodyFont,BODY_SPACING).forEach{pages+=Page("",it,size/s,"continued")}
  }
  if(p.carousel&&!overlay){
   split(p.article,fw,fullAvailable,p.bodySize*s,bodyFont,BODY_SPACING).forEach{pages+=Page("",it,size/s,"article")}
   if(p.closingText.isNotBlank()){
    // An optional closing uses the same pagination rules; it can never silently truncate.
    split(p.closingText,fw,fullAvailable,p.bodySize*s,bodyFont,BODY_SPACING).forEach{pages+=Page("",it,size/s,"closing")}
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
    paint.color=if(p.style==Style.SIGNATURE||p.style==Style.EDITORIAL||p.style==Style.QUOTE)Color.argb(205,246,246,244)else Color.argb(175,12,14,18)
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
   Style.SIGNATURE->Palette(c("#F7F7F3"),c("#F7F7F3"),c("#13151A"),c("#4B4D52"),c("#D9082D"),c("#303238"))
   Style.EDITORIAL->Palette(c("#FFFFFF"),c("#FFFFFF"),c("#191919"),c("#565A60"),c("#C50026"),c("#181A1E"))
   Style.PHOTO->Palette(c("#090D11"),c("#121820"),c("#F9F9F6"),c("#C8CBCD"),c("#FF667A"),c("#F4F4F0"))
   Style.BREAKING->Palette(c("#D9082D"),c("#14171C"),c("#F8F8F6"),c("#C9CDD0"),c("#FF7384"),c("#FFFFFF"))
   Style.QUOTE->Palette(c("#F0EFEC"),c("#FFFFFF"),c("#1F2024"),c("#55565A"),c("#D90930"),c("#27282D"))
   Style.STAT->Palette(c("#E8E9EB"),c("#131820"),c("#FEFEFD"),c("#C5C7CA"),c("#FF687E"),c("#262A32"))
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
  return if(plan.overlay)RectF(box.left,y,box.right,y+actual)else RectF(box.left-10*s,y-12*s,box.right+10*s,y+actual+16*s)
 }
 private fun slash(canvas:Canvas,x:Float,y:Float,w:Float,h:Float,paint:Paint){
  canvas.drawPath(Path().apply{moveTo(x+h*.7f,y);lineTo(x+w,y);lineTo(x+w-h*.7f,y+h);lineTo(x,y+h);close()},paint)
 }
 fun draw(c:Context,plan:Plan,index:Int,photo:Bitmap?=null):Bitmap{
  val p=plan.project;val page=plan.pages[index];val s=min(plan.width,plan.height)/360f;val t=palette(p.style)
  val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG);val font=p.font.face(c)
  val metadata=StudioFont.ESTEDAD.bodyFace(c);val brandRed=Color.rgb(213,8,43)
  val bitmap=Bitmap.createBitmap(plan.width,plan.height,Bitmap.Config.ARGB_8888)
  try{
   val canvas=Canvas(bitmap);val margin=24*s;val story=plan.height>plan.width*1.5f
   val headerY=(if(story)52 else 26)*s
   val headerH=brandHeight(c,p,plan.width,s)
   val blackMasthead=p.style==Style.SIGNATURE
   val headerInk=if(blackMasthead)Color.rgb(249,249,246)else t.chrome
   if(!plan.overlay){
    canvas.drawColor(t.background)
    if(blackMasthead&&p.showBrand){
     paint.color=Color.rgb(17,20,25)
     canvas.drawRect(0f,0f,plan.width.toFloat(),headerY+headerH+14*s,paint)
     paint.color=brandRed;slash(canvas,margin,headerY+headerH+11*s,58*s,6*s,paint)
    }
    if(p.style==Style.EDITORIAL){
     paint.color=Color.rgb(24,27,31);canvas.drawRect(0f,0f,plan.width.toFloat(),5*s,paint)
    }
   }
   val visiblePhoto=photo!=null&&plan.photo!=null&&page.kind=="cover"
   if(visiblePhoto){
    val frame=requireNotNull(plan.photo)
    this.photo(canvas,requireNotNull(photo),frame,p,paint)
    if(p.style!=Style.PHOTO){
     paint.color=if(p.style==Style.BREAKING)Color.WHITE else brandRed
     slash(canvas,frame.left,frame.bottom+4*s,36*s,3*s,paint)
    }
   }
   fun drawLayout(l:StaticLayout,x:Float,y:Float){canvas.save();canvas.translate(x,y);l.draw(canvas);canvas.restore()}
   fun persian(n:Int)=n.toString().padStart(2,'0').map{if(it in '0'..'9')'۰'+(it-'0')else it}.joinToString("")
   if(p.showBrand){
    if(plan.overlay){
     paint.color=if(blackMasthead)Color.rgb(17,20,25)else t.background;paint.alpha=245
     canvas.drawRect(RectF(margin-8*s,headerY-7*s,plan.width-margin+8*s,headerY+headerH+7*s),paint);paint.alpha=255
    }
    val logoRect=logoBox(plan)
    if(logoRect!=null){
     val b=logo(c)
     // Display pixels from the uploaded logo. Keep the untouched full asset in the APK.
     val source=if(p.fullLogo)Rect(0,0,b.width,b.height)else Rect((b.width*140f/1536).toInt(),(b.height*466f/1536).toInt(),(b.width*1404f/1536).toInt(),(b.height*1006f/1536).toInt())
     paint.color=Color.WHITE
     if(p.fullLogo)canvas.drawOval(RectF(logoRect).apply{inset(-2*s,-2*s)},paint)else canvas.drawRect(RectF(logoRect).apply{inset(-5*s,-5*s)},paint)
     canvas.save()
     if(p.fullLogo)canvas.clipPath(Path().apply{addOval(logoRect,Path.Direction.CW)})
     canvas.drawBitmap(b,source,logoRect,paint);canvas.restore()
    }else{
     val brand=brandLayout(c,p,plan.width,s,headerInk)
     drawLayout(brand,118*s,headerY+(headerH-brand.height)/2)
    }
    val folio=layout(persian(index+1),(70*s).toInt(),28*s,StudioFont.ESTEDAD.face(c),headerInk,rtl=false)
    drawLayout(folio,margin,headerY+max(0f,(headerH-folio.height)/2)-3*s)
    if(!plan.overlay&&!blackMasthead){
     paint.color=t.chrome;paint.alpha=110;paint.strokeWidth=.6f*s
     canvas.drawLine(margin,headerY+headerH+10*s,plan.width-margin,headerY+headerH+10*s,paint);paint.alpha=255
    }
   }
   val box=pageBox(plan,page);val pad=if(plan.overlay)14*s else 0f;val w=(box.width()-2*pad).toInt()
   val plate=surfaceBox(c,plan,page)
   val plated=plan.overlay||p.style==Style.PHOTO||p.style==Style.BREAKING||p.style==Style.QUOTE||p.style==Style.STAT
   if(plated){
    paint.color=t.surface
    if(!plan.overlay&&p.style==Style.QUOTE)paint.setShadowLayer(6*s,0f,3*s,Color.argb(30,0,0,0))
    canvas.drawRect(plate,paint);paint.clearShadowLayer()
    paint.color=brandRed
    if(p.style==Style.PHOTO||p.style==Style.STAT)slash(canvas,plate.left,plate.top,42*s,4*s,paint)
    if(p.style==Style.BREAKING||p.style==Style.QUOTE)canvas.drawRect(plate.right-2*s,plate.top,plate.right,plate.bottom,paint)
   }
   var y=contentTop(c,plan,page)+pad
   val category=label(p,page.kind)
   if(category.isNotBlank()){
    val labelWidth=(w-22*s).toInt();val cat=layout(category,labelWidth,9.5f*s,metadata,Color.WHITE)
    val lineWidth=(0 until cat.lineCount).maxOf{cat.getLineWidth(it)}
    val badgeWidth=min(w.toFloat(),lineWidth+22*s)
    paint.color=brandRed
    val left=box.right-pad-badgeWidth
    canvas.drawPath(Path().apply{moveTo(left+5*s,y-3*s);lineTo(box.right-pad,y-3*s);lineTo(box.right-pad,y+cat.height+3*s);lineTo(left,y+cat.height+3*s);close()},paint)
    drawLayout(cat,box.left+pad+11*s,y)
    y+=cat.height+16*s
   }
   if(p.style==Style.QUOTE&&page.kind=="cover"){
    paint.typeface=Typeface.create("serif",Typeface.NORMAL);paint.textSize=72*s;paint.color=brandRed
    canvas.drawText("“",box.right-pad-32*s,y+48*s,paint);y+=38*s
   }
   val title=if(page.title.isBlank())null else layout(page.title,w,page.titleSize*s,font,t.ink,emphasis=if(p.style==Style.STAT)t.accent else null)
   val body=if(page.body.isBlank())null else bodyLayout(c,p,page.body,w,s,t.muted)
   if(title!=null){
    drawLayout(title,box.left+pad,y);y+=title.height
    if(body!=null){
     paint.color=t.accent;paint.strokeWidth=1f*s
     canvas.drawLine(box.right-pad-32*s,y+8*s,box.right-pad,y+8*s,paint);y+=16*s
    }
   }
   if(body!=null)drawLayout(body,box.left+pad,y)
   val footer=plan.height-(if(story)63 else 32)*s
   if(p.showSite||plan.pages.size>1){
    if(plan.overlay){
     paint.color=t.background;paint.alpha=245
     canvas.drawRect(RectF(margin-8*s,footer-13*s,plan.width-margin+8*s,footer+23*s),paint);paint.alpha=255
    }
    paint.color=t.chrome;paint.alpha=110;paint.strokeWidth=.65f*s
    canvas.drawLine(margin,footer-9*s,plan.width-margin,footer-9*s,paint);paint.alpha=255
    paint.color=if(p.style==Style.BREAKING)Color.WHITE else brandRed
    slash(canvas,plan.width-margin-18*s,footer-11*s,18*s,4*s,paint)
    if(p.showSite){
     var size=9*s;val fw=(plan.width-margin*2-70*s).toInt()
     while(layout(p.site,fw,size,metadata,rtl=false).lineCount>1&&size>6*s)size-=.5f*s
     drawLayout(layout(p.site,fw,size,metadata,t.chrome,rtl=false),margin,footer)
    }
    if(plan.pages.size>1)drawLayout(layout("${persian(index+1)} / ${persian(plan.pages.size)}",(60*s).toInt(),9*s,metadata,t.chrome,rtl=false),plan.width-margin-60*s,footer)
   }
   return bitmap
  }catch(t:Throwable){bitmap.recycle();throw t}
 }
}
