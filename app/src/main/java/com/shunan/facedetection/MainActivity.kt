package com.shunan.facedetection

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionPoint
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var userBitmap: Bitmap
    private lateinit var modelBitmap: Bitmap
    private lateinit var options: FirebaseVisionFaceDetectorOptions
    private var userRect = RectF()
    private var modelRect = RectF(588f, 200f, 853f, 515f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(applicationContext)
        options = FirebaseVisionFaceDetectorOptions.Builder()
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                .build()

        userBitmap = BitmapFactory.decodeResource(resources, R.drawable.sample)
        modelBitmap = BitmapFactory.decodeResource(resources, R.drawable.sample_men)

        //bitmap = Bitmap.createScaledBitmap(bitmap, 360, (360 * ratio).toInt(), false)
        processUserFace(FirebaseVisionImage.fromBitmap(userBitmap))

        imageView.setOnClickListener {
            loadImagefromGallery()
        }
    }

    fun swapFaces() {
        userBitmap = Bitmap.createBitmap(userBitmap, userRect.left.toInt(), userRect.top.toInt(), (userRect.right - userRect.left).toInt(), (userRect.bottom - userRect.top).toInt())
        userBitmap = Bitmap.createScaledBitmap(userBitmap, modelRect.width().toInt(), modelRect.height().toInt(), false)
        modelRect.width()
        var userOutput = Bitmap.createBitmap(userBitmap.width, userBitmap.height, Bitmap.Config.ARGB_8888)
        var canvas = Canvas(userOutput)
        var paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = -0x1000000
        canvas.drawOval(userRect, paint)


        val rect = RectF(0f, 0f, userBitmap.width.toFloat(), userBitmap.height.toFloat())
        canvas.drawOval(rect, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(userBitmap, 0f, 0f, paint)
        //extraImageView.setImageBitmap(userBitmap)


        val output = Bitmap.createBitmap(modelBitmap.width, modelBitmap.height, Bitmap.Config.ARGB_8888)
        canvas = Canvas(output)
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = -0x1000000
        canvas.drawBitmap(modelBitmap,0f,0f,paint)

        canvas.drawBitmap(userOutput, modelRect.left, modelRect.top, paint)

        imageView.setImageBitmap(output)
        progressBar.visibility = View.GONE
        statusLabel.text = "Image Processed"
    }

    private fun processUserFace(image: FirebaseVisionImage) {
        statusLabel.text = "Processing User Face"
        val detector = FirebaseVision.getInstance().getVisionFaceDetector(options)
        detector.detectInImage(image)
                .addOnSuccessListener {
                    if(it.size == 0){
                        statusLabel.text = "Face not found"
                        progressBar.visibility = View.GONE
                    }
                    for (face in it) {
                        val faceContourPoints: List<FirebaseVisionPoint> = face.getContour(FirebaseVisionFaceContour.FACE).points
                                ?: ArrayList()
                        userRect = getRect(faceContourPoints)
                        //processModelFace(FirebaseVisionImage.fromBitmap(modelBitmap))
                        swapFaces()
                    }
                }
                .addOnFailureListener {
                    statusLabel.text = "Something went wrong while processing user face"
                }
    }

    private fun processModelFace(image: FirebaseVisionImage) {

        statusLabel.text = "Processing Model Face"
        val detector = FirebaseVision.getInstance().getVisionFaceDetector(options)
        detector.detectInImage(image)
                .addOnSuccessListener {
                    for (face in it) {
                        val faceContourPoints: List<FirebaseVisionPoint> = face.getContour(FirebaseVisionFaceContour.FACE).points
                                ?: ArrayList()
                        modelRect = getRect(faceContourPoints)
                        swapFaces()
                    }
                }
                .addOnFailureListener {
                    statusLabel.text = "Something went wrong while processing model face"
                }
    }

    private fun getRect(faceContourPoints: List<FirebaseVisionPoint>): RectF {
        val rect = RectF(10000f, 10000f, 0f, 0f)
        for (point in faceContourPoints) {
            rect.left = if (rect.left > point.x) point.x else rect.left
            rect.top = if (rect.top > point.y) point.y else rect.top
            rect.right = if (rect.right < point.x) point.x else rect.right
            rect.bottom = if (rect.bottom < point.y) point.y else rect.bottom
        }
        return rect
    }


    private fun loadImagefromGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, 101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101 && resultCode == RESULT_OK) {
            val imageUri = data!!.data
            val image = FirebaseVisionImage.fromFilePath(applicationContext, imageUri)
            userBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
            imageView.setImageBitmap(null)
            statusLabel.text = "Processing Image"
            progressBar.visibility = View.VISIBLE

            processUserFace(image)
        }
    }
}
