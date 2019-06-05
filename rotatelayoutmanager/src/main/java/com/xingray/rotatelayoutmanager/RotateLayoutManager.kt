package com.xingray.rotatelayoutmanager

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.util.SparseArray
import android.util.SparseBooleanArray
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView


class RotateLayoutManager : RecyclerView.LayoutManager() {
    companion object {
        /**
         * 最大存储item信息存储数量，
         * 超过设置数量，则动态计算来获取
         */
        private const val MAX_RECT_COUNT = 100
    }

    /**
     * 滑动总偏移量
     */
    private var mOffsetAll: Int = 0

    /**
     * Item宽
     */
    private var mDecoratedChildWidth: Int = 0

    /**
     * Item高
     */
    private var mDecoratedChildHeight: Int = 0

    /**
     * Item间隔与item宽的比例
     */
    private val mIntervalRatio = 1.2f

    /**
     * 起始ItemX坐标
     */
    private var mStartX = 0

    /**
     * 起始Item Y坐标
     */
    private var mStartY = 0

    /**
     * 保存所有的Item的上下左右的偏移量信息
     */
    private val mAllItemFrames = SparseArray<Rect>()

    /**
     * 记录Item是否出现过屏幕且还没有回收。true表示出现过屏幕上，并且还没被回收
     */
    private val mItemAttachedFlags = SparseBooleanArray()

    /**
     * RecyclerView的Item回收器
     */
    private var mRecycle: RecyclerView.Recycler? = null

    /**
     * RecyclerView的状态器
     */
    private var mState: RecyclerView.State? = null

    /**
     * 滚动动画
     */
    private var mAnimation: ValueAnimator? = null

    /**
     * 正显示在中间的Item
     */
    private var mSelectPosition = 0

    /**
     * 滑动的方向：左
     */
    private val SCROLL_LEFT = 1

    /**
     * 滑动的方向：右
     */
    private val SCROLL_RIGHT = 2

    /**
     * 选中监听
     */
    private var mItemSelectListener: ItemSelectListener? = null

    /**
     * 是否启动Item灰度值渐变
     */
    private val mItemGradualGrey = false

    /**
     * 是否启动Item半透渐变
     */
    private val mItemGradualAlpha = false

    private var mScaleX = 0.7f
    private var mScaleY = 0.7f
    private var mTranslationY: Float = 0.toFloat()
    private val mGrayFrom = 0.9f
    private val mGrayTo = 0.6f
    private var mItemSwitchRatio = 0.5f
    private var mScrollStateListener: ScrollStateListener? = null
    private var mScrollState: Int = RecyclerView.SCROLL_STATE_IDLE
    private var mAnimationListener: Animator.AnimatorListener? = null
    private var mViewScrollState: State = State.IDLE
    private var mAnimationRunning: Boolean = false

    fun setTranslationY(translationY: Float) {
        mTranslationY = translationY
    }

    fun setScaleX(scaleX: Float) {
        mScaleX = scaleX
    }

    fun setScaleY(scaleY: Float) {
        mScaleY = scaleY
    }

    fun setItemSwitchRatio(itemSwitchRatio: Float) {
        mItemSwitchRatio = itemSwitchRatio
    }

