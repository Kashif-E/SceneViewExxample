package com.infinity.examplesceneview

import android.content.DialogInterface
import android.graphics.Color.LTGRAY
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.assets.RenderableSource.*
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.CompletionException

class MainActivity : AppCompatActivity() {

    lateinit var modelNode: TransformableNode
    var mSession : Session ? = null
    var mUserRequestInstall = true
    lateinit var model: ModelRenderable

    lateinit var  transformationSystem: TransformationSystem
    var link : String = "https://firebasestorage.googleapis.com/v0/b/modelviewerapp.appspot.com/o/models%2Fout.glb?alt=media&token=442d2d08-9b61-49e1-a059-e7f89a83155c"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //making a transformation system so we can interact with the 3d model
        transformationSystem = TransformationSystem(resources.displayMetrics, FootprintSelectionVisualizer())

        //builds the model from the link
        renderObject()

        //change scene view background color
        sceneform_scene_view.renderer?.setClearColor(Color(LTGRAY))
        sceneform_scene_view.scene
            .addOnPeekTouchListener { hitTestResult: HitTestResult?, motionEvent: MotionEvent? ->
                transformationSystem.onTouch(
                    hitTestResult,
                    motionEvent
                )
            }

    }

    private fun onRenderableLoaded(modelRenderable: ModelRenderable) {


        modelNode = TransformableNode(transformationSystem).apply {
            setParent(sceneform_scene_view.scene)
            translationController.isEnabled = true
            scaleController.isEnabled = true
            scaleController.minScale=0.01f
            scaleController.maxScale=2f
            rotationController.isEnabled = true
            localPosition = Vector3(0f,  0f, - 2.3f)
            renderable = modelRenderable
        }

        transformationSystem.selectNode(modelNode)


    }

    private fun renderObject() {

        ModelRenderable.builder()
            .setSource(
                this, builder().setSource(
                    this,
                    Uri.parse(link),
                    SourceType.GLB
                )
                    .setRecenterMode(RecenterMode.ROOT)
                    .build()

            )
            .setRegistryId(link)
            .build()
            .thenAccept { modelRenderable: ModelRenderable ->


                model= modelRenderable
                onRenderableLoaded(model)
            }
            .exceptionally { throwable: Throwable? ->
                val message: String = if (throwable is CompletionException) {

                    "Internet is not working"
                } else {

                    "Can't load Model"
                }
                val mainHandler = Handler(Looper.getMainLooper())
                val finalMessage: String = message
                val myRunnable = Runnable {
                    AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage(finalMessage + "")
                        .setPositiveButton("Retry") { dialogInterface: DialogInterface, _: Int ->
                            renderObject()
                            dialogInterface.dismiss()
                        }
                        .setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
                        .show()
                }
                mainHandler.post(myRunnable)
                null
            }
    }
    override fun onResume() {
        super.onResume()
        try {
             if (mSession == null)
             {
                 when(ArCoreApk.getInstance().requestInstall(this , mUserRequestInstall))
                 {
                     ArCoreApk.InstallStatus.INSTALLED ->
                     {
                         mSession = Session( this)
                         Toast.makeText(this," ARCore Session Started", Toast.LENGTH_SHORT).show()
                     }
                     ArCoreApk.InstallStatus.INSTALL_REQUESTED ->
                     {
                         mUserRequestInstall = false
                     }
                 }

             }
        }catch ( e: UnavailableArcoreNotInstalledException)
        {
            Toast.makeText(this," You need to install arcore to use ar service", Toast.LENGTH_SHORT).show()
        }catch ( e :UnavailableUserDeclinedInstallationException)
        {
            Toast.makeText(this," Please Allow Arcore Installatiom", Toast.LENGTH_SHORT).show()
        }
        catch (e : UnavailableApkTooOldException)
        {
            Toast.makeText(this," Update ARcore Apk ", Toast.LENGTH_SHORT).show()
        }
    }

}