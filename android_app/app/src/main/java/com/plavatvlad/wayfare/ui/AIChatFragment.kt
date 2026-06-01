package com.plavatvlad.wayfare.ui

import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.text.TextUtilsCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.plavatvlad.wayfare.BuildConfig
import com.plavatvlad.wayfare.R
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.graphics.Typeface
import android.widget.ScrollView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AIChatFragment : BottomSheetDialogFragment() {

    private lateinit var chatHistory: TextView
    private lateinit var chatInput: EditText
    private lateinit var sendBtn: Button
    private lateinit var clearBtn: Button
    private lateinit var scrollView: ScrollView
    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private var isLoading = false
    private val loadingLine = formatLine("AI:", "typing...")
    private val chatLines = mutableListOf<CharSequence>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_ai_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dialog = dialog as? BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        )

        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            val screenHeight = resources.displayMetrics.heightPixels
            it.layoutParams.height = (screenHeight * 0.75).toInt()
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isDraggable = false
            behavior.skipCollapsed = true
        }

        chatHistory = view.findViewById(R.id.chatHistory)
        chatInput = view.findViewById(R.id.chatInput)
        sendBtn = view.findViewById(R.id.sendBtn)
        clearBtn = view.findViewById(R.id.clearBtn)
        scrollView = view.findViewById(R.id.scrollArea)

        loadChat()

        sendBtn.setOnClickListener {
            val message = chatInput.text.toString().trim()
            if (message.isEmpty()) return@setOnClickListener


            val userLine = formatLine("You:", message)
            chatLines.add(userLine)
            renderChat()
            saveMessage("user", message)

            chatInput.setText("")

            sendToAI()
        }
        clearBtn.setOnClickListener {
            showClearConfirmation()
        }
    }

    private fun sendToAI() {
        isLoading = true
        val messages = JSONArray()

        // system context
        messages.put(
            JSONObject().apply {
                put("role", "system")
                put(
                    "content",
                    "You are Wayfare Companion, a helpful travel assistant. " +
                            "Keep answers short, practical, and location-focused."
                )
            }
        )

        // rebuild messages from chatLines (simple MVP approach)
        chatLines.forEach { line ->
            val text = line.toString()

            when {
                text.startsWith("\n\nYou:") -> {
                    messages.put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", text.removePrefix("\n\nYou:").trim())
                        }
                    )
                }
                text.startsWith("\n\nAI:") -> {
                    messages.put(
                        JSONObject().apply {
                            put("role", "assistant")
                            put("content", text.removePrefix("\n\nAI:").trim())
                        }
                    )
                }
            }
        }

        val bodyJson = JSONObject().apply {
            put("model", "openrouter/free")
            put("messages", messages)
        }

        chatLines.add(loadingLine)
        renderChat()

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .addHeader("Authorization", "Bearer " + BuildConfig.OPENROUTER_KEY)
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Log.e("AIAPI", "Request failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val raw = response.body?.string() ?: return

                    try {
                        val json = JSONObject(raw)

                        val reply = json
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")

                        handler.post {
                            appendAIMessage(reply)
                        }

                    } catch (e: Exception) {
                        Log.e("AIAPI", "Parse error", e)
                    }
                }
            }
        })
    }

    private fun appendAIMessage(reply: String) {
        isLoading = false
        chatLines.remove(loadingLine)
        val aiLine = formatLine("AI:", reply)
        chatLines.add(aiLine)
        saveMessage("assistant", reply.trim())

        renderChat()
    }

    private fun renderChat() {
        chatHistory.text = android.text.TextUtils.concat(*chatLines.toTypedArray())
        scrollToBottom()
    }

    private fun formatLine(prefix: String, message: String): CharSequence {
        val full = "\n\n$prefix $message"

        // Remove markdown markers while tracking bold ranges
        val cleanText = StringBuilder()
        val boldRanges = mutableListOf<IntRange>()

        var i = 0
        var boldStart = -1

        while (i < full.length) {
            if (i + 1 < full.length && full[i] == '*' && full[i + 1] == '*') {
                if (boldStart == -1) {
                    boldStart = cleanText.length
                } else {
                    boldRanges += boldStart until cleanText.length
                    boldStart = -1
                }
                i += 2
            } else {
                cleanText.append(full[i])
                i++
            }
        }

        val spannable = SpannableString(cleanText.toString())

        // Prefix bold
        val prefixStart = 2
        val prefixEnd = prefixStart + prefix.length

        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            prefixStart,
            prefixEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Prefix underline
        spannable.setSpan(
            UnderlineSpan(),
            prefixStart,
            prefixEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Markdown bold
        boldRanges.forEach { range ->
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                range.first,
                range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return spannable
    }

    private fun scrollToBottom() {
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun saveMessage(role: String, text: String) {
        val message = hashMapOf(
            "role" to role,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Log.d("AIAPI", "sent history")
        db.collection("users")
            .document(uid)
            .collection("messages")
            .add(message)
    }

    private fun loadChat() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users")
            .document(uid)
            .collection("messages")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->
                chatLines.clear()

                for (doc in result) {
                    val role = doc.getString("role") ?: ""
                    val text = doc.getString("text") ?: ""

                    val line = if (role == "user") {
                        formatLine("You:", text)
                    } else {
                        formatLine("AI:", text)
                    }

                    chatLines.add(line)
                }

                renderChat()
            }
    }

    private fun showClearConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear conversation")
            .setMessage("Are you sure you want to delete the entire conversation?")
            .setPositiveButton("Clear") { _, _ ->
                clearConversation()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearConversation() {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val messagesRef = db
            .collection("users")
            .document(uid)
            .collection("messages")

        messagesRef.get()
            .addOnSuccessListener { snapshot ->

                val batch = db.batch()

                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                batch.commit()
                    .addOnSuccessListener {

                        chatLines.clear()
                        renderChat()

                        Log.d("AIChat", "Conversation cleared")
                    }
                    .addOnFailureListener {
                        Log.e("AIChat", "Failed to clear conversation", it)
                    }
            }
    }
}