    fun setScrollStateListener(listener: ScrollStateListener) {
        mScrollStateListener = listener
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.WRAP_CONTENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        if (itemCount <= 0 || state!!.isPreLayout) {
            mOffsetAll = 0
            return
        }
        mAllItemFrames.clear()
        mItemAttachedFlags.clear()
        if (mDecoratedChildWidth == 0 || mDecoratedChildHeight == 0) {
            val scrap = recycler!!.getViewForPosition(0)
            measureChildWithMargins(scrap, 0, 0)
            mDecoratedChildWidth = getDecoratedMeasuredWidth(scrap)
            mDecoratedChildHeight = getDecoratedMeasuredHeight(scrap)
        }

        val horizontalSpace = getHorizontalSpace()
        mStartX = Math.round((horizontalSpace - mDecoratedChildWidth) * 1.0f / 2)
        mStartY = Math.round((getVerticalSpace() - mDecoratedChildHeight) * 1.0f / 2)

        var i = 0
        while (i < itemCount && i < MAX_RECT_COUNT) {
            var frame = mAllItemFrames.get(i)
            if (frame == null) {
                frame = Rect()
            }
            val offset = mStartX + getIntervalDistance() * i
            frame.set(
                Math.round(offset),
                mStartY,
                Math.round(offset + mDecoratedChildWidth),
                mStartY + mDecoratedChildHeight
            )
            mAllItemFrames.put(i, frame)
            mItemAttachedFlags.put(i, false)
            i++
        }

        detachAndScrapAttachedViews(recycler!!)
        val b = (mRecycle == null || mState == null) && mSelectPosition != 0
        if (b) {
            mOffsetAll = calculateOffsetForPosition(mSelectPosition)
        }

        layoutItems(recycler, state, SCROLL_RIGHT)

        mRecycle = recycler
        mState = state
    }

    override fun scrollHorizontallyBy(
        dx: Int, recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        if (mAnimation?.isRunning == true) {
            mAnimation?.cancel()
            mAnimationRunning = false
        }
        var travel = dx
        if (dx + mOffsetAll < 0) {
            travel = -mOffsetAll
        } else if (dx + mOffsetAll > getMaxOffset()) {
            travel = (getMaxOffset() - mOffsetAll) as Int
        }

        mOffsetAll += travel
        layoutItems(recycler, state, if (dx > 0) SCROLL_RIGHT else SCROLL_LEFT)
        return travel
    }

