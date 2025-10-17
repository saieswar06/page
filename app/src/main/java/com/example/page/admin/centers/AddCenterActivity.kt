package com.example.page.admin.centers

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.page.R
import com.example.page.api.AddCenterRequest
import com.example.page.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddCenterActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_add_center)

        etName = findViewById(R.id.etName)
        etAddress = findViewById(R.id.etAddress)
        btnSave = findViewById(R.id.btnSave)

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val address = etAddress.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Center name is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = AddCenterRequest(
                center_name = name,
                address = if (address.isEmpty()) null else address
            )

            addCenter(request)
        }
    }

    private fun addCenter(request: AddCenterRequest) {
        RetrofitClient.getInstance(this).addCenter(request)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@AddCenterActivity, "✅ Center added successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                        Toast.makeText(this@AddCenterActivity, "❌ Failed: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(this@AddCenterActivity, "⚠️ Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }
}
