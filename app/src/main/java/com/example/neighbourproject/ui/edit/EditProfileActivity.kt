package com.example.neighbourproject.ui.edit

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.example.neighbourproject.databinding.ActivityEditProfileBinding
import com.example.neighbourproject.neighbour.data.Gender
import com.example.neighbourproject.neighbour.data.People
import com.example.neighbourproject.neighbour.data.RelationshipStatus
import com.example.neighbourproject.ui.search.SearchActivity
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.*
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import java.io.File


open class EditProfileActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "EditProfileActivity"
        private const val REQUEST_GALLERY = 1001
        private const val REQUEST_CAMERA = 2002
    }

    private val model: EditViewModel by viewModels()

    private lateinit var binding: ActivityEditProfileBinding

    private lateinit var profile: People

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        profile = model.getUserProfile() ?: People()

        if (profile.image != "") {
            Glide.with(this)
                .load(profile.image)
                .into(binding.circularPhoto)
        }

        binding.genderSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, Gender.values())

        binding.relationshipSpinner.adapter =
            ArrayAdapter(
                this, android.R.layout.simple_spinner_item, RelationshipStatus.values()
            )

        binding.nameEditText.setText(profile.firstName)
        binding.lastnameEditText.setText(profile.lastName)
        binding.ageEditText.setText(profile.age.toString())
        binding.genderSpinner.setSelection(profile.gender.ordinal)
        binding.relationshipSpinner.setSelection(profile.relationshipStatus.ordinal)

        binding.emailEditText.setText(profile.email)

        binding.addInterestRecycler.adapter = InterestAddAdapter(profile, model)

        binding.saveButton.setOnClickListener {
            profile.firstName = binding.nameEditText.text.toString()
            profile.lastName = binding.lastnameEditText.text.toString()
            profile.age = binding.ageEditText.text.toString().toInt()
            profile.gender = Gender.valueOf(binding.genderSpinner.selectedItem.toString())
            profile.relationshipStatus =
                RelationshipStatus.valueOf(binding.relationshipSpinner.selectedItem.toString())

            profile.email = binding.emailEditText.text.toString()

            model.editUserProfile(profile)

            //TODO decide when to push image to firestore

            startActivity(Intent(this, SearchActivity::class.java))
            finish()
        }

        //TODO perhaps a selector to select image
        binding.takePhotoButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.CAMERA
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.CAMERA),
                    REQUEST_CAMERA
                )
            } else {
                capturePhoto()
            }
        }

        binding.circularPhoto.setOnClickListener {
           if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_GALLERY
                )
            } else {
                chooseImageGallery()
            }
        }
    }

    private val cameraResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode == Activity.RESULT_OK){
            result.data?.let { intent ->
                Log.d(TAG, "Camera: $intent")
                val bitMap = intent.extras?.get("data") as Bitmap
                Log.d(TAG, "Camera: - bitmap $bitMap")
                binding.circularPhoto.setImageBitmap(bitMap)
                //TODO Write stuff to firestore
            }
        }
    }

    private fun capturePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        //TODO save picture to internal storage
        cameraResultLauncher.launch(intent)
    }

    private val galleryResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode == Activity.RESULT_OK){
            result.data?.let { intent ->
                Log.d(TAG, "Gallery: $intent")
                val url = intent.data
                Log.d(TAG, "Gallery - url: $url")
                binding.circularPhoto.setImageURI(url)
                //TODO Write stuff to firestore
            }
        }
    }

    private fun chooseImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryResultLauncher.launch(intent)
    }

   /* private fun upLoadImageToFirebaseStorage() {
        if (imageUri != null) {
            val filename = UUID.randomUUID().toString()
            val ref = FirebaseStorage.getInstance().getReference("/Images/$filename")
            Log.d(TAG, "upLoadImageToFirebaseStorage ${ref.toString()}")
            ref.putFile(imageUri!!)
                .addOnSuccessListener {
                    Log.d(
                        TAG,
                        "upLoadImageToFirebaseStorage: successfully uploaded image: ${it.metadata?.path}"
                    )

                    ref.downloadUrl.addOnSuccessListener { uri ->
                        Log.d(TAG, "File Location: $uri")
                        profile.image = uri.toString()
                    }
                }
        }
    }

    private var imageUri: Uri? = null
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_CAMERA && data != null) {
            val bitMap = data.extras?.get("data") as Bitmap
            binding.circularPhoto.setImageBitmap(bitMap)

            val filename = UUID.randomUUID().toString()
            val ref = FirebaseStorage.getInstance().getReference("/Images/$filename")
            val baos = ByteArrayOutputStream()
            bitMap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val fileData = baos.toByteArray()

            ref.putBytes(fileData)
                .addOnSuccessListener {
                    Log.d(
                        TAG,
                        "upLoadImageToFirebaseStorage: successfully uploaded image: ${it.metadata?.path}"
                    )

                    ref.downloadUrl.addOnSuccessListener {
                        Log.d(TAG, "File Location: $it")
                        profile.image = it.toString()
                        profile.let {
                            model.editUserProfile(it)
                        }
                    }
                }
        }
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_CHOOSE && data != null) {
            imageUri = data.data
            binding.circularPhoto.setImageURI(imageUri)

            upLoadImageToFirebaseStorage()
        }
    }
*/
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: Permission Granted!")
                capturePhoto()
            } else {
                Log.d(TAG, "onRequestPermissionsResult: Permission Denied!")
            }
        }
        if (requestCode == REQUEST_GALLERY) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: Permission Granted!")
                chooseImageGallery()
            } else {
                Log.d(TAG, "onRequestPermissionsResult: Permission Denied!")
            }
        }
    }
}
