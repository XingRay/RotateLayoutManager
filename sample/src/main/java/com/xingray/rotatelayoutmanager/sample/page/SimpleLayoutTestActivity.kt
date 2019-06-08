package com.xingray.rotatelayoutmanager.sample.page

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.xingray.recycleradapter.BaseViewHolder
import com.xingray.recycleradapter.RecyclerAdapter
import com.xingray.rotatelayoutmanager.FixedGridLayoutManager
import com.xingray.rotatelayoutmanager.sample.R
import com.xingray.rotatelayoutmanager.sample.util.showToast
import kotlin.random.Random

/**
 * xxx
 *
 * @author : leixing
 * @date : 2019/6/7 13:13
 * @version : 1.0.0
 * mail : leixing@baidu.com
 *
 */
class SimpleLayoutTestActivity : AppCompatActivity() {
    companion object {
        fun start(context: Context) {
            val starter = Intent(context, SimpleLayoutTestActivity::class.java)
            context.startActivity(starter)
        }
    }

    var rvList: RecyclerView? = null
    var mAdapter: RecyclerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_layout_test)

        rvList = findViewById(R.id.rv_list)
        initList(rvList)
        loadData()
    }

    private fun initList(rvList: RecyclerView?) {
        if (rvList == null) {
            return
        }

        rvList.layoutManager = FixedGridLayoutManager()
        mAdapter = RecyclerAdapter(applicationContext)
            .typeSupport(TestData::class.java)
            .viewSupport(R.layout.item_test_list, TestViewHolder::class.java) { _, _, t ->
                showToast(t.name)
            }.registerType()
        rvList.adapter = mAdapter
    }

    private fun loadData() {
        val list = mutableListOf<TestData>()
        val colors = arrayOf(
            Color.BLUE,
            Color.CYAN,
            Color.GREEN,
            Color.MAGENTA,
            Color.RED
        )
        val r = Random(System.currentTimeMillis())

        for (i in 0 until 100) {
            list.add(TestData("test$i", colors[r.nextInt(0, colors.size)]))
        }

        mAdapter?.update(list)
    }

    private data class TestData(val name: String, val color: Int)

    private class TestViewHolder(itemView: View) :
        BaseViewHolder<TestData>(itemView) {
        private val tvText: TextView = itemView.findViewById(R.id.tv_text)

        override fun bindItemView(t: TestData, position: Int) {
            tvText.text = t.name
            tvText.setBackgroundColor(t.color)
        }
    }
}