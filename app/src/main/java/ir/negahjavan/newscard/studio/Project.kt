package ir.negahjavan.newscard.studio

import android.content.Context
import android.graphics.Typeface
import android.util.AtomicFile
import ir.negahjavan.newscard.R
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class StudioFont(val fa:String,val res:Int) {
 VAZIR("وزیرمتن",R.font.vazirmatn_bold),LALEZAR("لاله‌زار",R.font.lalezar),CAIRO("قاهره",R.font.cairo),KUFI("کوفی",R.font.kufi),AMIRI("امیری",R.font.amiri),AREF("عارف",R.font.aref);
 fun face(c:Context)=Typeface.create(c.resources.getFont(res),700,false)
 fun bodyFace(c:Context)=if(this==VAZIR)c.resources.getFont(R.font.vazirmatn_regular)else Typeface.create(c.resources.getFont(res),400,false)
}
enum class Style(val fa:String){SIGNATURE("امضای طلایی"),EDITORIAL("صفحهٔ مجله"),PHOTO("عکس‌محور"),BREAKING("خبر فوری"),QUOTE("نقل‌قول"),STAT("عدد خبر")}
enum class Ratio(val fa:String,val w:Int,val h:Int){STORY("استوری",1080,1920),POST("پست ۴:۵",1080,1350),SQUARE("مربع",1080,1080),WIDE("افقی",1920,1080)}
enum class Position(val fa:String){AUTO("هوشمند"),TOP("بالا"),CENTER("وسط"),BOTTOM("پایین")}
data class Project(
 val id:String=UUID.randomUUID().toString(),val name:String="پروژهٔ تازه",
 val raw:String="",val title:String="",val summary:String="",val article:String="",
 val image:String="",val video:String="",val mode:String="card",val ratio:Ratio=Ratio.STORY,
 val font:StudioFont=StudioFont.VAZIR,val style:Style=Style.SIGNATURE,
 val auto:Boolean=true,val titleSize:Float=21f,val bodySize:Float=12f,
 val position:Position=Position.AUTO,val crop:Boolean=false,val x:Float=.5f,val y:Float=.5f,val zoom:Float=1f,
 val videoCrop:Boolean=false,val vx:Float=.5f,val vy:Float=.5f,val vz:Float=1f,
 val brand:String="نگاه جوان",val showBrand:Boolean=true,val brandSize:Float=12f,
 val site:String="negahjavan.ir",val showSite:Boolean=true,val category:String="خبر",val byline:String="",
 val carousel:Boolean=false,val closingText:String="",val fullHd:Boolean=true,val exports:List<String> = emptyList()
){
 fun caption()=listOf(title.trim(),article.ifBlank{summary}.trim(),if(showBrand)brand else "",if(showSite)site else "").filter{it.isNotBlank()}.distinct().joinToString("\n\n")
 fun json()=JSONObject().apply{
  put("id",id);put("name",name);put("raw",raw);put("title",title);put("summary",summary);put("article",article)
  put("image",image);put("video",video);put("mode",mode);put("ratio",ratio.name);put("font",font.name);put("style",style.name)
  put("auto",auto);put("titleSize",titleSize);put("bodySize",bodySize);put("position",position.name)
  put("crop",crop);put("x",x);put("y",y);put("zoom",zoom);put("videoCrop",videoCrop);put("vx",vx);put("vy",vy);put("vz",vz)
  put("brand",brand);put("showBrand",showBrand);put("brandSize",brandSize);put("site",site);put("showSite",showSite)
  put("category",category);put("byline",byline);put("carousel",carousel);put("closingText",closingText);put("fullHd",fullHd);put("exports",org.json.JSONArray(exports))
 }
 companion object{
  fun read(j:JSONObject):Project{
   fun f(k:String,d:Float,l:Float,h:Float)=j.optDouble(k,d.toDouble()).toFloat().let{if(it.isFinite())it.coerceIn(l,h)else d}
   return Project(id=j.optString("id",UUID.randomUUID().toString()),name=j.optString("name","پروژهٔ تازه"),raw=j.optString("raw"),title=j.optString("title"),summary=j.optString("summary"),article=j.optString("article"),image=j.optString("image"),video=j.optString("video"),mode=j.optString("mode","card"),
    ratio=Ratio.entries.firstOrNull{it.name==j.optString("ratio")}?:Ratio.STORY,font=StudioFont.entries.firstOrNull{it.name==j.optString("font")}?:StudioFont.VAZIR,style=Style.entries.firstOrNull{it.name==j.optString("style")}?:Style.SIGNATURE,
    auto=j.optBoolean("auto",true),titleSize=f("titleSize",21f,16f,40f),bodySize=f("bodySize",12f,10f,18f),position=Position.entries.firstOrNull{it.name==j.optString("position")}?:Position.AUTO,
    crop=j.optBoolean("crop"),x=f("x",.5f,0f,1f),y=f("y",.5f,0f,1f),zoom=f("zoom",1f,1f,3f),videoCrop=j.optBoolean("videoCrop"),vx=f("vx",.5f,0f,1f),vy=f("vy",.5f,0f,1f),vz=f("vz",1f,1f,3f),
    brand=j.optString("brand","نگاه جوان"),showBrand=j.optBoolean("showBrand",true),brandSize=f("brandSize",12f,8f,18f),site=j.optString("site","negahjavan.ir"),showSite=j.optBoolean("showSite",true),category=j.optString("category","خبر"),byline=j.optString("byline"),carousel=j.optBoolean("carousel"),closingText=j.optString("closingText"),fullHd=j.optBoolean("fullHd",true),exports=j.optJSONArray("exports")?.let{a->List(a.length()){a.getString(it)}}?:emptyList())
  }
 }
}
object ProjectStore{
 private fun dir(c:Context)=File(c.filesDir,"studio-projects").apply{mkdirs()}
 fun save(c:Context,p:Project,archive:Boolean=false){
  val safeId=p.id.replace(Regex("[^a-zA-Z0-9-]"),"_")
  val f=AtomicFile(File(dir(c),if(archive)"project-$safeId.json"else"current.json"));val out=f.startWrite()
  try{out.write(p.json().toString().toByteArray());f.finishWrite(out)}catch(t:Throwable){f.failWrite(out);throw t}
 }
 fun load(c:Context)=runCatching{Project.read(JSONObject(File(dir(c),"current.json").readText()))}.getOrDefault(Project())
 fun list(c:Context)=dir(c).listFiles().orEmpty().filter{it.name.startsWith("project-")}.sortedByDescending{it.lastModified()}.mapNotNull{runCatching{Project.read(JSONObject(it.readText()))}.getOrNull()}
}
object StudioText{
 fun prepare(raw:String):Pair<String,String>{
  val text=raw.trim();val first=text.lineSequence().firstOrNull().orEmpty().trim()
  val title=if(text.contains('\n')&&first.length<=320)first else Regex("[.!؟?\\n]").find(text)?.let{text.substring(0,it.range.last+1)}?:text
  return title to text.removePrefix(title).trim()
 }
 fun summary(title:String,body:String):String{
  val keys=title.split(Regex("\\s+")).filter{it.length>3}.toSet()
  return body.split(Regex("(?<=[.!؟?؛])\\s+|\\n+")).map{it.trim()}.filter{it.length in 20..220&&it!=title}.maxByOrNull{s->keys.count{s.contains(it)}*5+if(s.any{it.isDigit()})3 else 0}.orEmpty()
 }
 fun keepUnits(t:String)=Regex("[0-9۰-۹٠-٩]+(?:[٫.,][0-9۰-۹٠-٩]+)?[ \\t]+(?:درصد|میلیون|میلیارد|تومان|ریال|دلار|کیلومتر|متر|نفر|٪)(?:[ \\t]+(?:تومان|ریال|دلار))?").replace(t){m->m.value.replace(' ','\u00a0').replace('\t','\u00a0')}
}
