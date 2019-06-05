package com.xingray.rotatelayoutmanager.sample.util

import android.app.Activity
import android.widget.Toast

/**
 * xxx
 *
 * @author : leixing
 * @date : 2019/6/5 16:38
 * @version : 1.0.0
 * mail : leixing@baidu.com
 *
 */

fun Activity.showToast(msg: String) {
    Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
}