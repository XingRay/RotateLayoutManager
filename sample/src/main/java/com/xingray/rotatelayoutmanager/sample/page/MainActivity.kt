package com.xingray.rotatelayoutmanager.sample.page

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xingray.recycleradapter.BaseViewHolder
import com.xingray.recycleradapter.RecyclerAdapter
import com.xingray.rotatelayoutmanager.sample.R

class MainActivity : AppCompatActivity() {

    var rvList: RecyclerView? = null
    var mAdapter: RecyclerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rvList = findViewById(R.id.rv_list)

        initList(rvList)
        loadData()

    }

    private fun initList(list: RecyclerView?) {
        if (list == null) {
            return
        }
        list.layoutManager = LinearLayoutManager(applicationContext)
        mAdapter = RecyclerAdapter(applicationContext)
            .typeSupport(Test::class.java)
            .viewSupport(R.layout.item_main_test_list, TestViewHolder::class.java) { _, _, t ->
                t.starter.invoke()
            }.registerType()
        list.adapter = mAdapter
        list.addItemDecoration(DividerItemDecoration(applicationContext, DividerItemDecoration.VERTICAL))
    }

    private fun loadData() {
        mAdapter?.update(listOf(
            Test("rotate test") {
                RotateRecyclerViewTestActivity.start(this)
            },
            Test("simple layout") {
                SimpleLayoutTestActivity.start(this)
            }
        ))
    }

    private data class Test(val name: String, val starter: () -> Unit)

    private class TestViewHolder(itemView: View) : BaseViewHolder<Test>(itemView) {
        private val tvText: TextView = itemView.findViewById(R.id.tv_text)

        override fun bindItemView(t: Test, position: Int) {
            tvText.text = t.name
        }
    }
}
