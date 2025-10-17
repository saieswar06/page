package com.example.page

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.page.api.LoginRequest
import com.example.page.api.LoginResponse
import com.example.page.api.RetrofitClient
import com.example.page.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var signInClient: SignInClient

    // ✅ Web Client ID (matches your backend .env)
    private val WEB_CLIENT_ID =
        "1029622596309-k8impr2gjmpupjgu5kvhpb6j7dvc7u49.apps.googleusercontent.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Initialize Google Sign-In Client
        signInClient = Identity.getSignInClient(this)

        // ✅ Manual Login
        binding.loginButton.setOnClickListener {
            val username = binding.MobileNumber.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(username, password, "manual")
            }
        }

        // ✅ Google Sign-In
        binding.googleSignInButton.setOnClickListener {
            startGoogleSignIn()
        }
    }

    // ✅ Start Google Sign-In
    private fun startGoogleSignIn() {
        val signInRequest = GetSignInIntentRequest.builder()
            .setServerClientId(WEB_CLIENT_ID)
            .build()

        signInClient.getSignInIntent(signInRequest)
            .addOnSuccessListener { result ->
                googleSignInLauncher.launch(IntentSenderRequest.Builder(result.intentSender).build())
            }
            .addOnFailureListener { e ->
                Log.e("GoogleSignIn", "Sign-in failed: ${e.localizedMessage}", e)
                Toast.makeText(this, "Google Sign-In unavailable: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    // ✅ Handle Google Sign-In Result
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            try {
                val credential = signInClient.getSignInCredentialFromIntent(result.data)
                val email = credential.id ?: ""
                val idToken = credential.googleIdToken ?: ""

                if (email.isNotEmpty()) {
                    Toast.makeText(this, "Signed in as $email", Toast.LENGTH_SHORT).show()
                    loginUser(email, idToken, "google")
                } else {
                    Toast.makeText(this, "Failed to retrieve Google account", Toast.LENGTH_SHORT)
                        .show()
                }

            } catch (e: ApiException) {
                Log.e("GoogleSignIn", "Error: ${e.statusCode}", e)
                Toast.makeText(
                    this,
                    "Google Sign-In error: ${e.statusCode}",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e("GoogleSignIn", "Exception: ${e.message}", e)
                Toast.makeText(this, "Unexpected error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

    // ✅ Unified Login Function
    private fun loginUser(username: String, password: String, loginType: String) {
        val request = LoginRequest(username, password, loginType)
        val api = RetrofitClient.getInstance(this)

        api.login(request).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    if (loginResponse.token.isNotEmpty()) {
                        Toast.makeText(
                            this@MainActivity,
                            "Login Successful",
                            Toast.LENGTH_SHORT
                        ).show()

                        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("token", loginResponse.token)
                            putString("name", loginResponse.user.name)
                            putString("email", loginResponse.user.email)
                            apply()
                        }

                        startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Invalid credentials",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Login failed: ${response.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(
                    this@MainActivity,
                    "Server Error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("API", "Login failed", t)
            }
        })
    }
}
