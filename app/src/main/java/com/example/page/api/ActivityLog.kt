package com.example.page.api

import com.google.gson.annotations.SerializedName

data class ActivityLog(
    @SerializedName("activity_id")
    val activityId: String?,

    @SerializedName("activity_name")
    val activityName: String?,

    @SerializedName("form_name")
    val formName: String?,

    // the server uses different names for the changed field/description/target
    @SerializedName("targeted_field")
    val targetedField: String?,

    @SerializedName("description_id")
    val descriptionId: String?,

    @SerializedName("reason_text")
    val reasonText: String?,

    @SerializedName("performed_by_name")
    val performedByName: String?,

    @SerializedName("user_name")
    val userName: String?,

    @SerializedName("center_name")
    val centerName: String?,

    @SerializedName("center_code")
    val centerCode: String?,

    @SerializedName("performed_on")
    val recordId: String?,

    @SerializedName("page_type")
    val pageType: String?,

    // server uses "timestamps" in your logs
    @SerializedName("timestamps")
    val timestamp: String?
)
