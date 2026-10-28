package ir.negahjavan.newscard.studio

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock

private data class Preview(val key:Project,val plan:Plan,val bitmap:Bitmap,val info:StudioVideo.Info?)
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun StudioApp(vm:StudioViewModel=viewModel()){
 val c=LocalContext.current;val scope=rememberCoroutineScope();val p=vm.project
 var editor by rememberSaveable{mutableIntStateOf(0)}
 var projects by remember{mutableStateOf(false)}
 var page by remember{mutableIntStateOf(0)}
 var preview by remember{mutableStateOf<Preview?>(null)}
 var preparing by remember{mutableStateOf(false)}
 var error by remember{mutableStateOf("")}
 var busy by remember{mutableStateOf(false)}
 var progress by remember{mutableIntStateOf(0)}
 var job by remember{mutableStateOf<Job?>(null)}
 val key=p.copy(raw="",name="",exports=emptyList())
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch{
  try{vm.update(vm.project.copy(image=StudioMedia.import(c,uri)))}catch(cancel:CancellationException){throw cancel}catch(e:Exception){vm.status="تصویر باز نشد: ${e.message}"}
 }}
 val videoPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){
  try{c.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);vm.update(vm.project.copy(video=uri.toString()))}catch(e:Exception){vm.status="دسترسی پایدار به ویدئو برقرار نشد؛ فایل را از حافظهٔ گوشی انتخاب کنید."}
 }}
 LaunchedEffect(key,page){
  preparing=true;error=""
  try{
   delay(180)
   if(key.title.isBlank()){preview=null;return@LaunchedEffect}
   val photo=if(key.mode=="video")null else StudioMedia.photo(c,key.image)
   val info=if(key.mode=="video"&&key.video.isNotBlank())withContext(Dispatchers.IO){StudioVideo.info(c,Uri.parse(key.video))}else null
   val result=withContext(Dispatchers.Default){StudioMedia.mutex.withLock{
    val width=if(key.mode=="video"&&!key.fullHd)720 else key.ratio.w
    val height=if(key.mode=="video"&&!key.fullHd)(key.ratio.h*720f/key.ratio.w).toInt().let{it-it%2}else key.ratio.h
    val plan=StudioRenderer.plan(c,key,photo,key.mode=="video",width,height)
    if(info!=null)StudioVideo.validate(plan,info)
    Preview(key,plan,StudioRenderer.draw(c,plan,page.coerceIn(plan.pages.indices),photo),info)
   }};preview=result
  }catch(cancel:CancellationException){throw cancel}catch(e:Exception){error=e.message?:"پیش‌نمایش ساخته نشد"}finally{preparing=false}
 }
 val view=LocalView.current
 DisposableEffect(busy){view.keepScreenOn=busy;onDispose{view.keepScreenOn=false}}
 val ready=preview?.takeIf{it.key==key&&!preparing&&error.isBlank()}
 fun export(share:Boolean){
  val snap=ready?:return;busy=true;progress=0;vm.status="در حال ساخت خروجی…"
  job=scope.launch{
   try{
    val uris=if(p.mode=="video"){
     val file=StudioVideo.export(c,Uri.parse(p.video),snap.plan){progress=it}
     try{listOf(StudioVideo.save(c,file).toString())}finally{withContext(Dispatchers.IO){file.delete()}}
    }else StudioMedia.export(c,snap.plan,StudioMedia.photo(c,p.image))
    vm.update(p.copy(exports=uris));vm.archive(false);vm.status="${uris.size} خروجی در گالری ذخیره شد."
    if(share)StudioMedia.share(c,p,uris)
   }catch(cancel:CancellationException){vm.status="ساخت خروجی لغو شد";throw cancel}catch(e:Exception){vm.status="خروجی ساخته نشد: ${e.message}"}finally{busy=false}
  }
 }
 CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl){
  Scaffold(topBar={TopAppBar(title={Column{Text("نگاه جوان");Text("استودیوی کارت · ۱.۸.۰",style=MaterialTheme.typography.labelSmall)}},actions={TextButton({projects=true},enabled=!busy){Text("پروژه‌ها")}})},bottomBar={
   Surface(shadowElevation=8.dp){Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp)){
    if(busy){LinearProgressIndicator(progress={progress/100f},modifier=Modifier.fillMaxWidth());TextButton({job?.cancel()}){Text("لغو خروجی")}}
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
     Button({export(false)},enabled=ready!=null&&!busy&&(p.mode!="video"||p.video.isNotBlank()),modifier=Modifier.weight(1f)){Text("ذخیره")}
     Button({if(p.exports.isNotEmpty())runCatching{StudioMedia.share(c,p,p.exports)}.onFailure{vm.status="اشتراک فایل ذخیره‌شده ناموفق بود؛ دوباره ذخیره کنید."}else export(true)},enabled=ready!=null&&!busy&&(p.mode!="video"||p.video.isNotBlank()),modifier=Modifier.weight(1f)){Text("اشتراک")}
    }
   }}
  }){padding->
   Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("card" to "کارت خبر","post" to "پست","video" to "ویدئو").forEach{(mode,label)->FilterChip(p.mode==mode,{page=0;vm.update(p.copy(mode=mode,ratio=if(mode=="post")Ratio.POST else if(mode=="video")Ratio.STORY else p.ratio))},enabled=!busy,label={Text(label)},modifier=Modifier.weight(1f))}}
    if(vm.status.isNotBlank())Text(vm.status,style=MaterialTheme.typography.bodySmall)
    HorizontalDivider();Text("پیش‌نمایش خروجی",style=MaterialTheme.typography.titleLarge)
    val shown=preview
    Box(Modifier.fillMaxWidth().then(if(shown==null)Modifier.height(100.dp)else Modifier.heightIn(max=420.dp).aspectRatio(shown.plan.width.toFloat()/shown.plan.height)),contentAlignment=Alignment.Center){
     if(shown!=null){if(shown.key.mode=="video"&&shown.info!=null&&shown.key.video.isNotBlank())VideoPreview(shown)else Image(shown.bitmap.asImageBitmap(),"پیش‌نمایش کارت",modifier=Modifier.fillMaxSize())}else Text("برای شروع تیتر را وارد کنید.")
     if(preparing)CircularProgressIndicator()
    }
    if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
    if(shown!=null&&shown.plan.pages.size>1&&p.mode!="video"){
     val current=page.coerceIn(shown.plan.pages.indices)
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton({page=maxOf(0,current-1)},enabled=!busy&&current>0){Text("قبلی")};Text("صفحهٔ ${current+1} از ${shown.plan.pages.size}");TextButton({page=minOf(shown.plan.pages.lastIndex,current+1)},enabled=!busy&&current<shown.plan.pages.lastIndex){Text("بعدی")}}
    }
    if(shown!=null)Text("${shown.plan.pages.size} صفحه · ${shown.plan.width} × ${shown.plan.height} پیکسل",style=MaterialTheme.typography.bodySmall)
    OutlinedButton({StudioMedia.copy(c,p);vm.status="کپشن کامل کپی شد"},enabled=p.title.isNotBlank()){Text("کپی کپشن کامل")}
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("متن خبر","طراحی","رسانه").forEachIndexed{i,label->FilterChip(editor==i,{editor=i},enabled=!busy,label={Text(label)},modifier=Modifier.weight(1f))}}
    if(editor==0){
     OutlinedTextField(p.raw,{vm.update(p.copy(raw=it))},enabled=!busy,label={Text("متن کامل خبر")},modifier=Modifier.fillMaxWidth(),minLines=3,maxLines=7)
     Button({page=0;vm.prepare()},enabled=!busy&&p.raw.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("آماده‌سازی تیتر و خلاصه")}
     OutlinedTextField(p.title,{page=0;vm.update(p.copy(title=it))},enabled=!busy,label={Text("تیتر · قابل ویرایش")},modifier=Modifier.fillMaxWidth(),maxLines=5)
     OutlinedTextField(p.summary,{page=0;vm.update(p.copy(summary=it))},enabled=!busy,label={Text("خلاصه · اختیاری")},modifier=Modifier.fillMaxWidth(),maxLines=5)
     OutlinedTextField(p.article,{vm.update(p.copy(article=it))},enabled=!busy,label={Text("متن کامل کپشن / اسلایدها")},modifier=Modifier.fillMaxWidth(),maxLines=5)
     OutlinedTextField(p.category,{vm.update(p.copy(category=it))},enabled=!busy,label={Text("دسته‌بندی")},modifier=Modifier.fillMaxWidth())
    }
    if(editor==2){
     if(p.mode=="video"){
      Button({videoPicker.launch(arrayOf("video/*"))},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(if(p.video.isBlank())"انتخاب ویدئو"else"تغییر ویدئو")}
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("خروجی Full HD");Switch(p.fullHd,{vm.update(p.copy(fullHd=it))},enabled=!busy)}
      Text("ویدئو بازکدگذاری می‌شود؛ نمایش کامل پیش‌فرض است. متن چندصفحه‌ای در طول ویدئو نمایش داده می‌شود.",style=MaterialTheme.typography.bodySmall)
     }else{
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({picker.launch(arrayOf("image/*"))},enabled=!busy){Text(if(p.image.isBlank())"افزودن عکس"else"تغییر عکس")};if(p.image.isNotBlank())TextButton({vm.update(p.copy(image=""))},enabled=!busy){Text("حذف عکس")}}
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("کاروسل با متن کامل");Switch(p.carousel,{page=0;vm.update(p.copy(carousel=it))},enabled=!busy)}
      if(p.carousel)OutlinedTextField(p.closingText,{vm.update(p.copy(closingText=it))},enabled=!busy,label={Text("متن صفحهٔ پایانی · خالی بگذارید تا حذف شود")},modifier=Modifier.fillMaxWidth(),maxLines=4)
     }
    }
    if(editor==1){
     Ratio.entries.chunked(2).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{r->FilterChip(p.ratio==r,{page=0;vm.update(p.copy(ratio=r))},enabled=!busy,label={Text(r.fa)},modifier=Modifier.weight(1f))}}}
     if(!busy)Settings(p){vm.update(it)}
    }
    Spacer(Modifier.height(16.dp))
   }
  }
  if(projects)AlertDialog(onDismissRequest={projects=false},title={Text("پروژه‌های من")},text={Column(Modifier.verticalScroll(rememberScrollState())){
   OutlinedTextField(p.name,{vm.update(p.copy(name=it))},label={Text("نام پروژه")});Text("متن، فونت، قاب و تنظیمات به‌صورت خودکار ذخیره می‌شوند.",style=MaterialTheme.typography.bodySmall)
   vm.saved.forEach{saved->TextButton({vm.open(saved);page=0;projects=false}){Text(saved.name)}}
  }},confirmButton={TextButton({vm.archive();projects=false}){Text("ذخیره در پروژه‌ها")}},dismissButton={TextButton({vm.fresh();page=0;projects=false}){Text("پروژهٔ جدید")}})
 }
}
@androidx.annotation.OptIn(UnstableApi::class)
@Composable private fun VideoPreview(preview:Preview){
 val c=LocalContext.current
 val player=remember(preview.key){ExoPlayer.Builder(c).build().apply{setVideoEffects(StudioVideo.effects(c,preview.plan,requireNotNull(preview.info)));setMediaItem(MediaItem.fromUri(preview.key.video));repeatMode=ExoPlayer.REPEAT_MODE_ONE;prepare()}}
 val owner=LocalLifecycleOwner.current
 DisposableEffect(player,owner){val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_PAUSE)player.pause()};owner.lifecycle.addObserver(observer);onDispose{owner.lifecycle.removeObserver(observer);player.release()}}
 AndroidView(factory={PlayerView(it).apply{useController=true}},update={it.player=player},modifier=Modifier.fillMaxSize())
}
