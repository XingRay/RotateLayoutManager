import android.view.View
import androidx.recyclerview.widget.RecyclerView

class RotateLayoutManager(offsetMul: Float, offsetAdd: Int) : RecyclerView.LayoutManager() {


    /**
     * [mItemOffset] = [mDecoratedChildWidth]*[mOffsetMul]+[mOffsetAdd]
     */
    private var mItemOffset = 0
    private val mOffsetMul = offsetMul
    private val mOffsetAdd = offsetAdd

    private var mDecoratedChildWidth = 0
    private var mDecoratedChildHeight = 0

    private var mFirstVisiblePosition = 0
    private var mOffset = 0

    private var mHorizontalSpace = 0

    var mVisibleItemCount = 0

    constructor() : this(1f, 0)

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.WRAP_CONTENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        if (recycler == null || state == null) {
            return
        }

        if (itemCount == 0) {
            detachAndScrapAttachedViews(recycler)
            return
        }

        if (childCount == 0) {
            val view = recycler.getViewForPosition(0)
            addView(view)
            measureChildWithMargins(view, 0, 0)

            mDecoratedChildWidth = getDecoratedMeasuredWidth(view)
            mDecoratedChildHeight = getDecoratedMeasuredHeight(view)

            detachAndScrapView(view, recycler)
        }

        updateVisibleAttributes()

        var childTop = paddingTop
        var childLeft = 0

        if (childCount == 0) {
            mFirstVisiblePosition = 0
            childLeft = paddingLeft + (mHorizontalSpace - mDecoratedChildWidth).shr(1)
        } else if (mVisibleItemCount < state.itemCount) {
            mFirstVisiblePosition = 0
            childLeft = paddingLeft + (mHorizontalSpace - mDecoratedChildWidth).shr(1)
        } else {
            val firstView = getChildAt(0)
            if (firstView == null) {
                childLeft = 0
            } else {
                childLeft = getDecoratedLeft(firstView)
            }

            if (mHorizontalSpace > getViewSpanWith()) {
                childLeft = paddingLeft
            }
        }


    }

    private fun getViewSpanWith(): Int {
        return when (itemCount) {
            0 -> 0
            1 -> mDecoratedChildWidth
            else -> mDecoratedChildWidth + (itemCount - 1) * mItemOffset
        }
    }

    override fun supportsPredictiveItemAnimations(): Boolean {
        return false
    }

    override fun onItemsRemoved(recyclerView: RecyclerView, positionStart: Int, itemCount: Int) {

    }

    override fun onAdapterChanged(oldAdapter: RecyclerView.Adapter<*>?, newAdapter: RecyclerView.Adapter<*>?) {
        removeAllViews()
    }

    override fun scrollToPosition(position: Int) {

    }

    override fun smoothScrollToPosition(recyclerView: RecyclerView?, state: RecyclerView.State?, position: Int) {

    }

    override fun canScrollHorizontally(): Boolean {
        return true
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        return 0
    }

    override fun findViewByPosition(position: Int): View? {
        return null
    }

    fun getCenterPosition(): Int {
        return 0
    }

    fun getFirstVisiblePosition(): Int {
        return 0
    }

    private fun updateVisibleAttributes() {
        mHorizontalSpace = width - paddingRight - paddingLeft
        mItemOffset = (mOffsetMul * mDecoratedChildWidth).toInt() + mOffsetAdd
        val h = mHorizontalSpace + mDecoratedChildWidth
        mVisibleItemCount = h / mItemOffset + if (h % mItemOffset > 0) 1 else 0
    }
}