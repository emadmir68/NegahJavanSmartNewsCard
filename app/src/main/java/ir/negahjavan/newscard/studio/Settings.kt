package ir.negahjavan.newscard.studio

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun SettingSlider(label:String,value:Float,range:ClosedFloatingPointRange<Float>,change:(Float)->Unit){
 Text("$label · ${"%.1f".format(value)}",style=MaterialTheme.typography.labelLarge);Slider(value=value,onValueChange=change,valueRange=range)
}
@Composable fun Settings(p:Project,change:(Project)->Unit){
 var tab by rememberSaveable{mutableIntStateOf(0)}
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("اندازه","قالب","قاب","برند").forEachIndexed{i,t->FilterChip(selected=tab==i,onClick={tab=i},label={Text(t)},modifier=Modifier.weight(1f))}}
 when(tab){
  0->{
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("تنظیم هوشمند تیتر و متن");Switch(p.auto,{change(p.copy(auto=it))})}
   Text(if(p.auto)"ابتدا تیتر، متن و فاصله‌ها برای یک قاب خوانا تنظیم می‌شوند؛ فقط متن طولانی‌تر صفحه‌بندی می‌شود."else"اندازهٔ انتخابی حفظ می‌شود؛ متن بلند به صفحهٔ بعد می‌رود.",style=MaterialTheme.typography.bodySmall)
   SettingSlider(if(p.auto)"حداکثر اندازهٔ تیتر"else"اندازهٔ تیتر",p.titleSize,16f..40f){change(p.copy(titleSize=it))}
   SettingSlider(if(p.auto)"حداکثر اندازهٔ متن"else"اندازهٔ متن",p.bodySize,10f..18f){change(p.copy(bodySize=it))}
   Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){Position.entries.forEach{v->FilterChip(p.position==v,{change(p.copy(position=v))},label={Text(v.fa)})}}
  }
  1->{
   Style.entries.chunked(2).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{v->FilterChip(p.style==v,{change(p.copy(style=v))},label={Text(v.fa)},modifier=Modifier.weight(1f))}}}
   Text("ترکیب آمادهٔ تایپوگرافی")
   Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
    FilterChip(p.font==StudioFont.ESTEDAD&&p.bodyFont==StudioFont.CAIRO,{change(p.copy(font=StudioFont.ESTEDAD,bodyFont=StudioFont.CAIRO,titleSize=26f,bodySize=13f,auto=true))},label={Text("هویت برند")})
    FilterChip(p.font==StudioFont.LALEZAR&&p.bodyFont==StudioFont.ESTEDAD,{change(p.copy(font=StudioFont.LALEZAR,bodyFont=StudioFont.ESTEDAD,titleSize=28f,bodySize=13f,auto=true))},label={Text("نمایشی")})
    FilterChip(p.font==StudioFont.AMIRI&&p.bodyFont==StudioFont.ESTEDAD,{change(p.copy(font=StudioFont.AMIRI,bodyFont=StudioFont.ESTEDAD,titleSize=28f,bodySize=13f,auto=true))},label={Text("کلاسیک")})
   }
   Text("فونت تیتر")
   StudioFont.entries.chunked(3).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){row.forEach{v->FilterChip(p.font==v,{change(p.copy(font=v))},label={Text(v.fa)},modifier=Modifier.weight(1f))}}}
   Text("فونت متن خبر و خلاصه")
   StudioFont.entries.chunked(3).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){row.forEach{v->FilterChip(p.bodyFont==v,{change(p.copy(bodyFont=v))},label={Text(v.fa)},modifier=Modifier.weight(1f))}}}
   if(p.style==Style.QUOTE)OutlinedTextField(p.byline,{change(p.copy(byline=it))},label={Text("گویندهٔ نقل‌قول (اختیاری)")},modifier=Modifier.fillMaxWidth())
  }
  2->{
   val video=p.mode=="video";val crop=if(video)p.videoCrop else p.crop
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    FilterChip(!crop,{change(if(video)p.copy(videoCrop=false)else p.copy(crop=false))},label={Text("نمایش کامل")})
    FilterChip(crop,{change(if(video)p.copy(videoCrop=true)else p.copy(crop=true))},label={Text("برش و جابه‌جایی")})
   }
   Text(if(crop)"محدودهٔ برش را در پیش‌نمایش بررسی کنید."else"تمام تصویر حفظ می‌شود؛ فضای اضافی با پس‌زمینه پر می‌شود.",style=MaterialTheme.typography.bodySmall)
   if(crop){
    SettingSlider("بزرگ‌نمایی",if(video)p.vz else p.zoom,1f..3f){change(if(video)p.copy(vz=it)else p.copy(zoom=it))}
    SettingSlider("افقی",if(video)p.vx else p.x,0f..1f){change(if(video)p.copy(vx=it)else p.copy(x=it))}
    SettingSlider("عمودی",if(video)p.vy else p.y,0f..1f){change(if(video)p.copy(vy=it)else p.copy(y=it))}
    TextButton({change(if(video)p.copy(vx=.5f,vy=.5f,vz=1f)else p.copy(x=.5f,y=.5f,zoom=1f))}){Text("بازنشانی قاب")}
   }
  }
  3->{
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("لوگو و نام رسانه");Switch(p.showBrand,{change(p.copy(showBrand=it))})}
   if(p.showBrand){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("نشان نگاه جوان");Switch(p.useLogo,{change(p.copy(useLogo=it))})}
    if(p.useLogo){
     Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
      FilterChip(!p.fullLogo,{change(p.copy(fullLogo=false))},label={Text("نوشتار")})
      FilterChip(p.fullLogo,{change(p.copy(fullLogo=true))},label={Text("با عنوان رسانه")})
     }
     Text("رنگ نشان با زمینهٔ کارت هماهنگ می‌شود.",style=MaterialTheme.typography.bodySmall)
    }else OutlinedTextField(p.brand,{change(p.copy(brand=it))},label={Text("نام رسانه · حداکثر ۶۰ نویسه")},modifier=Modifier.fillMaxWidth())
    SettingSlider(if(p.useLogo)"اندازهٔ لوگو"else"اندازهٔ نام رسانه",p.brandSize,8f..18f){change(p.copy(brandSize=it))}
   }
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("نشانی سایت");Switch(p.showSite,{change(p.copy(showSite=it))})}
   if(p.showSite)OutlinedTextField(p.site,{change(p.copy(site=it))},label={Text("نشانی سایت · حداکثر ۸۰ نویسه")},modifier=Modifier.fillMaxWidth())
  }
 }
}
