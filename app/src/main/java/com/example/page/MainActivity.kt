package com.example.page

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.example.page.databinding.ActivityMainBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var credentialManager: CredentialManager

    // TODO: Replace with your actual Web Client ID from Google Cloud Console
    private val WEB_CLIENT_ID = "1029622596309-k8impr2gjmpupjgu5kvhpb6j7dvc7u49.apps.googleusercontent.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        credentialManager = CredentialManager.create(this)

        binding.btnGoogleSignIn.setOnClickListener {
            startGoogleSignIn()
        }
    }

    private fun startGoogleSignIn() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@MainActivity,
                    request = request
                )
                handleSignInSuccess(result)
            } catch (e: GetCredentialException) {
                handleSignInError(e)
            }
        }
    }

    private fun handleSignInSuccess(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        val displayName = googleIdTokenCredential.displayName
                        val email = googleIdTokenCredential.id

                        Log.d("GoogleSignIn", "✅ ID Token: $idToken")
                        Log.d("GoogleSignIn", "✅ Display Name: $displayName")
                        Log.d("GoogleSignIn", "✅ Email: $email")

                        Toast.makeText(
                            this,
                            "Signed in successfully as $displayName",
                            Toast.LENGTH_LONG
                        ).show()

                        // TODO: Send `idToken` to your backend for verification
                        // Example: sendTokenToBackend(idToken)

                    } catch (e: Exception) {
                        Log.e("GoogleSignIn", "❌ Error parsing credential: ${e.message}", e)
                        Toast.makeText(this, "Error parsing sign-in data", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("GoogleSignIn", "⚠️ Unexpected credential type: ${credential.type}")
                }
            }

            else -> {
                Log.e("GoogleSignIn", "⚠️ Unexpected credential type")
            }
        }
    }

    private fun handleSignInError(e: GetCredentialException) {
        Log.e("GoogleSignIn", "❌ Sign-in failed: ${e.message}", e)
        Toast.makeText(this, "Sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }

    // Optional: Method to send token to your backend
    private fun sendTokenToBackend(idToken: String) {
        // TODO: Implement your backend API call here
        // Example using Retrofit:
        // apiService.verifyGoogleToken(idToken)
    }
}