    /**
     * 布局Item
     *
     * 注意：1，先清除已经超出屏幕的item
     *
     *      2，再绘制可以显示在屏幕里面的item
     */
    private fun layoutItems(
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?, scrollDirection: Int
    ) {
        if (recycler == null || state == null) {
            return
        }
        if (state.isPreLayout) {
            return
        }
        try {
            val displayFrame = Rect(
                mOffsetAll,
                0,
                mOffsetAll + getHorizontalSpace(),
                getVerticalSpace()
            )

            var position: Int
            @Suppress("ReplaceRangeToWithUntil")
            for (i in 0..childCount - 1) {
                val child = getChildAt(i) ?: continue
                position = getPosition(child)

                val rect = getFrame(position)
                val adjustRect = adjust(rect, displayFrame)
                if (!Rect.intersects(displayFrame, adjustRect)) {
                    removeAndRecycleView(child, recycler)
                    mItemAttachedFlags.delete(position)
                } else {
                    layoutItem(child, adjustRect, adjustRect !== rect, displayFrame)
                    mItemAttachedFlags.put(position, true)
                }
            }

            val itemCount = state.itemCount

            @Suppress("ReplaceRangeToWithUntil")
            for (i in 0..itemCount - 1) {
                val rect = getFrame(i)
                val adjustRect = adjust(rect, displayFrame)
                if (Rect.intersects(displayFrame, adjustRect) && !mItemAttachedFlags.get(i)) {
                    val scrap = recycler.getViewForPosition(i)
                    measureChildWithMargins(scrap, 0, 0)
                    if (scrollDirection == SCROLL_LEFT) {
                        addView(scrap, 0)
                    } else {
                        addView(scrap)
                    }
                    layoutItem(scrap, adjustRect, adjustRect !== rect, displayFrame)
                    mItemAttachedFlags.put(i, true)
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                throw e
            }
        }

    }

    private fun adjust(rect: Rect, displayFrame: Rect): Rect {
        val left = rect.left
        val right = rect.right
        val width = right - left
        val halfWidth = width shr 1
        val frameLeft = displayFrame.left
        val frameRight = displayFrame.right
        val halfFrameWidth = frameRight - frameLeft shr 1

        if (left < frameLeft && left >= frameLeft - halfFrameWidth + halfWidth) {
            val result = Rect(rect)
            result.left = (frameLeft shl 1) - left
            result.right = result.left + width
            return result
        } else if (right > frameRight && right <= frameRight + halfFrameWidth - halfWidth) {
            val result = Rect(rect)
            result.right = (frameRight shl 1) - right
            result.left = result.right - width
            return result
        } else {
            return rect
        }
    }

    /**
     * 布局Item位置
     *
     * @param child 要布局的Item
     * @param frame 位置信息
     */
    private fun layoutItem(child: View, frame: Rect, isAdjust: Boolean, displayFrame: Rect) {
        layoutDecorated(
            child,
            frame.left - mOffsetAll,
            frame.top,
            frame.right - mOffsetAll,
            frame.bottom
        )
        if (isAdjust) {
            child.scaleX = mScaleX
            child.scaleY = mScaleY
            child.translationY = mTranslationY
            if (mItemGradualGrey) {
                greyItem(child, mGrayTo)
            }
        } else {
            val ratio = calcRatio(displayFrame, frame)
            child.scaleX = calcValue(1f, mScaleX, ratio)
            child.scaleY = calcValue(1f, mScaleY, ratio)
            child.translationY = calcValue(0f, mTranslationY, ratio)
            if (mItemGradualGrey) {
                greyItem(child, calcValue(mGrayFrom, mGrayTo, ratio))
            }
        }

        if (mItemGradualAlpha) {
            child.alpha = computeAlpha(frame.left - mOffsetAll)
        }
    }

    private fun calcValue(from: Float, to: Float, ratio: Float): Float {
        return from * (1 - ratio) + to * ratio
    }

    private fun calcRatio(outerRect: Rect, innerRect: Rect): Float {
        val outCenter = outerRect.right + outerRect.left shr 1
        val innerCenter = innerRect.right + innerRect.left shr 1
        val offset = Math.abs(outCenter - innerCenter)

        val outerWidth = outerRect.right - outerRect.left shr 1
        val innerWidth = innerRect.right - innerRect.left shr 1
        val offsetMax = Math.abs(outerWidth - innerWidth)

        return offset * 1.0f / offsetMax
    }

    /**
     * 动态获取Item的位置信息
     *
     * @param index item位置
     * @return item的Rect信息
     */
    private fun getFrame(index: Int): Rect {
        var frame = mAllItemFrames.get(index)
        if (frame == null) {
            frame = Rect()
            val offset = mStartX + getIntervalDistance() * index
            frame.set(
                Math.round(offset),
                mStartY,
                Math.round(offset + mDecoratedChildWidth),
                mStartY + mDecoratedChildHeight
            )
        }

        return frame
    }

    /**
     * 变化Item的灰度值
     *
     * @param child 需要设置灰度值的Item
     */
    private fun greyItem(child: View, value: Float) {
        val cm = ColorMatrix(
            floatArrayOf(
                value,
                0f,
                0f,
                0f,
                0f,
                0f,
                value,
                0f,
                0f,
                0f,
                0f,
                0f,
                value,
                0f,
                0f,
                0f,
                0f,
                0f,
                1f,
                0f
            )
        )
        val greyPaint = Paint()
        greyPaint.colorFilter = ColorMatrixColorFilter(cm)

        child.setLayerType(View.LAYER_TYPE_HARDWARE, greyPaint)
        if (value >= 1) {
            child.setLayerType(View.LAYER_TYPE_NONE, null)
        }
    }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        mScrollState = state
        when (state) {
            RecyclerView.SCROLL_STATE_IDLE -> fixOffsetWhenFinishScroll()
            RecyclerView.SCROLL_STATE_DRAGGING, RecyclerView.SCROLL_STATE_SETTLING -> updateScrollState()
        }
    }

    override fun scrollToPosition(position: Int) {
        if (position < 0 || position > itemCount - 1) {
            return
        }
        mOffsetAll = calculateOffsetForPosition(position)
        val direction = if (position > mSelectPosition) SCROLL_RIGHT else SCROLL_LEFT
        updateSelectPosition(position, SelectTrigger.SCROLL_TO)
        layoutItems(mRecycle, mState, direction)
    }

