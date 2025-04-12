package com.example.test


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileViewModel : ViewModel() {

    private val _text = MutableLiveData<String>()
    val text: LiveData<String> = _text

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name") ?: "N/A"
                        val email = document.getString("email") ?: "N/A"
                        val mob = document.getString("mob") ?: "N/A"

                        _text.value = "Name: $name\n\nMob: $mob\n\nEmail: $email"
                    }
                }
                .addOnFailureListener {
                    _text.value = "Error loading profile data"
                }
        } else {
            _text.value = "User not logged in"
        }
    }
}



//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//
//class ProfileViewModel : ViewModel() {
//    private val _text = MutableLiveData<String>().apply {
//        value = "Name: Arabinda Pradhan\n\nMob: 9861513686\n\nemail: arabindapradhan0987@gmail.com"
//    }
//    val text: LiveData<String> = _text
//}