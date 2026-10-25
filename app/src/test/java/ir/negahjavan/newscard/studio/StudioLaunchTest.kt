package ir.negahjavan.newscard.studio
import ir.negahjavan.newscard.MainActivity
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StudioLaunchTest{
 @Test fun activityStartsAndRestoresSavedProject(){
  val controller=Robolectric.buildActivity(MainActivity::class.java).setup();val activity=controller.get();assertNotNull(activity.window.decorView)
  val p=Project(title="تیتر ذخیره‌شده",font=StudioFont.KUFI,titleSize=21f,videoCrop=false);ProjectStore.save(activity,p);controller.pause().stop().destroy()
  val again=Robolectric.buildActivity(MainActivity::class.java).setup();assertEquals(p,ProjectStore.load(again.get()));again.pause().stop().destroy()
 }
}