    override fun smoothScrollToPosition(recyclerView: RecyclerView?, state: RecyclerView.State?, position: Int) {
        updateSelectPosition(position, SelectTrigger.SCROLL_TO)
        val finalOffset = calculateOffsetForPosition(position)
        if (mRecycle == null || mState == null) {
            updateScrollState()
            return
        }
        startScroll(mOffsetAll, finalOffset, SelectTrigger.SCROLL_TO)
    }

    override fun canScrollHorizontally(): Boolean {
        return true
    }

    override fun onAdapterChanged(oldAdapter: RecyclerView.Adapter<*>?, newAdapter: RecyclerView.Adapter<*>?) {
        removeAllViews()
        mRecycle = null
        mState = null
        mOffsetAll = 0
        mItemAttachedFlags.clear()
        mAllItemFrames.clear()
        updateSelectPosition(0, SelectTrigger.DATA)
    }

    /**
     * 获取整个布局的水平空间大小
     */
    private fun getHorizontalSpace(): Int {
        return width - paddingRight - paddingLeft
    }

    /**
     * 获取整个布局的垂直空间大小
     */
    private fun getVerticalSpace(): Int {
        return height - paddingBottom - paddingTop
    }

    /**
     * 获取最大偏移量
     */
    private fun getMaxOffset(): Float {
        return (itemCount - 1) * getIntervalDistance()
    }

    /**
     * 计算Item半透值
     *
     * @param x Item的偏移量
     * @return 缩放系数
     */
    private fun computeAlpha(x: Int): Float {
        var alpha = 1 - Math.abs(x - mStartX) * 1.0f / Math.abs(mStartX + mDecoratedChildWidth / mIntervalRatio)
        if (alpha < 0.3f) {
            alpha = 0.3f
        }
        if (alpha > 1) {
            alpha = 1.0f
        }
        return alpha
    }

    /**
     * 计算Item所在的位置偏移
     *
     * @param position 要计算Item位置
     */
    private fun calculateOffsetForPosition(position: Int): Int {
        return Math.round(getIntervalDistance() * position)
    }

    /**
     * 修正停止滚动后，Item滚动到中间位置
     */
    private fun fixOffsetWhenFinishScroll() {
        val intervalDistance = getIntervalDistance()
        var selectPosition = (mOffsetAll * 1.0f / intervalDistance).toInt()
        val offset = mOffsetAll % intervalDistance
        if (selectPosition >= mSelectPosition) {
            if (offset > intervalDistance * mItemSwitchRatio) {
                selectPosition++
            }
        } else {
            if (offset > intervalDistance * (1 - mItemSwitchRatio)) {
                selectPosition++
            }
        }

        val finalOffsetAll = Math.round(selectPosition * intervalDistance)
        updateSelectPosition(selectPosition, SelectTrigger.USER_TOUCH)
        startScroll(mOffsetAll, finalOffsetAll, SelectTrigger.USER_TOUCH)
    }

    /**
     * 滚动到指定X轴位置
     *
     * @param from X轴方向起始点的偏移量
     * @param to   X轴方向终点的偏移量
     */
    private fun startScroll(from: Int, to: Int, selectTrigger: SelectTrigger) {
        if (mAnimation?.isRunning == true) {
            mAnimation?.cancel()
            mAnimationRunning = false
        }
        if (from == to) {
            updateScrollState()
            return
        }
        val direction = if (from < to) SCROLL_RIGHT else SCROLL_LEFT
        mAnimation = ValueAnimator.ofInt(from, to)
        mAnimation?.duration = 500
        mAnimation?.interpolator = DecelerateInterpolator()
        mAnimation?.addUpdateListener { animation ->
            mOffsetAll = animation.animatedValue as Int
            layoutItems(mRecycle, mState, direction)
        }
        mAnimationListener = object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                mAnimationRunning = true
                updateScrollState()
            }

