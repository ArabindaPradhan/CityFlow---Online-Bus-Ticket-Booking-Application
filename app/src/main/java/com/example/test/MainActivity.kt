package com.example.test

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.*
import com.example.test.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var bottomNavView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        bottomNavView = findViewById(R.id.btm_nav_view)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_notifications,
                R.id.nav_profile,
                R.id.nav_settings,
                R.id.nav_about,
                R.id.nav_contact
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    navController.navigate(R.id.navigation_home)
                    true
                }
                R.id.navigation_dashboard -> {
                    navController.navigate(R.id.navigation_dashboard)
                    true
                }
                R.id.navigation_notifications -> {
                    navController.navigate(R.id.navigation_notifications)
                    true
                }
                else -> false
            }
        }

        // ✅ Sync BottomNavigationView when navigating (Fix for Back button)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_home -> bottomNavView.menu.findItem(R.id.navigation_home).isChecked = true
                R.id.navigation_dashboard -> bottomNavView.menu.findItem(R.id.navigation_dashboard).isChecked = true
                R.id.navigation_notifications -> bottomNavView.menu.findItem(R.id.navigation_notifications).isChecked = true
            }
        }

        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        setupSocialMediaLinks()
        setupMenuItems()
        updateNavHeader()
    }

    private fun setupMenuItems() {
        val logoutItem = binding.navView.menu.findItem(R.id.nav_logout)
        logoutItem.setOnMenuItemClickListener {
            logoutUser()
            true
        }

        val shareItem = binding.navView.menu.findItem(R.id.nav_share)
        shareItem.setOnMenuItemClickListener {
            shareApp()
            true
        }
    }

    private fun updateNavHeader() {
        val navigationView: NavigationView = binding.navView
        val headerView: View = navigationView.getHeaderView(0)

        val userNameTextView = headerView.findViewById<TextView>(R.id.nav_user_name)
        val userEmailTextView = headerView.findViewById<TextView>(R.id.nav_user_email)

        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name") ?: "User"
                        val email = document.getString("email") ?: "user@example.com"

                        userNameTextView.text = "Hii, $name"
                        userEmailTextView.text = email
                    }
                }
                .addOnFailureListener {
                    userNameTextView.text = "Hii, User"
                    userEmailTextView.text = "user@example.com"
                }
        } else {
            userNameTextView.text = "Hii, Guest"
            userEmailTextView.text = "guest@example.com"
        }
    }

    private fun logoutUser() {
        firebaseAuth.signOut()
        val intent = Intent(this, SignIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out this amazing app!")
            putExtra(
                Intent.EXTRA_TEXT,
                "Hey! Download this awesome app from Play Store:\nhttps://play.google.com/store/apps/details?id=${packageName}"
            )
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun setupSocialMediaLinks() {
        val facebookIcon: ImageView = findViewById(R.id.icon_facebook)
        val instagramIcon: ImageView = findViewById(R.id.icon_instagram)
        val linkedInIcon: ImageView = findViewById(R.id.icon_linkedIn)

        facebookIcon.setOnClickListener { openUrl("https://www.facebook.com") }
        instagramIcon.setOnClickListener { openUrl("https://www.instagram.com") }
        linkedInIcon.setOnClickListener { openUrl("https://www.linkedin.com") }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment).navController
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
