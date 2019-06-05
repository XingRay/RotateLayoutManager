package com.xingray.rotatelayoutmanager

import androidx.recyclerview.widget.RecyclerView

/**
 * xxx
 *
 * @author : leixing
 * @date : 2019/6/5 17:57
 * @version : 1.0.0
 * mail : leixing@baidu.com
 *
 */
class RotateChildDrawingOrderCallback(layoutManager: RotateLayoutManager) : RecyclerView.ChildDrawingOrderCallback {

    private val mLayoutManager = layoutManager

    override fun onGetChildDrawingOrder(childCount: Int, i: Int): Int {
        var center = mLayoutManager.getCenterPosition() - mLayoutManager.getFirstVisiblePosition()

        // 计算正在显示的所有Item的中间位置
        if (center < 0) {
            center = 0
        } else if (center > childCount) {
            center = childCount
        }

        return when {
            i == center -> childCount - 1
            i > center -> center + childCount - 1 - i
            else -> i
        }
    }
}