            override fun onAnimationEnd(animation: Animator) {
                mAnimationRunning = false
                notifySelectPositionUpdate(selectTrigger)
                updateScrollState()
            }

            override fun onAnimationCancel(animation: Animator) {
                mAnimationRunning = false
                mAnimation?.removeListener(mAnimationListener)
                updateScrollState()
            }

            override fun onAnimationRepeat(animation: Animator) {

            }
        }
        mAnimation?.addListener(mAnimationListener)
        mAnimation?.start()
    }

    /**
     * 获取Item间隔
     */
    private fun getIntervalDistance(): Float {
        return mDecoratedChildWidth * mIntervalRatio
    }

    /**
     * 获取第一个可见的Item位置
     *
     * Note:该Item为绘制在可见区域的第一个Item，有可能被第二个Item遮挡
     */
    fun getFirstVisiblePosition(): Int {
        val displayFrame = Rect(mOffsetAll, 0, mOffsetAll + getHorizontalSpace(), getVerticalSpace())
        val cur = getCenterPosition()
        for (i in cur - 1..0) {
            val rect = getFrame(i)
            if (!Rect.intersects(displayFrame, rect)) {
                return i + 1
            }
        }
        return 0
    }

    /**
     * 获取最后一个可见的Item位置
     *
     * Note:该Item为绘制在可见区域的最后一个Item，有可能被倒数第二个Item遮挡
     */
    fun getLastVisiblePosition(): Int {
        val displayFrame = Rect(mOffsetAll, 0, mOffsetAll + getHorizontalSpace(), getVerticalSpace())
        val cur = getCenterPosition()
        @Suppress("ReplaceRangeToWithUntil")
        for (i in cur + 1..itemCount - 1) {
            val rect = getFrame(i)
            if (!Rect.intersects(displayFrame, rect)) {
                return i - 1
            }
        }
        return cur
    }

    /**
     * 获取可见范围内最大的显示Item个数
     */
    fun getMaxVisibleCount(): Int {
        val oneSide = ((getHorizontalSpace() - mStartX) / getIntervalDistance()) as Int
        return oneSide * 2 + 1
    }

    /**
     * 获取中间位置
     *
     * Note:该方法主要用于[RecyclerView.getChildDrawingOrder]判断中间位置
     *
     * 如果需要获取被选中的Item位置，调用[.getSelectedPosition]
     */
    fun getCenterPosition(): Int {
        var pos = (mOffsetAll / getIntervalDistance()).toInt()
        val more = (mOffsetAll % getIntervalDistance()).toInt()
        if (more > getIntervalDistance() * 0.5f) {
            pos++
        }
        return pos
    }

    /**
     * 设置选中监听
     *
     * @param l 监听接口
     */
    fun setOnSelectedListener(l: ItemSelectListener) {
        mItemSelectListener = l
    }

    /**
     * 获取被选中Item位置
     */
    fun getSelectedPosition(): Int {
        return mSelectPosition
    }

    private fun updateSelectPosition(position: Int, selectTrigger: SelectTrigger) {
        if (mSelectPosition == position) {
            return
        }
        mSelectPosition = position
        notifySelectPositionUpdate(selectTrigger)
    }

    /**
     * 计算当前选中位置，并回调
     */
    private fun notifySelectPositionUpdate(selectTrigger: SelectTrigger) {
        mItemSelectListener?.onItemSelected(mSelectPosition, selectTrigger)
    }

    private fun updateScrollState() {
        val state = when {
            mAnimationRunning -> State.ANIMATION
            mScrollState == RecyclerView.SCROLL_STATE_IDLE -> State.IDLE
            else -> State.SCROLLING
        }

        if (mViewScrollState != state) {
            mViewScrollState = state
            if (mScrollStateListener != null) {
                mScrollStateListener?.onScrollStateUpdate(mViewScrollState)
            }
        }
    }

    fun getScrollState(): State {
        return mViewScrollState
    }

    /**
     * 滑动状态
     */
    enum class State {
        IDLE,
        SCROLLING,
        ANIMATION
    }
}