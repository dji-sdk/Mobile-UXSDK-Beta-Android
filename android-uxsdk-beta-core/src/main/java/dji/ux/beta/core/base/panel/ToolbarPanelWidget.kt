/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.ux.beta.core.base.panel

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import dji.ux.beta.core.R
import dji.ux.beta.core.extension.getColor
import dji.ux.beta.core.extension.listen
import dji.ux.beta.core.extension.setBorder
import dji.ux.beta.core.util.DisplayUtil
import dji.ux.beta.core.util.ViewIDGenerator


/**
 * TODO Documentation once widget is implemented
 *
 */
abstract class ToolbarPanelWidget<T> @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        configuration: PanelWidgetConfiguration
) : PanelWidget<PanelItem, T>(
        context,
        attrs,
        defStyleAttr,
        configuration),
        OnToolClickListener {

    private val toolBarItemList = mutableListOf<ToolBarItem>()

    private val toolsRecyclerView: RecyclerView = RecyclerView(context)
    private val viewPager: ViewPager = ViewPager(context)
    private val toolPageAdapter = CustomPagerAdapter(context, toolBarItemList)
    private val toolBarAdapter = ToolBarRecyclerAdapter(context, toolBarItemList, panelWidgetConfiguration)

    init {
        check(panelWidgetConfiguration.panelWidgetType == PanelWidgetType.TOOLBAR_LEFT
                || panelWidgetConfiguration.panelWidgetType == PanelWidgetType.TOOLBAR_RIGHT
                || panelWidgetConfiguration.panelWidgetType == PanelWidgetType.TOOLBAR_TOP) {
            "PanelWidgetConfiguration.panelWidgetType should be PanelWidgetType.TOOLBAR_LEFT " +
                    "or PanelWidgetType.TOOLBAR_RIGHT or PanelWidgetType.TOOLBAR_TOP"
        }

        toolsRecyclerView.id = ViewIDGenerator.generateViewId()
        viewPager.id = ViewIDGenerator.generateViewId()
        addView(toolsRecyclerView)
        addView(viewPager)
        val layoutManager = if (panelWidgetConfiguration.panelWidgetType == PanelWidgetType.TOOLBAR_TOP) {
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        } else {
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        }

        toolsRecyclerView.layoutManager = layoutManager
        //TODO Convert magic numbers in the functions to customizations
        when (panelWidgetConfiguration.panelWidgetType) {
            PanelWidgetType.TOOLBAR_LEFT -> setupToolBarLeft()
            PanelWidgetType.TOOLBAR_RIGHT -> setupToolBarRight()
            else -> setupToolBarTop()
        }
        viewPager.adapter = toolPageAdapter
        toolBarAdapter.onToolClickListener = this
        toolsRecyclerView.adapter = toolBarAdapter
    }


    override fun initPanelWidget(context: Context, attrs: AttributeSet?, defStyleAttr: Int, widgetConfiguration: PanelWidgetConfiguration?) {
        // No implementation
    }

    override fun size(): Int {
        return toolBarItemList.size
    }

    override fun removeAllWidgets() {
        TODO("Not yet implemented")
    }

    override fun onToolClick(position: Int) {
        viewPager.currentItem = position
    }

    fun <T> insert(index: Int, widgetView: T) where T : View, T : ToolbarPanelItem {
        val toolbarPanelItem = ToolBarItem(index,
                widgetView.barIcon,
                widgetView.barTitle,
                widgetView)
        toolBarItemList.add(index, toolbarPanelItem)
        updateAdapter()
    }

    fun insert(index: Int, template: ToolbarPanelTemplate) {
        val toolbarPanelItem = ToolBarItem(index = index,
                toolBarIcon = template.toolbarIcon,
                toolBarTitle = template.toolbarTitle,
                isFromTemplate = true,
                template = template)
        toolBarItemList.add(index, toolbarPanelItem)
        updateAdapter()
    }

    private fun updateAdapter() {
        toolBarAdapter.viewList = toolBarItemList
        toolBarAdapter.notifyDataSetChanged()
        toolPageAdapter.viewList = toolBarItemList
        toolPageAdapter.notifyDataSetChanged()
    }

    private fun setupToolBarLeft() {
        val set = ConstraintSet()
        set.clone(this)
        set.clear(viewPager.id)
        set.clear(toolsRecyclerView.id)
        set.setHorizontalChainStyle(toolsRecyclerView.id, ConstraintSet.CHAIN_PACKED)
        set.connect(toolsRecyclerView.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
        set.connect(toolsRecyclerView.id, ConstraintSet.RIGHT, viewPager.id, ConstraintSet.LEFT)
        set.connect(toolsRecyclerView.id, ConstraintSet.TOP, getParentTopId(), ConstraintSet.TOP)
        set.connect(toolsRecyclerView.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        set.constrainPercentHeight(toolsRecyclerView.id, 1f)
        set.constrainPercentWidth(toolsRecyclerView.id, 0.1f)
        set.connect(viewPager.id, ConstraintSet.LEFT, toolsRecyclerView.id, ConstraintSet.RIGHT)
        set.connect(viewPager.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
        set.connect(viewPager.id, ConstraintSet.TOP, getParentTopId(), ConstraintSet.TOP)
        set.connect(viewPager.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        set.constrainPercentHeight(viewPager.id, 1f)
        set.constrainPercentWidth(viewPager.id, 0.9f)
        toolsRecyclerView.setBorder(
                backgroundColor = getColor(R.color.uxsdk_black),
                borderColor = getColor(R.color.uxsdk_white_40_percent),
                rightBorder = DisplayUtil.dipToPx(context, 0.2f).toInt())
        set.applyTo(this)
    }

    private fun setupToolBarRight() {
        val set = ConstraintSet()
        set.clone(this)
        set.clear(viewPager.id)
        set.clear(toolsRecyclerView.id)
        set.setHorizontalChainStyle(toolsRecyclerView.id, ConstraintSet.CHAIN_PACKED)
        set.connect(viewPager.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
        set.connect(viewPager.id, ConstraintSet.RIGHT, toolsRecyclerView.id, ConstraintSet.LEFT)
        set.connect(viewPager.id, ConstraintSet.TOP, getParentTopId(), ConstraintSet.TOP)
        set.connect(viewPager.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        set.constrainPercentHeight(viewPager.id, 1f)
        set.constrainPercentWidth(viewPager.id, 0.9f)
        set.connect(toolsRecyclerView.id, ConstraintSet.LEFT, viewPager.id, ConstraintSet.RIGHT)
        set.connect(toolsRecyclerView.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
        set.connect(toolsRecyclerView.id, ConstraintSet.TOP, getParentTopId(), ConstraintSet.TOP)
        set.connect(toolsRecyclerView.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        set.constrainPercentHeight(toolsRecyclerView.id, 1f)
        set.constrainPercentWidth(toolsRecyclerView.id, 0.1f)
        toolsRecyclerView.setBorder(
                backgroundColor = getColor(R.color.uxsdk_black),
                borderColor = getColor(R.color.uxsdk_white_40_percent),
                leftBorder = DisplayUtil.dipToPx(context, 0.2f).toInt())
        set.applyTo(this)
    }

    private fun setupToolBarTop() {
        val set = ConstraintSet()
        set.clone(this)
        set.clear(viewPager.id)
        set.clear(toolsRecyclerView.id)
        set.setVerticalChainStyle(toolsRecyclerView.id, ConstraintSet.CHAIN_PACKED)
        set.connect(toolsRecyclerView.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
        set.connect(toolsRecyclerView.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
        set.connect(toolsRecyclerView.id, ConstraintSet.TOP, getParentTopId(), ConstraintSet.TOP)
        set.connect(toolsRecyclerView.id, ConstraintSet.BOTTOM, viewPager.id, ConstraintSet.TOP)
        set.constrainPercentHeight(toolsRecyclerView.id, 0.2f)
        set.constrainPercentWidth(toolsRecyclerView.id, 1f)
        set.connect(viewPager.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
        set.connect(viewPager.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
        set.connect(viewPager.id, ConstraintSet.TOP, toolsRecyclerView.id, ConstraintSet.BOTTOM)
        set.connect(viewPager.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        set.constrainPercentHeight(viewPager.id, 0.8f)
        set.constrainPercentWidth(viewPager.id, 1f)
        toolsRecyclerView.setBorder(
                backgroundColor = getColor(R.color.uxsdk_black),
                borderColor = getColor(R.color.uxsdk_white_40_percent),
                bottomBorder = DisplayUtil.dipToPx(context, 0.2f).toInt())
        set.applyTo(this)
    }

    @Throws(UnsupportedOperationException::class)
    override fun getWidget(index: Int): PanelItem? =
            throw UnsupportedOperationException(("This call is not supported. "))

    @Throws(UnsupportedOperationException::class)
    override fun addWidgets(items: Array<PanelItem>) =
            throw UnsupportedOperationException(("This call is not supported."))


    @Throws(UnsupportedOperationException::class)
    override fun addWidget(index: Int, item: PanelItem) =
            throw UnsupportedOperationException(("This call is not supported. " +
                    "Try insert instead"))

    @Throws(UnsupportedOperationException::class)
    override fun removeWidget(index: Int): PanelItem? =
            throw UnsupportedOperationException(("This call is not supported. " +
                    "Try //TODO "))

    override fun getIdealDimensionRatioString(): String? = null


    data class ToolBarItem(
            val index: Int,
            val toolBarIcon: Drawable?,
            val toolBarTitle: String?,
            val widgetView: View? = null,
            val isFromTemplate: Boolean = false,
            val template: ToolbarPanelTemplate? = null)

    private class ToolBarRecyclerAdapter(
            private val context: Context,
            var viewList: List<ToolBarItem>,
            private val panelConfiguration: PanelWidgetConfiguration
    ) : RecyclerView.Adapter<ToolBarRecyclerAdapter.ItemHolder>() {

        var onToolClickListener: OnToolClickListener? = null
        var selectedPosition: Int = 0
        var selectedColor: Int = getColor(context, R.color.uxsdk_white_20_percent)
        var unSelectedColor: Int = getColor(context, R.color.uxsdk_transparent)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
            val view: View = LayoutInflater.from(context).inflate(R.layout.uxsdk_view_toolbar, parent, false)
            val param = view.layoutParams
            if (panelConfiguration.panelWidgetType == PanelWidgetType.TOOLBAR_TOP) {
                param.height = ViewGroup.LayoutParams.MATCH_PARENT
                param.width = ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                param.height = ViewGroup.LayoutParams.WRAP_CONTENT
                param.width = ViewGroup.LayoutParams.MATCH_PARENT
            }
            view.layoutParams = param
            return ItemHolder(view).listen { pos ->
                notifyItemChanged(selectedPosition)
                selectedPosition = pos
                onToolClickListener?.onToolClick(pos)
                notifyItemChanged(selectedPosition)
            }
        }

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            holder.toolBarIcon.setImageDrawable(viewList[position].toolBarIcon)
            holder.toolBarTitle.text = viewList[position].toolBarTitle
            if (position == selectedPosition) {
                holder.itemView.setBackgroundColor(selectedColor)
            } else {
                holder.itemView.setBackgroundColor(unSelectedColor)
            }
        }

        override fun getItemCount(): Int = viewList.size


        class ItemHolder(convertView: View) : RecyclerView.ViewHolder(convertView) {
            val toolBarIcon: ImageView = convertView.findViewById(R.id.image_view_tool_icon)
            val toolBarTitle: TextView = convertView.findViewById(R.id.text_view_tool_title)
        }

    }


    private class CustomPagerAdapter(
            private val context: Context,
            var viewList: List<ToolBarItem>
    ) : PagerAdapter() {

        override fun instantiateItem(collection: ViewGroup, position: Int): Any {
            val toolbarItem = viewList[position]

            val viewToBeAdded: View? =
                    // Check if the view is created from template
                    if (toolbarItem.isFromTemplate) {
                        val template = toolbarItem.template as ToolbarPanelTemplate
                        // Check if the view is already created
                        if (template.doesViewExist()) {
                            template.widget
                        } else {
                            // Create view dynamically
                            template.createFromClassName(context)
                        }
                    } else {
                        // Widget is not added from template
                        toolbarItem.widgetView
                    }
            collection.addView(viewToBeAdded)
            return viewToBeAdded as View
        }

        override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
            collection.removeView(view as View)
        }

        override fun getCount(): Int = viewList.size

        override fun isViewFromObject(view: View, modelObject: Any): Boolean = view === modelObject

        override fun getPageTitle(position: Int): CharSequence? = viewList[position].toolBarTitle
    }

}

interface OnToolClickListener {
    fun onToolClick(position: Int)
}