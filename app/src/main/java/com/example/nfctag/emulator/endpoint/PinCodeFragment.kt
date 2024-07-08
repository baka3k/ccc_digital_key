package com.example.nfctag.emulator.endpoint

import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.example.nfctag.R
import com.example.nfctag.databinding.FragmentPinCodeBinding
import com.example.nfctag.utils.Keyboard
import hi.baka3k.data.preference.Preference

/**
 * An example full-screen fragment that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class PinCodeFragment : Fragment() {
    private var visible: Boolean = false
    private var _binding: FragmentPinCodeBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPinCodeBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        visible = true
        binding.pinview.setPinViewEventListener { pinview, fromUser ->
            val value = pinview.value
            //NOTE: 0000 - pin code default - is just only for testing false, do not use
            if (value.length == 4 && value.equals("0000")) {
                Preference.getInstance(requireContext().applicationContext).setByPassPinState(true)
                // go to next screen
                binding.mess.text = "OK"
                binding.mess.setTextColor(Color.WHITE)
                gotoNextScreen()
            } else {
                pincodeInvalid()
            }
        }
        Keyboard.hideKeyboard(requireActivity())
        binding.btnConfirmPin.setOnClickListener {
            val value = binding.pinview.value
            if (TextUtils.isEmpty(value)) {
                pincodeInvalid()
            } else {
                //NOTE: 0000 - pin code default - is just only for testing false, do not use
                if (value.length == 4 && value.equals("0000")) {
                    Preference.getInstance(requireContext().applicationContext)
                        .setByPassPinState(true)
                    // go to next screen
                    binding.mess.text = "OK"
                    binding.mess.setTextColor(Color.WHITE)
                    gotoNextScreen()
                } else {
                    pincodeInvalid()
                }
            }

        }
    }

    private fun pincodeInvalid() {
        Toast.makeText(
            activity?.applicationContext,
            "Pin Invalid",
            Toast.LENGTH_SHORT
        ).show()
        binding.mess.text = "Pin Invalid"
        binding.mess.setTextColor(Color.RED)
        shakeView(binding.pinview)
        shakeView(binding.mess)
        Preference.getInstance(requireContext().applicationContext).setByPassPinState(false)
    }

    private fun gotoNextScreen() {
        Keyboard.hideKeyboard(requireActivity())
        parentFragmentManager.commit {
            replace<DigitalKeyFragment>(R.id.fragment_container_view)
            setReorderingAllowed(true)
            addToBackStack(null)
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }

    override fun onPause() {
        super.onPause()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        activity?.window?.decorView?.systemUiVisibility = 0
        show()
    }

    private fun toggle() {
        if (visible) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        visible = false
    }

    @Suppress("InlinedApi")
    private fun show() {
        // Show the system bar
        visible = true
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }


    companion object {
        private const val AUTO_HIDE = true
        private const val AUTO_HIDE_DELAY_MILLIS = 3000
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun shakeView(view: View) {
        val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
        view.startAnimation(anim)
    }
}