package com.kel4.katarak_project

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import await
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.kel4.katarak_project.databinding.FragmentPredictionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PredictionFragment : Fragment() {

    private lateinit var binding: FragmentPredictionBinding
    private lateinit var imageView: ImageView
    private lateinit var resultText: TextView
    private lateinit var uploadBtn: Button
    private lateinit var cameraBtn: Button
    private lateinit var predictBtn: Button

    private var imageUri: Uri? = null
    private var tflite: Interpreter? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                imageUri = it
                imageView.setImageURI(it)
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                imageUri?.let {
                    imageView.setImageURI(it)
                }
            } else {
                Toast.makeText(requireContext(), "Gagal mengambil foto", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_prediction, container, false)
        binding = FragmentPredictionBinding.bind(view)

        imageView = view.findViewById(R.id.imageView)
        resultText = view.findViewById(R.id.resultText)
        uploadBtn = view.findViewById(R.id.uploadBtn)
        cameraBtn = view.findViewById(R.id.cameraBtn)
        predictBtn = view.findViewById(R.id.predictBtn)

        lifecycleScope.launch {
            tflite = loadModelFromAssets("Katarak_model.tflite")
            if (tflite == null) {
                Toast.makeText(requireContext(), "Gagal memuat model", Toast.LENGTH_LONG).show()
            }
        }

        uploadBtn.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        cameraBtn.setOnClickListener {
            imageUri = createImageUri()
            takePictureLauncher.launch(imageUri)
        }

        predictBtn.setOnClickListener {
            imageUri?.let { uri ->
                lifecycleScope.launch {
                    try {
                        val bitmap = loadBitmapFromUri(uri) ?: return@launch
                        val label = runInference(bitmap)

                        resultText.text = label

                        if (label.contains("wajah", ignoreCase = true)) {
                            Toast.makeText(requireContext(), label, Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        if (label.contains("Katarak", ignoreCase = true)) {
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, fragment_katarak_infoo())
                                .addToBackStack(null)
                                .commit()
                        } else {
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, fragment_normal_info())
                                .addToBackStack(null)
                                .commit()
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(requireContext(), "Terjadi kesalahan saat prediksi", Toast.LENGTH_SHORT).show()
                    }
                }
            } ?: run {
                Toast.makeText(requireContext(), "Silakan pilih atau ambil gambar terlebih dahulu", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private suspend fun loadModelFromAssets(modelPath: String): Interpreter? {
        return withContext(Dispatchers.IO) {
            try {
                val assetManager = requireContext().assets
                val inputStream = assetManager.open(modelPath)
                val byteArray = inputStream.readBytes()
                val buffer = ByteBuffer.allocateDirect(byteArray.size).apply {
                    order(ByteOrder.nativeOrder())
                    put(byteArray)
                    rewind()
                }
                Interpreter(buffer)
            } catch (e: Exception) {
                Log.e("Model", "Failed to load model: ${e.message}")
                null
            }
        }
    }

    private fun createImageUri(): Uri {
        val imageFile = File(requireContext().cacheDir, "photo_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            imageFile
        )
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
                }
            } else {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private suspend fun runInference(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val model = tflite ?: return@withContext "Model error"

        val inputImage = InputImage.fromBitmap(bitmap, 0)

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()

        val detector = FaceDetection.getClient(options)
        val faces = detector.process(inputImage).await()

        if (faces.isEmpty()) {
            return@withContext "Tidak ada wajah terdeteksi"
        }

        val face = faces[0]
        val bounds = face.boundingBox

        if (bounds.width() < bitmap.width * 0.2 || bounds.height() < bitmap.height * 0.2) {
            return@withContext "Wajah terpotong, silakan coba lagi"
        }

        val x = bounds.left.coerceAtLeast(0)
        val y = bounds.top.coerceAtLeast(0)
        val width = bounds.width().coerceAtMost(bitmap.width - x)
        val height = bounds.height().coerceAtMost(bitmap.height - y)

        val faceBitmap = Bitmap.createBitmap(bitmap, x, y, width, height)
        val resized = Bitmap.createScaledBitmap(faceBitmap, 224, 224, true)

        val inputBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3).apply {
            order(ByteOrder.nativeOrder())
        }

        val intValues = IntArray(224 * 224)
        resized.getPixels(intValues, 0, 224, 0, 0, 224, 224)

        for (pixel in intValues) {
            inputBuffer.putFloat((pixel shr 16 and 0xFF) / 255f)
            inputBuffer.putFloat((pixel shr 8 and 0xFF) / 255f)
            inputBuffer.putFloat((pixel and 0xFF) / 255f)
        }

        val output = Array(1) { FloatArray(1) }
        model.run(inputBuffer, output)

        val confidence = output[0][0]
        val label = if (confidence < 0.4f) "Prediksi: Katarak" else "Prediksi: Normal"

        return@withContext label
    }
}
