package com.xingray.rotatelayoutmanager


/**
 * 选中监听接口
 */
interface ItemSelectListener {
    /**
     * 监听选中回调
     *
     * @param position 显示在中间的Item的位置
     */
    fun onItemSelected(position: Int, selectTrigger: SelectTrigger)
}