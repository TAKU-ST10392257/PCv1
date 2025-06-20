// Takudzwa Murwira – ST10392257, Jason Daniel Isaacs – ST10039248, Daniel Gorin – ST10438307 and Moegammad-Yaseen Salie – ST10257795
//PROG7313

//References:
//            https://medium.com/@SeanAT19/how-to-use-mpandroidchart-in-android-studio-c01a8150720f
//            https://chatgpt.com/
//            https://www.youtube.com/playlist?list=PLWz5rJ2EKKc8SmtMNw34wvYkqj45rV1d3
//            https://www.youtube.com/playlist?list=PLSrm9z4zp4mEPOfZNV9O-crOhoMa0G2-o
package vcmsa.projects.pcv1.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.navigation.activity
import kotlinx.coroutines.launch
import vcmsa.projects.pcv1.R
import vcmsa.projects.pcv1.data.AppDatabase
import vcmsa.projects.pcv1.data.UserRepository
import vcmsa.projects.pcv1.databinding.FragmentProfileBinding
import vcmsa.projects.pcv1.ui.auth.LandingActivity
import vcmsa.projects.pcv1.util.DarkModeManager
import vcmsa.projects.pcv1.util.SessionManager
import androidx.core.net.toUri

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var session: SessionManager
    private lateinit var repo: UserRepository
    private var currentUserId: Int = -1
    // Image picker launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                // Request persistent URI permission
                requireContext().contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // ignore if permission not needed or already granted
            }

            binding.imageProfile.setImageURI(it)
            binding.textProfileInitial.visibility = View.GONE

            lifecycleScope.launch {
                repo.updateProfileImage(currentUserId, it.toString())
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate layout and initialize session and repository
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        session = SessionManager(requireContext())
        currentUserId = session.getUserId()
        repo = UserRepository(AppDatabase.getInstance(requireContext()).userDao())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Set username text
        val username = session.getUsername() ?: "User"
        binding.textUsername.text = username

        // Contact Us button listener
        binding.btnContactUs.setOnClickListener { // Access button directly via binding
            sendEmail()
        }
        // Load and display profile image or fallback
        lifecycleScope.launch {
            val uriString = repo.getProfileImage(currentUserId)
            if (!uriString.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(uriString)
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    inputStream?.close() // Validate URI access
                    binding.imageProfile.setImageURI(uri)
                    binding.textProfileInitial.visibility = View.GONE
                } catch (e: Exception) {
                    setFallbackImage(username)
                }
            } else {
                setFallbackImage(username)
            }
        }
        // Image click listener to pick new image
        binding.imageProfile.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        // Logout button listener
        binding.btnLogout.setOnClickListener {
            session.clear()
            val intent = Intent(requireContext(), LandingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
        // Dark mode switch setup
        binding.switchDarkMode.isChecked = DarkModeManager.isDarkModeEnabled(requireContext())
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            DarkModeManager.setDarkMode(requireContext(), isChecked)
        }
    }

    private fun setFallbackImage(username: String) {
        // Set default profile image and initial
        binding.imageProfile.setImageResource(R.drawable.circle_background)
        binding.textProfileInitial.text = username.firstOrNull()?.uppercase() ?: "?"
        binding.textProfileInitial.visibility = View.VISIBLE
    }

    private fun sendEmail() {
        // Compose and launch email intent
        val recipientEmail = "st10392257@vcconnect.edu.za" // **REPLACE THIS**
        val subject = "Contact Us Feedback"
        // val body = "Hello..."

        val mailto = "mailto:" + Uri.encode(recipientEmail) +
                "?subject=" + Uri.encode(subject)
        // + "&body=" + Uri.encode(body)

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = mailto.toUri()
        }

        if (activity?.packageManager?.resolveActivity(emailIntent, 0) != null) {
            startActivity(emailIntent)
        } else {
            Toast.makeText(context, "No email app found.", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        // Clear binding reference
        _binding = null
    }
}
