package com.kel4.katarak_project

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.navigation.NavigationView
import com.kel4.katarak_project.databinding.ActivityPredictionBinding

class PredictionActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityPredictionBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var homeFragment: HomeFragment
    private lateinit var predictionFragment: PredictionFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPredictionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up ActionBarDrawerToggle untuk tombol hamburger
        toggle = ActionBarDrawerToggle(this, binding.drawerLayout, R.string.open_drawer, R.string.close_drawer)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Menampilkan tombol hamburger
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set Navigation item selected listener
        binding.navView.setNavigationItemSelectedListener(this)

        // Menampilkan fragment Home pertama kali
        if (savedInstanceState == null) {
            homeFragment = HomeFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, homeFragment)
                .commit()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> {
                // Navigasi ke HomeFragment
                homeFragment = HomeFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, homeFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()

            }
            R.id.prediction -> {
                // Navigasi ke PredictionFragment
                predictionFragment = PredictionFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, predictionFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()

            }
        }
        // Menutup drawer setelah memilih item
        binding.drawerLayout.closeDrawers()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Mengaktifkan tombol hamburger untuk membuka dan menutup drawer
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
