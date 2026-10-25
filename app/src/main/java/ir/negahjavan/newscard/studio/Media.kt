package ir.negahjavan.newscard.studio

import android.content.*
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import android.util.LruCache
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

object StudioMedia{
 val mutex=Mutex()
 private val cache=object:LruCache<String,Bitmap>(32*1024*1024){override fun sizeOf(key:String,value:Bitmap)=value.allocationByteCount}
 suspend fun photo(c:Context,uri:String):Bitmap?=withContext(Dispatchers.IO){
  if(uri.isBlank())return@withContext null
  cache.get(uri)?.let{return@withContext it}
  val b=ImageDecoder.decodeBitmap(ImageDecoder.createSource(c.contentResolver,Uri.parse(uri))){d,info,_->
   val max=maxOf(info.size.width,info.size.height);if(max>2048)d.setTargetSize((info.size.width*2048L/max).toInt().coerceAtLeast(1),(info.size.height*2048L/max).toInt().coerceAtLeast(1));d.allocator=ImageDecoder.ALLOCATOR_SOFTWARE
  };cache.put(uri,b);b
 }
 suspend fun import(c:Context,uri:Uri):String=withContext(Dispatchers.IO){
  val b=requireNotNull(photo(c,uri.toString()));val dir=File(c.filesDir,"studio-media").apply{mkdirs()};val file=File(dir,"${UUID.randomUUID()}.png")
  try{file.outputStream().use{check(b.compress(Bitmap.CompressFormat.PNG,100,it))};Uri.fromFile(file).toString()}catch(t:Throwable){file.delete();throw t}
 }
 suspend fun export(c:Context,plan:Plan,photo:Bitmap?):List<String>{
  val made=mutableListOf<String>()
  try{
   withContext(Dispatchers.IO){mutex.withLock{
    plan.pages.indices.forEach{i->ensureActive();val bitmap=StudioRenderer.draw(c,plan,i,photo)
     try{
      val values=ContentValues().apply{put(MediaStore.Images.Media.DISPLAY_NAME,"NegahJavan_${System.currentTimeMillis()}_${i+1}.png");put(MediaStore.Images.Media.MIME_TYPE,"image/png");put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/NegahJavan");put(MediaStore.Images.Media.IS_PENDING,1)}
      val uri=requireNotNull(c.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values));made+=uri.toString()
      requireNotNull(c.contentResolver.openOutputStream(uri)).use{check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}
      ensureActive();check(c.contentResolver.update(uri,ContentValues().apply{put(MediaStore.Images.Media.IS_PENDING,0)},null,null)==1)
     }finally{bitmap.recycle()}
    }
   }};return made
  }catch(t:Throwable){withContext(NonCancellable+Dispatchers.IO){made.forEach{runCatching{c.contentResolver.delete(Uri.parse(it),null,null)}}};throw t}
 }
 fun copy(c:Context,p:Project){(c.getSystemService(Context.CLIPBOARD_SERVICE)as ClipboardManager).setPrimaryClip(ClipData.newPlainText("کپشن",p.caption()))}
 fun share(c:Context,p:Project,uris:List<String>){
  require(uris.isNotEmpty());copy(c,p)
  val parsed=ArrayList(uris.map(Uri::parse));val intent=Intent(if(parsed.size==1)Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply{
   type=if(p.mode=="video")"video/mp4"else"image/png"
   if(parsed.size==1)putExtra(Intent.EXTRA_STREAM,parsed.first())else putParcelableArrayListExtra(Intent.EXTRA_STREAM,parsed)
   putExtra(Intent.EXTRA_TEXT,p.caption());clipData=ClipData.newUri(c.contentResolver,"نگاه جوان",parsed.first()).apply{parsed.drop(1).forEach{addItem(ClipData.Item(it))}};addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  };c.startActivity(Intent.createChooser(intent,"اشتراک خروجی"))
 }
}
