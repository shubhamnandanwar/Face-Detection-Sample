package com.shunan.facedetection

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.SparseIntArray
import android.view.Surface
import android.view.View
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionPoint
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    var ORIENTATIONS = SparseIntArray()
    lateinit var bitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(applicationContext)

        bitmap = BitmapFactory.decodeResource(resources, R.drawable.sample)

        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)

        bitmap = BitmapFactory.decodeResource(resources, R.drawable.sample)
        val ratio = bitmap.height / bitmap.width.toFloat()


        bitmap = Bitmap.createScaledBitmap(bitmap, 360, (360 * ratio).toInt(), false)
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        processImage(image)

        imageView.setOnClickListener {
            loadImagefromGallery()
        }
    }

    fun shapeCrop(src: Bitmap, rect: RectF) {
        val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = -0x1000000
        canvas.drawOval(rect, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        canvas.drawBitmap(src, 0f, 0f, paint)
        imageView.setImageBitmap(output)
    }

    fun processImage(image: FirebaseVisionImage) {
        statusLabel.text = "Looking for Face"
        imageView.setImageBitmap(bitmap)

        // High-accuracy landmark detection and face classification
        val options = FirebaseVisionFaceDetectorOptions.Builder()
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .build()


        val detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options)

        val result = detector.detectInImage(image)
                .addOnSuccessListener {

                    for (face in it) {
                        val boundBox = face.boundingBox
                        if (boundBox != null) {
                            //TODO - validate bitmap bounds
                            val x1 = Math.max(0, boundBox.left - 48)
                            val y1 = Math.max(0, boundBox.top - 48)
                            val x2 = Math.min(bitmap.width, boundBox.right + 48)
                            val y2 = Math.min(bitmap.height, boundBox.bottom + 48)
                            bitmap = Bitmap.createBitmap(bitmap, x1, y1, x2 - x1, y2 - y1)
                            imageView.setImageBitmap(bitmap)
                            val image = FirebaseVisionImage.fromBitmap(bitmap!!)
                            detectFace(image)
                        }

                    }
                }
                .addOnFailureListener {
                    statusLabel.text = "Something went wrong"
                }
    }

    fun detectFace(image: FirebaseVisionImage) {
        statusLabel.text = "Marking Face"
        val options = FirebaseVisionFaceDetectorOptions.Builder()
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                .build()


        val detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options)

        val result = detector.detectInImage(image)
                .addOnSuccessListener {
                    if (it.size == 0) {
                        statusLabel.text = "Face not found"
                        progressBar.visibility = View.GONE
                    }

                    for (face in it) {

                        val faceContourPoints: List<FirebaseVisionPoint> = face.getContour(FirebaseVisionFaceContour.FACE).points
                                ?: ArrayList()

                        val path = Path()

                        for (i in 1..(faceContourPoints.size - 1)) {
                            val oldPoint = faceContourPoints.get(i - 1)
                            val currPoint = faceContourPoints.get(i)
                            path.moveTo(oldPoint.x, oldPoint.y)
                            path.lineTo(currPoint.x, currPoint.y)
                        }

                        path.close()
                        statusLabel.text = ""
                        progressBar.visibility = View.GONE
                        shapeCrop(bitmap, getRect(faceContourPoints))
                    }
                }
                .addOnFailureListener {
                    statusLabel.text = "Something went wrong"
                }
    }

    fun getRect(faceContourPoints: List<FirebaseVisionPoint>): RectF {
        val rect = RectF(10000f, 10000f, 0f, 0f)
        for (point in faceContourPoints) {
            rect.left = if (rect.left > point.x) point.x else rect.left
            rect.top = if (rect.top > point.y) point.y else rect.top
            rect.right = if (rect.right < point.x) point.x else rect.right
            rect.bottom = if (rect.bottom < point.y) point.y else rect.bottom
        }
        return rect
    }


    fun loadImagefromGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, 101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101 && resultCode == RESULT_OK) {
            val imageUri = data!!.data
            val image = FirebaseVisionImage.fromFilePath(applicationContext, imageUri)
            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)

            processImage(image)
        }

    }
}
