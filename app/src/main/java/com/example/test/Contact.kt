package com.example.test

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.test.databinding.FragmentContactBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URLEncoder

class Contact : Fragment() {

    private lateinit var textViewEmail: TextView
    private lateinit var textViewPhone: TextView
    private lateinit var imageViewEmail: ImageView
    private lateinit var imageViewPhone: ImageView

    private var _binding: FragmentContactBinding? = null
    private val binding get() = _binding!!

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactBinding.inflate(inflater, container, false)
        val view = binding.root

        textViewEmail = view.findViewById(R.id.textViewEmail)
        textViewPhone = view.findViewById(R.id.textViewPhone)
        imageViewEmail = view.findViewById(R.id.imageViewEmail)
        imageViewPhone = view.findViewById(R.id.imageViewPhone)

        // Load user details from Firebase
        loadUserDetails()

        // Email click listeners
        val emailClickListener = View.OnClickListener { copyEmailToClipboard() }
        imageViewEmail.setOnClickListener(emailClickListener)
        textViewEmail.setOnClickListener(emailClickListener)

        // Phone click listeners
        val phoneClickListener = View.OnClickListener { makePhoneCall() }
        imageViewPhone.setOnClickListener(phoneClickListener)
        textViewPhone.setOnClickListener(phoneClickListener)

        // Submit button click
        binding.buttonSubmit.setOnClickListener { submitQuery() }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadUserDetails() {
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name") ?: ""
                        val email = document.getString("email") ?: ""

                        // Prefill the EditText fields
                        binding.editTextName.setText(name)
                        binding.editTextEmail.setText(email)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun copyEmailToClipboard() {
        val email = "cityflow@gmail.com"
        val clipboard =
            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Email", email)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(requireContext(), "Email copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun makePhoneCall() {
        val phoneNumber = "9861513686"
        val callIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        startActivity(callIntent)
    }

    private fun submitQuery() {
        val name = binding.editTextName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val message = binding.editTextMessage.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || message.isEmpty()) {
            Toast.makeText(requireContext(), "All fields are required!", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable button and show progress bar
        binding.buttonSubmit.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        // Encode data for URL
        val encodedName = URLEncoder.encode(name, "UTF-8")
        val encodedEmail = URLEncoder.encode(email, "UTF-8")
        val encodedMessage = URLEncoder.encode(message, "UTF-8")

        // Construct the GET request URL
        val url = "https://script.google.com/macros/s/AKfycbx4HJ-8S0fyXpnuMTzol7_UjKaYb3illydz1B6mc8mLd-7iZ4X3U7pW4v3GeaEvZjxS/exec" +
                "?userName=$encodedName&userEmail=$encodedEmail&querryMsg=$encodedMessage&action=create"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                // Hide progress bar and enable button
                binding.progressBar.visibility = View.GONE
                binding.buttonSubmit.isEnabled = true

                if (response.contains("Created", ignoreCase = true)) {
                    Toast.makeText(requireContext(), "Query Submitted Successfully!", Toast.LENGTH_SHORT).show()
                    binding.editTextMessage.text.clear()
                } else {
                    Toast.makeText(requireContext(), "Submission failed: $response", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                // Hide progress bar and enable button after failure
                binding.progressBar.visibility = View.GONE
                binding.buttonSubmit.isEnabled = true
                Toast.makeText(requireContext(), "Failed to submit query: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )

        val requestQueue = Volley.newRequestQueue(requireContext())
        requestQueue.add(request)
    }

}