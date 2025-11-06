package com.example.page

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.page.api.ActivityLog
import java.util.*

class ActivityLogAdapter(private var items: List<ActivityLog>) :
    RecyclerView.Adapter<ActivityLogAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.list_item_activity_log, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
        // ensure the item height divides the RecyclerView height into 10 rows if possible
        val parent = holder.itemView.parent
        if (parent is RecyclerView) {
            val total = parent.height
            if (total > 0) {
                val params = holder.itemView.layoutParams
                val desired = total / 10
                if (params.height != desired) {
                    params.height = desired
                    holder.itemView.layoutParams = params
                }
            } else {
                // fallback: ensure a reasonable min height (72dp)
                holder.itemView.minimumHeight = (72 * holder.itemView.resources.displayMetrics.density).toInt()
            }
        } else {
            holder.itemView.minimumHeight = (72 * holder.itemView.resources.displayMetrics.density).toInt()
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(list: List<ActivityLog>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: CardView = itemView.findViewById(R.id.card_row)
        private val ivStatus: ImageView = itemView.findViewById(R.id.iv_status)
        private val tvMessage: TextView = itemView.findViewById(R.id.tv_message)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp)

        fun bind(log: ActivityLog) {
            val actor = log.performedByName ?: log.userName ?: "User"

            val raw = log.activityName ?: ""
            val action = friendlyActionText(raw, log.activityId)

            // Improved target logic
            val target = when {
                !log.centerName.isNullOrBlank() -> log.centerName
                !log.targetedField.isNullOrBlank() -> log.targetedField
                !log.pageType.isNullOrBlank() -> log.pageType
                !log.formName.isNullOrBlank() -> log.formName
                !log.reasonText.isNullOrBlank() -> log.reasonText
                log.descriptionId != null -> "Details #${log.descriptionId}"
                log.recordId != null -> "Record #${log.recordId}"
                else -> ""
            }

            val ts = log.timestamp ?: ""

            // single-line message
            val msg = if (!target.isNullOrBlank()) "$actor $action — $target" else "$actor $action"
            tvMessage.text = msg
            tvTimestamp.text = ts

            // icon selection
            val iconName = iconFor(raw, log.activityId)
            val context = itemView.context
            val iconRes = context.resources.getIdentifier(iconName, "drawable", context.packageName)
            if (iconRes != 0) {
                ivStatus.setImageResource(iconRes)
            } else {
                ivStatus.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_info))
            }

            // set card background color based on action
            val colorHex = colorFor(raw, log.activityId)
            try {
                card.setCardBackgroundColor(Color.parseColor(colorHex))
            } catch (_: Exception) {
                card.setCardBackgroundColor(Color.WHITE)
            }

            // optionally tint icon for contrast (darker)
            try {
                val iconTint = when {
                    action.contains("login", true) -> "#2E7D32"
                    action.contains("logout", true) -> "#D84315"
                    action.contains("create", true) -> "#0D47A1"
                    action.contains("update", true) -> "#F57F17"
                    action.contains("delete", true) -> "#B71C1C"
                    action.contains("restore", true) -> "#00796B"
                    action.contains("view", true) -> "#37474F"
                    else -> "#666666"
                }
                ivStatus.setColorFilter(Color.parseColor(iconTint))
            } catch (_: Exception) { /* ignore */ }
        }

        private fun friendlyActionText(raw: String, id: String?): String {
            if (!raw.isNullOrBlank()) {
                return when (raw.uppercase(Locale.ROOT)) {
                    "USER_LOGIN" -> "logged in"
                    "USER_LOGOUT" -> "logged out"
                    "USER_CREATED", "CREATED" -> "created"
                    "USER_UPDATED", "UPDATED" -> "updated"
                    "USER_DELETED", "DELETED" -> "deleted"
                    "USER_RESTORED", "RESTORED" -> "restored"
                    "VIEW_SPECIFIC_CENTER", "VIEW_SPECIFIC_USER", "VIEW" -> "viewed"
                    else -> raw.replace('_', ' ').lowercase(Locale.getDefault())
                }
            }
            val idAsInt = id?.toIntOrNull()
            return when (idAsInt) {
                11 -> "logged in"
                12 -> "logged out"
                1, 6 -> "created"
                2, 7 -> "updated"
                3, 8 -> "deleted"
                21, 22 -> "restored"
                23, 24 -> "viewed"
                else -> "performed an action"
            }
        }

        private fun iconFor(raw: String?, id: String?): String {
            val idAsInt = id?.toIntOrNull()
            return when {
                raw?.contains("LOGIN", true) == true || idAsInt == 11 -> "ic_login"
                raw?.contains("LOGOUT", true) == true || idAsInt == 12 -> "ic_logout"
                raw?.contains("DELETE", true) == true || idAsInt == 3 || idAsInt == 8 -> "ic_delete"
                raw?.contains("RESTORE", true) == true || idAsInt == 21 || idAsInt == 22 -> "ic_restore"
                raw?.contains("UPDATE", true) == true || idAsInt == 2 || idAsInt == 7 -> "ic_edit"
                raw?.contains("CREATE", true) == true || idAsInt == 1 || idAsInt == 6 -> "ic_add"
                raw?.contains("VIEW", true) == true || idAsInt == 23 || idAsInt == 24 -> "ic_view"
                else -> "ic_info"
            }
        }

        private fun colorFor(raw: String?, id: String?): String {
            val idAsInt = id?.toIntOrNull()
            return when {
                raw?.contains("LOGIN", true) == true || idAsInt == 11 -> "#E6F9EC"    // pale green
                raw?.contains("LOGOUT", true) == true || idAsInt == 12 -> "#FFF6E0"   // pale orange
                raw?.contains("DELETE", true) == true || idAsInt == 3 || idAsInt == 8 -> "#FFF1F2" // pale red
                raw?.contains("RESTORE", true) == true || idAsInt == 21 || idAsInt == 22 -> "#E8F7F3" // pale teal
                raw?.contains("UPDATE", true) == true || idAsInt == 2 || idAsInt == 7 -> "#FFFDF0" // pale yellow
                raw?.contains("CREATE", true) == true || idAsInt == 1 || idAsInt == 6 -> "#EAF0FF" // pale blue
                raw?.contains("VIEW", true) == true || idAsInt == 23 || idAsInt == 24 -> "#F3F6FF" // very pale blue
                else -> "#FFFFFF"
            }
        }
    }
}
