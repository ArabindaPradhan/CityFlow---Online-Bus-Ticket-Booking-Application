package com.example.test

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class SignUp : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.textView.setOnClickListener {
            val intent = Intent(this, SignIn::class.java)
            startActivity(intent)
        }
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                val intent = Intent(this, SignIn::class.java)
                startActivity(intent)
                finish()
            }
        }

        binding.button.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val name = binding.nameEt.text.toString().trim()
        val email = binding.emailEt.text.toString().trim()
        val mob = binding.mobEt.text.toString().trim()
        val pass = binding.passET.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || mob.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("SignUpActivity", "SignUp button clicked with email: $email")

        // ✅ Show loading indicator and disable inputs
        showLoading(true)

        firebaseAuth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = firebaseAuth.currentUser?.uid
                    Log.d("SignUpActivity", "User created successfully: UID = $uid")

                    if (uid != null) {
                        saveUserToFirestore(uid, name, email, mob)
                    } else {
                        showError("Failed to get user ID after registration")
                    }
                } else {
                    showError("SignUp Failed: ${task.exception?.message}")
                }
            }
    }

    private fun saveUserToFirestore(uid: String, name: String, email: String, mob: String) {
        val userMap = hashMapOf(
            "name" to name,
            "email" to email,
            "mob" to mob
        )

        firestore.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                Log.d("SignUpActivity", "User data saved in Firestore")

                Toast.makeText(this, "Registration Successful! Please login.", Toast.LENGTH_SHORT).show()

                firebaseAuth.signOut()
                Log.d("SignUpActivity", "User signed out after registration")

                navigateToSignIn()
            }
            .addOnFailureListener { e ->
                showError("Error saving data: ${e.message}")
            }
    }

    private fun navigateToSignIn() {
        val intent = Intent(this, SignIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        Log.e("SignUpActivity", message)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        showLoading(false) // Hide loading and re-enable inputs
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.button.isEnabled = false
            binding.nameEt.isEnabled = false
            binding.emailEt.isEnabled = false
            binding.mobEt.isEnabled = false
            binding.passET.isEnabled = false
        } else {
            binding.progressBar.visibility = View.GONE
            binding.button.isEnabled = true
            binding.nameEt.isEnabled = true
            binding.emailEt.isEnabled = true
            binding.mobEt.isEnabled = true
            binding.passET.isEnabled = true
        }
    }
}


//import android.content.Intent
//import android.os.Bundle
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import com.example.test.databinding.ActivitySignUpBinding
//import com.google.firebase.auth.FirebaseAuth
//
//class SignUp : AppCompatActivity() {
//
//    private lateinit var binding: ActivitySignUpBinding
//    private lateinit var firebaseAuth: FirebaseAuth
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivitySignUpBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        firebaseAuth = FirebaseAuth.getInstance()
//
//        binding.textView.setOnClickListener {
//            val intent = Intent(this, SignIn::class.java)
//            startActivity(intent)
//        }
//        binding.button.setOnClickListener {
//            val email = binding.emailEt.text.toString()
//            val pass = binding.passET.text.toString()
//            val confirmPass = binding.confirmPassEt.text.toString()
//
//            if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
//                if (pass == confirmPass) {
//
//                    firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
//                        if (it.isSuccessful) {
//                            val intent = Intent(this, SignIn::class.java)
//                            startActivity(intent)
//                        } else {
//                            Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
//
//                        }
//                    }
//                } else {
//                    Toast.makeText(this, "Password is not matching", Toast.LENGTH_SHORT).show()
//                }
//            } else {
//                Toast.makeText(this, "Empty Fields Are not Allowed !!", Toast.LENGTH_SHORT).show()
//
//            }
//        }
//    }
//}