package com.example.tvtube

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class MainActivity : AppCompatActivity() {
    private val items = ArrayList<StreamInfoItem>()
    private val titles = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NewPipe.init(AppDownloader(), Localization("en", "US"), ContentCountry("US"))

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 32) }
        val input = EditText(this).apply {
            hint = "ابحث في يوتيوب"; setSingleLine(); textSize = 24f
            imeOptions = EditorInfo.IME_ACTION_SEARCH
        }
        val btn = Button(this).apply { text = "بحث"; textSize = 20f }
        status = TextView(this).apply { textSize = 16f }
        adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, titles) {
            override fun getView(p: Int, v: android.view.View?, g: android.view.ViewGroup): android.view.View {
                val t = super.getView(p, v, g) as TextView
                t.textSize = 20f; t.maxLines = 2
                return t
            }
        }
        val list = ListView(this).apply { this.adapter = this@MainActivity.adapter }
        list.setOnItemClickListener { _, _, pos, _ ->
            startActivity(Intent(this, PlayerActivity::class.java).putExtra("url", items[pos].url))
        }
        input.setOnEditorActionListener { _, _, _ -> search(input.text.toString(), list); true }
        btn.setOnClickListener { search(input.text.toString(), list) }
        listOf(input, btn, status, list).forEach { root.addView(it) }
        setContentView(root)
    }

    private fun search(q: String, list: ListView) {
        if (q.isBlank()) return
        status.text = "جاري البحث..."
        Thread {
            try {
                val h = ServiceList.YouTube.searchQHFactory
                    .fromQuery(q, listOf(YoutubeSearchQueryHandlerFactory.VIDEOS), "")
                val res = SearchInfo.getInfo(ServiceList.YouTube, h)
                val vids = res.relatedItems.filterIsInstance<StreamInfoItem>()
                runOnUiThread {
                    items.clear(); items.addAll(vids)
                    titles.clear(); titles.addAll(vids.map { "${it.name}\n${it.uploaderName ?: ""}" })
                    adapter.notifyDataSetChanged()
                    status.text = "${vids.size} نتيجة"
                    list.requestFocus()
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "خطأ: ${e.message}" }
            }
        }.start()
    }
}
