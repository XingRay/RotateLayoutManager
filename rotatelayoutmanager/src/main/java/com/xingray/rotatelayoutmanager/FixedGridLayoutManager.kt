package com.xingray.rotatelayoutmanager

import android.graphics.PointF
import android.util.Log
import android.util.SparseArray
import android.view.View
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

/**
 * xxx
 *
 * @author : leixing
 * @date : 2019/6/6 18:35
 * @version : 1.0.0
 * mail : leixing@baidu.com
 *
 */
class FixedGridLayoutManager : RecyclerView.LayoutManager() {

    companion object {
        private val TAG = FixedGridLayoutManager::class.java.simpleName
        private const val DIRECTION_NONE = 0
        private const val DIRECTION_DOWN = 1
        private const val DIRECTION_UP = 2
    }

    private var mForceClearOffsets: Boolean = false
    private var mFirstVisiblePosition: Int = 0
    private var mVisibleColumnCount: Int = 0
    private var mVisibleRowCount: Int = 0

    private var mLeftOffset = 0
    private var mTopOffset = 0

    private var mChildWidth = 0
    private var mChildHeight = 0

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT)
    }


    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        if (recycler == null || state == null || state.itemCount == 0) {
            return
        }

        if (itemCount == 0) {
            detachAndScrapAttachedViews(recycler)
            return
        }

        if (childCount == 0) {
            val scrap = recycler.getViewForPosition(0)
            addView(scrap)
            measureChildWithMargins(scrap, 0, 0)

            mChildWidth = getDecoratedMeasuredWidth(scrap)
            mChildHeight = getDecoratedMeasuredHeight(scrap)

            detachAndScrapView(scrap, recycler)
        }

        updateWindowSizing()

        var childLeft: Int
        var childTop: Int

        if (childCount == 0) {
            mFirstVisiblePosition = 0
            childLeft = 0
            childTop = 0
        } else if (getVisibleChildCount() > itemCount) {
            mFirstVisiblePosition = 0
            childLeft = 0
            childTop = 0
        } else {
            val topChild = getChildAt(0)
            if (mForceClearOffsets) {
                childLeft = 0
                childTop = 0
                mForceClearOffsets = false
            } else {
                if (topChild == null) {
                    childLeft = 0
                    childTop = 0
                } else {
                    childLeft = getDecoratedLeft(topChild)
                    childTop = getDecoratedTop(topChild)
                }
            }

            var lastVisiblePosition = positionOfIndex(getVisibleChildCount() - 1)
            if (lastVisiblePosition >= itemCount) {
                lastVisiblePosition = itemCount - 1
                val lastColumn = mVisibleColumnCount - 1
                val lastRow = mVisibleRowCount - 1

                mFirstVisiblePosition =
                    Math.max(lastVisiblePosition - lastColumn - (lastRow * getTotalColumnCount()), 0)

                childLeft = getHorizontalSpace() - (mChildWidth * mVisibleColumnCount)
                childTop = getVerticalSpace() - (mChildHeight * mVisibleRowCount)

                if (getFirstVisibleRow() == 0) {
                    childTop = Math.min(childTop, 0)
                }

                if (getFirstVisibleColumn() == 0) {
                    childLeft = Math.min(childLeft, 0)
                }
            }
        }

        detachAndScrapAttachedViews(recycler)
        fillGrid(DIRECTION_NONE, childLeft, childTop, recycler)

    }

    override fun onAdapterChanged(oldAdapter: RecyclerView.Adapter<*>?, newAdapter: RecyclerView.Adapter<*>?) {
        removeAllViews()
    }

    override fun scrollToPosition(position: Int) {
        if (position < 0 || position > itemCount) {
            Log.d(TAG, "scrollToPosition() called with: position = [$position]")
            return
        }

        mForceClearOffsets = true
        mFirstVisiblePosition = position

        requestLayout()
    }

    override fun smoothScrollToPosition(recyclerView: RecyclerView?, state: RecyclerView.State?, position: Int) {
        if (recyclerView == null || state == null) {
            return
        }
        if (position < 0 || position >= itemCount) {
            Log.e(TAG, "smoothScrollToPosition: position: $position")
            return
        }

        val context = recyclerView.context
        val scroller = object : LinearSmoothScroller(context) {
            override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
                val rowOffset = getGlobalRowOfPosition(targetPosition) - getGlobalRowOfPosition(mFirstVisiblePosition)
                val columnOffset =
                    getGlobalColumnOfPosition(targetPosition) - getGlobalColumnOfPosition(mFirstVisiblePosition)
                return PointF((columnOffset * mChildWidth).toFloat(), (rowOffset * mChildHeight).toFloat())
            }
        }

        scroller.targetPosition = position
        startSmoothScroll(scroller)

    }

    private fun getGlobalRowOfPosition(targetPosition: Int): Int {
        return 0
    }

    private fun getGlobalColumnOfPosition(targetPosition: Int): Int {
        return 0
    }

    private fun fillGrid(direction: Int, left: Int, top: Int, recycler: RecyclerView.Recycler) {
        val viewCache = SparseArray<View>()

        if (childCount != 0) {
            for (i in 0 until childCount) {
                val position = positionOfIndex(i)
                val child = getChildAt(position)
                viewCache.put(position, child)
            }

            for (i in 0 until viewCache.size()) {
                detachView(viewCache.valueAt(i))
            }
        }

        for (i in 0 until getVisibleChildCount()) {
            val position = positionOfIndex(i)
            var view = viewCache.get(position)
            if (view == null) {
                view = recycler.getViewForPosition(position)
                measureChildWithMargins(view, 0, 0)
                layoutDecorated(
                    view, mLeftOffset, mTopOffset,
                    mLeftOffset + mChildWidth, mTopOffset + mChildHeight
                )
            } else {
                attachView(view)
                viewCache.remove(position)
            }
        }

        for (i in 0 until viewCache.size()) {
            recycler.recycleView(viewCache.valueAt(i))
        }

    }

    private fun getVisibleChildCount(): Int {
        return 0
    }

    private fun positionOfIndex(i: Int): Int {
        return 0
    }

    private fun updateWindowSizing() {

    }

    override fun canScrollVertically(): Boolean {
        return true
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        Log.d(TAG, "scrollVerticallyBy() called with: dy = [$dy], recycler = [$recycler], state = [$state]")
        if (recycler == null) {
            return 0
        }
        if (childCount == 0) {
            return 0
        }

        val topView = getChildAt(0)
        val bottomView = getChildAt(childCount - 1)
        if (topView == null || bottomView == null) {
            return 0
        }

        val viewSpan = getDecoratedBottom(bottomView) - getDecoratedTop(topView)
        if (viewSpan <= getVerticalSpace()) {
            return 0
        }

        val maxRowCount = getTotalRowCount()
        val topBoundReached = getFirstVisibleRow() == 0
        val bottomBoundReached = getLastVisibleRow() == 0

        val delta = if (dy > 0) {
            if (bottomBoundReached) {
                val bottomOffset = if (rowOfIndex(childCount - 1) >= (maxRowCount - 1)) {
                    getVerticalSpace() - getDecoratedBottom(bottomView) + paddingBottom
                } else {
                    getVerticalSpace() - (getDecoratedBottom(bottomView) + mChildHeight) + paddingBottom
                }
                Math.max(-dy, bottomOffset)
            } else {
                -dy
            }
        } else {
            if (topBoundReached) {
                val topOffset = -getDecoratedTop(topView) + paddingTop
                Math.min(-dy, topOffset)
            } else {
                -dy
            }
        }

        offsetChildrenVertical(delta)

        if (dy > 0) {
            if (getDecoratedBottom(topView) < 0 && !bottomBoundReached) {
                fillGrid(DIRECTION_DOWN, recycler)
            } else if (!bottomBoundReached) {
                fillGrid(DIRECTION_NONE, recycler)
            }
        } else {
            if (getDecoratedTop(topView) > 0 && !topBoundReached) {
                fillGrid(DIRECTION_UP, recycler)
            } else if (!topBoundReached) {
                fillGrid(DIRECTION_NONE, recycler)
            }
        }

        return -delta
    }

    private fun fillGrid(direction: Int, recycler: RecyclerView.Recycler) {

    }

    private fun rowOfIndex(i: Int): Int {
        return 0
    }

    private fun getLastVisibleRow(): Int {
        return 0
    }

    private fun getFirstVisibleRow(): Int {
        return 0
    }

    private fun getFirstVisibleColumn(): Int {
        return 0
    }

    private fun getTotalRowCount(): Int {
        return 0
    }

    private fun getTotalColumnCount(): Int {
        return 0
    }

    private fun getHorizontalSpace(): Int {
        return 0
    }

    private fun getVerticalSpace(): Int {
        return 0
    }
}