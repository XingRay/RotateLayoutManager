package com.xingray.rotatelayoutmanager

enum class SelectTrigger {

    /**
     * 用户滑动卡片选中Item
     */
    USER_TOUCH,

    /**
     * 程序调用[RecyclerView.scrollToPosition]
     * 或者 [RecyclerView.smoothScrollToPosition]后选中Item
     */
    SCROLL_TO,

    /**
     * 数据更新导致选中位置变化
     */
    DATA
}