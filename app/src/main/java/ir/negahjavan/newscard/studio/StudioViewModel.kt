package ir.negahjavan.newscard.studio

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class StudioViewModel(app:Application):AndroidViewModel(app){
 var project by mutableStateOf(ProjectStore.load(app));private set
 var saved by mutableStateOf(ProjectStore.list(app));private set
 var status by mutableStateOf("")
 private val writes=Channel<Project>(Channel.CONFLATED)
 init{viewModelScope.launch(Dispatchers.IO){for(p in writes)try{ProjectStore.save(app,p)}catch(e:Exception){withContext(Dispatchers.Main){status="ذخیرهٔ پروژه ناموفق بود: ${e.message}"}}}}
 fun update(p:Project){val changed=p.copy(name="",exports=emptyList())!=project.copy(name="",exports=emptyList());project=if(changed)p.copy(exports=emptyList())else p;writes.trySend(project)}
 fun open(p:Project){project=p;writes.trySend(p)}
 fun archive(announce:Boolean=true){val p=project;viewModelScope.launch{try{withContext(Dispatchers.IO){ProjectStore.save(getApplication(),p,true)};saved=withContext(Dispatchers.IO){ProjectStore.list(getApplication())};if(announce)status="پروژه ذخیره شد"}catch(cancel:CancellationException){throw cancel}catch(e:Exception){status="ذخیرهٔ پروژه ناموفق بود: ${e.message}"}}}
 fun fresh(){archive();open(Project())}
 fun prepare(){val source=project;viewModelScope.launch{val result=withContext(Dispatchers.Default){StudioText.prepareProject(source)};if(project.raw==source.raw){update(project.copy(title=result.title,article=result.article,summary=result.summary));status="تیتر و تمام بندهای خبر آماده شدند؛ متن را پیش از خروجی بررسی کنید."}}}
}
