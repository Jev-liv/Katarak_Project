package com.kel4.katarak_project

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout fragment_home.xml
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Ambil tombol dari layout
        val button = view.findViewById<Button>(R.id.prediksi_katarak)

        // Saat tombol diklik, ganti fragment ke PredictionFragment
        button.setOnClickListener {
            val predictionFragment = PredictionFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, predictionFragment) // Ganti sesuai ID container Anda
                .addToBackStack(null)
                .commit()
        }

        return view
    }
}
