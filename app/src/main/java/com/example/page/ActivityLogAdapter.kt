package com.example.page

import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.page.api.ActivityLog
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class ActivityLogAdapter(private var items: List<ActivityLog>) :
    RecyclerView.Adapter<ActivityLogAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.list_item_activity_log, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], position + 1)
    }

    override fun getItemCount(): Int = items.size

    fun submitList(list: List<ActivityLog>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAvatar: TextView = itemView.findViewById(R.id.tv_avatar)
        private val tvMessage: TextView = itemView.findViewById(R.id.tv_message)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp)
        private val tvLinkChip: TextView = itemView.findViewById(R.id.tv_link_chip)
        private val tvStatusChip: TextView = itemView.findViewById(R.id.tv_status_chip)
        private val tvRelative: TextView = itemView.findViewById(R.id.tv_relative)

        fun bind(log: ActivityLog, serial: Int) {
            // pick performer name (prefer performedByName then userName)
            val name = log.performedByName ?: log.userName ?: "User"
            tvAvatar.text = initialsOf(name)

            // friendly action text
            val actor = name
            val action = when {
                !log.activityName.isNullOrBlank() -> prettyActivityText(log.activityName)
                else -> "did something"
            }

            // target (center or form) to display inline and as chip
            val target = log.centerName ?: log.formName
            val base = if (target != null) "$actor $action on " else "$actor $action"
            val full = if (target != null) "$base$target" else base

            // make actor bold & blue
            val spannable = SpannableString(full)
            val actorStart = 0
            val actorEnd = actor.length
            spannable.setSpan(StyleSpan(android.graphics.Typeface.BOLD), actorStart, actorEnd, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(ForegroundColorSpan(0xFF1E6BF3.toInt()), actorStart, actorEnd, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
            tvMessage.text = spannable

            // chips
            if (!target.isNullOrBlank()) {
                tvLinkChip.visibility = View.VISIBLE
                tvLinkChip.text = target
            } else {
                tvLinkChip.visibility = View.GONE
            }

            // show reason or activity name on status chip for deactivation/restore/delete or explicit reason_text
            val a = log.activityName?.uppercase(Locale.ROOT) ?: ""
            if (a.contains("RESTORE") || a.contains("DEACTIVAT") || a.contains("DELETE") || !log.reasonText.isNullOrBlank()) {
                tvStatusChip.visibility = View.VISIBLE
                tvStatusChip.text = if (!log.reasonText.isNullOrBlank()) log.reasonText else a.replace('_', ' ')
            } else {
                tvStatusChip.visibility = View.GONE
            }

            // exact timestamp (right) and relative
            tvTimestamp.text = log.timestamp ?: ""
            tvRelative.text = relativeTimeText(log.timestamp)
        }

        private fun initialsOf(name: String): String {
            val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
            return when {
                parts.isEmpty() -> "U"
                parts.size == 1 -> parts[0].substring(0, 1).uppercase(Locale.ROOT)
                else -> (parts[0].substring(0, 1) + parts[1].substring(0, 1)).uppercase(Locale.ROOT)
            }
        }

        private fun prettyActivityText(raw: String): String {
            return when (raw.uppercase(Locale.ROOT)) {
                "USER_LOGIN" -> "logged in"
                "CENTER_DEACTIVATED" -> "deactivated"
                "CENTER_RESTORED" -> "restored"
                "VIEW_SPECIFIC_CENTER" -> "viewed"
                else -> raw.replace('_', ' ').lowercase(Locale.ROOT)
            }
        }

        private fun relativeTimeText(ts: String?): String {
            if (ts.isNullOrBlank()) return ""
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            fmt.timeZone = TimeZone.getDefault()
            return try {
                val date = fmt.parse(ts)
                date?.let {
                    val diff = System.currentTimeMillis() - it.time
                    val minutes = (diff / 60000).toInt()
                    when {
                        minutes < 1 -> "just now"
                        minutes < 60 -> "about $minutes min ago"
                        minutes < 60 * 24 -> "about ${minutes / 60} hours ago"
                        else -> "about ${minutes / (60 * 24)} days ago"
                    }
                } ?: ""
            } catch (e: ParseException) {
                ""
            }
        }
    }
}
