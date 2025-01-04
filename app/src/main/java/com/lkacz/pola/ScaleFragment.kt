// Filename: ScaleFragment.kt
package com.lkacz.pola

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData

class ScaleFragment : Fragment() {

    private var header: String? = null
    private var body: String? = null
    private var item: String? = null
    private var responses: List<Pair<String, String?>> = emptyList()

    private lateinit var logger: Logger
    private val selectedResponse = MutableLiveData<String>()

    private val mediaPlayers = mutableListOf<MediaPlayer>()
    private lateinit var videoView: VideoView
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logger = Logger.getInstance(requireContext())
        arguments?.let {
            header = it.getString(ARG_HEADER)
            body = it.getString(ARG_BODY)
            item = it.getString(ARG_ITEM)
            val isBranch = it.getBoolean(ARG_IS_BRANCH, false)
            if (isBranch) {
                val rawList = it.getStringArrayList(ARG_BRANCH_RESPONSES) ?: arrayListOf()
                responses = rawList.map { raw ->
                    val split = raw.split("||")
                    val disp = split.getOrNull(0) ?: ""
                    val lbl = split.getOrNull(1)?.ifEmpty { null }
                    disp to lbl
                }
            } else {
                val normalList = it.getStringArrayList(ARG_NORMAL_RESPONSES) ?: arrayListOf()
                responses = normalList.map { resp -> resp to null }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scale, container, false)

        val headerTextView: TextView = view.findViewById(R.id.headerTextView)
        webView = view.findViewById(R.id.htmlSnippetWebView)
        webView.visibility = View.GONE
        val bodyTextView: TextView = view.findViewById(R.id.introductionTextView)
        videoView = view.findViewById(R.id.videoView2)
        videoView.visibility = View.GONE
        val itemTextView: TextView = view.findViewById(R.id.itemTextView)
        val buttonContainer: LinearLayout = view.findViewById(R.id.buttonContainer)

        setupWebView()

        val resourcesFolderUri = ResourcesFolderManager(requireContext()).getResourcesFolderUri()

        val cleanHeader = parseAndPlayAudioIfAny(header.orEmpty(), resourcesFolderUri)
        val refinedHeader = checkAndLoadHtml(cleanHeader, resourcesFolderUri)
        checkAndPlayMp4(header.orEmpty(), resourcesFolderUri)
        headerTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), resourcesFolderUri, refinedHeader)
        headerTextView.textSize = FontSizeManager.getHeaderSize(requireContext())
        headerTextView.setTextColor(ColorManager.getHeaderTextColor(requireContext()))
        applyHeaderAlignment(headerTextView)

        val cleanBody = parseAndPlayAudioIfAny(body.orEmpty(), resourcesFolderUri)
        val refinedBody = checkAndLoadHtml(cleanBody, resourcesFolderUri)
        checkAndPlayMp4(body.orEmpty(), resourcesFolderUri)
        bodyTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), resourcesFolderUri, refinedBody)
        bodyTextView.textSize = FontSizeManager.getBodySize(requireContext())
        bodyTextView.setTextColor(ColorManager.getBodyTextColor(requireContext()))
        applyBodyAlignment(bodyTextView)

        val cleanItem = parseAndPlayAudioIfAny(item.orEmpty(), resourcesFolderUri)
        val refinedItem = checkAndLoadHtml(cleanItem, resourcesFolderUri)
        checkAndPlayMp4(item.orEmpty(), resourcesFolderUri)
        itemTextView.text = HtmlMediaHelper.toSpannedHtml(requireContext(), resourcesFolderUri, refinedItem)
        itemTextView.textSize = FontSizeManager.getItemSize(requireContext())
        itemTextView.setTextColor(ColorManager.getItemTextColor(requireContext()))

        val density = resources.displayMetrics.density
        val marginDp = SpacingManager.getResponseButtonMargin(requireContext())
        val marginPx = (marginDp * density + 0.5f).toInt()
        val paddingHdp = SpacingManager.getResponseButtonPaddingHorizontal(requireContext())
        val paddingHpx = (paddingHdp * density + 0.5f).toInt()
        val paddingVdp = SpacingManager.getResponseButtonPaddingVertical(requireContext())
        val paddingVpx = (paddingVdp * density + 0.5f).toInt()
        val extraSpacingDp = SpacingManager.getResponseSpacing(requireContext())
        val extraSpacingPx = (extraSpacingDp * density + 0.5f).toInt()

        responses.forEachIndexed { index, (displayText, label) ->
            val parsedText = parseAndPlayAudioIfAny(displayText, resourcesFolderUri)
            val refinedButton = checkAndLoadHtml(parsedText, resourcesFolderUri)
            checkAndPlayMp4(displayText, resourcesFolderUri)

            val button = Button(context).apply {
                text = HtmlMediaHelper.toSpannedHtml(requireContext(), resourcesFolderUri, refinedButton)
                textSize = FontSizeManager.getResponseSize(requireContext())
                setTextColor(ColorManager.getResponseTextColor(requireContext()))
                setBackgroundColor(ColorManager.getButtonBackgroundColor(requireContext()))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = if (index == 0) marginPx else (marginPx + extraSpacingPx)
                    bottomMargin = marginPx
                    leftMargin = marginPx
                    rightMargin = marginPx
                }
                setPadding(paddingHpx, paddingVpx, paddingHpx, paddingVpx)

                setOnClickListener {
                    selectedResponse.value = displayText
                    logger.logScaleFragment(
                        header ?: "Default Header",
                        body ?: "Default Body",
                        item ?: "Default Item",
                        index + 1,
                        displayText
                    )
                    val mainActivity = activity as? MainActivity
                    if (label.isNullOrEmpty()) {
                        mainActivity?.loadNextFragment()
                    } else {
                        mainActivity?.loadFragmentByLabel(label)
                    }
                }
            }
            buttonContainer.addView(button)
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayers.forEach { it.release() }
        mediaPlayers.clear()
        if (this::videoView.isInitialized && videoView.isPlaying) {
            videoView.stopPlayback()
        }
        if (this::webView.isInitialized) {
            webView.destroy()
        }
    }

    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        webView.webChromeClient = WebChromeClient()
    }

    private fun parseAndPlayAudioIfAny(text: String, resourcesFolderUri: Uri?): String {
        return AudioPlaybackHelper.parseAndPlayAudio(
            context = requireContext(),
            rawText = text,
            mediaFolderUri = resourcesFolderUri,
            mediaPlayers = mediaPlayers
        )
    }

    private fun checkAndPlayMp4(text: String, resourcesFolderUri: Uri?) {
        val pattern = Regex("<([^>]+\\.mp4(?:,[^>]+)?)>", RegexOption.IGNORE_CASE)
        val match = pattern.find(text) ?: return
        val group = match.groupValues[1]
        val segments = group.split(",")
        val fileName = segments[0].trim()
        val volume = if (segments.size > 1) {
            val vol = segments[1].trim().toFloatOrNull()
            if (vol != null && vol in 0f..100f) vol / 100f else 1.0f
        } else 1.0f

        if (resourcesFolderUri != null) {
            val parentFolder = DocumentFile.fromTreeUri(requireContext(), resourcesFolderUri) ?: return
            val videoFile = parentFolder.findFile(fileName)
            if (videoFile != null && videoFile.exists() && videoFile.isFile) {
                videoView.visibility = View.VISIBLE
                playVideoFile(videoFile.uri, volume)
            }
        }
    }

    private fun playVideoFile(videoUri: Uri, volume: Float) {
        videoView.setVideoURI(videoUri)
        videoView.setOnPreparedListener { mp ->
            mp.start()
            mp.setVolume(volume, volume)
        }
    }

    private fun checkAndLoadHtml(text: String, resourcesFolderUri: Uri?): String {
        if (text.isBlank() || resourcesFolderUri == null) return text
        val pattern = Regex("<([^>]+\\.html)>", RegexOption.IGNORE_CASE)
        val match = pattern.find(text) ?: return text
        val matchedFull = match.value
        val fileName = match.groupValues[1].trim()

        val parentFolder = DocumentFile.fromTreeUri(requireContext(), resourcesFolderUri)
            ?: return text
        val htmlFile = parentFolder.findFile(fileName)
        if (htmlFile != null && htmlFile.exists() && htmlFile.isFile) {
            try {
                requireContext().contentResolver.openInputStream(htmlFile.uri)?.use { inputStream ->
                    val htmlContent = inputStream.bufferedReader().readText()
                    webView.visibility = View.VISIBLE
                    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return text.replace(matchedFull, "")
    }

    private fun applyHeaderAlignment(textView: TextView) {
        val prefs = requireContext().getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
        val alignment = prefs.getString("HEADER_ALIGNMENT", "CENTER")?.uppercase()
        when (alignment) {
            "LEFT" -> textView.gravity = Gravity.START
            "RIGHT" -> textView.gravity = Gravity.END
            else -> textView.gravity = Gravity.CENTER
        }
    }

    private fun applyBodyAlignment(textView: TextView) {
        val prefs = requireContext().getSharedPreferences("ProtocolPrefs", Context.MODE_PRIVATE)
        val alignment = prefs.getString("BODY_ALIGNMENT", "CENTER")?.uppercase()
        when (alignment) {
            "LEFT" -> textView.gravity = Gravity.START
            "RIGHT" -> textView.gravity = Gravity.END
            else -> textView.gravity = Gravity.CENTER
        }
    }

    companion object {
        private const val ARG_HEADER = "ARG_HEADER"
        private const val ARG_BODY = "ARG_BODY"
        private const val ARG_ITEM = "ARG_ITEM"
        private const val ARG_IS_BRANCH = "ARG_IS_BRANCH"
        private const val ARG_NORMAL_RESPONSES = "ARG_NORMAL_RESPONSES"
        private const val ARG_BRANCH_RESPONSES = "ARG_BRANCH_RESPONSES"

        @JvmStatic
        fun newInstance(
            header: String?,
            body: String?,
            item: String?,
            responses: List<String>
        ) = ScaleFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_HEADER, header)
                putString(ARG_BODY, body)
                putString(ARG_ITEM, item)
                putBoolean(ARG_IS_BRANCH, false)
                putStringArrayList(ARG_NORMAL_RESPONSES, ArrayList(responses))
            }
        }

        @JvmStatic
        fun newBranchInstance(
            header: String?,
            body: String?,
            item: String?,
            branchResponses: List<Pair<String, String?>>
        ) = ScaleFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_HEADER, header)
                putString(ARG_BODY, body)
                putString(ARG_ITEM, item)
                putBoolean(ARG_IS_BRANCH, true)
                val rawList = branchResponses.map { "${it.first}||${it.second ?: ""}" }
                putStringArrayList(ARG_BRANCH_RESPONSES, ArrayList(rawList))
            }
        }
    }
}
