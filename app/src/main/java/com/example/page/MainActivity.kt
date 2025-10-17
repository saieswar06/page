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
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var signInClient: SignInClient

    private val WEB_CLIENT_ID =
        "1029622596309-k8impr2gjmpupjgu5kvhpb6j7dvc7u49.apps.googleusercontent.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        signInClient = Identity.getSignInClient(this)

        // ✅ ECCE Worker Manual Login
        binding.loginButton.setOnClickListener {
            val phone = binding.MobileNumber.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                loginECCEWorker(phone, password)
            }
        }

        // ✅ Google Sign-In
        binding.googleSignInButton.setOnClickListener { startGoogleSignIn() }
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
                val idToken = credential.googleIdToken ?: ""

                if (idToken.isNotEmpty()) {
                    Toast.makeText(this, "Signed in with Google", Toast.LENGTH_SHORT).show()
                    loginWithGoogle(idToken)
                } else {
                    Toast.makeText(this, "Failed to retrieve Google token", Toast.LENGTH_SHORT)
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

    // ✅ ECCE Worker Login
    private fun loginECCEWorker(phone: String, password: String) {
        val api = RetrofitClient.getInstance(this)

        val request = LoginRequest(
            mobile = phone,
            password = password,
            loginType = "ecce"
        )

        Log.d("API_REQUEST", "Sending ECCE Login: ${Gson().toJson(request)}")

        api.login(request).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    onLoginSuccess(loginResponse)
                } else {
                    val error = response.errorBody()?.string()
                    Log.e("API", "Login failed: ${response.code()} $error")
                    Toast.makeText(
                        this@MainActivity,
                        "Invalid credentials or unauthorized (${response.code()})",
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

    // ✅ Google Login
    private fun loginWithGoogle(idToken: String) {
        val api = RetrofitClient.getInstance(this)
        val body = mapOf("idToken" to idToken)

        api.googleLogin(body).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    onLoginSuccess(loginResponse)
                } else {
                    val error = response.errorBody()?.string()
                    Log.e("API", "Google Login failed: ${response.code()} $error")
                    Toast.makeText(
                        this@MainActivity,
                        "Google login failed (${response.code()})",
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
                Log.e("API", "Google login failed", t)
            }
        })
    }

    // ✅ Common success handler
    private fun onLoginSuccess(loginResponse: LoginResponse) {
        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()

        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        prefs.edit().apply {
            putString("token", loginResponse.token)
            putString("name", loginResponse.user.name)
            putString("email", loginResponse.user.email)
            putString("role", "ecce")
            apply()
        }

        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}
