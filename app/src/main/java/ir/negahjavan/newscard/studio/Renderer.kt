package ir.negahjavan.newscard.studio

import android.content.Context
import android.graphics.*
import android.graphics.text.LineBreaker
import android.text.*
import kotlin.math.*

// Source text stays intact for captions and pagination. Feature/visualTitle are a
// measured presentation of the same words, used only by the statistical cover.
data class Page(val title:String,val body:String,val titleSize:Float,val kind:String="cover",val feature:String="",val visualTitle:String=title,val byline:String="")
data class Plan(val project:Project,val width:Int,val height:Int,val text:RectF,val photo:RectF?,val pages:List<Page>,val overlay:Boolean=false,val fullText:RectF=RectF(text))
object StudioRenderer{
 private const val HEAD_SPACING=.92f
 private const val BODY_SPACING=1.06f
 private const val TEXT_GAP=12f
 fun layout(text:String,width:Int,size:Float,font:Typeface,color:Int=Color.WHITE,rtl:Boolean=true,emphasis:Int?=null,spacing:Float=HEAD_SPACING,center:Boolean=false):StaticLayout{
  val safe=SpannableString(StudioText.keepPhrases(text))
  if(emphasis!=null)Regex("[0-9۰-۹٠-٩]+(?:[٫.,][0-9۰-۹٠-٩]+)?(?:\\u00a0(?:درصد|میلیون|میلیارد|تومان|ریال|دلار|٪))*").find(safe)?.let{
   safe.setSpan(android.text.style.ForegroundColorSpan(emphasis),it.range.first,it.range.last+1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
  }
  return StaticLayout.Builder.obtain(safe,0,safe.length,TextPaint(Paint.ANTI_ALIAS_FLAG).apply{textSize=size;typeface=font;this.color=color},width.coerceAtLeast(1))
   .setTextDirection(if(rtl)TextDirectionHeuristics.RTL else TextDirectionHeuristics.FIRSTSTRONG_LTR)
   .setAlignment(if(center)Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
   .setBreakStrategy(LineBreaker.BREAK_STRATEGY_BALANCED).setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
   .setIncludePad(true).setLineSpacing(0f,spacing).build()
 }
 private val sentenceEnd=Regex("[.!?؟؛](?:[»”\"])?(?:\\s+|$)|\\n+")
 fun split(text:String,width:Int,height:Float,size:Float,font:Typeface,spacing:Float=HEAD_SPACING,sentences:Boolean=false):List<String>{
  if(text.isBlank())return emptyList()
  val result=mutableListOf<String>();var rest=text.trim()
  while(rest.isNotEmpty()){
   val l=layout(rest,width,size,font,spacing=spacing)
   if(l.height<=height){result+=rest;break}
   var count=(0 until l.lineCount).takeWhile{l.getLineBottom(it)<=height}.size
   while(count>0&&layout(rest.substring(0,l.getLineEnd(count-1)).trimEnd(),width,size,font,spacing=spacing).height>height)count--
   require(count>0){"فضای متن کافی نیست؛ اندازهٔ فونت را کاهش دهید."}
   var end=l.getLineEnd(count-1)
   if(sentences){
    val boundary=sentenceEnd.findAll(rest.take(end)).lastOrNull()?.range?.last?.plus(1)
    if(boundary!=null&&boundary>=end*.45f)end=boundary
   }
   require(end>0);result+=rest.substring(0,end).trimEnd();rest=rest.substring(end).trimStart()
  }
  return result
 }
 private fun bodyLayout(c:Context,p:Project,text:String,w:Int,s:Float,color:Int=Color.WHITE)=layout(text,w,p.bodySize*s,p.bodyFont.bodyFace(c),color,spacing=BODY_SPACING)
 private fun creditLayout(c:Context,p:Project,text:String,w:Int,s:Float,color:Int=Color.WHITE)=layout(text,w,p.bodySize*.92f*s,p.bodyFont.bodyFace(c),color,spacing=BODY_SPACING)
 private fun headerY(width:Int,height:Int,s:Float)=(if(height>width*1.5f)36 else 18)*s
 private fun brandHeight(c:Context,p:Project,width:Int,s:Float)=if(p.useLogo)StudioBrandMark.size(p,s).second else max(22*s,brandLayout(c,p,width,s).height.toFloat())
 fun logoBox(plan:Plan):RectF?{
  val p=plan.project;if(!p.showBrand||!p.useLogo)return null
  val s=min(plan.width,plan.height)/360f;val(w,h)=StudioBrandMark.size(p,s)
  val y=headerY(plan.width,plan.height,s)
  return RectF(plan.width-24*s-w,y,plan.width-24*s,y+h)
 }
 fun showsPageNumber(plan:Plan)=plan.pages.size>1
 private fun label(p:Project,kind:String)=when(kind){
  "article","continued","title","attribution"->"ادامهٔ خبر"
  "closing"->"همراه ما"
  else->when(p.style){Style.BREAKING->"خبر فوری";Style.STAT->if(p.category=="خبر")"گزارش آماری"else p.category;Style.QUOTE->if(p.category=="خبر")"نقل‌قول"else p.category;else->p.category}
 }
 private fun labelLayout(c:Context,p:Project,w:Int,s:Float,kind:String,color:Int)=layout(label(p,kind),(w-12*s).toInt(),(if(p.style==Style.BREAKING&&kind=="cover")12f else 9.5f)*s,StudioFont.ESTEDAD.bodyFace(c),color)
 private fun reserve(c:Context,p:Project,w:Int,s:Float,kind:String):Float{
  val labelHeight=if(label(p,kind).isBlank())0f else labelLayout(c,p,w,s,kind,Color.WHITE).height+10*s
  return labelHeight+if(p.style==Style.QUOTE&&kind=="cover")30*s else 0f
 }
 private val metric=Regex("[+−-]?[0-9۰-۹٠-٩]+(?:[٫.,٬][0-9۰-۹٠-٩]+)*(?:[ \\u00a0]*(?:درصد|میلیون|میلیارد|تومان|ریال|دلار|نفر|کیلومتر|متر|٪))*")
 private fun cover(p:Project,text:String,size:Float,feature:Boolean=true):Page{
  val match=if(feature&&p.style==Style.STAT&&text.length<=180)metric.find(text)else null
  return if(match==null)Page(text,"",size)else Page(text,"",size,feature=match.value,visualTitle=text.removeRange(match.range).trim().replace(Regex("[ \\t]{2,}")," "))
 }
 private fun featureLayout(c:Context,page:Page,w:Int,s:Float,color:Int):StaticLayout{
  var size=(page.titleSize*2.1f).coerceIn(36f,60f)*s
  val face=StudioFont.ESTEDAD.face(c)
  while(size>24*s&&layout(page.feature,w,size,face,center=true).lineCount>1)size-=s
  return layout(page.feature,w,size,face,color,center=true)
 }
 private fun titleHeight(c:Context,p:Project,page:Page,w:Int,s:Float):Float{
  val feature=if(page.feature.isBlank())0f else featureLayout(c,page,w,s,Color.WHITE).height.toFloat()
  val title=if(page.visualTitle.isBlank())0f else layout(page.visualTitle,w,page.titleSize*s,p.font.face(c)).height.toFloat()
  return feature+title+if(feature>0&&title>0)8*s else 0f
 }
 private fun contentHeight(c:Context,p:Project,page:Page,w:Int,s:Float):Float{
  val title=titleHeight(c,p,page,w,s)
  val body=if(page.body.isBlank())0f else bodyLayout(c,p,page.body,w,s).height.toFloat()
  val credit=if(page.byline.isBlank())0f else creditLayout(c,p,page.byline,w,s).height+14*s
  return title+body+credit+if(title>0&&body>0)TEXT_GAP*s else 0f
 }
 fun pageBox(plan:Plan,page:Page)=RectF(if(page.kind=="cover")plan.text else plan.fullText)
 fun blockHeight(c:Context,plan:Plan,page:Page):Float{
  val p=plan.project;val s=min(plan.width,plan.height)/360f;val pad=if(plan.overlay)14*s else 0f
  val w=(pageBox(plan,page).width()-2*pad).toInt()
  return reserve(c,p,w,s,page.kind)+2*pad+contentHeight(c,p,page,w,s)
 }
 private fun brandLayout(c:Context,p:Project,width:Int,s:Float,color:Int=Color.WHITE):StaticLayout{
  val font=StudioFont.ESTEDAD.face(c);val w=(width-120*s).toInt();var size=p.brandSize*s
  while(layout(p.brand,w,size,font).lineCount>1&&size>6*s)size-=.5f*s
  return layout(p.brand,w,size,font,color)
 }
 private fun bodyTarget(p:Project):String{
  if(p.summary.length<=240)return p.summary
  val end=sentenceEnd.find(p.summary)?.range?.last?.plus(1)
  return if(end!=null&&end<=240)p.summary.take(end)else p.summary.take(160).substringBeforeLast(' ',p.summary.take(160))
 }
 fun plan(c:Context,p:Project,photo:Bitmap?=null,overlay:Boolean=false,width:Int=p.ratio.w,height:Int=p.ratio.h):Plan{
  require(p.title.isNotBlank()){ "تیتر را وارد کنید." }
  require(p.brand.length<=60&&p.site.length<=80&&p.category.length<=30){"نام رسانه، نشانی یا دسته‌بندی بیش از اندازه طولانی است."}
  val s=min(width,height)/360f;val margin=24*s;val story=height>width*1.5f
  val top=if(p.showBrand)headerY(width,height,s)+brandHeight(c,p,width,s)+26*s else (if(story)52 else 28)*s
  val bottom=height-(if(story)100 else 66)*s
  val full=RectF(margin,top,width-margin,bottom)
  val box=RectF(full);var image:RectF?=null
  val font=p.font.face(c);val bodyFont=p.bodyFont.bodyFace(c)
  val target=bodyTarget(p)
  if(photo!=null&&!overlay){
   image=RectF(full)
   val portraitColumn=!p.crop&&photo.height>photo.width*1.15f&&height<width*1.5f
   if(width>height*1.3f||portraitColumn){
    image.right=width*(if(portraitColumn).43f else .46f);box.left=image.right+18*s
   }else{
    val gap=20*s;val tw=box.width().toInt()
    val draft=cover(p,p.title,p.titleSize).copy(body=target,byline=if(p.style==Style.QUOTE&&p.byline.length<120)p.byline else "")
    val measured=reserve(c,p,tw,s,"cover")+contentHeight(c,p,draft,tw,s)
    val minimumSize=(if(p.auto)min(p.titleSize,16f)else p.titleSize)*s
    val first=layout(p.title,tw,minimumSize,font)
    val firstLine=p.title.substring(0,first.getLineEnd(0)).trimEnd()
    val minimum=reserve(c,p,tw,s,"cover")+layout(firstLine,tw,minimumSize,font).height+8*s
    val textHeight=measured.coerceIn(max(full.height()*.22f,minimum),max(full.height()*.64f,minimum))
    if(p.style==Style.PHOTO){image.left=0f;image.right=width.toFloat()}
    val preferred=when(p.style){Style.PHOTO->.62f;Style.EDITORIAL->.44f;Style.BREAKING->.38f;Style.QUOTE->.28f;Style.STAT->.32f;else->.50f}
    val photoHeight=min(image.width()*photo.height/photo.width,min(full.height()*preferred,full.height()-textHeight-gap)).coerceAtLeast(24*s)
    if(p.style in listOf(Style.BREAKING,Style.QUOTE,Style.STAT)){
     box.bottom=full.top+max(textHeight,full.height()-photoHeight-gap)
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
  var size=p.titleSize
  var first=cover(p,p.title.trim(),size)
  val wantedBody=if(target.isBlank())0f else bodyLayout(c,p,target,w,s).height+TEXT_GAP*s
  val wantedCredit=if(p.style==Style.QUOTE&&p.byline.isNotBlank()&&p.byline.length<120)creditLayout(c,p,p.byline,w,s).height+14*s else 0f
  if(p.auto)while(size>16f&&titleHeight(c,p,first,w,s)+wantedBody+wantedCredit>available){size=max(16f,size-.5f);first=cover(p,p.title.trim(),size)}
  val pages=mutableListOf<Page>()
  if(titleHeight(c,p,first,w,s)<=available){pages+=first}
  else{
   val segment=split(p.title,w,available,size*s,font).first()
   pages+=cover(p,segment,size,feature=false)
   val rest=p.title.trim().substring(segment.length).trimStart()
   split(rest,fw,fullAvailable,size*s,font).forEach{pages+=Page(it,"",size,"title")}
  }
  var summary=p.summary.trim()
  if(summary.isNotBlank()){
   val last=pages.last();val lw=if(last.kind=="cover")w else fw
   val remaining=(if(last.kind=="cover")available else fullAvailable)-titleHeight(c,p,last,lw,s)-TEXT_GAP*s
   if(remaining>=bodyLayout(c,p,"آ",lw,s).height){
    val wholeFits=bodyLayout(c,p,summary,lw,s).height<=remaining
    val fitsFresh=bodyLayout(c,p,summary,fw,s).height<=fullAvailable
    // Prefer a complete short sentence on a fresh page over half a sentence.
    if(wholeFits||summary.length>240||!fitsFresh){
     val firstBody=split(summary,lw,remaining,p.bodySize*s,bodyFont,BODY_SPACING,sentences=true).first()
     pages[pages.lastIndex]=last.copy(body=firstBody);summary=summary.substring(firstBody.length).trimStart()
    }
   }
   split(summary,fw,fullAvailable,p.bodySize*s,bodyFont,BODY_SPACING,sentences=true).forEach{pages+=Page("",it,size,"continued")}
  }
  if(p.style==Style.QUOTE&&p.byline.isNotBlank()){
   val last=pages.last();val lw=if(last.kind=="cover")w else fw
   val budget=if(last.kind=="cover")available else fullAvailable
   val withCredit=last.copy(byline=p.byline.trim())
   if(contentHeight(c,p,withCredit,lw,s)<=budget)pages[pages.lastIndex]=withCredit
   else split(p.byline,fw,fullAvailable-14*s,p.bodySize*.92f*s,bodyFont,BODY_SPACING,sentences=true).forEach{pages+=Page("","",size,"attribution",byline=it)}
  }
  if(p.carousel&&!overlay){
   split(p.article,fw,fullAvailable,p.bodySize*s,bodyFont,BODY_SPACING,sentences=true).forEach{pages+=Page("",it,size,"article")}
   split(p.closingText,fw,fullAvailable,p.bodySize*s,bodyFont,BODY_SPACING,sentences=true).forEach{pages+=Page("",it,size,"closing")}
  }
  return Plan(p,width,height,box,image,pages,overlay,full)
 }
 fun cropRect(sw:Int,sh:Int,dw:Float,dh:Float,x:Float,y:Float,zoom:Float):RectF{
  val scale=max(dw/sw,dh/sh)*zoom.coerceIn(1f,3f);val w=dw/scale;val h=dh/scale
  val left=(sw-w)*x.coerceIn(0f,1f);val top=(sh-h)*y.coerceIn(0f,1f)
  return RectF(left,top,left+w,top+h)
 }
 fun containedPhotoRect(sw:Int,sh:Int,d:RectF):RectF{
  val scale=min(d.width()/sw,d.height()/sh)
  return RectF(d.centerX()-sw*scale/2,d.centerY()-sh*scale/2,d.centerX()+sw*scale/2,d.centerY()+sh*scale/2)
 }
 private fun photo(canvas:Canvas,b:Bitmap,d:RectF,p:Project,paint:Paint){
  canvas.save();canvas.clipRect(d)
  if(p.crop){val r=cropRect(b.width,b.height,d.width(),d.height(),p.x,p.y,p.zoom);canvas.drawBitmap(b,Rect(r.left.toInt(),r.top.toInt(),r.right.toInt(),r.bottom.toInt()),d,paint)}
  else canvas.drawBitmap(b,null,containedPhotoRect(b.width,b.height,d),paint)
  canvas.restore()
 }
 private data class Palette(val background:Int,val surface:Int,val ink:Int,val muted:Int,val accent:Int,val chrome:Int)
 private fun palette(style:Style):Palette{
  fun c(hex:String)=Color.parseColor(hex)
  return when(style){
   Style.SIGNATURE->Palette(c("#F7F7F3"),c("#F7F7F3"),c("#13151A"),c("#42464C"),c("#D9082D"),c("#303238"))
   Style.EDITORIAL->Palette(c("#FFFFFF"),c("#FFFFFF"),c("#191919"),c("#454A50"),c("#C50026"),c("#181A1E"))
   Style.PHOTO->Palette(c("#090D11"),c("#121820"),c("#F9F9F6"),c("#D4D7D9"),c("#FF667A"),c("#F4F4F0"))
   Style.BREAKING->Palette(c("#C70B28"),c("#C70B28"),c("#FFFFFC"),c("#FFE5E8"),c("#FFFFFC"),c("#FFFFFF"))
   Style.QUOTE->Palette(c("#F0EFEC"),c("#F0EFEC"),c("#1F2024"),c("#45464B"),c("#D90930"),c("#27282D"))
   Style.STAT->Palette(c("#101923"),c("#101923"),c("#FEFEFD"),c("#CFD5DC"),c("#FF7688"),c("#F0F3F6"))
  }
 }
 fun contentTop(c:Context,plan:Plan,page:Page):Float{
  val p=plan.project;val box=pageBox(plan,page)
  val factor=when(p.position){
   Position.TOP->0f;Position.CENTER->.5f;Position.BOTTOM->1f
   Position.AUTO->when{plan.overlay->1f;page.kind!="cover"->0f;plan.photo==null->.5f;p.style in listOf(Style.QUOTE,Style.STAT)->.5f;else->0f}
  }
  return box.top+max(0f,box.height()-blockHeight(c,plan,page))*factor
 }
 fun surfaceBox(c:Context,plan:Plan,page:Page):RectF{
  val s=min(plan.width,plan.height)/360f;val box=pageBox(plan,page)
  val y=contentTop(c,plan,page);val actual=blockHeight(c,plan,page)
  return if(plan.overlay)RectF(box.left,y,box.right,y+actual)else RectF(box.left-10*s,y-12*s,box.right+10*s,y+actual+16*s)
 }
 fun draw(c:Context,plan:Plan,index:Int,photo:Bitmap?=null):Bitmap{
  val p=plan.project;val page=plan.pages[index];val s=min(plan.width,plan.height)/360f;val t=palette(p.style)
  val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
  val metadata=StudioFont.ESTEDAD.bodyFace(c)
  val bitmap=Bitmap.createBitmap(plan.width,plan.height,Bitmap.Config.ARGB_8888)
  try{
   val canvas=Canvas(bitmap);val margin=24*s;val story=plan.height>plan.width*1.5f
   val headerY=headerY(plan.width,plan.height,s);val headerH=brandHeight(c,p,plan.width,s)
   val blackMasthead=p.style==Style.SIGNATURE
   val headerInk=if(blackMasthead)Color.rgb(249,249,246)else t.chrome
   if(!plan.overlay){
    canvas.drawColor(t.background)
    if(blackMasthead&&p.showBrand){paint.color=Color.rgb(17,20,25);canvas.drawRect(0f,0f,plan.width.toFloat(),headerY+headerH+12*s,paint)}
   }
   if(photo!=null&&plan.photo!=null&&page.kind=="cover")this.photo(canvas,photo,plan.photo,p,paint)
   fun drawLayout(l:StaticLayout,x:Float,y:Float){canvas.save();canvas.translate(x,y);l.draw(canvas);canvas.restore()}
   fun persian(n:Int)=n.toString().padStart(2,'0').map{if(it in '0'..'9')'۰'+(it-'0')else it}.joinToString("")
   if(p.showBrand){
    if(plan.overlay){
     paint.color=if(blackMasthead)Color.rgb(17,20,25)else t.background;paint.alpha=245
     canvas.drawRect(RectF(margin-8*s,headerY-7*s,plan.width-margin+8*s,headerY+headerH+7*s),paint);paint.alpha=255
    }
    val logoRect=logoBox(plan)
    if(logoRect!=null)StudioBrandMark.draw(c,canvas,logoRect,p,s)
    else{val brand=brandLayout(c,p,plan.width,s,headerInk);drawLayout(brand,96*s,headerY+(headerH-brand.height)/2)}
    if(!plan.overlay&&!blackMasthead){
     paint.color=t.chrome;paint.alpha=55;paint.strokeWidth=.45f*s
     canvas.drawLine(margin,headerY+headerH+10*s,plan.width-margin,headerY+headerH+10*s,paint);paint.alpha=255
    }
   }
   val box=pageBox(plan,page);val pad=if(plan.overlay)14*s else 0f;val w=(box.width()-2*pad).toInt()
   if(plan.overlay||p.style==Style.PHOTO){paint.color=t.surface;canvas.drawRect(surfaceBox(c,plan,page),paint)}
   var y=contentTop(c,plan,page)+pad
   if(label(p,page.kind).isNotBlank()){
    val categoryColor=if(p.style in listOf(Style.QUOTE,Style.STAT))t.muted else t.accent
    val cat=labelLayout(c,p,w,s,page.kind,categoryColor)
    if(p.style !in listOf(Style.QUOTE,Style.STAT)){
     paint.color=t.accent
     canvas.drawCircle(box.right-pad-2*s,y+cat.height*.52f,2*s,paint)
    }
    drawLayout(cat,box.left+pad,y);y+=cat.height+10*s
   }
   if(p.style==Style.QUOTE&&page.kind=="cover"){
    paint.typeface=Typeface.create("serif",Typeface.NORMAL);paint.textSize=52*s;paint.color=t.accent
    canvas.drawText("“",box.right-pad-24*s,y+36*s,paint);y+=30*s
   }
   if(page.feature.isNotBlank()){
    val feature=featureLayout(c,page,w,s,t.accent);drawLayout(feature,box.left+pad,y);y+=feature.height
    if(page.visualTitle.isNotBlank())y+=8*s
   }
   if(page.visualTitle.isNotBlank()){
    val title=layout(page.visualTitle,w,page.titleSize*s,p.font.face(c),t.ink)
    drawLayout(title,box.left+pad,y);y+=title.height
    if(page.body.isNotBlank())y+=TEXT_GAP*s
   }else if(page.feature.isNotBlank()&&page.body.isNotBlank())y+=TEXT_GAP*s
   if(page.body.isNotBlank()){
    val body=bodyLayout(c,p,page.body,w,s,t.muted);drawLayout(body,box.left+pad,y);y+=body.height
   }
   if(page.byline.isNotBlank()){
    paint.color=t.chrome;paint.alpha=90;paint.strokeWidth=.5f*s
    canvas.drawLine(box.right-pad-22*s,y+7*s,box.right-pad,y+7*s,paint);paint.alpha=255
    y+=14*s;drawLayout(creditLayout(c,p,page.byline,w,s,t.ink),box.left+pad,y)
   }
   val footer=plan.height-(if(story)63 else 32)*s
   if(p.showSite||showsPageNumber(plan)){
    if(plan.overlay){
     paint.color=t.background;paint.alpha=245
     canvas.drawRect(RectF(margin-8*s,footer-13*s,plan.width-margin+8*s,footer+23*s),paint);paint.alpha=255
    }
    paint.color=t.chrome;paint.alpha=55;paint.strokeWidth=.45f*s
    canvas.drawLine(margin,footer-9*s,plan.width-margin,footer-9*s,paint);paint.alpha=255
    if(p.showSite){
     var size=9*s;val fw=(plan.width-margin*2-70*s).toInt()
     while(layout(p.site,fw,size,metadata,rtl=false).lineCount>1&&size>6*s)size-=.5f*s
     drawLayout(layout(p.site,fw,size,metadata,t.chrome,rtl=false),margin,footer)
    }
    if(showsPageNumber(plan))drawLayout(layout("${persian(index+1)} / ${persian(plan.pages.size)}",(60*s).toInt(),9*s,metadata,t.chrome,rtl=false),plan.width-margin-60*s,footer)
   }
   return bitmap
  }catch(t:Throwable){bitmap.recycle();throw t}
 }
}
