package com.everlog.ui.views.viewpager.wrap

import android.util.SparseArray
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter

/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Raanan Nevet
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
/**
 * Simple implementation of @see ObjectAtPositionInterface can be extended by any custom adapter
 */
abstract class ObjectAtPositionPagerAdapter : PagerAdapter(), ObjectAtPositionInterface {
    private var objects = SparseArray<Any>()
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val `object` = instantiateItemObject(container, position)
        objects.put(position, `object`)
        return `object`
    }

    /**
     * Replaces @see PagerAdapter#instantiateItem and handles objects tracking for getObjectAtPosition
     */
    abstract fun instantiateItemObject(container: ViewGroup, position: Int): Any
    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        objects.remove(position)
        destroyItemObject(container, position, `object`)
    }

    /**
     * Replaces @see PagerAdapter#destroyItem and handles objects tracking for getObjectAtPosition
     */
    abstract fun destroyItemObject(container: ViewGroup, position: Int, `object`: Any?)
    override fun getObjectAtPosition(position: Int): Any? {
        return objects[position]
    }
}