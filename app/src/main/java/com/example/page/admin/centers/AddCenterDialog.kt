package com.example.page.admin.centers

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.example.page.R
import com.example.page.api.AddCenterRequest

class AddCenterDialog(
    context: Context,
    private val initialData: AddCenterRequest? = null,
    private val onSubmit: (AddCenterRequest) -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_add_center)

        val etName = findViewById<EditText>(R.id.etName)
        val etAddress = findViewById<EditText>(R.id.etAddress)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // ✅ Pre-fill data if editing
        initialData?.let {
            etName.setText(it.center_name)
            etAddress.setText(it.address)
        }

        // ✅ Save button click
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val address = etAddress.text.toString().trim()

            if (name.isEmpty()) {
                etName.error = "Center name is required"
                return@setOnClickListener
            }

            val request = AddCenterRequest(
                center_name = name,
                address = if (address.isEmpty()) null else address
            )

            onSubmit(request)
            dismiss()
        }
    }
}
